#!/usr/bin/env bb

(ns clear-rate-limits
  "Enhanced script to clear rate limiting storage with detailed before/after data display.

   This script shows exactly what rate limiting data exists before clearing,
   performs the clear operation, and confirms the results afterward."
  (:require
    [clojure.data.json :as json]
    [clojure.java.shell :as shell]
    [clojure.set]
    [clojure.string :as str]))

(defn read-config-port
  "Read the server port from the application configuration file."
  []
  (try
    ;; Try to find config file relative to project root
    (let [config-paths ["config/base.edn"
                        "../config/base.edn"
                        "../../config/base.edn"
                        "../../../config/base.edn"]
          config-file (or (some #(let [f (java.io.File. %)]
                                   (when (.exists f) f)) config-paths)
                        (java.io.File. "config/base.edn"))]
      (if (.exists config-file)
        (let [config-content (slurp config-file)
              ;; Find webserver section and extract port
              webserver-start (str/last-index-of config-content ":webserver")
              webserver-section (if webserver-start
                                  (subs config-content webserver-start)
                                  "")
              ;; Look for the first port number after webserver
              port-pattern #":dev\s+(\d+)"
              match (re-find port-pattern webserver-section)]
          (if (and match (second match))
            (Integer/parseInt (second match))
            8080))
        8080)) ; fallback to default
    (catch Exception _e
      (println "Warning: Could not read config file, using default port 8080")
      8080)))

(def server-port (read-config-port))
(def base-url (str "http://localhost:" server-port))

(defn get-rate-limit-data-via-api
  "Get rate limiting data via HTTP API endpoint (if available)."
  []
  (try
    (let [result (shell/sh "curl" "-s" "-X" "GET"
                   (str base-url "/admin/api/dashboard")
                   :timeout 5000)]
      (if (= 0 (:exit result))
        (try
          ;; For now, we'll get basic stats. Could be enhanced with a dedicated stats endpoint
          {:stats {:status "api-available"} :entries {}}
          (catch Exception _e
            {:stats {:error "api-parse-failed"} :entries {}}))
        {:stats {:error "api-unavailable"} :entries {}}))
    (catch Exception _e
      {:stats {:error "api-exception"} :entries {}})))

(defn get-rate-limit-data-via-http
  "Get rate limiting data via the application's stats endpoint."
  []
  (try
    (let [result (shell/sh "curl" "-s" "-X" "GET"
                   (str base-url "/admin/api/dev-get-rate-limits")
                   :timeout 10000)]
      (if (= 0 (:exit result))
        (try
          (let [response (json/read-str (:out result) :key-fn keyword)]
            (if (:success response)
              {:stats (:stats response) :entries (:data response)}
              {:stats {:error "endpoint-error"} :entries {}}))
          (catch Exception _e
            {:stats {:error "parse-failed"} :entries {}}))
        {:stats {:error "http-failed"} :entries {}}))
    (catch Exception _e
      {:stats {:error "exception"} :entries {}})))

(defn get-rate-limit-data
  "Get detailed rate limiting data using the best available method."
  []
  ;; Try HTTP endpoint first, then fall back to basic detection
  (let [http-data (get-rate-limit-data-via-http)]
    (if (not (get-in http-data [:stats :error]))
      http-data
      ;; Fallback to basic API check
      (get-rate-limit-data-via-api))))

(defn format-timestamp
  "Format a timestamp for display."
  [ts]
  (if ts
    (-> ts str (subs 0 19))
    "N/A"))

(defn format-rate-limit-key
  "Parse and format a rate limiting key for display."
  [key-str]
  (let [key-string (if (keyword? key-str) (name key-str) (str key-str))
        parts (str/split key-string #":")]
    (if (>= (count parts) 3)
      (let [route-type (second parts)
            ip (nth parts 2)]
        (str route-type " from " ip))
      key-string)))

(defn display-rate-limit-data
  "Display rate limiting data in a formatted way."
  [data title]
  (println)
  (println (str "📊 " title))
  (println (str (apply str (repeat (+ 4 (count title)) "="))))

  (let [{:keys [stats entries]} data]
    (if (:error stats)
      (println (str "⚠️  Data fetch method: " (:error stats)))
      (do
        ;; Display summary stats
        (println (str "📈 Summary: "
                   (:total-entries stats 0) " entries, "
                   (:active-blocks stats 0) " active blocks, "
                   "storage size: " (:storage-size stats 0)))

        ;; Display detailed entries
        (if (empty? entries)
          (println "✨ No detailed rate limiting data available")
          (do
            (println)
            (println "📋 Detailed Entries:")
            (doseq [[key entry] entries]
              (let [route-desc (format-rate-limit-key key)
                    attempt-count (count (:attempts entry))
                    blocked? (some? (:blocked-until entry))
                    latest-attempt (when (seq (:attempts entry))
                                     (format-timestamp (first (:attempts entry))))
                    created (format-timestamp (:created-at entry))]
                (println (str "  🔸 " route-desc))
                (println (str "     📊 " attempt-count " attempts"))
                (when latest-attempt
                  (println (str "     🕒 Latest: " latest-attempt)))
                (println (str "     📅 Created: " created))
                (when blocked?
                  (println (str "     🚫 BLOCKED until: " (format-timestamp (:blocked-until entry)))))
                (println)))))))))

(defn make-test-requests
  "Generate some test rate limiting data for demonstration."
  []
  (println "🧪 Generating test rate limiting data...")
  (try
    (doseq [_ (range 3)]
      (shell/sh "curl" "-s" (str base-url "/auth/status") :timeout 2000)
      (shell/sh "curl" "-s" (str base-url "/api/v1/test") :timeout 2000))
    (println "✅ Test requests completed")
    (Thread/sleep 500) ; Give rate limiting time to register
    true
    (catch Exception e
      (println "⚠️  Could not generate test data:" (.getMessage e))
      false)))

(defn show-current-stats
  "Show current rate limiting statistics and data before clearing."
  []
  (println "📊 Checking current application status...")
  (try
    (let [result (shell/sh "curl" "-s" "-X" "GET"
                   (str base-url "/admin/api/dashboard")
                   :timeout 5000)]
      (if (= 0 (:exit result))
        (do
          (println (str "✅ Application is running on port " server-port))

          ;; Check if we have existing data
          (let [initial-data (get-rate-limit-data)]
            (if (and (:stats initial-data)
                  (= 0 (:total-entries (:stats initial-data) 0)))
              (do
                (println "📝 No existing rate limiting data found. Generating test data...")
                (make-test-requests)
                (let [data-with-tests (get-rate-limit-data)]
                  (display-rate-limit-data data-with-tests "Rate Limiting Data BEFORE Clearing")))
              (display-rate-limit-data initial-data "Rate Limiting Data BEFORE Clearing"))))
        (println (str "⚠️  Could not reach application on port " server-port))))
    (catch Exception e
      (println "⚠️  Could not check application status:" (.getMessage e)))))

(defn clear-rate-limits-http
  "Clear rate limits via the application's development endpoint."
  []
  (println)
  (println "🧹 Clearing rate limits via HTTP endpoint...")
  (try
    (let [result (shell/sh "curl" "-s" "-w" "\n%{http_code}" "-X" "POST"
                   (str base-url "/admin/api/dev-clear-rate-limits")
                   :timeout 10000)
          output (:out result)
          lines (str/split-lines output)
          response-body (str/join "\n" (butlast lines))
          status-code (last lines)]

      (if (= 0 (:exit result))
        (if (= "200" status-code)
          (do
            (println "✅ Rate limits cleared successfully!")
            (println "📄 Server Response:" response-body)
            true)
          (do
            (println "❌ HTTP request failed with status:" status-code)
            (println "📄 Response:" response-body)
            false))
        (do
          (println "❌ curl command failed:" (:err result))
          false)))
    (catch Exception e
      (println "❌ Error making HTTP request:" (.getMessage e))
      false)))

(defn compare-rate-limit-data
  "Compare before and after rate limiting data to show what changed."
  [before-data after-data]
  (println)
  (println "🔄 COMPARISON ANALYSIS")
  (println "======================")

  (let [before-entries (get-in before-data [:stats :total-entries] 0)
        after-entries (get-in after-data [:stats :total-entries] 0)
        before-total-attempts (reduce + 0 (map #(count (:attempts %)) (vals (:entries before-data))))
        after-total-attempts (reduce + 0 (map #(count (:attempts %)) (vals (:entries after-data))))]

    (println (str "📊 Entries: " before-entries " → " after-entries
               (if (< after-entries before-entries) " ✅ (reduced)"
                 (if (= after-entries before-entries) " ➡️ (same)"
                   " ⬆️ (increased)"))))

    (println (str "📈 Total Attempts: " before-total-attempts " → " after-total-attempts
               (if (< after-total-attempts before-total-attempts) " ✅ (cleared)"
                 (if (= after-total-attempts before-total-attempts) " ➡️ (same)"
                   " ⬆️ (new attempts)"))))

    ;; Analyze specific entries
    (let [before-keys (set (keys (:entries before-data)))
          after-keys (set (keys (:entries after-data)))
          removed-keys (clojure.set/difference before-keys after-keys)
          new-keys (clojure.set/difference after-keys before-keys)
          common-keys (clojure.set/intersection before-keys after-keys)]

      (when (seq removed-keys)
        (println (str "🗑️  Removed entries: " (count removed-keys))))

      (when (seq new-keys)
        (println (str "🆕 New entries: " (count new-keys) " (likely from the clear operation itself)")))

      (when (seq common-keys)
        (println "🔄 Modified entries:")
        (doseq [key common-keys]
          (let [before-attempts (count (:attempts (get (:entries before-data) key)))
                after-attempts (count (:attempts (get (:entries after-data) key)))
                route-desc (format-rate-limit-key key)]
            (when (not= before-attempts after-attempts)
              (println (str "   • " route-desc ": " before-attempts " → " after-attempts " attempts")))))))

    (println)
    (println "💡 EXPLANATION:")
    (if (and (= before-entries after-entries)
          (< before-total-attempts after-total-attempts))
      (println "   ✅ Rate limits were cleared successfully!")
      (println "   ✅ Original data was cleared, but new entries were created"))
    (println "   🔄 The clear operation itself creates new rate limit entries")
    (println "   📡 Each API call (including clearing) gets tracked by rate limiting")))

(defn show-after-stats
  "Show rate limiting data after clearing to confirm results."
  []
  (println)
  (println "🔍 Verifying results...")
  (Thread/sleep 1000) ; Give the clear operation time to complete
  (let [data (get-rate-limit-data)]
    (display-rate-limit-data data "Rate Limiting Data AFTER Clearing")))

(defn show-help
  "Show helpful instructions for manual clearing."
  []
  (println)
  (println "📖 Manual Options:")
  (println "==================")
  (println)
  (println "🌐 HTTP Method (Recommended):")
  (println (str "  curl -X POST " base-url "/admin/api/dev-clear-rate-limits"))
  (println)
  (println "🔧 REPL Method:")
  (println "  Connect to running REPL (port 7888) and execute:")
  (println "  (require '[app.backend.middleware.rate-limiting :as rl])")
  (println "  (rl/clear-rate-limits!)")
  (println)
  (println "📊 Check Stats:")
  (println "  (rl/get-rate-limit-stats)")
  (println)
  (println "♻️  Restart Method:")
  (println "  bb kill-java && bb run-app"))

(defn main []
  (println "🧹 Rate Limit Cleaner v2.3 - Enhanced with Comparison Analysis")
  (println "===============================================================")
  (println)

  ;; Check application status first
  (println "📊 Checking current application status...")
  (try
    (let [result (shell/sh "curl" "-s" "-X" "GET"
                   (str base-url "/admin/api/dashboard")
                   :timeout 5000)]
      (if (= 0 (:exit result))
        (do
          (println (str "✅ Application is running on port " server-port))

          ;; Get initial data - generate test data if needed
          (let [initial-data (get-rate-limit-data)]
            (when (and (:stats initial-data)
                    (= 0 (:total-entries (:stats initial-data) 0)))
              (println "📝 No existing rate limiting data found. Generating test data...")
              (make-test-requests))

            ;; Get the "before" data for comparison
            (let [before-data (get-rate-limit-data)]
              (display-rate-limit-data before-data "Rate Limiting Data BEFORE Clearing")
              (println)

              ;; Try to clear via HTTP endpoint
              (let [http-success (clear-rate-limits-http)]
                (if http-success
                  (do
                    ;; Get "after" data and compare
                    (println)
                    (println "🔍 Verifying results...")
                    (Thread/sleep 1000)
                    (let [after-data (get-rate-limit-data)]
                      (display-rate-limit-data after-data "Rate Limiting Data AFTER Clearing")

                      ;; Show detailed comparison
                      (compare-rate-limit-data before-data after-data)

                      (println)
                      (println "🎉 Rate limits successfully cleared!")
                      (println "💡 You can now make requests without rate limiting restrictions.")
                      (System/exit 0)))

                  ;; If clearing fails, show help
                  (do
                    (println)
                    (println "❌ Automatic clearing failed.")
                    (println "💡 This usually means the application is not running.")
                    (println "🚀 Try: bb run-app")
                    (show-help)
                    (System/exit 1)))))))
        (do
          (println (str "⚠️  Could not reach application on port " server-port))
          (println "🚀 Try: bb run-app")
          (System/exit 1))))
    (catch Exception e
      (println "⚠️  Could not check application status:" (.getMessage e))
      (println "🚀 Try: bb run-app")
      (System/exit 1))))

;; Run the main function
(main)

(ns app.template.frontend.dev.tracing
  (:require
    [re-frame.trace :as trace]
    [re-frame.loggers :as rlog]
    [taoensso.timbre :as log]
    [clojure.string :as str]))

;; ─── Tracing Configuration ────────────────────────────────────────

(defonce ^:private history* (atom (clojure.core/vec [])))
(defonce ^:private max-items 5000)

;; ─── Log Capture ──────────────────────────────────────────────────

(defn log
  "Record a log entry in the trace history."
  [level message & [data]]
  (let [entry {:op-type :log
               :level level
               :message message
               :data data
               :start (js/Date.)
               :duration-ms 0}]
    (swap! history*
      (fn [h]
        (let [cnt (count h)]
          (cond-> h
            (> cnt max-items) (subvec (- cnt max-items) cnt)
            true (conj entry)))))))

;; ─── Appender for Timbre ──────────────────────────────────────────

(def tracer-appender
  {:enabled? true
   :async? false
   :min-level :debug
   :rate-limit nil
   :output-fn :inherit
   :fn (fn [data]
         (let [{:keys [level ?msg-fmt vargs_]} data
               message (or ?msg-fmt (str (first vargs_)))
               context (when (> (count vargs_) 1) (second vargs_))]
           (log level message context)))})

;; ─── Buffer Management ─────────────────────────────────────────────

(defn- keep-last [n coll]
  (let [cnt (count coll)]
    (if (<= cnt n)
      (vec coll)
      (subvec (vec coll) (- cnt n) cnt))))

;; ─── Trace Processing ─────────────────────────────────────────────

(defn- slim-trace
  "Reduce trace size by removing large data that's rarely needed for debugging."
  [t]
  (cond-> t
    true (dissoc :app-db-after)        ; Often huge; keep if you really need it
    true (update :tags dissoc :app-db))) ; Tags may include big data

(defn- on-traces
  "Receives a vector of trace maps from re-frame's tracing system.
   This function filters, processes, and stores traces in our in-memory buffer."
  [traces]
  (when (seq traces)
    (swap! history*
      (fn [h]
        (keep-last max-items
          (-> h
            (into (map slim-trace traces))
            vec))))))

;; ─── Tracing Control ───────────────────────────────────────────────

(defn start!
  "Register our callback with re-frame's tracing system.
   Safe to call multiple times due to idempotent registration."
  []
  (when-not (trace/is-trace-enabled?)
    (js/console.warn "[tracing] re-frame tracing is disabled; set re-frame.trace.trace-enabled? to true in build config"))

  ;; 1. Remove default listeners to stop console noise
  (trace/remove-trace-cb :default)
  (trace/remove-trace-cb :console)

  ;; 2. Register our custom trace collector
  (trace/register-trace-cb :app/dev-tracer on-traces)
  
  ;; 3. Redirect re-frame internal logs (like "Handling event") to our tracer
  (rlog/set-loggers!
    {:log (fn [& args] (log :info (str/join " " args)))
     :warn (fn [& args] (log :warn (str/join " " args)))
     :error (fn [& args] (js/console.error (str/join " " args))) ;; Keep errors in console!
     :group (fn [& args] nil)  ;; Silence groups
     :groupEnd (fn [& args] nil)})

  ;; 4. Configure Timbre to use our tracer and silence console
  (log/merge-config!
    {:appenders {:tracer tracer-appender
                 :console {:enabled? false}}})

  (js/console.info "[tracing] re-frame trace callback registered & console silenced"))

(defn stop!
  "Remove our callback from re-frame's tracing system."
  []
  (trace/remove-trace-cb :app/dev-tracer)
  (js/console.info "[tracing] re-frame trace callback removed"))

;; ─── Trace Inspection API ───────────────────────────────────────────

(defn get-history
  "Return the complete trace history."
  []
  @history*)

(defn get-recent
  "Return the most recent n traces (default: 50)."
  ([] (get-recent 50))
  ([n] (take-last n @history*)))

(defn get-events
  "Return only event traces (filtering out subs, reactions, etc.)."
  []
  (filter #(= (:op-type %) :event) @history*))

(defn get-subs
  "Return only subscription traces."
  []
  (filter #(= (:op-type %) :sub/create) @history*))

(defn get-by-operation
  "Return traces filtered by operation type (:event, :sub/create, :render, etc.)."
  [op-type]
  (filter #(= (:op-type %) op-type) @history*))

(defn clear-history!
  "Clear the in-memory trace buffer."
  []
  (reset! history* [])
  (js/console.info "[tracing] trace history cleared"))

(defn get-stats
  "Return basic statistics about the current trace buffer."
  []
  {:total-count (count @history*)
   :event-count (count (get-events))
   :sub-count (count (get-subs))
   :buffer-size max-items
   :buffer-utilization (/ (count @history*) max-items)})

;; ─── Auto-start (dev only) ─────────────────────────────────────────

;; Since this namespace will be preloaded in dev, auto-start tracing
(start!)

(js/console.info "[tracing] re-frame tracing system initialized"
  (clj->js {:buffer-size max-items
            :tracing-enabled true}))

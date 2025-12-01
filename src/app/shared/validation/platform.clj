(ns app.shared.validation.platform
  "Platform-specific validation utilities for Clojure (JVM)."
  (:require
    [app.shared.patterns :as patterns]
    [clojure.string :as str]
    [malli.core :as m]
    [malli.error :as me]))

;; ========================================
;; JVM-specific validation functions
;; ========================================

(defn validate-file-path
  "Validate file path using Java File API."
  [path]
  (when path
    (try
      (let [file (java.io.File. path)]
        (.exists file))
      (catch Exception _
        false))))

(defn validate-uuid-format
  "Validate UUID format using Java UUID parsing."
  [uuid-str]
  (when uuid-str
    (try
      (java.util.UUID/fromString uuid-str)
      true
      (catch IllegalArgumentException _
        false))))

;; Function removed - use app.shared.patterns/valid-email? directly

(defn validate-phone-number
  "Validate phone number format (JVM implementation)."
  [phone]
  (when phone
    (let [cleaned (str/replace phone #"[^\d]" "")]
      (and (>= (count cleaned) 10)
        (<= (count cleaned) 15)))))

(defn validate-url-format
  "Validate URL format using Java URL parsing."
  [url]
  (when url
    (try
      (java.net.URL. url)
      true
      (catch Exception _
        false))))

;; ========================================
;; Database-specific validation
;; ========================================

(defn validate-connection-string
  "Validate database connection string format."
  [conn-str]
  (when conn-str
    (and (string? conn-str)
      (str/starts-with? conn-str "jdbc:")
      (str/includes? conn-str "postgresql"))))

(defn validate-sql-identifier
  "Validate SQL identifier (table/column names)."
  [identifier]
  (when identifier
    (let [pattern #"^[a-zA-Z_][a-zA-Z0-9_]*$"]
      (boolean (re-matches pattern identifier)))))

;; ========================================
;; Performance utilities
;; ========================================

(defn validate-with-timeout
  "Validate with timeout (JVM implementation using futures)."
  [validator value timeout-ms]
  (try
    (let [future-result (future (validator value))
          result (deref future-result timeout-ms ::timeout)]
      (if (= result ::timeout)
        {:valid? false :error "Validation timeout"}
        {:valid? result}))
    (catch Exception e
      {:valid? false :error (str "Validation error: " (.getMessage e))})))

;; ========================================
;; Malli integration
;; ========================================

(defn create-platform-schema
  "Create platform-specific Malli schema."
  []
  [:map
   [:file-path {:optional true} [:fn validate-file-path]]
   [:uuid {:optional true} [:fn validate-uuid-format]]
   [:email {:optional true} [:fn patterns/valid-email?]]
   [:phone {:optional true} [:fn validate-phone-number]]
   [:url {:optional true} [:fn validate-url-format]]])

(defn validate-with-platform-schema
  "Validate data using platform-specific schema."
  [data]
  (let [schema (create-platform-schema)]
    (if (m/validate schema data)
      {:valid? true :data data}
      {:valid? false :errors (me/humanize (m/explain schema data))})))

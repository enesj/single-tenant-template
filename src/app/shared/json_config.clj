(ns app.shared.json-config
  "Centralized JSON encoding configuration for the application.
   Sets up Cheshire encoders for Java time types and other problematic types."
  (:require
    [cheshire.generate :refer [add-encoder]]
    [clojure.string :as str]
    [taoensso.timbre :as log])
  (:import
    (java.math BigDecimal BigInteger)
    (java.sql Date Time Timestamp)
    (java.time
      Instant
      LocalDate
      LocalDateTime
      LocalTime
      OffsetDateTime
      ZonedDateTime)
    (java.util UUID)
    (org.postgresql.jdbc PgArray)))

;; ============================================================================
;; JSON Encoders for Java Types
;; ============================================================================

(defn configure-json-encoders!
  "Configure Cheshire JSON encoders for all Java types that need custom serialization.
   This should be called once during application startup."
  []
  (log/debug "Configuring JSON encoders for Java types...")

  ;; Java Time API types
  (add-encoder LocalDate
    (fn [date json-generator]
      (.writeString json-generator (.toString date))))

  (add-encoder LocalDateTime
    (fn [datetime json-generator]
      (.writeString json-generator (.toString datetime))))

  (add-encoder LocalTime
    (fn [time json-generator]
      (.writeString json-generator (.toString time))))

  (add-encoder ZonedDateTime
    (fn [datetime json-generator]
      (.writeString json-generator (.toString datetime))))

  (add-encoder OffsetDateTime
    (fn [datetime json-generator]
      (.writeString json-generator (.toString datetime))))

  (add-encoder Instant
    (fn [instant json-generator]
      (.writeString json-generator (.toString instant))))

  ;; SQL types
  (add-encoder Timestamp
    (fn [timestamp json-generator]
      (.writeString json-generator (.toString timestamp))))

  (add-encoder Date
    (fn [date json-generator]
      (.writeString json-generator (.toString date))))

  (add-encoder Time
    (fn [time json-generator]
      (.writeString json-generator (.toString time))))

  ;; UUID type
  (add-encoder UUID
    (fn [uuid json-generator]
      (.writeString json-generator (.toString uuid))))

  ;; Numeric types that might cause issues
  (add-encoder BigDecimal
    (fn [decimal json-generator]
      (.writeNumber json-generator (.doubleValue decimal))))

  (add-encoder BigInteger
    (fn [integer json-generator]
      (.writeNumber json-generator (.longValue integer))))

  ;; PostgreSQL array types
  (add-encoder PgArray
    (fn [pg-array json-generator]
      (try
        ;; Get the array data as a string first, then parse it
        (let [array-str (.toString pg-array)
              ;; Remove the curly braces and split by comma
              cleaned-str (-> array-str
                            (subs 1 (dec (count array-str)))  ; Remove { and }
                            (str/split #","))
              ;; Clean up each element
              array-data (mapv str/trim cleaned-str)]
          (.writeObject json-generator array-data))
        (catch Exception e
          (log/warn "Failed to serialize PgArray, using empty array:" (.getMessage e))
          (.writeObject json-generator [])))))

  (log/info "JSON encoders configured successfully"))

;; ============================================================================
;; Initialization
;; ============================================================================

(defn init!
  "Initialize JSON configuration. Safe to call multiple times."
  []
  (configure-json-encoders!))

;; Auto-configure on namespace load (for development convenience)
(when-not (System/getProperty "app.skip-json-config")
  (init!))

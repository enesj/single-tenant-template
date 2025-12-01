(ns app.shared.data
  (:require
    [clojure.walk :as walk]))

(defn get-server-port
  "Get the server port from service container config.
   Returns the configured port or default 8080 for development."
  [service-container]
  (let [config (:config service-container)]
    (or (get-in config [:webserver :port]) 8080)))

(defn sanitize-for-serialization
  "Recursively sanitizes an object for JSON/EDN serialization by converting
   problematic Java types (like UUIDs, dates, BigDecimals) to strings.
   This ensures data can be safely stored in session cookies or sent as JSON."
  [obj]
  (walk/postwalk
    (fn [x]
      #?(:clj (cond
                ;; Handle all UUID types
                (instance? java.util.UUID x) (str x)
                ;; Handle all time/date types
                (instance? java.time.LocalDateTime x) (str x)
                (instance? java.time.ZonedDateTime x) (str x)
                (instance? java.time.OffsetDateTime x) (str x)
                (instance? java.time.Instant x) (str x)
                (instance? java.time.LocalDate x) (str x)
                (instance? java.time.LocalTime x) (str x)
                ;; Handle SQL types
                (instance? java.sql.Timestamp x) (str x)
                (instance? java.sql.Date x) (str x)
                (instance? java.sql.Time x) (str x)
                ;; Handle numeric types that might not serialize
                (instance? java.math.BigDecimal x) (str x)
                (instance? java.math.BigInteger x) (str x)
                ;; Handle any other potentially problematic Java objects
                (and (not (nil? x))
                  (not (string? x))
                  (not (number? x))
                  (not (boolean? x))
                  (not (keyword? x))
                  (not (symbol? x))
                  (not (coll? x))
                  (.startsWith (.getName (class x)) "java.")) (str x)
                :else x)
         :cljs (cond
                   ;; Handle Date objects in ClojureScript
                 (instance? js/Date x) (.toISOString x)
                   ;; Handle UUID strings that are already string-like
                 (uuid? x) (str x)
                 :else x)))
    obj))

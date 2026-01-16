(ns app.shared.date
  "Date/time helpers; keep parsing/formatting consistent."
  (:require
    [app.shared.date-core :as core]
    [app.shared.date-arithmetic :as arithmetic]
    [app.shared.date-range :as range]
    #?(:clj [java-time.api :as time])
    #?(:clj [taoensso.timbre :as log])))

;; Re-export core functions
(def now core/now)
(def today core/today)
(def valid-date? core/valid-date?)
(def ensure-date core/ensure-date)
(def parse-iso-date core/parse-iso-date)
(def parse-date-string core/parse-date-string)
(def format-iso-date core/format-iso-date)
(def format-display-date core/format-display-date)

;; Re-export arithmetic functions
(def add-days arithmetic/add-days)
(def days-between arithmetic/days-between)
(def start-of-month arithmetic/start-of-month)
(def end-of-month arithmetic/end-of-month)

;; Re-export range functions
(def date-in-range? range/date-in-range?)
(def normalize-date-range range/normalize-date-range)
(def date-range range/date-range)
(def format-date-range range/format-date-range)

;; Aliases for compatibility
(def format-date
  "Format a date to ISO string (YYYY-MM-DD).
   Accepts date objects or date strings."
  (comp core/format-iso-date core/ensure-date))

(def format-date-display core/format-display-date)
(def parse-date core/parse-date-string)

(def is-valid-date?
  "Check if a value represents a valid date.
   Accepts date objects or parseable date strings."
  (fn [value]
    (let [date-obj (core/ensure-date value)]
      (core/valid-date? date-obj))))

(defn format-date-for-api
  "Format a date value for API submission.
   Handles various date objects and returns YYYY-MM-DD format or nil if not a valid date."
  [date]
  (when date
    (let [date-obj (core/ensure-date date)]
      (core/format-iso-date date-obj))))

;; Backend-specific utilities
#?(:clj
   (defn session-expired?
     "Check if session has expired - handles both LocalDateTime objects and string timestamps"
     [session]
     (let [expires-at (:expires-at session)]
       (if expires-at
         (let [expires-time (cond
                              (instance? java.time.LocalDateTime expires-at)
                              expires-at

                              (string? expires-at)
                              (try
                                (time/local-date-time expires-at)
                                (catch Exception e
                                  (log/warn "Failed to parse expires-at timestamp:" expires-at "error:" (.getMessage e))
                                  nil))

                              :else
                              (do
                                (log/warn "Unknown expires-at format:" (type expires-at) expires-at)
                                nil))]
           (if expires-time
             (do
               (log/debug "Session expiration check - current:" (time/local-date-time) "expires:" expires-time "expired?" (time/after? (time/local-date-time) expires-time))
               (time/after? (time/local-date-time) expires-time))
             true))
         true))))

;; ClojureScript-specific utilities
#?(:cljs
   (defn process-highlighted-dates
     "Process highlighted dates into a format React DayPicker can use."
     [dates]
     (when dates
       (cond
         (coll? dates)
         (into-array
           (keep (fn [d]
                   (cond
                     (instance? js/Date d) d
                     (string? d) (core/parse-date-string d)
                     :else nil))
             dates))

         (instance? js/Date dates)
         dates

         (string? dates)
         (core/parse-date-string dates)

         :else
         dates))))

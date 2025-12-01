(ns app.shared.date
  "Cross-platform date utilities for the hosting application.
   Provides consistent date formatting, parsing, and manipulation functions
   that work in both Clojure and ClojureScript environments."
  (:require
    #?(:clj
       [java-time :as time]
       :cljs [clojure.string :as str])
    #?(:clj [taoensso.timbre :as log]
       :cljs [taoensso.timbre :as log])))

;; -------------------------
;; Platform-specific imports and helpers
;; -------------------------

#?(:clj
   (import (java.time LocalDate LocalDateTime ZonedDateTime)))

;; -------------------------
;; Helper Functions
;; -------------------------

(defn pad-zero
  "Pad a number with leading zero if less than 10"
  [n]
  (if (< n 10) (str "0" n) (str n)))

;; -------------------------
;; Current Time Functions
;; -------------------------

(defn now
  "Get current date/time in the appropriate platform format"
  []
  #?(:clj (time/local-date-time)
     :cljs (js/Date.)))

(defn today
  "Get today's date at midnight in local timezone"
  []
  #?(:clj (time/local-date)
     :cljs (let [now (js/Date.)]
             (js/Date. (.getFullYear now)
               (.getMonth now)
               (.getDate now)
               0 0 0 0))))

;; -------------------------
;; Date Formatting
;; -------------------------

(defn format-iso-date
  "Convert a date to ISO date string format (YYYY-MM-DD).
   Returns nil if input is not a valid date."
  [date]
  #?(:clj
     (when date
       (cond
         (instance? LocalDate date)
         (.toString date)

         (instance? LocalDateTime date)
         (.toString (.toLocalDate date))

         (instance? ZonedDateTime date)
         (.toString (.toLocalDate date))

         (string? date)
         date  ; Assume already formatted

         :else nil))

     :cljs
     (when (and date (instance? js/Date date) (not (js/isNaN (.getTime date))))
       (let [year (.getFullYear date)
             month (inc (.getMonth date))  ; getMonth is 0-indexed
             day (.getDate date)]
         (str year "-" (pad-zero month) "-" (pad-zero day))))))

(defn format-display-date
  "Format a date for user-friendly display using locale settings.
   Returns localized date string or fallback message."
  [date & {:keys [fallback] :or {fallback "Select a date"}}]
  #?(:clj
     (if date
       (try
         (cond
           (instance? LocalDate date)
           (.toString date)  ; Could be enhanced with locale formatting

           (instance? LocalDateTime date)
           (.toString (.toLocalDate date))

           (string? date)
           date

           :else fallback)
         (catch Exception e
           (log/warn "Failed to format date for display:" e)
           fallback))
       fallback)

     :cljs
     (if (and date (instance? js/Date date) (not (js/isNaN (.getTime date))))
       (try
         (.toLocaleDateString date)
         (catch :default e
           (log/warn "Failed to format date for display:" e)
           fallback))
       fallback)))

(defn format-date-range
  "Format a date range for display.
   Can be called in two ways:
   1. With a map/object: (format-date-range {:from date1 :to date2})
   2. With two dates: (format-date-range date1 date2)"
  ([date-range-or-from]
   (format-date-range date-range-or-from nil :separator " - " :fallback "Select date range"))
  ([date-range-or-from to]
   (format-date-range date-range-or-from to :separator " - " :fallback "Select date range"))
  ([date-range-or-from to & {:keys [separator fallback]
                             :or {separator " - "
                                  fallback "Select date range"}}]
   (let [;; Extract from and to values based on calling pattern
         [from-val to-val]
         (cond
           ;; Called with nil arguments
           (and (nil? date-range-or-from) (nil? to))
           [nil nil]

           ;; Called with a map
           (and (map? date-range-or-from) (nil? to))
           [(:from date-range-or-from) (:to date-range-or-from)]

           #?@(:cljs
               [;; Called with a JS object (ClojureScript only)
                (and (object? date-range-or-from)
                  (not (instance? js/Date date-range-or-from))
                  (nil? to))
                [(.-from ^js date-range-or-from) (.-to ^js date-range-or-from)]])

           ;; Called with two date arguments
           :else
           [date-range-or-from to])]
     (cond
       ;; Range with from and to dates
       (and from-val to-val)
       (str (format-display-date from-val :fallback "?")
         separator
         (format-display-date to-val :fallback "?"))

       ;; Range with only from date
       from-val
       (format-display-date from-val)

       ;; No valid range
       :else
       fallback))))

;; -------------------------
;; Date Parsing
;; -------------------------

(defn parse-iso-date
  "Parse an ISO date string (YYYY-MM-DD) into platform-appropriate date object.
   Returns nil if parsing fails."
  [date-str]
  (when (and date-str (string? date-str))
    (try
      #?(:clj
         (when (re-matches #"^\d{4}-\d{2}-\d{2}$" date-str)
           (time/local-date date-str))

         :cljs
         (when (re-matches #"^\d{4}-\d{2}-\d{2}$" date-str)
           (let [[year month day] (str/split date-str #"-")
                 year-int (js/parseInt year)
                 month-int (js/parseInt month)
                 day-int (js/parseInt day)]
             ;; Validate date components before creating Date object
             (when (and (>= year-int 1000) (<= year-int 9999)
                     (>= month-int 1) (<= month-int 12)
                     (>= day-int 1) (<= day-int 31))
               (let [date (js/Date. year-int (dec month-int) day-int)]
                 ;; Verify the date wasn't auto-corrected by JS
                 (when (and (= (.getFullYear date) year-int)
                         (= (.getMonth date) (dec month-int))
                         (= (.getDate date) day-int))
                   date))))))
      (catch #?(:clj Exception :cljs :default) e
        (log/warn "Failed to parse ISO date string:" date-str e)
        nil))))

(defn parse-date-string
  "Parse a date string into platform-appropriate date object.
   Supports multiple formats:
   - ISO format (YYYY-MM-DD)
   - US format (MM/DD/YYYY) - ClojureScript only
   Returns nil if parsing fails."
  [date-str]
  (when (and date-str (string? date-str))
    (try
      #?(:clj
         (cond
           ;; ISO format YYYY-MM-DD
           (re-matches #"^\d{4}-\d{2}-\d{2}$" date-str)
           (time/local-date date-str)

           ;; ISO datetime format
           (re-matches #"^\d{4}-\d{2}-\d{2}T.*" date-str)
           (time/local-date-time date-str)

           :else nil)

         :cljs
         (let [result
               (cond
                 ;; ISO format YYYY-MM-DD
                 (re-matches #"^\d{4}-\d{2}-\d{2}$" date-str)
                 (parse-iso-date date-str)

                 ;; US format MM/DD/YYYY
                 (re-matches #"^\d{1,2}/\d{1,2}/\d{4}$" date-str)
                 (let [[month day year] (str/split date-str #"/")
                       year-int (js/parseInt year)
                       month-int (js/parseInt month)
                       day-int (js/parseInt day)]
                   ;; Validate date components before creating Date object
                   (when (and (>= year-int 1000) (<= year-int 9999)
                           (>= month-int 1) (<= month-int 12)
                           (>= day-int 1) (<= day-int 31))
                     (let [date (js/Date. year-int (dec month-int) day-int)]
                       ;; Verify the date wasn't auto-corrected by JS
                       (when (and (= (.getFullYear date) year-int)
                               (= (.getMonth date) (dec month-int))
                               (= (.getDate date) day-int))
                         date))))

                 ;; Try native JS Date parsing as fallback
                 :else
                 (js/Date. date-str))]
           ;; Validate that the date is actually valid
           (when (and result
                   (instance? js/Date result)
                   (not (js/isNaN (.getTime result))))
             result)))
      (catch #?(:clj Exception :cljs :default) e
        (log/warn "Failed to parse date string:" date-str e)
        nil))))

(defn ensure-date
  "Ensure value is a platform-appropriate date object.
   - If already a date, returns it
   - If a string, attempts to parse it
   - Otherwise returns nil"
  [value]
  #?(:clj
     (cond
       (instance? LocalDate value) value
       (instance? LocalDateTime value) (.toLocalDate value)
       (string? value) (parse-date-string value)
       :else nil)

     :cljs
     (cond
       (instance? js/Date value) value
       (string? value) (parse-date-string value)
       :else nil)))

;; -------------------------
;; Date Validation
;; -------------------------

(defn valid-date?
  "Check if a value is a valid date object"
  [value]
  #?(:clj
     (or (instance? LocalDate value)
       (instance? LocalDateTime value)
       (instance? ZonedDateTime value))

     :cljs
     (and (instance? js/Date value)
       (not (js/isNaN (.getTime value))))))

;; -------------------------
;; Date Manipulation
;; -------------------------

(defn add-days
  "Add (or subtract with negative n) days to a date"
  [date n]
  (when (valid-date? date)
    #?(:clj
       (cond
         (instance? LocalDate date)
         (.plusDays date n)

         (instance? LocalDateTime date)
         (.plusDays date n)

         :else nil)

       :cljs
       (let [new-date (js/Date. (.getTime date))]
         (.setDate new-date (+ (.getDate new-date) n))
         new-date))))

(defn days-between
  "Calculate the number of days between two dates"
  [date1 date2]
  (when (and (valid-date? date1) (valid-date? date2))
    #?(:clj
       (let [d1 (if (instance? LocalDate date1) date1 (.toLocalDate date1))
             d2 (if (instance? LocalDate date2) date2 (.toLocalDate date2))]
         (.until d1 d2 java.time.temporal.ChronoUnit/DAYS))

       :cljs
       (let [ms-per-day (* 24 60 60 1000)
             diff (- (.getTime date2) (.getTime date1))]
         (js/Math.abs (js/Math.floor (/ diff ms-per-day)))))))

(defn start-of-month
  "Get the first day of the month for a given date"
  [date]
  (when (valid-date? date)
    #?(:clj
       (cond
         (instance? LocalDate date)
         (.withDayOfMonth date 1)

         (instance? LocalDateTime date)
         (.withDayOfMonth date 1)

         :else nil)

       :cljs
       (js/Date. (.getFullYear date)
         (.getMonth date)
         1))))

(defn end-of-month
  "Get the last day of the month for a given date"
  [date]
  (when (valid-date? date)
    #?(:clj
       (cond
         (instance? LocalDate date)
         (.withDayOfMonth date (.lengthOfMonth date))

         (instance? LocalDateTime date)
         (.withDayOfMonth date (.lengthOfMonth (.toLocalDate date)))

         :else nil)

       :cljs
       (js/Date. (.getFullYear date)
         (inc (.getMonth date))
         0))))

;; -------------------------
;; Date Range Utilities
;; -------------------------

(defn date-in-range?
  "Check if a date falls within a range (inclusive)"
  [date start end]
  (when (and (valid-date? date)
          (valid-date? start)
          (valid-date? end))
    #?(:clj
       (let [d (if (instance? LocalDate date) date (.toLocalDate date))
             s (if (instance? LocalDate start) start (.toLocalDate start))
             e (if (instance? LocalDate end) end (.toLocalDate end))]
         (and (not (.isBefore d s))
           (not (.isAfter d e))))

       :cljs
       (let [time (.getTime date)
             start-time (.getTime start)
             end-time (.getTime end)]
         (and (>= time start-time)
           (<= time end-time))))))

(defn normalize-date-range
  "Normalize a date range object, ensuring from/to are Date objects"
  [range-obj]
  (when range-obj
    (let [;; Extract from and to values safely
          from-val (if (map? range-obj)
                     (:from range-obj)
                     #?(:cljs (when (object? range-obj)
                                (.-from ^js range-obj))
                        :clj nil))
          to-val (if (map? range-obj)
                   (:to range-obj)
                   #?(:cljs (when (object? range-obj)
                              (.-to ^js range-obj))
                      :clj nil))
          ;; Ensure they are Date objects
          from (ensure-date from-val)
          to (ensure-date to-val)]
      (cond-> {}
        from (assoc :from from)
        to (assoc :to to)))))

(defn date-range
  "Generate a sequence of dates between start and end (inclusive)"
  [start end]
  (if (and start end)
    (let [start-date (ensure-date start)
          end-date (ensure-date end)]
      (if (and start-date end-date)
        #?(:clj
           (let [s (if (instance? LocalDate start-date) start-date (.toLocalDate start-date))
                 e (if (instance? LocalDate end-date) end-date (.toLocalDate end-date))]
             (take-while #(not (.isAfter % e))
               (iterate #(.plusDays % 1) s)))

           :cljs
           (let [dates (atom [])
                 current (js/Date. start-date)]
             (while (<= (.getTime current) (.getTime end-date))
               (swap! dates conj (js/Date. current))
               (.setDate current (inc (.getDate current))))
             @dates))
        []))
    []))

;; -------------------------
;; Session and Expiration Utilities (Backend specific)
;; -------------------------

#?(:clj
   (defn session-expired?
     "Check if session has expired - handles both LocalDateTime objects and string timestamps"
     [session]
     (let [expires-at (or (:expires_at session) (:expires-at session))]
       (if expires-at
         (let [expires-time (cond
                              ;; Already a LocalDateTime object
                              (instance? java.time.LocalDateTime expires-at)
                              expires-at

                              ;; String timestamp from sanitized session
                              (string? expires-at)
                              (try
                                (time/local-date-time expires-at)
                                (catch Exception e
                                  (log/warn "Failed to parse expires_at timestamp:" expires-at "error:" (.getMessage e))
                                  nil))

                              ;; Unknown format
                              :else
                              (do
                                (log/warn "Unknown expires_at format:" (type expires-at) expires-at)
                                nil))]
           (if expires-time
             (do
               (log/debug "Session expiration check - current:" (time/local-date-time) "expires:" expires-time "expired?" (time/after? (time/local-date-time) expires-time))
               (time/after? (time/local-date-time) expires-time))
             true))                             ; If can't parse expiration time, consider expired
         (do
           (log/debug "No expires_at found in session, keys:" (keys session))
           true)))))

;; -------------------------
;; ClojureScript-specific utilities
;; -------------------------

#?(:cljs
   (defn process-highlighted-dates
     "Process highlighted dates into a format React DayPicker can use.
      Accepts:
      - Collection of dates (strings or Date objects)
      - Single date object
      - Single date string
      - Matcher function
      Returns an array or the processed value."
     [dates]
     (when dates
       (cond
         ;; Collection of dates
         (coll? dates)
         (into-array
           (keep (fn [d]
                   (cond
                     (instance? js/Date d) d
                     (string? d) (parse-date-string d)
                     :else nil))
             dates))

         ;; Single date object
         (instance? js/Date dates)
         dates

         ;; Single date string
         (string? dates)
         (parse-date-string dates)

         ;; Default - return as is (could be a matcher function)
         :else
         dates))))

;; -------------------------
;; Public API Aliases for Compatibility
;; -------------------------

(def format-date
  "Format a date to ISO string (YYYY-MM-DD).
   Accepts date objects or date strings."
  (comp format-iso-date ensure-date))

(def format-date-display format-display-date)
(def parse-date parse-date-string)

(def is-valid-date?
  "Check if a value represents a valid date.
   Accepts date objects or parseable date strings."
  (fn [value]
    (let [date-obj (ensure-date value)]
      (valid-date? date-obj))))

(defn format-date-range-display
  "Format a date range for display. Alias for format-date-range."
  [from to]
  (format-date-range {:from from :to to}))

;; -------------------------
;; API formatting utilities
;; -------------------------

(defn format-date-for-api
  "Format a date value for API submission.
   Handles various date objects and returns YYYY-MM-DD format or nil if not a valid date."
  [date]
  (when date
    (let [date-obj (ensure-date date)]
      (format-iso-date date-obj))))

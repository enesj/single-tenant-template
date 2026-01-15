(ns app.shared.date-core
  "Fundamental date utilities used by other date namespaces."
  (:require
    #?(:clj [java-time.api :as time]
       :cljs [clojure.string :as str])
    [taoensso.timbre :as log]))

#?(:clj
   (import (java.time LocalDate LocalDateTime ZonedDateTime)))

(defn pad-zero
  "Pad a number with leading zero if less than 10"
  [n]
  (if (< n 10) (str "0" n) (str n)))

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
  "Parse a date string into platform-appropriate date object."
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
  "Ensure value is a platform-appropriate date object."
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

(defn format-iso-date
  "Convert a date to ISO date string format (YYYY-MM-DD)."
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
         date

         :else nil))

     :cljs
     (when (and date (instance? js/Date date) (not (js/isNaN (.getTime date))))
       (let [year (.getFullYear date)
             month (inc (.getMonth date))
             day (.getDate date)]
         (str year "-" (pad-zero month) "-" (pad-zero day))))))

(defn format-display-date
  "Format a date for user-friendly display using locale settings."
  [date & {:keys [fallback] :or {fallback "Select a date"}}]
  #?(:clj
     (if date
       (try
         (cond
           (instance? LocalDate date)
           (.toString date)

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

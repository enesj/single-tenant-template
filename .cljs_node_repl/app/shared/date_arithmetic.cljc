(ns app.shared.date-arithmetic
  "Date arithmetic and manipulation functions."
  (:require
    [app.shared.date-core :as core]))

(defn add-days
  "Add (or subtract with negative n) days to a date"
  [date n]
  (when (core/valid-date? date)
    #?(:clj
       (cond
         (instance? java.time.LocalDate date)
         (.plusDays date n)

         (instance? java.time.LocalDateTime date)
         (.plusDays date n)

         :else nil)

       :cljs
       (let [new-date (js/Date. (.getTime date))]
         (.setDate new-date (+ (.getDate new-date) n))
         new-date))))

(defn days-between
  "Calculate the number of days between two dates"
  [date1 date2]
  (when (and (core/valid-date? date1) (core/valid-date? date2))
    #?(:clj
       (let [d1 (if (instance? java.time.LocalDate date1) date1 (.toLocalDate date1))
             d2 (if (instance? java.time.LocalDate date2) date2 (.toLocalDate date2))]
         (.until d1 d2 java.time.temporal.ChronoUnit/DAYS))

       :cljs
       (let [ms-per-day (* 24 60 60 1000)
             diff (- (.getTime date2) (.getTime date1))]
         (js/Math.abs (js/Math.floor (/ diff ms-per-day)))))))

(defn start-of-month
  "Get the first day of the month for a given date"
  [date]
  (when (core/valid-date? date)
    #?(:clj
       (cond
         (instance? java.time.LocalDate date)
         (.withDayOfMonth date 1)

         (instance? java.time.LocalDateTime date)
         (.withDayOfMonth date 1)

         :else nil)

       :cljs
       (js/Date. (.getFullYear date)
         (.getMonth date)
         1))))

(defn end-of-month
  "Get the last day of the month for a given date"
  [date]
  (when (core/valid-date? date)
    #?(:clj
       (cond
         (instance? java.time.LocalDate date)
         (.withDayOfMonth date (.lengthOfMonth date))

         (instance? java.time.LocalDateTime date)
         (.withDayOfMonth date (.lengthOfMonth (.toLocalDate date)))

         :else nil)

       :cljs
       (js/Date. (.getFullYear date)
         (inc (.getMonth date))
         0))))

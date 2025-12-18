(ns app.shared.date-range
  "Date range utilities and sequence generation."
  (:require
    [app.shared.date-core :as core]
    [app.shared.date-arithmetic :as arithmetic]
    #?(:cljs [goog.object :as gobj])))

(defn date-in-range?
  "Check if a date falls within a range (inclusive)"
  [date start end]
  (when (and (core/valid-date? date)
          (core/valid-date? start)
          (core/valid-date? end))
    #?(:clj
       (let [d (if (instance? java.time.LocalDate date) date (.toLocalDate date))
             s (if (instance? java.time.LocalDate start) start (.toLocalDate start))
             e (if (instance? java.time.LocalDate end) end (.toLocalDate end))]
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
                     #?(:cljs (when (and (not (nil? range-obj)) (not (instance? js/Date range-obj)))
                                (gobj/get range-obj "from"))
                        :clj nil))
          to-val (if (map? range-obj)
                   (:to range-obj)
                   #?(:cljs (when (and (not (nil? range-obj)) (not (instance? js/Date range-obj)))
                              (gobj/get range-obj "to"))
                      :clj nil))
          ;; Ensure they are Date objects
          from (core/ensure-date from-val)
          to (core/ensure-date to-val)]
      (cond-> {}
        from (assoc :from from)
        to (assoc :to to)))))

(defn date-range
  "Generate a sequence of dates between start and end (inclusive)"
  [start end]
  (if (and start end)
    (let [start-date (core/ensure-date start)
          end-date (core/ensure-date end)]
      (if (and start-date end-date)
        #?(:clj
           (let [s (if (instance? java.time.LocalDate start-date) start-date (.toLocalDate start-date))
                 e (if (instance? java.time.LocalDate end-date) end-date (.toLocalDate end-date))]
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

(defn format-date-range
  "Format a date range for display."
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
                (and (not (nil? date-range-or-from))
                  (not (instance? js/Date date-range-or-from))
                  (not (map? date-range-or-from))
                  (nil? to))
                [(gobj/get date-range-or-from "from") (gobj/get date-range-or-from "to")]])

           ;; Called with two date arguments
           :else
           [date-range-or-from to])]
     (cond
       ;; Range with from and to dates
       (and from-val to-val)
       (str (core/format-display-date from-val :fallback "?")
         separator
         (core/format-display-date to-val :fallback "?"))

       ;; Range with only from date
       from-val
       (core/format-display-date from-val)

       ;; No valid range
       :else
       fallback))))

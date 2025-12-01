(ns app.shared.validation.constraints
  "Constraint validators including comparisons and SQL function mappings.
   This namespace handles check constraints and comparison operations."
  (:require
    [clojure.string :as str])
  #?(:clj (:import
           [java.util Date])))

;; SQL to Clojure function mapping
(def sql-fn->clj-fn
  "Maps SQL function names to their Clojure equivalents"
  {:length count
   :upper str/upper-case
   :lower str/lower-case
   :trim str/trim
   :substring subs
   :replace str/replace
   :concat str/join
   ;; Date/Time functions
   :date #?(:clj (fn [x] (if (instance? Date x) x (Date/parse x)))
            :cljs (fn [x] (if (instance? js/Date x) x (js/Date.parse x))))
   :now #?(:clj (fn [] (Date.))
           :cljs (fn [] (js/Date.)))
   :age #?(:clj (fn [end start]
                  (let [end-date (if (instance? Date end) end (Date/parse end))
                        start-date (if (instance? Date start) start (Date/parse start))]
                    (quot (- (.getTime end-date) (.getTime start-date)) (* 1000 60 60 24 365))))
           :cljs (fn [end start]
                   (let [end-date (if (instance? js/Date end) end (js/Date. (js/Date.parse end)))
                         start-date (if (instance? js/Date start) start (js/Date. (js/Date.parse start)))]
                     (quot (- (.getTime end-date) (.getTime start-date)) (* 1000 60 60 24 365)))))
   :extract-year #?(:clj (fn [date]
                           (let [cal (doto (java.util.Calendar/getInstance)
                                       (.setTime (if (instance? Date date) date (Date/parse date))))]
                             (.get cal java.util.Calendar/YEAR)))
                    :cljs (fn [date]
                            (.getFullYear (if (instance? js/Date date) date (js/Date. (js/Date.parse date))))))
   :extract-month #?(:clj (fn [date]
                            (let [cal (doto (java.util.Calendar/getInstance)
                                        (.setTime (if (instance? Date date) date (Date/parse date))))]
                              (inc (.get cal java.util.Calendar/MONTH))))
                     :cljs (fn [date]
                             (inc (.getMonth (if (instance? js/Date date) date (js/Date. (js/Date.parse date)))))))
   :extract-day #?(:clj (fn [date]
                          (let [cal (doto (java.util.Calendar/getInstance)
                                      (.setTime (if (instance? Date date) date (Date/parse date))))]
                            (.get cal java.util.Calendar/DAY_OF_MONTH)))
                   :cljs (fn [date]
                           (.getDate (if (instance? js/Date date) date (js/Date. (js/Date.parse date))))))})

(defn- create-value-validator
  "Creates a Malli validation function for direct value comparisons with custom error message"
  [op-str op-func value]
  [:fn {:error/message (str/capitalize (str "Value must be " op-str " " value))}
   (fn [validation-value] (op-func validation-value value))])

(defn- create-sql-validator
  "Creates a Malli validation function for SQL function comparisons"
  [fn-name value-fn op-str op-func value]
  [:fn {:error/message (str/capitalize (str fn-name " must be " op-str " " value))}
   (fn [validation-value] (op-func (value-fn validation-value) value))])

(defn create-comparison-validator
  "Creates a Malli validator for comparison operations"
  [op args]
  (let [op-meta (case op
                  ;; Keyword operators
                  :> {:op-str "greater than" :op-func >}
                  :>= {:op-str "greater than or equal to" :op-func >=}
                  :< {:op-str "less than" :op-func <}
                  :<= {:op-str "less than or equal to" :op-func <=}
                  := {:op-str "equal to" :op-func =}
                  ;; String operators (for compatibility with models data)
                  ">" {:op-str "greater than" :op-func >}
                  ">=" {:op-str "greater than or equal to" :op-func >=}
                  "<" {:op-str "less than" :op-func <}
                  "<=" {:op-str "less than or equal to" :op-func <=}
                  "=" {:op-str "equal to" :op-func =}
                  ;; For unknown operators, log and return nil
                  (do
                    #?(:clj (println "Warning: Unknown comparison operator:" op "with args:" args)
                       :cljs (js/console.warn "Unknown comparison operator:" op "with args:" args))
                    nil))]
    (if op-meta
      (let [op-str (:op-str op-meta)
            op-func (:op-func op-meta)
            value (last args)]
        (if (vector? (first args))
          (let [fn-name (ffirst args)]
            (if-let [value-fn (get sql-fn->clj-fn fn-name)]
              (create-sql-validator fn-name value-fn op-str op-func value)
              (create-value-validator op-str op-func value)))
          (create-value-validator op-str op-func value)))
      ;; For unknown operators, return a no-op validator
      [:fn (constantly true)])))

(defn- raw-check?
  "Check if a constraint is a :raw SQL constraint (handles both keyword and string forms)"
  [constraint]
  (when (vector? constraint)
    (let [op (first constraint)]
      (or (= op :raw) (= op "raw")))))

(defn get-field-checks
  "Extract check constraints for a field from models.edn definition.
   Filters out :raw constraints since they are SQL-specific and cannot be validated client-side."
  [field-def]
  (let [checks (get-in field-def [2 :check])]
    (if (vector? checks)
      ;; Skip :raw constraints - they are SQL-specific and can't be validated on the frontend
      ;; Handle both keyword :raw and string "raw" (from JSON serialization)
      (if (raw-check? checks)
        []
        (if (or (= (first checks) :and) (= (first checks) "and"))
          ;; Filter out :raw constraints from :and compound checks
          (let [non-raw-checks (remove raw-check? (rest checks))]
            (if (seq non-raw-checks)
              (into [:and] (map #(create-comparison-validator (first %) (rest %)) non-raw-checks))
              []))
          (let [[op & args] checks]
            (create-comparison-validator op args))))
      [])))

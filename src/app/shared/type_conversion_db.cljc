(ns app.shared.type-conversion-db
  "Database-specific type casting and formatting logic.
   Handles HoneySQL formatting and PostgreSQL type preparations."
  (:require
    #?@(:clj [[taoensso.timbre :as log]
              [cheshire.core :as json]])
    #?@(:cljs [[taoensso.timbre :as log]])
    [app.shared.field-metadata :as field-meta]
    [clojure.string :as str]))

;; =============================================================================
;; Helpers for Metadata Resolution
;; =============================================================================

(defn- ->keyword
  "Best-effort conversion to keyword while preserving underscores."
  [v]
  (cond
    (keyword? v) v
    (string? v) (keyword v)
    (symbol? v) (keyword (name v))
    :else (keyword (str v))))

(defn- candidate-field-keys
  "Generate lookup variants for a field key to handle kebab/snake casing."
  [field-name]
  (let [kw (->keyword field-name)
        raw (name kw)
        snake (keyword (str/replace raw "-" "_"))
        kebab (keyword (str/replace raw "_" "-"))]
    (->> [kw snake kebab]
      (distinct))))

(defn resolve-field-type
  "Resolve a field type using models metadata with casing fallbacks."
  [models entity field-name]
  (when (and models entity)
    (let [entity-key (field-meta/normalize-entity-key entity)]
      (some (fn [candidate]
              (field-meta/get-field-type models entity-key candidate))
        (candidate-field-keys field-name)))))

;; =============================================================================
;; Database Type Casting (HoneySQL format)
;; =============================================================================

(defn cast-for-database
  "Cast a value to the appropriate PostgreSQL type for HoneySQL."
  [field-type value]
  (let [result (cond
                 (nil? value) nil

                 (or (= field-type :jsonb) (= field-type :map))
                 [:cast [:lift value] :jsonb]

                 (or (= field-type :timestamp) (= field-type :timestamptz))
                 (let [converted-value (cond
                                         #?@(:clj [(instance? java.util.Date value) (str value)])
                                         #?@(:cljs [(instance? js/Date value) (.toISOString value)])
                                         :else value)]
                   [:cast converted-value field-type])

                 (= field-type :date)
                 (let [converted-value (cond
                                         #?@(:clj [(instance? java.util.Date value) (str value)])
                                         #?@(:cljs [(instance? js/Date value) (.toISOString value)])
                                         :else value)]
                   [:cast converted-value :date])

                 (= field-type :decimal) [:cast value :decimal]
                 (= field-type :integer) [:cast value :integer]
                 (= field-type :inet) [:cast value :inet]
                 (= field-type :boolean) [:cast value :boolean]
                 (= field-type :uuid) [:cast value :uuid]
                 (= field-type :text) value

                 (vector? field-type)
                 (case (first field-type)
                   :enum (let [enum-type-name (second field-type)
                               enum-type-str (-> (name enum-type-name)
                                               (str/replace "-" "_"))]
                           [:raw (str "CAST('" value "' AS " enum-type-str ")")])

                   :array (let [array-element-type (second field-type)
                                pg-array-type-str (str (name array-element-type) "[]")]
                            (cond
                              (or (nil? value) (and (vector? value) (empty? value)))
                              [:raw (str "ARRAY[]::" pg-array-type-str)]

                              (vector? value)
                              (if (= array-element-type :uuid)
                                (let [uuid-strs (map str value)
                                      array-literal (str "ARRAY[" (str/join "," (map #(str "'" % "'") uuid-strs)) "]")]
                                  [:raw (str array-literal "::" pg-array-type-str)])
                                (let [array-literal (str "ARRAY[" (str/join "," (map #(str "'" % "'") value)) "]")]
                                  [:raw (str array-literal "::" pg-array-type-str)]))

                              :else
                              [:cast value [:raw pg-array-type-str]]))

                   :decimal [:cast value :decimal]
                   :varchar value
                   value)

                 (keyword? field-type)
                 (let [enum-type-str (-> (name field-type)
                                       (str/replace "-" "_"))]
                   [:raw (str "CAST('" value "' AS " enum-type-str ")")])

                 :else value)]
    result))

(defn cast-field-value [field-type value]
  (cast-for-database field-type value))

;; =============================================================================
;; Formatters
;; =============================================================================

(defn format-date-for-db
  "Format a date value for database storage."
  [date-value]
  (cond
    #?@(:clj [(instance? java.util.Date date-value) (str date-value)])
    #?@(:cljs [(instance? js/Date date-value) (.toISOString date-value)])
    :else date-value))

(defn format-json-for-db
  "Format a value for JSONB storage in the database."
  [value]
  (cond
    (string? value) value
    :else
    #?(:clj (json/generate-string value)
       :cljs (js/JSON.stringify value))))

;; =============================================================================
;; Data Preparation
;; =============================================================================

(defn prepare-data-for-db
  "Cast each field in `data` using model metadata and return DB-ready map."
  ([models entity data]
   (prepare-data-for-db models entity data {}))
  ([models entity data {:keys [include-nils? field-type-resolver preserve-unknown?]
                        :or {include-nils? false preserve-unknown? true}}]
   (let [data-map (cond-> (or data {})
                    (not (map? data)) (into {}))
         result (empty data-map)
         resolver (or field-type-resolver
                    (when models
                      (fn [field-name]
                        (resolve-field-type models entity field-name))))]
     (reduce-kv
       (fn [acc field-name value]
         (let [field-type (when resolver (resolver field-name))
               casted (if field-type
                        (cast-field-value field-type value)
                        value)
               has-value? (some? casted)
               keep? (cond
                       has-value? true
                       field-type include-nils?
                       preserve-unknown? include-nils?
                       :else false)]
           (cond-> acc
             keep? (assoc field-name casted))))
       result
       data-map))))

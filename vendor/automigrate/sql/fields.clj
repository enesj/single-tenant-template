(ns automigrate.sql.fields
  "Field type system and option transformation utilities."
  (:require
   [automigrate.fields :as fields]
   [automigrate.util.db :as db-util]
   [automigrate.util.model :as model-util]
   [automigrate.util.spec :as spec-util]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [spec-dict :as d]))

;; Field option specs
(s/def :automigrate.sql.option->sql/type
  (s/and
    ::fields/type
    (s/conformer identity)))

(s/def :automigrate.sql.option->sql/null
  (s/and
    ::fields/null
    (s/conformer
      (fn [value]
        (if (true? value)
          nil
          [:not nil])))))

(s/def :automigrate.sql.option->sql/primary-key
  (s/and
    ::fields/primary-key
    (s/conformer
      (fn [_]
        [:primary-key]))))

(s/def :automigrate.sql.option->sql/check
  (s/and
    ::fields/check
    (s/conformer
      (fn [value]
        [:check value]))))

(s/def :automigrate.sql.option->sql/unique
  (s/and
    ::fields/unique
    (s/conformer
      (fn [_]
        :unique))))

(s/def :automigrate.sql.option->sql/default
  (s/and
    ::fields/default
    (s/conformer
      (fn [value]
        [:default value]))))

(defn- fk-opt->raw
  [option value]
  (mapv db-util/kw->raw [option value]))

(s/def ::foreign-key
  (d/dict*
    {:foreign-field ::fields/foreign-key}
    ^:opt {fields/ON-DELETE-OPTION ::fields/on-delete
           fields/ON-UPDATE-OPTION ::fields/on-update}))

(s/def :automigrate.sql.option->sql/foreign-key
  (s/and
    (s/conformer
      (fn [value]
        [(cons :references (model-util/kw->vec value))]))))

(s/def :automigrate.sql.option->sql/on-delete
  (s/and
    ::fields/on-delete
    (s/conformer
      (fn [value]
        (fk-opt->raw fields/ON-DELETE-OPTION value)))))

(s/def :automigrate.sql.option->sql/on-update
  (s/and
    ::fields/on-update
    (s/conformer
      (fn [value]
        (fk-opt->raw fields/ON-UPDATE-OPTION value)))))

(s/def :automigrate.sql.option->sql/array
  (s/and
    ::fields/array
    (s/conformer
      (fn [value]
        [:raw value]))))

(s/def :automigrate.sql.option->sql/comment
  (s/and
    ::fields/comment
    (s/conformer identity)))

(def options-specs
  [:automigrate.sql.option->sql/null
   :automigrate.sql.option->sql/primary-key
   :automigrate.sql.option->sql/unique
   :automigrate.sql.option->sql/default
   :automigrate.sql.option->sql/foreign-key
   :automigrate.sql.option->sql/on-delete
   :automigrate.sql.option->sql/on-update
   :automigrate.sql.option->sql/check
   :automigrate.sql.option->sql/array
   :automigrate.sql.option->sql/comment])

(s/def ::->foreign-key-complete
  (s/conformer
    (fn [value]
      (if-let [foreign-key (:foreign-key value)]
        (let [on-delete (get value fields/ON-DELETE-OPTION)
              on-update (get value fields/ON-UPDATE-OPTION)
              foreign-key-complete (cond-> foreign-key
                                     (some? on-delete) (concat on-delete)
                                     (some? on-update) (concat on-update))]
          (-> value
            (assoc fields/FOREIGN-KEY-OPTION foreign-key-complete)
            (dissoc fields/ON-DELETE-OPTION fields/ON-UPDATE-OPTION)))
        value))))

(s/def ::options->sql
  (s/and
    (d/dict*
      {:type :automigrate.sql.option->sql/type}
      (d/->opt (spec-util/specs->dict options-specs)))
    ::->foreign-key-complete))

(s/def ::fields
  (s/map-of keyword? ::options->sql))

;; Field type conversion
(defn field-type->sql
  "Convert field type definition to SQL."
  [{array-value :array
    type-value :type}]
  (let [type-sql (cond
                   ; :add-column clause in honeysql converts type name in kebab case into
                   ; two separated words. So, for custom enum types we have to convert
                   ; custom type name to snake case to use it in SQL as a single word.
                   (s/valid? ::fields/enum-type type-value)
                   (-> type-value last model-util/kw->snake-case)

                   (s/valid? ::fields/time-types type-value)
                   (let [[type-name precision] type-value]
                     [:raw (format "%s(%s)" (-> type-name name str/upper-case) precision)])

                   :else type-value)]
    ; Add array type if it exists
    (cond-> [type-sql]
      (some? array-value) (conj array-value))))

;; Comment generation
(defn create-comment-on-field-raw
  "Generate SQL for creating field comments."
  [{:keys [model-name field-name comment-val]}]
  (let [obj-str (->> [model-name field-name]
                  (map #(-> % (name) (model-util/kw->snake-case-str)))
                  (str/join "."))
        create-comment-tmp "COMMENT ON COLUMN %s IS %s"
        comment-val* (if (some? comment-val)
                       (format "'%s'" comment-val)
                       "NULL")]
    [:raw (format create-comment-tmp obj-str comment-val*)]))

(defn fields->comments-sql
  "Generate SQL for all field comments in a model."
  [model-name fields]
  (reduce
    (fn [acc [field-name options]]
      (if (some? (:comment options))
        (conj acc (create-comment-on-field-raw
                    {:model-name model-name
                     :field-name field-name
                     :comment-val (:comment options)}))
        acc))
    []
    fields))

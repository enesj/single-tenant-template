(ns automigrate.errors.extraction)

;; Constants
(def ^:private INDEX-FIELD-NAME-IN-SPEC 2)

;; Data extraction utilities (extracted from lines 17-110)
(defn problem-reason
  [problem]
  (or (:reason problem) (:pred problem)))

(defn duplicates
  "Return duplicated items in collection."
  [items]
  (->> items
    (frequencies)
    (filter #(> (val %) 1))
    (keys)))

(defn get-model-name
  [data]
  (if (= :automigrate.actions/->migrations (:main-spec data))
    (get-in (:origin-value data) [(first (:in data)) :model-name])
    (-> data :in first)))

(defn get-model-items-path
  [data items-key]
  {:pre [(contains? #{:fields :indexes :types} items-key)]}
  (let [model-name (get-model-name data)
        model (get (:origin-value data) model-name)
        in-path (:in data)
        index-fields-key (.indexOf in-path items-key)
        path-has-fields-key? (> index-fields-key 0)
        field-name (if path-has-fields-key?
                     (nth in-path (inc index-fields-key))
                     (nth in-path INDEX-FIELD-NAME-IN-SPEC))]
    (if (vector? model)
      [model-name field-name]
      [model-name items-key field-name])))

(defn get-options
  [data]
  (if (= :automigrate.actions/->migrations (:main-spec data))
    (:val data)
    (let [field-path (conj (get-model-items-path data :fields) 2)]
      (get-in (:origin-value data) field-path))))

(defn get-field-name
  [data]
  (if (and (= :automigrate.actions/->migrations (:main-spec data))
        (contains? #{:add-column :alter-column} (-> data :path first)))
    (get-in (:origin-value data) [(first (:in data)) :field-name])
    (let [path (get-model-items-path data :fields)
          last-item (peek path)]
      (if (keyword? last-item)
        last-item
        (get-in (:origin-value data) (conj path 0))))))

(defn get-fq-field-name
  "Return full qualified field name with model namespace."
  [data]
  (let [model-name (name (get-model-name data))
        field-name (name (get-field-name data))]
    (keyword model-name field-name)))

(defn get-index-name
  [data]
  (let [path (get-model-items-path data :indexes)
        last-item (peek path)]
    (if (keyword? last-item)
      last-item
      (get-in (:origin-value data) (conj path 0)))))

(defn get-type-name
  [data]
  (let [path (get-model-items-path data :types)
        last-item (peek path)]
    (if (keyword? last-item)
      last-item
      (get-in (:origin-value data) (conj path 0)))))

(defn get-fq-index-name
  "Return full qualified index name with model namespace."
  [data]
  (let [model-name (str (name (get-model-name data)) ".indexes")
        index-name (name (get-index-name data))]
    (keyword model-name index-name)))

(defn get-fq-type-name
  "Return full qualified type name with model namespace."
  [data]
  (let [model-name (str (name (get-model-name data)) ".types")
        type-name (name (get-type-name data))]
    (keyword model-name type-name)))

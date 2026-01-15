(ns automigrate.schema.diffing
  "Schema comparison and migration action generation"
  (:require
    [automigrate.actions :as actions]
    [automigrate.fields :as fields]
    [automigrate.util.model :as model-util]
    [automigrate.util.spec :as spec-util]
    [clojure.set :as set]
    [clojure.spec.alpha :as s]
    [differ.core :as differ]
    [weavejester.dependency :as dep]))

(def ^:private DROPPED-ENTITY-VALUE 0)
(def ^:private DEFAULT-ROOT-NODE :root)

(defn- new-field?
  [old-model fields-diff field-name]
  (and (contains? fields-diff field-name)
    (not (contains? (:fields old-model) field-name))))

(defn- drop-field?
  [fields-removals field-name]
  (= DROPPED-ENTITY-VALUE (get fields-removals field-name)))

(defn- options-dropped
  [removals]
  (-> (filter #(= DROPPED-ENTITY-VALUE (val %)) removals)
    (keys)
    (set)
    (set/difference #{:type})))

(defn- options-added
  "Get field options with deletion changes.
  Example:
  - old: `{:type [:decimal 10 2]}`
  - new: `{:type [:decimal 10]}`"
  [{:keys [to-add to-drop new-field]}]
  (reduce-kv
    (fn [m k v]
      (cond-> m
        ; Check diff value from differ not 0, then add new value for option
        ; to be able to see it in `:changes` key of migration action.
        (not= DROPPED-ENTITY-VALUE v) (assoc k (get new-field k))))
    to-add
    to-drop))

(defn- assoc-option-to-add
  [old-field changes option-key new-option-value]
  (let [old-option-value (if (contains? old-field option-key)
                           (get old-field option-key)
                           model-util/EMPTY-OPTION)]
    (-> changes
      (assoc-in [option-key :from] old-option-value)
      (assoc-in [option-key :to] new-option-value))))

(defn- assoc-option-to-drop
  [old-field changes option-key]
  (-> changes
    (assoc-in [option-key :from] (get old-field option-key))
    (assoc-in [option-key :to] model-util/EMPTY-OPTION)))

(defn- get-changes
  [old-options options-to-add options-to-drop]
  (as-> {} $
    (reduce-kv (partial assoc-option-to-add old-options) $ options-to-add)
    (reduce (partial assoc-option-to-drop old-options) $ options-to-drop)))

(defn- get-options-to-add
  "Update option in diff to option from new model if diff is a vector.

  It is a caveat how differ lib works with changes in vectors. For example, it uses
  in case when we change field type [:varchar 100] to [:varchar 200]. In diff we see [1 200]
  cause just second item of a vector has been changed. So for us it is important to have whole
  new type in options to add, and we just copy it from new model."
  [fields-diff field-name new-model]
  (let [field-options-diff (get fields-diff field-name)
        fields-options-new (get-in new-model [:fields field-name])]
    (reduce-kv
      (fn [m k v]
        (if (vector? v)
          (assoc m k (get fields-options-new k))
          (assoc m k v)))
      {}
      field-options-diff)))

(defn parse-fields-diff
  "Return field's migrations for model."
  [{:keys [model-diff removals old-model new-model model-name]}]
  (let [fields-diff (:fields model-diff)
        fields-removals (:fields removals)
        changed-fields (-> (set (keys fields-diff))
                         (set/union (set (keys fields-removals))))]
    (for [field-name changed-fields
          :let [options-to-add (get-options-to-add fields-diff field-name new-model)
                options-to-drop (get fields-removals field-name)
                new-field?* (new-field? old-model fields-diff field-name)
                drop-field?* (drop-field? fields-removals field-name)
                field-options-old (get-in old-model [:fields field-name])
                field-options-new (get-in new-model [:fields field-name])]]
      (cond
        new-field?* {:action actions/ADD-COLUMN-ACTION
                     :field-name field-name
                     :model-name model-name
                     :options options-to-add}
        drop-field?* {:action actions/DROP-COLUMN-ACTION
                      :field-name field-name
                      :model-name model-name}
        :else {:action actions/ALTER-COLUMN-ACTION
               :field-name field-name
               :model-name model-name
               :options field-options-new
               :changes (get-changes field-options-old
                          (options-added
                            {:to-add options-to-add
                             :to-drop options-to-drop
                             :new-field field-options-new})
                          (options-dropped options-to-drop))}))))

(defn- new-model?
  [alterations old-schema model-name]
  (and (contains? alterations model-name)
    (not (contains? old-schema model-name))))

(defn- drop-model?
  [removals model-name]
  (= DROPPED-ENTITY-VALUE (get removals model-name)))

(defn- new-index?
  [old-model indexes-diff index-name]
  (and (contains? indexes-diff index-name)
    (not (contains? (:indexes old-model) index-name))))

(defn- drop-index?
  [indexes-removals index-name]
  (= DROPPED-ENTITY-VALUE (get indexes-removals index-name)))

(defn parse-indexes-diff
  "Return index's migrations for model."
  [model-diff removals old-model new-model model-name]
  (let [indexes-diff (:indexes model-diff)
        indexes-removals (if (= DROPPED-ENTITY-VALUE (:indexes removals))
                           (->> (:indexes old-model)
                             (reduce-kv (fn [m k _v] (assoc m k DROPPED-ENTITY-VALUE)) {}))
                           (:indexes removals))
        changed-indexes (-> (set (keys indexes-diff))
                          (set/union (set (keys indexes-removals))))]
    (for [index-name changed-indexes
          :let [options-to-add (get indexes-diff index-name)
                options-to-alter (get-in new-model [:indexes index-name])
                new-index?* (new-index? old-model indexes-diff index-name)
                drop-index?* (drop-index? indexes-removals index-name)]]
      (cond
        new-index?* {:action actions/CREATE-INDEX-ACTION
                     :index-name index-name
                     :model-name model-name
                     :options options-to-add}
        drop-index?* {:action actions/DROP-INDEX-ACTION
                      :index-name index-name
                      :model-name model-name}
        :else {:action actions/ALTER-INDEX-ACTION
               :index-name index-name
               :model-name model-name
               :options options-to-alter}))))

(defn- new-type?
  [old-model types-diff type-name]
  (and (contains? types-diff type-name)
    (not (contains? (:types old-model) type-name))))

(defn- drop-type?
  [types-removals type-name type-from-dropped-model?]
  (or
    (= DROPPED-ENTITY-VALUE (get types-removals type-name))
    type-from-dropped-model?))

(defn parse-types-diff
  "Return type's migrations for model."
  [{:keys [model-diff model-removals old-model new-model model-name
           type-from-dropped-model?]}]
  (let [types-diff (:types model-diff)
        types-removals (if (or (= DROPPED-ENTITY-VALUE (:types model-removals))
                             type-from-dropped-model?)
                         (->> (:types old-model)
                           (reduce-kv (fn [m k _v] (assoc m k DROPPED-ENTITY-VALUE)) {}))
                         (:types model-removals))
        changed-types (-> (set (keys types-diff))
                        (set/union (set (keys types-removals))))]
    (for [type-name changed-types
          :let [options-to-add (get types-diff type-name)
                new-type?* (new-type? old-model types-diff type-name)
                drop-type?* (drop-type? types-removals type-name type-from-dropped-model?)
                type-options-old (get-in old-model [:types type-name])
                type-options-new (get-in new-model [:types type-name])]]
      (cond
        new-type?* {:action actions/CREATE-TYPE-ACTION
                    :type-name type-name
                    :model-name model-name
                    :options options-to-add}
        drop-type?* {:action actions/DROP-TYPE-ACTION
                     :type-name type-name
                     :model-name model-name}
        :else {:action actions/ALTER-TYPE-ACTION
               :type-name type-name
               :model-name model-name
               :options type-options-new
               :changes (get-changes
                          type-options-old
                          (options-added
                            {:to-add (dissoc type-options-new :type)})
                          #{})}))))

(defn- get-deps-for-model
  [model-fields]
  (mapv
    (fn [field]
      (cond
        (contains? field :foreign-key)
        (-> field :foreign-key model-util/kw->map)

        (s/valid? ::fields/enum-type (:type field))
        {:type-name (-> field :type last)}))
    (vals model-fields)))

(s/def ::model-name keyword?)
(s/def ::field-name keyword?)
(s/def ::type-name keyword?)

(s/def ::action-dependencies-ret-item
  (s/keys
    :opt-un [::model-name
             ::field-name
             ::type-name]))

(s/def ::action-dependencies-ret
  (s/coll-of ::action-dependencies-ret-item))

(defn action-dependencies
  "Return dependencies as vector of maps for an action or nil."
  [action]
  {:pre [(spec-util/assert! map? action)]
   :post [(spec-util/assert! ::action-dependencies-ret %)]}
  (let [changes-to-add (model-util/changes-to-add (:changes action))
        fk (condp contains? (:action action)
             #{actions/ADD-COLUMN-ACTION} (get-in action [:options :foreign-key])
             #{actions/ALTER-COLUMN-ACTION} (:foreign-key changes-to-add)
             nil)
        type-def (condp contains? (:action action)
                   #{actions/ADD-COLUMN-ACTION} (get-in action [:options :type])
                   #{actions/ALTER-COLUMN-ACTION} (:type changes-to-add)
                   nil)]
    (->> (condp contains? (:action action)
           #{actions/ADD-COLUMN-ACTION
             actions/ALTER-COLUMN-ACTION} (cond-> [{:model-name (:model-name action)}]
                                            (some? fk) (conj (model-util/kw->map fk))

                                            (s/valid? ::fields/enum-type type-def)
                                            (conj {:type-name (last type-def)}))
           #{actions/DROP-COLUMN-ACTION} (cond-> [{:model-name (:model-name action)}]
                                           (some? fk) (conj (model-util/kw->map fk)))
           #{actions/CREATE-TABLE-ACTION} (get-deps-for-model (:fields action))
           #{actions/CREATE-INDEX-ACTION
             actions/ALTER-INDEX-ACTION} (mapv (fn [field]
                                                 {:model-name (:model-name action)
                                                  :field-name field})
                                            (get-in action [:options :fields]))
           #{actions/DROP-TYPE-ACTION} [{:type-name (:type-name action)}]
           [])
      (remove nil?))))

(defn- parent-action?
  "Check if action is parent to one with presented dependencies."
  [old-schema deps next-action action]
  (let [model-names (set (map :model-name deps))
        type-names (set (map :type-name deps))]
    (if (= next-action action)
      false
      (condp contains? (:action action)
        #{actions/CREATE-TABLE-ACTION} (contains? model-names (:model-name action))
        #{actions/ADD-COLUMN-ACTION
          actions/ALTER-COLUMN-ACTION} (some
                                         #(and (= (:model-name action) (:model-name %))
                                            (= (:field-name action) (:field-name %)))
                                         deps)
        #{actions/DROP-COLUMN-ACTION} (let [field-type (get-in old-schema
                                                         [(:model-name action)
                                                          :fields
                                                          (:field-name action)
                                                          :type])]
                                        (and (s/valid? ::fields/enum-type field-type)
                                          (contains? type-names (last field-type))))
        #{actions/DROP-TABLE-ACTION} (let [fields (get-in old-schema
                                                    [(:model-name action) :fields])
                                           field-types (mapv :type (vals fields))]
                                       (or
                                         (contains? (->> (get-in old-schema
                                                           [(:model-name action)
                                                            :fields])
                                                      (get-deps-for-model)
                                                      (mapv :model-name)
                                                      (set))
                                           (:model-name next-action))

                                         (some
                                           #(and (s/valid? ::fields/enum-type %)
                                              (contains? type-names (last %)))
                                           field-types)))
        #{actions/CREATE-TYPE-ACTION
          actions/ALTER-TYPE-ACTION} (contains? type-names (:type-name action))
        false))))

(defn- assoc-action-deps
  "Assoc dependencies to graph by actions."
  [old-schema actions graph next-action]
  (let [deps (action-dependencies next-action)
        parent-actions (filterv
                         (partial parent-action? old-schema deps next-action)
                         actions)]
    (as-> graph $
      (dep/depend $ next-action DEFAULT-ROOT-NODE)
      (reduce #(dep/depend %1 next-action %2) $ parent-actions))))

(defn- compare-actions
  "Secondary comparator for sorting actions in migration the same way."
  [a b]
  (< (hash a) (hash b)))

(defn sort-actions
  "Apply order for migration's actions by foreign key between models."
  [old-schema actions]
  (->> actions
    (reduce (partial assoc-action-deps old-schema actions) (dep/graph))
    (dep/topo-sort compare-actions)
    (drop 1)))

(defn make-migration*
  [old-schema new-schema]
  (let [[alterations removals] (differ/diff old-schema new-schema)
        changed-models (-> (set (keys alterations))
                         (set/union (set (keys removals))))
        actions (for [model-name changed-models
                      :let [old-model (get old-schema model-name)
                            new-model (get new-schema model-name)
                            model-diff (get alterations model-name)
                            model-removals (get removals model-name)
                            new-model?* (new-model? alterations old-schema model-name)
                            drop-model?* (drop-model? removals model-name)]]
                  (concat
                    (cond
                      new-model?* [{:action actions/CREATE-TABLE-ACTION
                                    :model-name model-name
                                    :fields (or (:fields model-diff) (:fields new-model))}]
                      drop-model?* [{:action actions/DROP-TABLE-ACTION
                                     :model-name model-name}]
                      :else (parse-fields-diff {:model-diff model-diff
                                                :removals model-removals
                                                :old-model old-model
                                                :new-model new-model
                                                :model-name model-name}))
                    (parse-indexes-diff model-diff model-removals old-model new-model model-name)
                    (parse-types-diff
                      {:model-diff model-diff
                       :model-removals model-removals
                       :old-model old-model
                       :new-model new-model
                       :model-name model-name
                       :type-from-dropped-model? drop-model?*})))]
    (->> actions
      (flatten)
      (sort-actions old-schema)
      (actions/->migrations))))

(defn dependency-resolution
  "Resolve action dependencies for proper ordering"
  [actions old-schema]
  (sort-actions old-schema actions))

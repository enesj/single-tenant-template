(ns app.admin.frontend.events.user-settings.table-columns
  (:require
    [app.admin.frontend.events.user-settings.utils :as u]
    [clojure.string :as str]
    [re-frame.core :as rf]))

(defn- col->str [c]
  (cond
    (nil? c) nil
    (keyword? c) (name c)
    (string? c) c
    :else (str c)))

(defn- prune-cols-to-available
  "Keep only columns whose string form is present in avail-set.

  Returns a distinct vector of strings, preserving input order."
  [avail-set xs]
  (->> (or xs [])
    (map col->str)
    (filter avail-set)
    distinct
    vec))

(defn- prune-map-to-available
  "Prune a map keyed by column identifiers (keywords/strings), keeping entries
  whose key string form is present in avail-set."
  [avail-set m]
  (reduce-kv
    (fn [acc k v]
      (if (contains? avail-set (col->str k))
        (assoc acc k v)
        acc))
    {}
    (or m {})))

(defn- cleanup-entity-table-columns
  "Ensure the entity table-columns config stays valid when :available-columns
  changes by pruning dependent lists/maps to be subsets of :available-columns."
  [entity-config]
  (let [avail-set (set (map col->str (or (:available-columns entity-config) [])))]
    (-> entity-config
      (update :available-columns #(->> (or % []) (map col->str) (remove nil?) distinct vec))
      (update :always-visible #(prune-cols-to-available avail-set %))
      (update :default-visible-columns #(prune-cols-to-available avail-set %))
      (update :filterable-columns #(prune-cols-to-available avail-set %))
      (update :sortable-columns #(prune-cols-to-available avail-set %))
      (update :computed-fields #(prune-map-to-available avail-set %))
      (update :column-config #(prune-map-to-available avail-set %))
      (update :column-metadata #(prune-map-to-available avail-set %)))))

;; =============================================================================
;; Draft editing: table-columns defaults
;; =============================================================================

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/toggle-column-visibility-draft
  (fn [db [_ entity column-name]]
    (let [entity-kw (u/normalize-kw entity)
          column-kw (u/normalize-kw column-name)
          entity-config (u/safe-map (get-in db [:admin :user-settings :draft :table-columns entity-kw]))
          available (u/normalize-cols (:available-columns entity-config))
          always-visible-set (into #{} (u/normalize-cols (:always-visible entity-config)))
          has-default-visible? (contains? entity-config :default-visible-columns)
          default-visible (u/normalize-cols (if has-default-visible?
                                              (:default-visible-columns entity-config)
                                              available))
          visible-set (into #{} default-visible)
          visible-set (into visible-set always-visible-set)]
      (cond
        (or (nil? entity-kw) (nil? column-kw))
        db

        (not (some #{column-kw} available))
        db

        (contains? always-visible-set column-kw)
        db

        :else
        (let [currently-visible? (contains? visible-set column-kw)
              new-visible-set (if currently-visible?
                                (disj visible-set column-kw)
                                (conj visible-set column-kw))
              ;; Preserve ordering based on available-columns
              new-visible (->> available
                            (filter new-visible-set)
                            vec)]
          (assoc-in db
            [:admin :user-settings :draft :table-columns entity-kw :default-visible-columns]
            new-visible))))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/reset-columns-draft
  (fn [db [_ entity]]
    (let [entity-kw (u/normalize-kw entity)
          saved-entity-config (get-in db [:admin :user-settings :saved :table-columns entity-kw])]
      (if (nil? entity-kw)
        db
        (assoc-in db [:admin :user-settings :draft :table-columns entity-kw] (u/safe-map saved-entity-config))))))

;; =============================================================================
;; Draft editing: table-columns config (structural, not policy)
;; =============================================================================

(defn- col-key-variants
  [column-name]
  (let [col (col->str column-name)
        kebab (some-> col (str/replace "_" "-"))
        snake (some-> col (str/replace "-" "_"))]
    (->> [col
          kebab
          snake
          (some-> col keyword)
          (some-> kebab keyword)
          (some-> snake keyword)]
      (remove nil?)
      distinct
      vec)))

(defn- remove-column-metadata-entry
  [metadata column-name]
  (let [metadata (or metadata {})
        keys-to-remove (col-key-variants column-name)]
    (reduce dissoc metadata keys-to-remove)))

(defn- normalize-label
  [label]
  (let [label-str (some-> label str str/trim)]
    (when-not (str/blank? (or label-str ""))
      label-str)))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-table-column-label-draft
  (fn [db [_ entity column-name label]]
    (let [entity-kw (u/normalize-kw entity)
          col-str (col->str column-name)
          metadata-path [:admin :user-settings :draft :table-columns entity-kw :column-metadata]
          entity-path [:admin :user-settings :draft :table-columns entity-kw]
          label' (normalize-label label)
          current-metadata (get-in db metadata-path)
          current-entry (some (fn [k]
                                (let [sentinel ::not-found
                                      value (get current-metadata k sentinel)]
                                  (when-not (= value sentinel) value)))
                          (col-key-variants col-str))
          cleaned (remove-column-metadata-entry current-metadata col-str)
          preserved-entry (some-> (or current-entry {}) (dissoc :label))]
      (if (or (nil? entity-kw) (str/blank? (or col-str "")))
        db
        (if label'
          (assoc-in db metadata-path (assoc cleaned col-str (assoc (or current-entry {}) :label label')))
          (let [metadata' (cond-> cleaned
                            (seq preserved-entry) (assoc col-str preserved-entry))
                db' (assoc-in db metadata-path metadata')]
            (if (seq metadata')
              db'
              (update-in db' entity-path dissoc :column-metadata))))))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-table-column-list-draft
  (fn [db [_ entity list-type columns]]
    (let [entity-kw (u/normalize-kw entity)
          list-type-kw (u/normalize-kw list-type)]
      (if (or (nil? entity-kw) (nil? list-type-kw))
        db
        (let [path [:admin :user-settings :draft :table-columns entity-kw list-type-kw]
              db' (assoc-in db path (vec columns))]
          (if (= list-type-kw :available-columns)
            (update-in db' [:admin :user-settings :draft :table-columns entity-kw] cleanup-entity-table-columns)
            db'))))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/toggle-table-column-in-list-draft
  (fn [db [_ entity list-type column-name]]
    (let [entity-kw (u/normalize-kw entity)
          list-type-kw (u/normalize-kw list-type)
          col-str (col->str column-name)
          path [:admin :user-settings :draft :table-columns entity-kw list-type-kw]
          current-cols (vec (or (get-in db path) []))
          col-set (set (map col->str current-cols))]
      (if (or (nil? entity-kw) (nil? list-type-kw))
        db
        (let [new-cols (if (contains? col-set col-str)
                         (vec (remove #{col-str} (map col->str current-cols)))
                         (conj (vec (map col->str current-cols)) col-str))
              db' (assoc-in db path new-cols)]
          (if (= list-type-kw :available-columns)
            (update-in db' [:admin :user-settings :draft :table-columns entity-kw] cleanup-entity-table-columns)
            db'))))))


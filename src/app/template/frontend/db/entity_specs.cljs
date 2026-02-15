(ns app.template.frontend.db.entity-specs
  "Entity specs; keep field configs in sync with backend."
  (:require
    [app.shared.field-specs :as field-specs]
    [app.shared.labels :as labels]
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.db.paths :as paths]
    [clojure.string :as str]
    [re-frame.core :as rf]))

(defn- app-col->db-col
  "Best-effort conversion from app/kebab-case keyword to db/snake_case keyword.

  Used for looking up computed field metadata in table-columns config, where
  keys are often authored in snake_case (e.g. :supplier_display_name)."
  [col-kw]
  (when col-kw
    (-> col-kw name (str/replace "-" "_") keyword)))

(defn- normalize-entity-name
  "Normalize entity identifiers so lookups are consistent.

  - Accepts keywords/strings/symbols.
  - Treats snake_case and kebab-case as equivalent.
  - Returns an app/kebab-case keyword when possible."
  [entity-name]
  (some-> entity-name
    kw/ensure-keyword
    model-naming/db-keyword->app))

(defn- column-key-candidates
  [column-key]
  (let [app-kw (some-> column-key model-naming/ensure-app-keyword)
        app-name (some-> app-kw name)
        db-kw (some-> app-kw app-col->db-col)
        db-name (some-> db-kw name)]
    (->> [column-key
          (when (keyword? column-key) (name column-key))
          (when (string? column-key) (keyword column-key))
          app-kw
          app-name
          db-kw
          db-name]
      (remove nil?)
      distinct
      vec)))

(defn- lookup-column-entry
  [m column-key]
  (let [sentinel ::not-found]
    (some (fn [k]
            (let [v (get m k sentinel)]
              (when-not (= v sentinel) v)))
      (column-key-candidates column-key))))

(defn- resolve-column-label-override
  [table-config column-key]
  (let [label (some-> (lookup-column-entry (:column-metadata table-config) column-key)
                :label
                str
                str/trim)]
    (when (seq label)
      label)))

(defn- apply-column-label-override
  [table-config column-key field-spec]
  (if-let [label (resolve-column-label-override table-config column-key)]
    (assoc field-spec :label label)
    field-spec))

;; Event handler for initializing entity specs
(rf/reg-event-db
  ::initialize-entity-specs
  (fn [db _]
    (let [md (:models-data db)]
      (if md
        (assoc-in db [:entities :specs] (field-specs/entity-specs md))
        db))))

;; Subscription to get all entity specs
(rf/reg-sub
  :entity-specs
  (fn [db _]
    (:specs (:entities db))))

;; Subscription to get specs for a specific entity
(rf/reg-sub
  :entity-specs/by-name
  (fn [db [_ entity-name]]
    (let [entity-kw (normalize-entity-name entity-name)
          specs (get-in db [:entities :specs])
          base-spec (get specs entity-kw)
          base-fields (cond
                        (sequential? base-spec) base-spec
                        (map? base-spec) (vals base-spec)
                        :else [])
          ;; Route-aware table-columns config (admin vs user routes).
          route-name (get-in db (paths/current-route-name))
          admin-route? (and route-name (str/starts-with? (name route-name) "admin"))
          table-config (when entity-kw
                         (if admin-route?
                           (get-in db [:admin :config :table-columns entity-kw])
                           (get-in db [:domain :config :table-columns entity-kw])))
          normalize-col (fn [col] (model-naming/ensure-app-keyword col))
          available-cols (->> (or (:available-columns table-config) [])
                           (keep normalize-col)
                           vec)
          computed-cols (->> (or (:computed-fields table-config) {})
                          keys
                          (keep normalize-col)
                          set)
          field-id->kw (fn [field]
                         (when (map? field)
                           (some-> (:id field) keyword normalize-col)))
          base-by-id (into {}
                       (keep (fn [f]
                               (when-let [k (field-id->kw f)]
                                 [k f])))
                       base-fields)
          computed-meta-for (fn [col-kw]
                              (lookup-column-entry (:computed-fields table-config) col-kw))
          column-config-for (fn [col-kw]
                              (lookup-column-entry (:column-config table-config) col-kw))
          computed-field-spec (fn [col-kw]
                                (let [m (computed-meta-for col-kw)
                                      label (or (resolve-column-label-override table-config col-kw)
                                              (:label m)
                                              (labels/field-name->label col-kw))]
                                  {:id (name col-kw)
                                   :label label
                                   :type (or (:type m) :string)
                                   :admin (merge
                                            {:visible-in-table? true
                                             :filterable? true
                                             :sortable? true}
                                            (:admin m))}))
          ;; Computed field specs should be overridden by real field specs when both exist.
          merged-by-id (merge
                         (into {} (map (fn [k] [k (computed-field-spec k)]) computed-cols))
                         base-by-id)]
      ;; If table-columns provides an explicit order, use it as the canonical
      ;; list-view field order AND filter set (so config/locks/defaults apply
      ;; to the same columns the table renders).
      (if (seq available-cols)
        (mapv (fn [k]
                (let [field-spec (or (get merged-by-id k)
                                   (computed-field-spec k))
                      column-config (column-config-for k)
                      field-spec* (if (map? column-config)
                                    (merge field-spec column-config)
                                    field-spec)]
                  (apply-column-label-override table-config k field-spec*)))
          available-cols)
        ;; Fallback: preserve backend/models-derived field order, and append any
        ;; computed fields not already present.
        (let [base-ids (set (keep field-id->kw base-fields))
              base-fields* (mapv (fn [field]
                                   (if-let [field-id (field-id->kw field)]
                                     (apply-column-label-override table-config field-id field)
                                     field))
                             base-fields)
              missing-computed (remove base-ids computed-cols)]
          (vec (concat base-fields* (map computed-field-spec missing-computed))))))))

(rf/reg-sub
  :form-entity-specs
  (fn [db _]
    (let [md (:models-data db)]
      (when md
        (field-specs/form-entity-specs md)))))

(rf/reg-sub
  :form-entity-specs/by-name
  :<- [:form-entity-specs]
  (fn [specs [_ entity-name]]
    (get specs (normalize-entity-name entity-name))))

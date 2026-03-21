(ns app.template.frontend.db.entity-specs
  "Entity specs; keep field configs in sync with backend."
  (:require
    [app.shared.field-specs :as field-specs]
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.settings.resolver :as resolver]
    [re-frame.core :as rf]))

(defn- normalize-entity-name
  "Normalize entity identifiers so lookups are consistent.

  - Accepts keywords/strings/symbols.
  - Treats snake_case and kebab-case as equivalent.
  - Returns an app/kebab-case keyword when possible."
  [entity-name]
  (some-> entity-name
    kw/ensure-keyword
    model-naming/db-keyword->app))

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
    (let [entity-kw    (normalize-entity-name entity-name)
          specs        (get-in db [:entities :specs])
          base-spec    (get specs entity-kw)
          base-fields  (cond
                         (sequential? base-spec) base-spec
                         (map? base-spec) (vals base-spec)
                         :else [])
          locale       (or (:locale db) :bs)
          ;; Route-aware table-columns config via unified resolver.
          admin-route? (paths/admin-route? db)
          table-config (when entity-kw
                         (resolver/resolve-config-source
                           admin-route?
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
          column-config-for (fn [col-kw]
                              (resolver/lookup-column-entry (:column-config table-config) col-kw))
          ;; Computed field specs should be overridden by real field specs when both exist.
          merged-by-id (merge
                         (into {} (map (fn [k] [k (resolver/computed-field-spec locale table-config k)]) computed-cols))
                         base-by-id)]
      ;; If table-columns provides an explicit order, use it as the canonical
      ;; list-view field order AND filter set (so config/locks/defaults apply
      ;; to the same columns the table renders).
      (if (seq available-cols)
        (mapv (fn [k]
                (let [field-spec     (or (get merged-by-id k) (resolver/computed-field-spec locale table-config k))
                      col-cfg        (column-config-for k)
                      field-spec*    (if (map? col-cfg)
                                       (merge field-spec col-cfg)
                                       field-spec)]
                  (resolver/apply-column-label-override locale table-config k field-spec*)))
          available-cols)
        ;; Fallback: preserve backend/models-derived field order, and append any
        ;; computed fields not already present.
        (let [base-ids         (set (keep field-id->kw base-fields))
              base-fields*     (mapv (fn [field]
                                       (if-let [field-id (field-id->kw field)]
                                         (resolver/apply-column-label-override locale table-config field-id field)
                                         field))
                                 base-fields)
              missing-computed (remove base-ids computed-cols)]
          (vec (concat base-fields* (map #(resolver/computed-field-spec locale table-config %) missing-computed))))))))

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

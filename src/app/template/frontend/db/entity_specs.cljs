(ns app.template.frontend.db.entity-specs
  "Entity specs; keep field configs in sync with backend."
  (:require
    [app.shared.field-specs :as field-specs]
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
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
  :<- [:entity-specs]
  (fn [specs [_ entity-name]]
    (get specs (normalize-entity-name entity-name))))

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

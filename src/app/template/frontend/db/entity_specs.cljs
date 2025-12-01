(ns app.template.frontend.db.entity-specs
  (:require
    [app.shared.field-specs :as field-specs]
    [re-frame.core :as rf]))

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
    (get specs entity-name)))

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
    (get specs entity-name)))

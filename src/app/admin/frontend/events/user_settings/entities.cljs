(ns app.admin.frontend.events.user-settings.entities
  (:require
    [app.admin.frontend.events.user-settings.utils :as u]
    [re-frame.core :as rf]))

;; =============================================================================
;; Draft editing: entities config
;; =============================================================================

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-entity-title-draft
  (fn [db [_ entity new-title]]
    (let [entity-kw (u/normalize-kw entity)]
      (if (nil? entity-kw)
        db
        (assoc-in db [:admin :user-settings :draft :entities entity-kw :title] new-title)))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/reset-entity-draft
  (fn [db [_ entity]]
    (let [entity-kw (u/normalize-kw entity)
          saved-entity (get-in db [:admin :user-settings :saved :entities entity-kw])]
      (if (nil? entity-kw)
        db
        (assoc-in db [:admin :user-settings :draft :entities entity-kw] (u/safe-map saved-entity))))))


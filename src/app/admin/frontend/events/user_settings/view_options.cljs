(ns app.admin.frontend.events.user-settings.view-options
  (:require
    [app.admin.frontend.events.user-settings.utils :as u]
    [app.admin.frontend.utils.view-options-helpers :as vo-helpers]
    [re-frame.core :as rf]))

;; =============================================================================
;; Draft editing: view-options (defaults + locks)
;; =============================================================================

;; new-state schema:
;; - {:kind :inherit}
;; - {:kind :default :value boolean}
;; - {:kind :lock :value boolean}
(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-display-setting-draft
  (fn [db [_ entity setting-key new-state]]
    (let [entity-kw (u/normalize-kw entity)
          setting-kw (u/normalize-kw setting-key)
          kind (:kind new-state)
          value (:value new-state)
          valid-value? (if (= setting-kw :per-page)
                         (and (integer? value) (pos? value))
                         (boolean? value))]
      (if (or (nil? entity-kw) (nil? setting-kw))
        db
        (vo-helpers/apply-display-setting
          db [:admin :user-settings :draft :view-options entity-kw] setting-kw kind value valid-value?)))))

;; =============================================================================
;; Draft editing: column visibility policy (defaults + locks)
;; =============================================================================

;; new-state schema:
;; - {:kind :inherit}
;; - {:kind :default :value boolean}
;; - {:kind :lock :value boolean}
(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-column-visibility-setting-draft
  (fn [db [_ entity column-key new-state]]
    (let [entity-kw (u/normalize-kw entity)
          column-kw (u/normalize-kw column-key)
          kind (:kind new-state)
          value (:value new-state)]
      (if (or (nil? entity-kw) (nil? column-kw))
        db
        (vo-helpers/apply-column-visibility-setting
          db [:admin :user-settings :draft :view-options entity-kw] column-kw kind value)))))

;; =============================================================================
;; Bulk helpers: apply tristate to many display settings / columns
;; =============================================================================

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-display-settings-bulk
  (fn [db [_ entity setting-keys new-state]]
    (let [entity-kw (u/normalize-kw entity)
          ks (->> (or setting-keys []) (keep u/normalize-kw) vec)
          kind (:kind new-state)
          value (:value new-state)]
      (if (or (nil? entity-kw) (empty? ks))
        db
        (vo-helpers/apply-display-settings-bulk
          db [:admin :user-settings :draft :view-options entity-kw] ks kind value)))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/set-column-visibility-bulk
  (fn [db [_ entity column-keys new-state]]
    (let [entity-kw (u/normalize-kw entity)
          cols (->> (or column-keys []) (keep u/normalize-kw) vec)
          kind (:kind new-state)
          value (:value new-state)]
      (if (or (nil? entity-kw) (empty? cols))
        db
        (vo-helpers/apply-column-visibility-bulk
          db [:admin :user-settings :draft :view-options entity-kw] cols kind value)))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/reset-entity-display-draft
  (fn [db [_ entity]]
    (let [entity-kw (u/normalize-kw entity)
          saved-defaults (get-in db [:admin :user-settings :saved :view-options entity-kw :display-defaults])
          saved-locks (get-in db [:admin :user-settings :saved :view-options entity-kw :display-locks])
          saved-col-defaults (get-in db [:admin :user-settings :saved :view-options entity-kw :column-defaults])
          saved-col-locks (get-in db [:admin :user-settings :saved :view-options entity-kw :column-locks])]
      (cond
        (nil? entity-kw)
        db

        :else
        (-> db
          (cond->
            (map? saved-defaults)
            (assoc-in [:admin :user-settings :draft :view-options entity-kw :display-defaults] saved-defaults)

            (not (map? saved-defaults))
            (update-in [:admin :user-settings :draft :view-options entity-kw] dissoc :display-defaults))

          (cond->
            (map? saved-locks)
            (assoc-in [:admin :user-settings :draft :view-options entity-kw :display-locks] saved-locks)

            (not (map? saved-locks))
            (update-in [:admin :user-settings :draft :view-options entity-kw] dissoc :display-locks))

          (cond->
            (map? saved-col-defaults)
            (assoc-in [:admin :user-settings :draft :view-options entity-kw :column-defaults] saved-col-defaults)

            (not (map? saved-col-defaults))
            (update-in [:admin :user-settings :draft :view-options entity-kw] dissoc :column-defaults))

          (cond->
            (map? saved-col-locks)
            (assoc-in [:admin :user-settings :draft :view-options entity-kw :column-locks] saved-col-locks)

            (not (map? saved-col-locks))
            (update-in [:admin :user-settings :draft :view-options entity-kw] dissoc :column-locks)))))))

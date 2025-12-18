(ns app.admin.frontend.events.user-settings.form-fields
  (:require
    [app.admin.frontend.events.user-settings.utils :as u]
    [re-frame.core :as rf]))

;; =============================================================================
;; Draft editing: form-fields config
;; =============================================================================

#_(rf/reg-event-db
    :app.admin.frontend.events.user-settings/set-form-field-list-draft
    (fn [db [_ entity field-type fields]]
      (let [entity-kw (u/normalize-kw entity)
            field-type-kw (u/normalize-kw field-type)]
        (if (or (nil? entity-kw) (nil? field-type-kw))
          db
          (assoc-in db [:admin :user-settings :draft :form-fields entity-kw field-type-kw] (vec fields))))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/toggle-form-field-draft
  (fn [db [_ entity field-type field-name]]
    (let [entity-kw (u/normalize-kw entity)
          field-type-kw (u/normalize-kw field-type)
          field-str (if (keyword? field-name) (name field-name) (str field-name))
          path [:admin :user-settings :draft :form-fields entity-kw field-type-kw]
          current-fields (vec (or (get-in db path) []))
          field-set (set current-fields)]
      (if (or (nil? entity-kw) (nil? field-type-kw))
        db
        (let [new-fields (if (contains? field-set field-str)
                           (vec (remove #{field-str} current-fields))
                           (conj current-fields field-str))]
          (assoc-in db path new-fields))))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/reset-form-fields-draft
  (fn [db [_ entity]]
    (let [entity-kw (u/normalize-kw entity)
          saved-config (get-in db [:admin :user-settings :saved :form-fields entity-kw])]
      (if (nil? entity-kw)
        db
        (assoc-in db [:admin :user-settings :draft :form-fields entity-kw] (u/safe-map saved-config))))))


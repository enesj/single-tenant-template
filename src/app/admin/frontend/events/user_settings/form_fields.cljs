(ns app.admin.frontend.events.user-settings.form-fields
  (:require
    [app.admin.frontend.events.user-settings.utils :as u]
    [app.shared.model-naming :as model-naming]
    [re-frame.core :as rf]))

;; =============================================================================
;; Draft editing: form-fields config
;; =============================================================================

(defn- normalize-form-field-id
  [field-name]
  (some-> field-name model-naming/ensure-app-keyword model-naming/app-keyword->db name))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/toggle-form-field-draft
  (fn [db [_ entity field-type field-name]]
    (let [entity-kw (u/normalize-kw entity)
          field-type-kw (u/normalize-kw field-type)
          field-str (normalize-form-field-id field-name)
          path [:admin :user-settings :draft :form-fields entity-kw field-type-kw]
          current-fields (->> (or (get-in db path) [])
                           (keep normalize-form-field-id)
                           distinct
                           vec)
          field-set (set current-fields)]
      (if (or (nil? entity-kw) (nil? field-type-kw) (nil? field-str))
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


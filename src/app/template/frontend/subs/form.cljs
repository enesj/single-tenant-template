(ns app.template.frontend.subs.form
  (:require
    [app.shared.validation.builder :as validation-builder]
    [app.shared.validation.core :as validation-core]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(rf/reg-sub
  ::form-errors
  (fn [db [_ form-id]]
    (get-in db (paths/form-errors form-id))))

(rf/reg-sub
  ::submitting?
  (fn [db [_ form-id]]
    (get-in db (paths/form-submitting? form-id))))

(rf/reg-sub
  ::form-success
  (fn [db [_ form-id]]
    (get-in db (paths/form-success-all form-id))))

(rf/reg-sub
  ::field-validation-state
  (fn [db [_ entity-type field-id]]
    (let [field-id (keyword field-id)]
      {:error (get-in db (paths/form-field-error entity-type field-id))
       :success (get-in db (paths/form-success entity-type field-id))})))

(rf/reg-sub
  ::field-server-validation-state
  (fn [db [_ entity-type field-id]]
    (let [field-id (keyword field-id)]
      {:error (get-in db (paths/form-server-errors entity-type field-id))
       :success (get-in db (paths/form-success entity-type field-id))})))

(rf/reg-sub
  ::submitted?
  (fn [db [_ form-id]]
    (get-in db (paths/form-submitted? form-id))))

(rf/reg-sub
  ::all-fields-valid?
  (fn [db [_ entity-name entity-spec editing values]]
    (let [fields (map #(keyword (:id %)) entity-spec)
          required-fields (map #(keyword (:id %)) (filter :required entity-spec))
          entity-dirty-fields (get-in db (paths/form-dirty-fields entity-name) #{})
          fields-to-check (if editing
                            (filter #(contains? entity-dirty-fields %) fields)
                            required-fields)
          models-data (:models-data db)
          validators (when models-data (try (validation-builder/create-enhanced-validators models-data)
                                         (catch :default _ nil)))
          validation-results (map (fn [field-key]
                                    (let [value (get values field-key)
                                          validation-spec (when validators (get-in validators [entity-name field-key]))
                                          result (try
                                                   (validation-core/validation-result validation-spec value)
                                                   (catch :default _
                                                     {:valid? (if (string? value)
                                                                (not (empty? value))
                                                                (some? value))}))]
                                      (:valid? result)))
                               fields-to-check)]
      (every? true? validation-results))))

(rf/reg-sub
  ::dirty-fields
  (fn [db [_ entity-name]]
    (get-in db (paths/form-dirty-fields entity-name) #{})))

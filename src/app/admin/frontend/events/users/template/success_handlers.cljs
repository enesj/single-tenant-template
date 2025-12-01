(ns app.admin.frontend.events.users.template.success-handlers
  (:require
    [app.frontend.utils.state :as state-utils]
    [re-frame.core :as rf]))

;; =============================================================================
;; Success Handlers (Admin)
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.forms/admin-create-success
  (fn [{:keys [db]} [_ entity-type response]]
    (let [db' (state-utils/handle-entity-api-success
                db
                entity-type
                response
                {:admin-key :admin/created-entity
                 :loading-key :admin/form-submitting?})]
      {:db db'
       :dispatch-n (list
                     [:admin/hide-form-modal]
                     (when (= entity-type :users)
                       [:admin/fetch-entities :users]))})))

(rf/reg-event-fx
  :app.admin.frontend.forms/admin-update-success
  (fn [{:keys [db]} [_ entity-type response]]
    (let [db' (state-utils/handle-entity-api-success
                db
                entity-type
                response
                {:admin-key :admin/updated-entity
                 :loading-key :admin/form-submitting?})]
      {:db db'
       :dispatch-n (list
                     [:admin/hide-form-modal]
                     (when (= entity-type :users)
                       [:admin/fetch-entities :users]))})))

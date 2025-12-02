(ns app.admin.frontend.events.users.template.success-handlers
  "DEPRECATED: Admin form success handlers.
   
   These handlers are no longer used since admin form submissions now
   route through the bridge CRUD system (see form_interceptors.cljs).
   The bridge system handles success tracking via the shared crud/success module.
   
   This namespace is kept for reference but may be removed in a future cleanup."
  (:require
    [app.frontend.utils.state :as state-utils]
    [app.shared.frontend.crud.success :as crud-success]
    [re-frame.core :as rf]))

;; =============================================================================
;; DEPRECATED Success Handlers (Admin)
;; These events are no longer dispatched - see form_interceptors.cljs for new flow
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.forms/admin-create-success
  (fn [{:keys [db]} [_ entity-type response]]
    (let [entity-id (crud-success/extract-entity-id response)
          db' (-> (state-utils/handle-entity-api-success
                    db
                    entity-type
                    response
                    {:admin-key :admin/created-entity
                     :loading-key :admin/form-submitting?})
                (crud-success/track-recently-created entity-type entity-id))]
      {:db db'
       :dispatch-n (list
                     [:admin/hide-form-modal]
                     (when (= entity-type :users)
                       [:admin/fetch-entities :users]))})))

(rf/reg-event-fx
  :app.admin.frontend.forms/admin-update-success
  (fn [{:keys [db]} [_ entity-type response]]
    (let [entity-id (crud-success/extract-entity-id response)
          db' (-> (state-utils/handle-entity-api-success
                    db
                    entity-type
                    response
                    {:admin-key :admin/updated-entity
                     :loading-key :admin/form-submitting?})
                (crud-success/track-recently-updated entity-type entity-id))]
      {:db db'
       :dispatch-n (list
                     [:admin/hide-form-modal]
                     (when (= entity-type :users)
                       [:admin/fetch-entities :users]))})))

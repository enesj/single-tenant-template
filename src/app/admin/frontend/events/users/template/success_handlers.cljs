(ns app.admin.frontend.events.users.template.success-handlers
  (:require
    [app.frontend.utils.state :as state-utils]
    [re-frame.core :as rf]))

;; =============================================================================
;; Helper Functions
;; =============================================================================

(defn- extract-entity-id
  "Extract entity ID from response, handling both simple :id and namespaced keys."
  [response]
  (or (:id response)
    ;; Find any keyword with local name "id" (e.g., :users/id)
    (->> response
      (filter (fn [[k _]] (and (keyword? k) (= (name k) "id"))))
      first
      second)))

;; =============================================================================
;; Success Handlers (Admin)
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.forms/admin-create-success
  (fn [{:keys [db]} [_ entity-type response]]
    (let [entity-id (extract-entity-id response)
          current-created-ids (get-in db [:ui :recently-created entity-type])
          new-created-ids (conj (or current-created-ids #{}) entity-id)
          db' (-> (state-utils/handle-entity-api-success
                    db
                    entity-type
                    response
                    {:admin-key :admin/created-entity
                     :loading-key :admin/form-submitting?})
                ;; Track recently created entity for highlighting
                (assoc-in [:ui :recently-created entity-type] new-created-ids))]
      (js/console.log "[ADMIN] admin-create-success: entity-id:" (pr-str entity-id) "recently-created:" (pr-str new-created-ids))
      {:db db'
       :dispatch-n (list
                     [:admin/hide-form-modal]
                     (when (= entity-type :users)
                       [:admin/fetch-entities :users]))})))

(rf/reg-event-fx
  :app.admin.frontend.forms/admin-update-success
  (fn [{:keys [db]} [_ entity-type response]]
    (let [entity-id (extract-entity-id response)
          current-updated-ids (get-in db [:ui :recently-updated entity-type])
          new-updated-ids (conj (or current-updated-ids #{}) entity-id)
          db' (-> (state-utils/handle-entity-api-success
                    db
                    entity-type
                    response
                    {:admin-key :admin/updated-entity
                     :loading-key :admin/form-submitting?})
                ;; Track recently updated entity for highlighting
                (assoc-in [:ui :recently-updated entity-type] new-updated-ids))]
      (js/console.log "[ADMIN] admin-update-success: entity-id:" (pr-str entity-id) "recently-updated:" (pr-str new-updated-ids))
      {:db db'
       :dispatch-n (list
                     [:admin/hide-form-modal]
                     (when (= entity-type :users)
                       [:admin/fetch-entities :users]))})))

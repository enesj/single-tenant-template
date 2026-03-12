(ns app.admin.frontend.events.admins
  "Admin account management events"
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.utils.state :as state-utils]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Utility Helpers
;; ============================================================================

(defn- log-admin-operation [message & [data]]
  (log/info "🛡️ Admin operation:" message (or data "")))

(defn- create-loading-db-state [db loading-key]
  (assoc db loading-key true))

(defn- clear-loading-db-state [db loading-key]
  (assoc db loading-key false))

(defn- handle-admin-api-error [db error loading-key error-key operation]
  (log/error "🛡️ Admin API error during" operation error)
  (-> db
    (clear-loading-db-state loading-key)
    (assoc error-key (admin-http/extract-error-message error))))

;; ============================================================================
;; Load Admins Events
;; ============================================================================

(rf/reg-event-fx
  :admin/load-admins
  (fn [{:keys [db]} [_ params]]
    (log-admin-operation "Loading admins with params" params)
    {:db (state-utils/start-api-request db {:loading-key :admin/admins-loading?
                                            :error-key :admin/admins-error
                                            :entity-type :admins})
     :http-xhrio (admin-http/admin-get
                   {:uri "/admin/api/admins"
                    :params (or params {})
                    :on-success [:admin/load-admins-success]
                    :on-failure [:admin/load-admins-failure]})}))

(rf/reg-event-fx
  :admin/load-admins-success
  (fn [{:keys [db]} [_ response]]
    (log-admin-operation "Admins loaded successfully" {:count (count (:admins response))})
    (state-utils/handle-entity-api-success
      db
      :admins
      response
      {:loading-key :admin/admins-loading?
       :admin-key :admin/admins
       :sync-event [:app.admin.frontend.adapters.admins/sync-admins-to-template]})))

(rf/reg-event-db
  :admin/load-admins-failure
  (fn [db [_ error]]
    (handle-admin-api-error db error :admin/admins-loading? :admin/admins-error "load admins")
    (state-utils/handle-api-error db {:loading-key :admin/admins-loading?
                                      :error-key :admin/admins-error
                                      :entity-type :admins
                                      :error-message error})))

;; ============================================================================
;; View Admin Details Events
;; ============================================================================

(rf/reg-event-fx
  :admin/view-admin-details
  (fn [{:keys [db]} [_ admin-id]]
    (log-admin-operation "Loading admin details for" admin-id)
    {:db (-> (create-loading-db-state db :admin/loading-admin-details)
           (assoc :admin/admin-details-modal-open? true)
           (dissoc :admin/admin-details-error)
           (assoc :admin/current-admin-details nil))
     :http-xhrio (admin-http/admin-get
                   {:uri (str "/admin/api/admins/" admin-id)
                    :on-success [:admin/view-admin-details-success]
                    :on-failure [:admin/view-admin-details-failure]})}))

(rf/reg-event-db
  :admin/view-admin-details-success
  (fn [db [_ response]]
    (log-admin-operation "Admin details loaded successfully" (:admin response))
    (-> (clear-loading-db-state db :admin/loading-admin-details)
      (assoc :admin/admin-details-modal-open? true)
      (assoc :admin/current-admin-details (:admin response)))))

(rf/reg-event-db
  :admin/view-admin-details-failure
  (fn [db [_ error]]
    (let [db' (handle-admin-api-error db error :admin/loading-admin-details :admin/admin-details-error "load admin details")]
      (assoc db' :admin/admin-details-modal-open? true))))

;; ============================================================================
;; Update Admin Role Events
;; ============================================================================

(rf/reg-event-fx
  :admin/update-admin-role
  (fn [{:keys [db]} [_ admin-id new-role]]
    (log-admin-operation "Updating admin role" {:admin-id admin-id :new-role new-role})
    {:db (create-loading-db-state db :admin/updating-admin)
     :http-xhrio (admin-http/admin-request
                   {:method :put
                    :uri (str "/admin/api/admins/" admin-id "/role")
                    :params {:role new-role}
                    :on-success [:admin/update-admin-role-success admin-id]
                    :on-failure [:admin/update-admin-role-failure admin-id]})}))

(rf/reg-event-fx
  :admin/update-admin-role-success
  (fn [{:keys [db]} [_ admin-id _response]]
    (log-admin-operation "Admin role updated successfully" admin-id)
    {:db (clear-loading-db-state db :admin/updating-admin)
     :dispatch-n [[:admin/load-admins]
                  [:admin/show-success-message "Admin role updated successfully"]]}))

(rf/reg-event-fx
  :admin/update-admin-role-failure
  (fn [{:keys [db]} [_ admin-id error]]
    (log/error "Failed to update admin role" {:admin-id admin-id :error error})
    (let [message (or (admin-http/extract-error-message error) "Failed to update admin role")]
      {:db (handle-admin-api-error db error :admin/updating-admin :admin/admin-update-error "update admin role")
       :dispatch [:admin/show-error-message message]})))

(rf/reg-event-fx
  :admin/transfer-admin-ownership
  (fn [{:keys [db]} [_ admin-id]]
    (log-admin-operation "Transferring admin ownership" {:admin-id admin-id})
    {:db (create-loading-db-state db :admin/updating-admin)
     :http-xhrio (admin-http/admin-request
                   {:method :post
                    :uri (str "/admin/api/admins/" admin-id "/transfer-ownership")
                    :params {}
                    :on-success [:admin/transfer-admin-ownership-success admin-id]
                    :on-failure [:admin/transfer-admin-ownership-failure admin-id]})}))

(rf/reg-event-fx
  :admin/transfer-admin-ownership-success
  (fn [{:keys [db]} [_ admin-id _response]]
    (log-admin-operation "Admin ownership transferred successfully" admin-id)
    {:db (clear-loading-db-state db :admin/updating-admin)
     :dispatch-n [[:admin/show-success-message "Ownership transferred successfully"]
                  [:admin/load-admins]
                  [:admin/check-auth]
                  [:admin/navigate-client "/admin/dashboard"]]}))

(rf/reg-event-fx
  :admin/transfer-admin-ownership-failure
  (fn [{:keys [db]} [_ admin-id error]]
    (log/error "Failed to transfer admin ownership" {:admin-id admin-id :error error})
    (let [message (or (admin-http/extract-error-message error) "Failed to transfer ownership")]
      {:db (handle-admin-api-error db error :admin/updating-admin :admin/admin-update-error "transfer admin ownership")
       :dispatch [:admin/show-error-message message]})))

;; ============================================================================
;; Update Admin Status Events
;; ============================================================================

(rf/reg-event-fx
  :admin/update-admin-status
  (fn [{:keys [db]} [_ admin-id new-status]]
    (log-admin-operation "Updating admin status" {:admin-id admin-id :new-status new-status})
    {:db (create-loading-db-state db :admin/updating-admin)
     :http-xhrio (admin-http/admin-request
                   {:method :put
                    :uri (str "/admin/api/admins/" admin-id "/status")
                    :params {:status new-status}
                    :on-success [:admin/update-admin-status-success admin-id]
                    :on-failure [:admin/update-admin-status-failure admin-id]})}))

(rf/reg-event-fx
  :admin/update-admin-status-success
  (fn [{:keys [db]} [_ admin-id _response]]
    (log-admin-operation "Admin status updated successfully" admin-id)
    {:db (clear-loading-db-state db :admin/updating-admin)
     :dispatch-n [[:admin/load-admins]
                  [:admin/show-success-message "Admin status updated successfully"]]}))

(rf/reg-event-fx
  :admin/update-admin-status-failure
  (fn [{:keys [db]} [_ admin-id error]]
    (log/error "Failed to update admin status" {:admin-id admin-id :error error})
    (let [message (or (admin-http/extract-error-message error) "Failed to update admin status")]
      {:db (handle-admin-api-error db error :admin/updating-admin :admin/admin-update-error "update admin status")
       :dispatch [:admin/show-error-message message]})))

;; ============================================================================
;; Delete Admin Events
;; ============================================================================

(rf/reg-event-fx
  :admin/delete-admin
  (fn [{:keys [db]} [_ admin-id]]
    (log-admin-operation "Deleting admin" admin-id)
    {:db (-> (create-loading-db-state db :admin/updating-admin)
           (dissoc :admin/admin-update-error))
     :http-xhrio (admin-http/admin-request
                   {:method :delete
                    :uri "/admin/api/admins/batch"
                    :params {:ids [(str admin-id)]}
                    :on-success [:admin/delete-admin-success admin-id]
                    :on-failure [:admin/delete-admin-failure admin-id]})}))

(rf/reg-event-fx
  :admin/delete-admin-success
  (fn [{:keys [db]} [_ admin-id _response]]
    (log-admin-operation "Admin deleted successfully" admin-id)
    (let [db' (-> (clear-loading-db-state db :admin/updating-admin)
                (update :admin/admins
                  (fn [admins]
                    (->> admins
                      (remove (fn [a]
                                (= (or (:admins/id a) (:id a)) admin-id)))
                      vec)))
                (update-in (paths/entity-data :admins)
                  (fn [data] (dissoc (or data {}) admin-id)))
                (update-in (paths/entity-ids :admins)
                  (fn [ids] (->> (or ids [])
                              (remove #(= % admin-id))
                              vec)))
                (update-in (paths/entity-selected-ids :admins)
                  (fn [selected] (when selected (disj selected admin-id))))
                (dissoc :admin/admin-update-error :admin/error-message))]
      {:db db'
       :dispatch-n [[:admin/hide-delete-confirmation]
                    [:admin/show-success-message "Admin deleted successfully"]
                    [:admin/load-admins]]})))

(rf/reg-event-fx
  :admin/delete-admin-failure
  (fn [{:keys [db]} [_ admin-id error]]
    (log/error "Failed to delete admin" {:admin-id admin-id :error error})
    (let [db' (handle-admin-api-error db error :admin/updating-admin :admin/admin-update-error "delete admin")
          message (or (admin-http/extract-error-message error) "Failed to delete admin")]
      {:db db'
       :dispatch [:admin/show-error-message message]})))

;; ============================================================================
;; Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/admins
  (fn [db _]
    (:admin/admins db [])))

(rf/reg-sub
  :admin/admins-loading?
  (fn [db _]
    (:admin/admins-loading? db false)))

(rf/reg-sub
  :admin/admins-error
  (fn [db _]
    (:admin/admins-error db nil)))

(rf/reg-sub
  :admin/current-admin-details
  (fn [db _]
    (:admin/current-admin-details db nil)))

(rf/reg-sub
  :admin/admin-details-modal-open?
  (fn [db _]
    (:admin/admin-details-modal-open? db false)))

(rf/reg-sub
  :admin/updating-admin?
  (fn [db _]
    (:admin/updating-admin db false)))

;; ============================================================================
;; Fetch Entities Integration (for template system)
;; ============================================================================



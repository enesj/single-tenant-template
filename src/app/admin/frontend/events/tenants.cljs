(ns app.admin.frontend.events.tenants
  "Admin tenant management events — list, detail, members, role changes."
  (:require
    [app.template.frontend.api.http :as http]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- admin-token []
  (try (:admin/token @rf-db/app-db) (catch :default _ nil)))

(defn- admin-request
  "Build an admin API request with admin auth token."
  [opts]
  (let [token (admin-token)]
    (http/api-request
      (cond-> opts
        token (assoc :headers {"x-admin-token" token})))))

;; ============================================================================
;; Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/tenants
  (fn [db _]
    (get-in db [:admin :tenants :data])))

(rf/reg-sub
  :admin/tenants-total
  (fn [db _]
    (get-in db [:admin :tenants :total] 0)))

(rf/reg-sub
  :admin/tenants-loading?
  (fn [db _]
    (get-in db [:admin :tenants :loading?] false)))

(rf/reg-sub
  :admin/tenants-error
  (fn [db _]
    (get-in db [:admin :tenants :error])))

(rf/reg-sub
  :admin/tenants-search
  (fn [db _]
    (get-in db [:admin :tenants :search] "")))

(rf/reg-sub
  :admin/tenants-status-filter
  (fn [db _]
    (get-in db [:admin :tenants :status-filter])))

(rf/reg-sub
  :admin/tenant-detail
  (fn [db _]
    (get-in db [:admin :tenants :detail])))

(rf/reg-sub
  :admin/tenant-members
  (fn [db _]
    (get-in db [:admin :tenants :members])))

(rf/reg-sub
  :admin/tenant-members-loading?
  (fn [db _]
    (get-in db [:admin :tenants :members-loading?] false)))

;; ============================================================================
;; Fetch Tenants
;; ============================================================================

(rf/reg-event-fx
  ::fetch-tenants
  (fn [{:keys [db]} [_ params]]
    (let [search (or (:search params) (get-in db [:admin :tenants :search] ""))
          status (or (:status params) (get-in db [:admin :tenants :status-filter]))
          limit  (or (:limit params) 25)
          offset (or (:offset params) 0)]
      {:db (assoc-in db [:admin :tenants :loading?] true)
       :http-xhrio (admin-request
                     {:method :get
                      :uri "/admin/api/tenants"
                      :params (cond-> {:limit limit :offset offset}
                                (seq search) (assoc :search search)
                                status (assoc :status status))
                      :on-success [::fetch-tenants-success]
                      :on-failure [::fetch-tenants-failure]})})))

(rf/reg-event-db
  ::fetch-tenants-success
  (fn [db [_ response]]
    (-> db
      (assoc-in [:admin :tenants :loading?] false)
      (assoc-in [:admin :tenants :data] (:tenants response))
      (assoc-in [:admin :tenants :total] (:total response)))))

(rf/reg-event-db
  ::fetch-tenants-failure
  (fn [db [_ response]]
    (log/error "Failed to fetch tenants:" response)
    (-> db
      (assoc-in [:admin :tenants :loading?] false)
      (assoc-in [:admin :tenants :error] "Failed to fetch tenants"))))

;; ============================================================================
;; Search & Filter
;; ============================================================================

(rf/reg-event-fx
  ::set-search
  (fn [{:keys [db]} [_ search]]
    {:db (assoc-in db [:admin :tenants :search] search)
     :fx [[:dispatch [::fetch-tenants {:search search :offset 0}]]]}))

(rf/reg-event-fx
  ::set-status-filter
  (fn [{:keys [db]} [_ status]]
    {:db (assoc-in db [:admin :tenants :status-filter] status)
     :fx [[:dispatch [::fetch-tenants {:status status :offset 0}]]]}))

;; ============================================================================
;; Tenant Detail
;; ============================================================================

(rf/reg-event-fx
  ::fetch-tenant-detail
  (fn [{:keys [db]} [_ tenant-id]]
    {:db (-> db
           (assoc-in [:admin :tenants :loading?] true)
           (assoc-in [:admin :tenants :detail] nil))
     :http-xhrio (admin-request
                   {:method :get
                    :uri (str "/admin/api/tenants/" tenant-id)
                    :on-success [::fetch-tenant-detail-success]
                    :on-failure [::fetch-tenant-detail-failure]})}))

(rf/reg-event-db
  ::fetch-tenant-detail-success
  (fn [db [_ response]]
    (-> db
      (assoc-in [:admin :tenants :loading?] false)
      (assoc-in [:admin :tenants :detail] (:tenant response)))))

(rf/reg-event-db
  ::fetch-tenant-detail-failure
  (fn [db [_ response]]
    (log/error "Failed to fetch tenant detail:" response)
    (-> db
      (assoc-in [:admin :tenants :loading?] false)
      (assoc-in [:admin :tenants :error] "Failed to fetch tenant details"))))

;; ============================================================================
;; Tenant Members
;; ============================================================================

(rf/reg-event-fx
  ::fetch-tenant-members
  (fn [{:keys [db]} [_ tenant-id]]
    {:db (assoc-in db [:admin :tenants :members-loading?] true)
     :http-xhrio (admin-request
                   {:method :get
                    :uri (str "/admin/api/tenants/" tenant-id "/members")
                    :on-success [::fetch-tenant-members-success]
                    :on-failure [::fetch-tenant-members-failure]})}))

(rf/reg-event-db
  ::fetch-tenant-members-success
  (fn [db [_ response]]
    (-> db
      (assoc-in [:admin :tenants :members-loading?] false)
      (assoc-in [:admin :tenants :members] (:members response)))))

(rf/reg-event-db
  ::fetch-tenant-members-failure
  (fn [db [_ response]]
    (log/error "Failed to fetch tenant members:" response)
    (-> db
      (assoc-in [:admin :tenants :members-loading?] false)
      (assoc-in [:admin :tenants :error] "Failed to fetch tenant members"))))

;; ============================================================================
;; Change Member Role (admin superpower)
;; ============================================================================

(rf/reg-event-fx
  ::change-member-role
  (fn [{:keys [db]} [_ {:keys [tenant-id member-id role]}]]
    {:db (assoc-in db [:admin :tenants :error] nil)
     :http-xhrio (admin-request
                   {:method :put
                    :uri (str "/admin/api/tenants/" tenant-id "/members/" member-id "/role")
                    :params {:role role}
                    :on-success [::change-member-role-success tenant-id]
                    :on-failure [::change-member-role-failure]})}))

(rf/reg-event-fx
  ::change-member-role-success
  (fn [_cofx [_ tenant-id _response]]
    {:fx [[:dispatch [::fetch-tenant-members tenant-id]]]}))

(rf/reg-event-db
  ::change-member-role-failure
  (fn [db [_ response]]
    (log/error "Failed to change member role:" response)
    (assoc-in db [:admin :tenants :error]
              (or (get-in response [:response :message])
                "Failed to change member role"))))

;; ============================================================================
;; Remove Member (admin superpower)
;; ============================================================================

(rf/reg-event-fx
  ::remove-member
  (fn [{:keys [db]} [_ {:keys [tenant-id member-id]}]]
    {:db (assoc-in db [:admin :tenants :error] nil)
     :http-xhrio (admin-request
                   {:method :delete
                    :uri (str "/admin/api/tenants/" tenant-id "/members/" member-id)
                    :on-success [::remove-member-success tenant-id]
                    :on-failure [::remove-member-failure]})}))

(rf/reg-event-fx
  ::remove-member-success
  (fn [_cofx [_ tenant-id _response]]
    {:fx [[:dispatch [::fetch-tenant-members tenant-id]]]}))

(rf/reg-event-db
  ::remove-member-failure
  (fn [db [_ response]]
    (log/error "Failed to remove member:" response)
    (assoc-in db [:admin :tenants :error]
              (or (get-in response [:response :message])
                "Failed to remove member"))))

;; ============================================================================
;; Clear Detail (when navigating back to list)
;; ============================================================================

(rf/reg-event-db
  ::clear-detail
  (fn [db _]
    (-> db
      (update-in [:admin :tenants] dissoc :detail :members :error))))

(comment
  ;; (require 'app.admin.frontend.events.tenants :reload)
  :rcf)

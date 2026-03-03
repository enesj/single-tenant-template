(ns app.admin.frontend.events.tenants
  "Admin tenant management events — list, detail, members, role changes."
  (:require
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.ui-state :as list-ui-state]
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

(def ^:private tenants-entity-key :tenants)

(defn- current-list-pagination
  [db]
  (let [ui-state (get-in db (paths/list-ui-state tenants-entity-key))
        per-page (or (:per-page ui-state)
                   (get-in ui-state [:pagination :per-page])
                   25)
        current-page (or (:current-page ui-state)
                       (get-in ui-state [:pagination :current-page])
                       1)]
    {:limit per-page
     :offset (* (max 0 (dec current-page)) per-page)}))

(defn- tenants->entity-state
  [tenants]
  (let [normalized (->> (or tenants [])
                     (keep (fn [tenant]
                             (when-let [tenant-id (or (:id tenant) (:tenants/id tenant))]
                               [(str tenant-id) tenant]))))]
    {:ids (mapv first normalized)
     :entities-by-id (into {} normalized)}))

(rf/reg-event-fx
  ::load-list
  (fn [_cofx _]
    {:dispatch [::fetch-tenants {}]}))

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
    (let [{:keys [limit offset]} (merge (current-list-pagination db) params)
          search (if (contains? params :search)
                   (:search params)
                   (get-in db [:admin :tenants :search] ""))
          status (if (contains? params :status)
                   (:status params)
                   (get-in db [:admin :tenants :status-filter]))]
      {:db (-> db
             (assoc-in [:admin :tenants :loading?] true)
             (assoc-in [:admin :tenants :error] nil)
             (assoc-in (paths/entity-loading? tenants-entity-key) true)
             (assoc-in (paths/entity-error tenants-entity-key) nil))
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
    (let [tenants (vec (or (:tenants response) []))
          total (or (:total response) (count tenants))
          {:keys [ids entities-by-id]} (tenants->entity-state tenants)]
      (-> db
        (assoc-in [:admin :tenants :loading?] false)
        (assoc-in [:admin :tenants :error] nil)
        (assoc-in [:admin :tenants :data] tenants)
        (assoc-in [:admin :tenants :total] total)
        (assoc-in (paths/entity-loading? tenants-entity-key) false)
        (assoc-in (paths/entity-error tenants-entity-key) nil)
        (assoc-in (paths/entity-data tenants-entity-key) entities-by-id)
        (assoc-in (paths/entity-ids tenants-entity-key) ids)
        (assoc-in (paths/list-total-items tenants-entity-key) total)))))

(rf/reg-event-db
  ::fetch-tenants-failure
  (fn [db [_ response]]
    (log/error "Failed to fetch tenants:" response)
    (let [error-message "Failed to fetch tenants"]
      (-> db
        (assoc-in [:admin :tenants :loading?] false)
        (assoc-in [:admin :tenants :error] error-message)
        (assoc-in (paths/entity-loading? tenants-entity-key) false)
        (assoc-in (paths/entity-error tenants-entity-key) error-message)))))

;; ============================================================================
;; Search & Filter
;; ============================================================================

(rf/reg-event-fx
  ::set-search
  (fn [{:keys [db]} [_ search]]
    {:db (assoc-in db [:admin :tenants :search] search)
     :fx [[:dispatch [::list-ui-state/set-current-page tenants-entity-key 1]]]}))

(rf/reg-event-fx
  ::set-status-filter
  (fn [{:keys [db]} [_ status]]
    {:db (assoc-in db [:admin :tenants :status-filter] status)
     :fx [[:dispatch [::list-ui-state/set-current-page tenants-entity-key 1]]]}))

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

(ns app.admin.frontend.events.tenants-test
  (:require
    [app.admin.frontend.events.tenants :as tenants]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

;; ============================================================================
;; Test Infrastructure
;; ============================================================================

(defonce captured-http-requests (atom []))

(defn- install-fx-stubs! []
  (rf/reg-fx :http-xhrio (fn [req]
                           (swap! captured-http-requests conj req)
                           nil))
  (rf/reg-fx :dispatch-later (fn [_] nil)))

(defn- reset-db! []
  (install-fx-stubs!)
  (reset! captured-http-requests [])
  (reset! rf-db/app-db
    {:admin/token "test-admin-token"
     :admin/authenticated? true
     :admin {:tenants {:data nil
                       :total 0
                       :loading? false
                       :error nil
                       :search ""
                       :status-filter nil}}}))

(defn- last-request [] (last @captured-http-requests))

(defn- req-method [req] (or (:method req) :unknown))
(defn- req-uri [req] (:uri req))
(defn- req-params [req] (:params req))
(defn- req-headers [req] (:headers req))

;; ============================================================================
;; Fetch Tenants
;; ============================================================================

(deftest fetch-tenants-dispatches-get
  (testing "fetch-tenants sends GET to /admin/api/tenants"
    (reset-db!)
    (rf/dispatch-sync [::tenants/fetch-tenants])
    (let [req (last-request)]
      (is (= :get (req-method req)))
      (is (= "/admin/api/tenants" (req-uri req))))))

(deftest fetch-tenants-includes-admin-token
  (testing "fetch-tenants includes admin token header"
    (reset-db!)
    (rf/dispatch-sync [::tenants/fetch-tenants])
    (let [req (last-request)]
      (is (= "test-admin-token" (get (req-headers req) "x-admin-token"))))))

(deftest fetch-tenants-passes-search-params
  (testing "fetch-tenants passes search and pagination params"
    (reset-db!)
    (rf/dispatch-sync [::tenants/fetch-tenants {:search "acme" :limit 10 :offset 20}])
    (let [req (last-request)
          params (req-params req)]
      (is (= "acme" (:search params)))
      (is (= 10 (:limit params)))
      (is (= 20 (:offset params))))))

(deftest fetch-tenants-reads-current-list-pagination
  (testing "fetch-tenants reads pagination and filters from list/admin state"
    (reset-db!)
    (swap! rf-db/app-db assoc-in (paths/list-ui-state :tenants)
      {:per-page 10
       :current-page 3
       :pagination {:per-page 10
                    :current-page 3}})
    (swap! rf-db/app-db assoc-in [:admin :tenants :search] "acme")
    (swap! rf-db/app-db assoc-in [:admin :tenants :status-filter] "active")
    (rf/dispatch-sync [::tenants/fetch-tenants {}])
    (let [req (last-request)
          params (req-params req)]
      (is (= 10 (:limit params)))
      (is (= 20 (:offset params)))
      (is (= "acme" (:search params)))
      (is (= "active" (:status params))))))

(deftest fetch-tenants-success-stores-data
  (testing "fetch-tenants-success stores tenants in admin and canonical list state"
    (reset-db!)
    (rf/dispatch-sync [::tenants/fetch-tenants])
    (let [req (last-request)
          on-success (:on-success req)
          tenant {:id "t1" :name "Acme Corp" :slug "acme"}]
      (rf/dispatch-sync (conj on-success {:tenants [tenant]
                                          :total 1})))
    (let [db @rf-db/app-db]
      (is (= ["t1"] (mapv :id (get-in db [:admin :tenants :data]))))
      (is (= 1 (get-in db [:admin :tenants :total])))
      (is (= ["t1"] (get-in db (paths/entity-ids :tenants))))
      (is (= {"t1" {:id "t1" :name "Acme Corp" :slug "acme"}}
            (get-in db (paths/entity-data :tenants))))
      (is (= 1 (get-in db (paths/list-total-items :tenants))))
      (is (false? (get-in db [:admin :tenants :loading?]))))))

;; ============================================================================
;; Tenant Detail
;; ============================================================================

(deftest fetch-tenant-detail-dispatches-get
  (testing "fetch-tenant-detail sends GET to /admin/api/tenants/:id"
    (reset-db!)
    (rf/dispatch-sync [::tenants/fetch-tenant-detail "tenant-abc"])
    (let [req (last-request)]
      (is (= :get (req-method req)))
      (is (= "/admin/api/tenants/tenant-abc" (req-uri req))))))

;; ============================================================================
;; Tenant Members
;; ============================================================================

(deftest fetch-tenant-members-dispatches-get
  (testing "fetch-tenant-members sends GET to /admin/api/tenants/:id/members"
    (reset-db!)
    (rf/dispatch-sync [::tenants/fetch-tenant-members "tenant-abc"])
    (let [req (last-request)]
      (is (= :get (req-method req)))
      (is (= "/admin/api/tenants/tenant-abc/members" (req-uri req))))))

;; ============================================================================
;; Change Member Role
;; ============================================================================

(deftest change-member-role-dispatches-put
  (testing "change-member-role sends PUT with role"
    (reset-db!)
    (rf/dispatch-sync [::tenants/change-member-role
                       {:tenant-id "t1" :member-id "m1" :role "admin"}])
    (let [req (last-request)]
      (is (= :put (req-method req)))
      (is (= "/admin/api/tenants/t1/members/m1/role" (req-uri req)))
      (is (= "admin" (:role (req-params req)))))))

;; ============================================================================
;; Remove Member
;; ============================================================================

(deftest remove-member-dispatches-delete
  (testing "remove-member sends DELETE to /admin/api/tenants/:id/members/:mid"
    (reset-db!)
    (rf/dispatch-sync [::tenants/remove-member {:tenant-id "t1" :member-id "m1"}])
    (let [req (last-request)]
      (is (= :delete (req-method req)))
      (is (= "/admin/api/tenants/t1/members/m1" (req-uri req))))))

;; ============================================================================
;; Clear Detail
;; ============================================================================

(deftest clear-detail-removes-detail-and-members
  (testing "clear-detail dissocs detail, members, and error"
    (reset-db!)
    (swap! rf-db/app-db assoc-in [:admin :tenants :detail] {:id "t1"})
    (swap! rf-db/app-db assoc-in [:admin :tenants :members] [{:id "m1"}])
    (swap! rf-db/app-db assoc-in [:admin :tenants :error] "some error")
    (rf/dispatch-sync [::tenants/clear-detail])
    (let [db @rf-db/app-db]
      (is (nil? (get-in db [:admin :tenants :detail])))
      (is (nil? (get-in db [:admin :tenants :members])))
      (is (nil? (get-in db [:admin :tenants :error]))))))

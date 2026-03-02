(ns app.template.frontend.events.tenant-test
  (:require
    [app.template.frontend.events.tenant :as tenant]
    [app.template.frontend.helpers-test :as helpers]
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
    (merge helpers/valid-test-db-state
      {:tenant {:memberships nil
                :members nil
                :loading? false
                :error nil
                :success-message nil}
       :session {:tenant {:id "tenant-1" :name "Test Workspace" :slug "test-ws"}
                 :membership-role "owner"}})))

(defn- last-request [] (last @captured-http-requests))

(defn- req-method [req] (or (:method req) :unknown))
(defn- req-uri [req] (:uri req))
(defn- req-params [req] (:params req))

;; ============================================================================
;; Switch Tenant
;; ============================================================================

(deftest switch-tenant-dispatches-post
  (testing "switch-tenant sends POST to /api/v1/tenant/switch"
    (reset-db!)
    (rf/dispatch-sync [::tenant/switch-tenant {:tenant-id "tenant-2"}])
    (let [req (last-request)]
      (is (= :post (req-method req)))
      (is (= "/api/v1/tenant/switch" (req-uri req)))
      (is (= "tenant-2" (:tenant-id (req-params req)))))))

(deftest switch-tenant-sets-loading
  (testing "switch-tenant sets loading? to true"
    (reset-db!)
    (rf/dispatch-sync [::tenant/switch-tenant {:tenant-id "tenant-2"}])
    (is (true? (get-in @rf-db/app-db [:tenant :loading?])))))

;; ============================================================================
;; Fetch Memberships
;; ============================================================================

(deftest fetch-memberships-dispatches-get
  (testing "fetch-memberships sends GET to /api/v1/tenant/memberships"
    (reset-db!)
    (rf/dispatch-sync [::tenant/fetch-memberships])
    (let [req (last-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/tenant/memberships" (req-uri req))))))

(deftest fetch-memberships-success-stores-data
  (testing "fetch-memberships-success stores memberships in db"
    (reset-db!)
    (rf/dispatch-sync [::tenant/fetch-memberships])
    (let [req (last-request)
          on-success (:on-success req)]
      (rf/dispatch-sync (conj on-success {:memberships [{:tenant-id "t1" :tenant-name "WS 1" :role "owner"}
                                                        {:tenant-id "t2" :tenant-name "WS 2" :role "member"}]})))
    (let [memberships (get-in @rf-db/app-db [:tenant :memberships])]
      (is (= 2 (count memberships)))
      (is (= "WS 1" (:tenant-name (first memberships)))))))

;; ============================================================================
;; Fetch Members
;; ============================================================================

(deftest fetch-members-dispatches-get
  (testing "fetch-members sends GET to /api/v1/tenant/members"
    (reset-db!)
    (rf/dispatch-sync [::tenant/fetch-members])
    (let [req (last-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/tenant/members" (req-uri req))))))

;; ============================================================================
;; Invite Member
;; ============================================================================

(deftest invite-member-dispatches-post
  (testing "invite-member sends POST with email and role"
    (reset-db!)
    (rf/dispatch-sync [::tenant/invite-member {:email "new@test.com" :role "admin"}])
    (let [req (last-request)]
      (is (= :post (req-method req)))
      (is (= "/api/v1/tenant/invitations" (req-uri req)))
      (is (= "new@test.com" (:email (req-params req))))
      (is (= "admin" (:role (req-params req)))))))

(deftest invite-member-defaults-role-to-member
  (testing "invite-member defaults role to member when nil"
    (reset-db!)
    (rf/dispatch-sync [::tenant/invite-member {:email "new@test.com"}])
    (let [req (last-request)]
      (is (= "member" (:role (req-params req)))))))

;; ============================================================================
;; Change Member Role
;; ============================================================================

(deftest change-member-role-dispatches-put
  (testing "change-member-role sends PUT to /api/v1/tenant/members/:id/role"
    (reset-db!)
    (rf/dispatch-sync [::tenant/change-member-role {:member-id "m-1" :role "admin"}])
    (let [req (last-request)]
      (is (= :put (req-method req)))
      (is (= "/api/v1/tenant/members/m-1/role" (req-uri req)))
      (is (= "admin" (:role (req-params req)))))))

;; ============================================================================
;; Remove Member
;; ============================================================================

(deftest remove-member-dispatches-delete
  (testing "remove-member sends DELETE to /api/v1/tenant/members/:id"
    (reset-db!)
    (rf/dispatch-sync [::tenant/remove-member {:member-id "m-1"}])
    (let [req (last-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/tenant/members/m-1" (req-uri req))))))

;; ============================================================================
;; URL Slug
;; ============================================================================

(deftest set-url-slug-stores-in-db
  (testing "set-url-slug stores slug in tenant db path"
    (reset-db!)
    (rf/dispatch-sync [:tenant/set-url-slug "acme-corp"])
    (is (= "acme-corp" (get-in @rf-db/app-db [:tenant :url-slug])))))

;; ============================================================================
;; Role Subscriptions
;; ============================================================================

(deftest user-role-sub-returns-membership-role
  (testing ":user-role subscription returns membership role from session"
    (reset-db!)
    (rf/clear-subscription-cache!)
    ;; The :user-role sub is expected to prefer membership-role from session
    (let [role @(rf/subscribe [:user-role])]
      (is (= "owner" role)))))

(deftest tenant-url-slug-sub-returns-slug
  (testing ":tenant/url-slug subscription returns slug from db"
    (reset-db!)
    (rf/clear-subscription-cache!)
    (swap! rf-db/app-db assoc-in [:tenant :url-slug] "my-workspace")
    (rf/clear-subscription-cache!)
    (is (= "my-workspace" @(rf/subscribe [:tenant/url-slug])))))

;; ============================================================================
;; Clear Messages
;; ============================================================================

(deftest clear-messages-removes-error-and-success
  (testing "clear-messages dissocs error and success-message"
    (reset-db!)
    (swap! rf-db/app-db assoc-in [:tenant :error] "fail!")
    (swap! rf-db/app-db assoc-in [:tenant :success-message] "ok!")
    (rf/dispatch-sync [::tenant/clear-messages])
    (is (nil? (get-in @rf-db/app-db [:tenant :error])))
    (is (nil? (get-in @rf-db/app-db [:tenant :success-message])))))

(ns app.template.frontend.events.impersonation-test
  (:require
    [app.template.frontend.events.impersonation :as impersonation]
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
      {:impersonation {:grants nil
                       :loading? false
                       :error nil
                       :success-message nil}})))

(defn- last-request [] (last @captured-http-requests))

(defn- req-method [req] (or (:method req) :unknown))
(defn- req-uri [req] (:uri req))
(defn- req-params [req] (:params req))

;; ============================================================================
;; Fetch Grants
;; ============================================================================

(deftest fetch-grants-dispatches-get-request
  (testing "fetch-grants sends GET to /api/v1/tenant/impersonation-grants"
    (reset-db!)
    (rf/dispatch-sync [::impersonation/fetch-grants])
    (let [req (last-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/tenant/impersonation-grants" (req-uri req))))))

(deftest fetch-grants-sets-loading
  (testing "fetch-grants sets loading? to true"
    (reset-db!)
    (rf/dispatch-sync [::impersonation/fetch-grants])
    (is (true? (get-in @rf-db/app-db [:impersonation :loading?])))))

(deftest fetch-grants-success-stores-grants
  (testing "fetch-grants-success stores grants and clears loading"
    (reset-db!)
    (rf/dispatch-sync [::impersonation/fetch-grants])
    ;; Simulate success
    (let [req (last-request)
          on-success (:on-success req)]
      (rf/dispatch-sync (conj on-success {:grants [{:id "g1" :admin-email "admin@test.com" :role "viewer"}]})))
    (let [db @rf-db/app-db]
      (is (false? (get-in db [:impersonation :loading?])))
      (is (= 1 (count (get-in db [:impersonation :grants]))))
      (is (= "admin@test.com" (:admin-email (first (get-in db [:impersonation :grants]))))))))

;; ============================================================================
;; Create Grant
;; ============================================================================

(deftest create-grant-dispatches-post-request
  (testing "create-grant sends POST with admin-email and role"
    (reset-db!)
    (rf/dispatch-sync [::impersonation/create-grant {:admin-email "admin@test.com" :role "member"}])
    (let [req (last-request)]
      (is (= :post (req-method req)))
      (is (= "/api/v1/tenant/impersonation-grants" (req-uri req)))
      (is (= "admin@test.com" (:admin-email (req-params req))))
      (is (= "member" (:role (req-params req)))))))

(deftest create-grant-defaults-role-to-viewer
  (testing "create-grant defaults role to viewer when not provided"
    (reset-db!)
    (rf/dispatch-sync [::impersonation/create-grant {:admin-email "admin@test.com"}])
    (let [req (last-request)]
      (is (= "viewer" (:role (req-params req)))))))

;; ============================================================================
;; Revoke Grant
;; ============================================================================

(deftest revoke-grant-dispatches-delete-request
  (testing "revoke-grant sends DELETE to /api/v1/tenant/impersonation-grants/:id"
    (reset-db!)
    (rf/dispatch-sync [::impersonation/revoke-grant {:id "grant-123"}])
    (let [req (last-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/tenant/impersonation-grants/grant-123" (req-uri req))))))

;; ============================================================================
;; Clear Messages
;; ============================================================================

(deftest clear-messages-removes-error-and-success
  (testing "clear-messages removes error and success-message from db"
    (reset-db!)
    (swap! rf-db/app-db assoc-in [:impersonation :error] "some error")
    (swap! rf-db/app-db assoc-in [:impersonation :success-message] "some success")
    (rf/dispatch-sync [::impersonation/clear-messages])
    (let [db @rf-db/app-db]
      (is (nil? (get-in db [:impersonation :error])))
      (is (nil? (get-in db [:impersonation :success-message]))))))

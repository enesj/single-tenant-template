(ns app.admin.frontend.events.users.security-test
  (:require
    [app.admin.frontend.events.users.security] ;; ensure handlers are registered
    [app.admin.frontend.test-setup :as setup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

;; Helpers to ensure subs used by security wrappers exist if pulled transitively
(when-not (get-in @rf-db/app-db [:test :admin-session-registered?])
  (swap! rf-db/app-db assoc-in [:test :admin-session-registered?] true))

(deftest impersonate-user-produces-correct-request
  (testing ":admin/impersonate-user generates admin POST request to impersonate endpoint"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (let [user-id "248ae9b5-a155-47ae-a33f-c0f20d13791c"]
      (rf/dispatch-sync [:admin/impersonate-user user-id])
      (let [req (setup/last-http-request)]
        (is (= :post (:method req)))
        (is (= (str "/admin/api/users/impersonate/" user-id) (:uri req)))
        (is (= [:admin/impersonate-user-success] (:on-success req)))
        (is (= [:admin/impersonate-user-failure] (:on-failure req)))
        (is (= "test-token" (get-in req [:headers "x-admin-token"])))))))

(deftest reset-user-password-produces-correct-request
  (testing ":admin/reset-user-password generates admin POST request to reset-password endpoint"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (let [user-id "11111111-2222-3333-4444-555555555555"]
      (rf/dispatch-sync [:admin/reset-user-password user-id])
      (let [req (setup/last-http-request)]
        (is (= :post (:method req)))
        (is (= (str "/admin/api/users/reset-password/" user-id) (:uri req)))
        (is (= [:admin/reset-user-password-success user-id] (:on-success req)))
        (is (= [:admin/reset-user-password-failure] (:on-failure req)))
        (is (= "test-token" (get-in req [:headers "x-admin-token"])))))))

(deftest force-verify-email-produces-correct-request
  (testing ":admin/force-verify-email generates admin POST request to verify-email endpoint"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (let [user-id "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"]
      (rf/dispatch-sync [:admin/force-verify-email user-id])
      (let [req (setup/last-http-request)]
        (is (= :post (:method req)))
        (is (= (str "/admin/api/users/verify-email/" user-id) (:uri req)))
        (is (= [:admin/force-verify-email-success user-id] (:on-success req)))
        (is (= [:admin/force-verify-email-failure] (:on-failure req)))
        (is (= "test-token" (get-in req [:headers "x-admin-token"])))))))

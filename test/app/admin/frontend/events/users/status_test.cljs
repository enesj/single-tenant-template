(ns app.admin.frontend.events.users.status-test
  (:require
    [app.admin.frontend.events.users.status] ;; ensure handlers are registered
    [app.admin.frontend.test-setup :as setup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

;; Helpers to ensure subs used by security wrappers exist if pulled transitively
(when-not (get-in @rf-db/app-db [:test :admin-session-registered?])
  (swap! rf-db/app-db assoc-in [:test :admin-session-registered?] true))

(deftest update-user-role-produces-correct-request
  (testing ":admin/update-user-role generates admin PUT request to role endpoint"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (let [user-id "bbbbbbbb-2222-3333-4444-555555555555"]
      (rf/dispatch-sync [:admin/update-user-role user-id :member])
      (let [req (setup/last-http-request)]
        (is (= :put (:method req)))
        (is (= (str "/admin/api/users/role/" user-id) (:uri req)))
        (is (= {:role "member"} (:params req)))
        (is (= [:admin/update-user-role-success user-id "member"] (:on-success req)))
        (is (= [:admin/update-user-role-failure] (:on-failure req)))
        (is (= "test-token" (get-in req [:headers "x-admin-token"])))))))

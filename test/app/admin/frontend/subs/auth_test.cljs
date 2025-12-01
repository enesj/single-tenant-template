 (ns app.admin.frontend.subs.auth-test
   (:require
     [app.admin.frontend.subs.auth]  ;; register subs
     [cljs.test :refer [deftest is testing]]
     [re-frame.core :as rf]
     [re-frame.db :as rf-db]))

(deftest basic-auth-subs
  (testing "auth-related subscriptions return values from db"
    (reset! rf-db/app-db {:admin/login-loading? true
                          :admin/login-error "E"
                          :admin/authenticated? false
                          :admin/current-user {:email "a@test.com"}
                          :admin/auth-checking? false
                          :admin/token "T"})
    (is (true? @(rf/subscribe [:admin/login-loading?])))
    (is (= "E" @(rf/subscribe [:admin/login-error])))
    (is (false? @(rf/subscribe [:admin/authenticated?])))
    (is (= {:email "a@test.com"} @(rf/subscribe [:admin/current-user])))
    (is (true? @(rf/subscribe [:admin/loading?])))
    (is (= "T" @(rf/subscribe [:admin/token])))))

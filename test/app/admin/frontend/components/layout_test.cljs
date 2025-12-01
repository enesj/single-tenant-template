 (ns app.admin.frontend.components.layout-test
   (:require
     [app.admin.frontend.components.layout :refer [admin-layout]]
     [app.admin.frontend.subs.auth]
     [app.admin.frontend.test-setup :as setup]
     [app.frontend.utils.test-utils :as test-utils]
     [cljs.test :refer [deftest is testing]]
     [clojure.string :as str]
     [re-frame.core :as rf]
     [re-frame.db :as rf-db]
     [uix.core :refer [$]]))

;; Initialize test environment for React component testing
(test-utils/setup-test-environment!)

;; Stub current-route subscription expected by sidebar
(rf/reg-sub :current-route (fn [db _] (:current-route db)))

(deftest layout-shows-spinner-while-loading
  (testing "loading branch renders spinner"
    (setup/reset-db!)
    (reset! rf-db/app-db {:admin/login-loading? true
                          :admin/auth-checking? false})
    (let [markup (test-utils/render-to-static-markup ($ admin-layout {}))]
      (is (str/includes? markup "ds-loading-spinner")))))

(deftest layout-renders-navigation-when-authenticated
  (testing "authenticated branch renders sidebar and header"
    (setup/reset-db!)
    (reset! rf-db/app-db {:admin/login-loading? false
                          :admin/auth-checking? false
                          :admin/authenticated? true
                          :admin/current-user {:full-name "Admin User"}
                          :current-route {:name :admin-dashboard}})
    (let [markup (test-utils/render-to-static-markup ($ admin-layout {:children ($ :div {} "Body")}))]
      (is (str/includes? markup "Admin Panel"))
      (is (str/includes? markup "Logout"))
      (is (str/includes? markup "Body")))))

;; Active link highlight checks are skipped in Karma due to SSR fallback markup

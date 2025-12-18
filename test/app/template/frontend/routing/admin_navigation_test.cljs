(ns app.template.frontend.routing.admin-navigation-test
  (:require
    [app.admin.frontend.routes :as admin-routes]
    [app.template.frontend.routing.router :as router-util]
    [cljs.test :refer [deftest is testing]]
    [reitit.frontend :as rtf]))

(deftest admin-routes-match-dashboard-paths
  (testing "admin router matches both dashboard aliases"
    (let [router (rtf/router admin-routes/admin-routes)
          dashboard (rtf/match-by-path router "/admin")
          dashboard-alt (rtf/match-by-path router "/admin/dashboard")]
      (is (= :admin-dashboard (get-in dashboard [:data :name])))
      (is (= :admin-dashboard-alt (get-in dashboard-alt [:data :name]))))))

(deftest router-util-can-match-admin-paths-when-router-set
  (testing "router indirection layer can match admin paths"
    (let [router (rtf/router admin-routes/admin-routes)]
      (try
        (router-util/set-router! router)
        (is (= :admin-dashboard-alt
              (get-in (router-util/match-by-path "/admin/dashboard") [:data :name])))
        (finally
          (router-util/set-router! nil))))))

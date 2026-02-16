(ns app.admin.frontend.events.login-events-test
  (:require
    [app.admin.frontend.events.login-events :as login-events] ;; ensure handlers are registered
    [app.admin.frontend.test-setup :as setup]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest load-login-events-request-includes-limit-and-offset-from-list-ui-state
  (testing ":admin/load-login-events derives limit/offset from template list UI state"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :login-events) 20)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :login-events) 3)

    (rf/dispatch-sync [:admin/load-login-events])

    (let [req (setup/last-http-request)]
      (is (= :get (:method req)))
      (is (= "/admin/api/login-events" (:uri req)))
      (is (= 20 (get-in req [:params :limit])))
      (is (= 40 (get-in req [:params :offset])))
      (is (nil? (get-in req [:params :pagination]))
        "Request should not send nested :pagination map"))))

(deftest login-events-loaded-stores-server-total-items
  (testing "login events load success stores server :total into template list total-items"
    (setup/reset-db!)
    (rf/dispatch-sync [::login-events/login-events-loaded {:events [{:id "e-1"} {:id "e-2"}]
                                                         :total 84
                                                         :limit 20
                                                         :offset 0}])

    (let [db @rf-db/app-db]
      (is (= 84 (get-in db (paths/list-total-items :login-events)))))))

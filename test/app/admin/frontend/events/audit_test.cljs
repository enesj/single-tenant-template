(ns app.admin.frontend.events.audit-test
  (:require
    [app.admin.frontend.events.audit :as audit-events] ;; ensure handlers are registered
    [app.admin.frontend.test-setup :as setup]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest load-audit-logs-request-includes-limit-and-offset-from-list-ui-state
  (testing ":admin/load-audit-logs derives limit/offset from template list UI state"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (swap! rf-db/app-db assoc-in (paths/list-per-page :audit-logs) 20)
    (swap! rf-db/app-db assoc-in (paths/list-current-page :audit-logs) 4)

    (rf/dispatch-sync [:admin/load-audit-logs])

    (let [req (setup/last-http-request)]
      (is (= :get (:method req)))
      (is (= "/admin/api/audit" (:uri req)))
      (is (= 20 (get-in req [:params :limit])))
      (is (= 60 (get-in req [:params :offset])))
      (is (nil? (get-in req [:params :pagination]))
        "Request should not send nested :pagination map"))))

(deftest audit-logs-loaded-stores-server-total-items
  (testing "audit load success stores server :total into template list total-items"
    (setup/reset-db!)
    (rf/dispatch-sync [::audit-events/audit-logs-loaded {:logs [{:id "l-1"} {:id "l-2"}]
                                                     :total 101
                                                     :limit 20
                                                     :offset 0}])

    (let [db @rf-db/app-db]
      (is (= 101 (get-in db (paths/list-total-items :audit-logs)))))))

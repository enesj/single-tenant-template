 (ns app.admin.frontend.adapters.tenants-test
   (:require
     [app.admin.frontend.adapters.tenants :as tenants-adapter]
     [app.admin.frontend.test-setup :as setup]
     [app.template.frontend.db.paths :as paths]
     [cljs.test :refer [deftest is testing]]
     [re-frame.core :as rf]
     [re-frame.db :as rf-db]))

(deftest initialize-tenants-adapter-sets-default-ui
  (testing "initialize event seeds list UI defaults at top level"
    (setup/reset-db!)
    (rf/dispatch-sync [::tenants-adapter/initialize-tenants-adapter-with-config])
    (let [db @rf-db/app-db
          base (paths/list-ui-state :tenants)]
      (is (= 1 (get-in db (conj base :current-page))))
      (is (= 25 (get-in db (conj base :per-page)))))))

(deftest crud-delete-uses-versioned-endpoint-by-default
  (testing "template delete uses versioned API endpoint"
    (setup/reset-db!)
    (setup/install-http-stub!)
     ;; Ensure adapter side-effects (bridge registration) are initialized
    (tenants-adapter/init-tenants-adapter!)
    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :tenants "t-123"])
    (let [req (setup/last-http-request)]
      (is (= :delete (:method req)))
       ;; Delete uses versioned API by default in template HTTP utils
      (is (= "/api/v1/entities/tenants/t-123" (:uri req)))
      (is (= [:app.template.frontend.events.list.crud/delete-success :tenants "t-123"] (:on-success req)))
      (is (= [:app.template.frontend.events.list.crud/delete-failure :tenants] (:on-failure req))))))

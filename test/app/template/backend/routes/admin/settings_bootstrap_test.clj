(ns app.template.backend.routes.admin.settings-bootstrap-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.template.backend.routes.admin.settings-bootstrap :as settings-bootstrap]
    [app.template.backend.routes.admin.settings-io :as settings-io]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [next.jdbc :as jdbc]))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- clear-runtime-configs!
  [db]
  (jdbc/execute! db ["DELETE FROM frontend_runtime_configs"]))

(deftest bootstrap-seeds-runtime-configs-from-defaults
  (testing "bootstrap seeds missing channels so effective reads have defaults"
    (let [db fixtures/*test-db*
          admin-defaults {:view-options {:admins {:display-locks {:show-edit? true}}}
                          :form-fields {:admins {:create-fields [:email]}}
                          :table-columns {:admins {:available-columns ["id" "email"]}}}
          user-defaults {:entities {:expenses {:title "Expenses"}}
                         :view-options {:expenses {:display-defaults {:show-filtering? true}}}
                         :form-fields {:expenses {:create-fields ["total_amount"]}}
                         :table-columns {:expenses {:available-columns ["id" "total_amount"]}}}]
      (clear-runtime-configs! db)
      (clojure.core/with-redefs-fn
        {#'settings-bootstrap/admin-defaults (fn [config-key]
                                               (get admin-defaults config-key))
         #'settings-bootstrap/user-defaults (fn [config-key]
                                              (get user-defaults config-key))}
        (fn []
          (is (= :ok (settings-bootstrap/bootstrap-runtime-configs! db)))
          (is (= (:view-options admin-defaults) (settings-io/read-view-options db)))
          (is (= (:form-fields admin-defaults) (settings-io/read-form-fields db)))
          (is (= (:table-columns admin-defaults) (settings-io/read-table-columns db)))
          (is (= (:entities user-defaults) (settings-io/read-user-entities db)))
          (is (= (:view-options user-defaults) (settings-io/read-user-view-options db)))
          (is (= (:form-fields user-defaults) (settings-io/read-user-form-fields db)))
          (is (= (:table-columns user-defaults) (settings-io/read-user-table-columns db))))))))

(deftest bootstrap-fails-closed-when-defaults-cannot-be-built
  (testing "bootstrap throws instead of silently seeding empty runtime config"
    (let [db fixtures/*test-db*]
      (clear-runtime-configs! db)
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Bootstrap: failed to seed runtime frontend configs"
            (clojure.core/with-redefs-fn
              {#'settings-bootstrap/admin-defaults
               (fn [_] (throw (ex-info "boom" {:phase :admin-defaults})))}
              (fn []
                (settings-bootstrap/bootstrap-runtime-configs! db))))))))

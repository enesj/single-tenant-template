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
          (is (= ["id" "email"]
                (get-in (settings-io/read-table-columns db) [:admins :available-columns])))
          (is (= (:entities user-defaults) (settings-io/read-user-entities db)))
          (is (= (:view-options user-defaults) (settings-io/read-user-view-options db)))
          (is (= (:form-fields user-defaults) (settings-io/read-user-form-fields db)))
          (is (= (:table-columns user-defaults) (settings-io/read-user-table-columns db))))))))

        (deftest bootstrap-backfills-expense-category-default-config-on-existing-rows
          (testing "bootstrap additively reconciles stale expense-category config without clobbering existing customizations"
            (let [db fixtures/*test-db*
              stale-form-fields {:expense-categories {:create-fields ["name" "exclude_from_reports"]
                          :edit-fields ["name" "exclude_from_reports"]
                          :field-config {:name {:type "text" :label "Custom Name"}
                                 :exclude_from_reports {:type "checkbox" :label "Exclude from reports"}}}
                     :suppliers {:create-fields ["display_name"]}}
              stale-table-columns {:expense-categories {:available-columns ["name" "exclude_from_reports" "created_at" "updated_at" "id" "tenant_id"]
                            :default-visible-columns ["name" "exclude_from_reports" "created_at"]
                            :filterable-columns ["name" "exclude_from_reports" "created_at"]
                            :sortable-columns ["name" "exclude_from_reports" "created_at" "updated_at"]
                            :always-visible ["name"]
                            :column-metadata {:name {:label "Custom Name"}
                                  :exclude_from_reports {:label "Exclude from reports"}
                                  :created_at {:label-key :common/created-at}
                                  :updated_at {:label-key :common/updated-at}
                                  :id {:label-key :common/id}}}
                   :suppliers {:available-columns ["display_name"]}}
              expense-category-form-default {:expense-categories {:create-fields ["name" "exclude_from_reports" "is_default"]
                              :edit-fields ["name" "exclude_from_reports" "is_default"]
                              :field-config {:name {:type "text" :label "Name"}
                                     :exclude_from_reports {:type "checkbox" :label "Exclude from reports"}
                                     :is_default {:type "checkbox" :label "Default expense category"}}}}
              expense-category-table-default {:expense-categories {:available-columns ["name" "exclude_from_reports" "is_default" "created_at" "updated_at" "id" "tenant_id"]
                               :default-visible-columns ["name" "is_default" "exclude_from_reports" "created_at"]
                               :filterable-columns ["name" "is_default" "exclude_from_reports" "created_at"]
                               :sortable-columns ["name" "is_default" "exclude_from_reports" "created_at" "updated_at"]
                               :always-visible ["name"]
                               :column-metadata {:name {:label-key :common/expense-category-name}
                                     :exclude_from_reports {:label "Exclude from reports"}
                                     :is_default {:label-key :common/is-default}
                                     :created_at {:label-key :common/created-at}
                                     :updated_at {:label-key :common/updated-at}
                                     :id {:label-key :common/id}}}}
              admin-defaults {:view-options {}
                  :form-fields expense-category-form-default
                  :table-columns expense-category-table-default}
              user-defaults {:entities {}
                 :view-options {}
                 :form-fields expense-category-form-default
                 :table-columns expense-category-table-default}]
          (clear-runtime-configs! db)
          (settings-io/write-form-fields! db stale-form-fields)
          (settings-io/write-table-columns! db stale-table-columns)
          (settings-io/write-user-form-fields! db stale-form-fields)
          (settings-io/write-user-table-columns! db stale-table-columns)
          (clojure.core/with-redefs-fn
            {#'settings-bootstrap/admin-defaults (fn [config-key]
                           (get admin-defaults config-key))
             #'settings-bootstrap/user-defaults (fn [config-key]
                          (get user-defaults config-key))}
            (fn []
              (is (= :ok (settings-bootstrap/bootstrap-runtime-configs! db)))
              (doseq [form-fields [(settings-io/read-form-fields db)
                   (settings-io/read-user-form-fields db)]]
            (is (= ["name" "exclude_from_reports" "is_default"]
              (get-in form-fields [:expense-categories :create-fields])))
            (is (= ["name" "exclude_from_reports" "is_default"]
              (get-in form-fields [:expense-categories :edit-fields])))
            (is (= "Custom Name"
              (get-in form-fields [:expense-categories :field-config :name :label])))
            (is (= {:type "checkbox" :label "Default expense category"}
              (get-in form-fields [:expense-categories :field-config :is_default])))
            (is (= ["display_name"]
              (get-in form-fields [:suppliers :create-fields]))))
              (doseq [table-columns [(settings-io/read-table-columns db)
                     (settings-io/read-user-table-columns db)]]
            (is (= ["name" "exclude_from_reports" "is_default" "created_at" "updated_at" "id" "tenant_id"]
              (get-in table-columns [:expense-categories :available-columns])))
            (is (= ["name" "is_default" "exclude_from_reports" "created_at"]
              (get-in table-columns [:expense-categories :default-visible-columns])))
            (is (= ["name" "is_default" "exclude_from_reports" "created_at"]
              (get-in table-columns [:expense-categories :filterable-columns])))
            (is (= ["name" "is_default" "exclude_from_reports" "created_at" "updated_at"]
              (get-in table-columns [:expense-categories :sortable-columns])))
            (is (= {:label "Custom Name"}
              (get-in table-columns [:expense-categories :column-metadata :name])))
            (is (= {:label-key :common/is-default}
              (get-in table-columns [:expense-categories :column-metadata :is_default])))
            (is (= ["display_name"]
              (get-in table-columns [:suppliers :available-columns])))))))))

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

(ns app.admin.frontend.events.settings.table-columns-test
  (:require
    [app.admin.frontend.events.settings.table-columns]
    [app.admin.frontend.test-setup :as setup]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest load-table-columns-success-migrates-legacy-audit-config
  (testing "legacy audit table-column defaults are upgraded when loaded from the backend"
    (setup/reset-db!)
    (rf/dispatch-sync
      [:app.admin.frontend.events.settings/load-table-columns-success
       {:table-columns
        {:audit-logs
         {:available-columns ["action"
                              "entity-name"
                              "admin-email"
                              "admin-name"
                              "user-agent"
                              "id"
                              "actor-type"
                              "actor-id"
                              "target-type"
                              "target-id"
                              "metadata"
                              "created-at"
                              "updated-at"]
          :default-visible-columns ["action" "entity-name" "admin-email" "admin-name"]
          :filterable-columns ["action" "entity-name" "admin-email" "admin-name" "user-agent"]
          :sortable-columns ["action" "entity-name" "admin-email" "admin-name" "user-agent"]
          :always-visible ["action"]
          :computed-fields {}
          :column-config {:action {:width "140px"}}}}}])
    (let [audit-config (get-in @rf-db/app-db [:admin :config :table-columns :audit-logs])]
      (is (= ["created-at" "action" "actor-display-name" "entity-name" "context-summary"]
            (:default-visible-columns audit-config)))
      (is (some #{"actor-display-name"} (:available-columns audit-config)))
      (is (some #{"context-summary"} (:available-columns audit-config)))
      (is (= {:label "Actor"}
            (get-in audit-config [:column-metadata :actor-display-name])))
      (is (= {:label "Details"}
            (get-in audit-config [:column-metadata :context-summary])))
      (is (= {:type "join"
              :compute-fn "join-entity-name"
              :dependencies ["entity-type" "entity-id"]}
            (get-in audit-config [:computed-fields :entity-name]))))))

(deftest load-table-columns-success-migrates-legacy-admin-email-column
  (testing "legacy admin email-masked columns are upgraded to email"
    (setup/reset-db!)
    (rf/dispatch-sync
      [:app.admin.frontend.events.settings/load-table-columns-success
       {:table-columns
        {:admins
         {:available-columns ["id" "admin-ref" "email-masked" "full-name" "role"]
          :default-visible-columns ["admin-ref" "email-masked" "role"]
          :filterable-columns ["email-masked" "full-name"]
          :sortable-columns ["full-name"]
          :always-visible ["admin-ref"]
          :computed-fields {}
          :column-config {:email-masked {:width "220px"}}}}}])
    (let [admins-config (get-in @rf-db/app-db [:admin :config :table-columns :admins])]
      (is (= ["id" "admin-ref" "email" "full-name" "role"]
            (:available-columns admins-config)))
      (is (= ["admin-ref" "email" "role"]
            (:default-visible-columns admins-config)))
      (is (= ["email" "full-name"]
            (:filterable-columns admins-config)))
      (is (= {:width "220px"}
            (get-in admins-config [:column-config :email])))
      (is (nil? (get-in admins-config [:column-config :email-masked]))))))
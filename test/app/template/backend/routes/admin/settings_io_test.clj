(ns app.template.backend.routes.admin.settings-io-test
  "Tests for settings-io DB-only read/write behavior.

   Core DB contract behavior (defaults when unsaved, latest save wins,
   snapshot replacement, scope isolation) is covered by
   settings-io-runtime-test. This file focuses on:
   - legacy admin table-columns promotion transform
   - retired column pruning
   - read/write roundtrips via the public API"
  (:require
    [app.backend.fixtures :as fixtures]
    [app.template.backend.routes.admin.settings-io :as settings-io]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [next.jdbc :as jdbc]))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- seed-runtime-config!
  "Seed a raw runtime config row for testing."
  [db scope config-key data]
  (jdbc/execute-one!
    db
    [(str
       "INSERT INTO frontend_runtime_configs "
       "(id, scope, config_key, config_edn, created_at, updated_at) "
       "VALUES (gen_random_uuid(), ?, ?, ?, NOW(), NOW()) "
       "ON CONFLICT (scope, config_key) DO UPDATE SET "
       "config_edn = EXCLUDED.config_edn, updated_at = NOW()")
     scope
     (name config-key)
     (pr-str data)]))

(defn- clear-runtime-overrides!
  [db]
  (jdbc/execute! db ["DELETE FROM frontend_runtime_configs"]))

(defn- contains-value?
  [xs expected]
  (boolean (some #{expected} xs)))

;; ---------------------------------------------------------------------------
;; Legacy admin table-columns promotion
;; ---------------------------------------------------------------------------

(deftest read-table-columns-promotes-legacy-admin-runtime-defaults
  (testing "legacy persisted users/admins default-visible-columns are upgraded to all available columns"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)
      (seed-runtime-config! db "admin" :table-columns
        {:admins {:available-columns ["id"
                                      "email"
                                      "full-name"
                                      "role"
                                      "status"
                                      "last-login-at"
                                      "created-at"
                                      "updated-at"
                                      "password-hash"]
                  :default-visible-columns ["id"
                                            "email"
                                            "full-name"
                                            "role"
                                            "status"
                                            "last-login-at"
                                            "created-at"
                                            "updated-at"]}
         :users {:available-columns ["id" "email" "status" "created-at"]
                 :default-visible-columns ["id" "status" "created-at"]}})
      (let [resolved (settings-io/read-table-columns db)]
        ;; :admins legacy defaults match exactly → promoted to all available
        (is (= ["id" "email" "full-name" "role" "status"
                "last-login-at" "created-at" "updated-at" "password-hash"]
              (get-in resolved [:admins :default-visible-columns])))
        ;; :users legacy defaults match exactly → promoted to all available
        (is (= ["id" "email" "status" "created-at"]
              (get-in resolved [:users :default-visible-columns])))))))

(deftest read-table-columns-preserves-non-legacy-overrides
  (testing "admin column overrides that don't match legacy defaults are preserved as-is"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)
      (seed-runtime-config! db "admin" :table-columns
        {:admins {:available-columns ["id" "email" "role" "custom-col"]
                  :default-visible-columns ["id" "email" "custom-col"]}})
      (let [resolved (settings-io/read-table-columns db)]
        ;; Non-legacy defaults should NOT be promoted
        (is (= ["id" "email" "custom-col"]
              (get-in resolved [:admins :default-visible-columns])))))))

(deftest read-table-columns-migrates-legacy-admin-email-column
  (testing "legacy persisted admin email-masked columns are upgraded to email"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)
      (seed-runtime-config! db "admin" :table-columns
        {:admins {:available-columns ["id" "admin-ref" "email-masked" "full-name"]
                  :default-visible-columns ["admin-ref" "email-masked"]
                  :filterable-columns ["email-masked" "full-name"]
                  :column-config {:email-masked {:width "220px"}}}})
      (let [resolved (settings-io/read-table-columns db)]
        (is (= ["id" "admin-ref" "email" "full-name"]
              (get-in resolved [:admins :available-columns])))
        (is (= ["admin-ref" "email"]
              (get-in resolved [:admins :default-visible-columns])))
        (is (= ["email" "full-name"]
              (get-in resolved [:admins :filterable-columns])))
        (is (= {:width "220px"}
              (get-in resolved [:admins :column-config :email])))
        (is (nil? (get-in resolved [:admins :column-config :email-masked])))))))

;; ---------------------------------------------------------------------------
;; Retired column/field pruning
;; ---------------------------------------------------------------------------

(deftest read-table-columns-retires-known-columns
  (testing "retired columns are pruned from admin table-column reads"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)
      (seed-runtime-config! db "admin" :table-columns
        {:article-aliases {:available-columns ["id" "alias" "confidence" "article-id"]
                           :default-visible-columns ["id" "alias" "confidence"]
                           :filterable-columns ["alias" "confidence"]}
         :users {:available-columns ["id" "email" "avatar-url" "status"]
                 :default-visible-columns ["id" "email" "avatar-url"]}
         :expenses {:available-columns ["purchased_at" "is_posted" "created_at"]
                    :default-visible-columns ["purchased_at" "is_posted"]
                    :filterable-columns ["purchased_at" "is_posted"]
                    :sortable-columns ["purchased_at" "is_posted"]
                    :visible-columns ["purchased_at" "is_posted"]
                    :column-config {:is_posted {:label "Is posted"}}}})
      (let [resolved (settings-io/read-table-columns db)]
        ;; "confidence" should be removed from article-aliases
        (is (not (contains-value? (get-in resolved [:article-aliases :available-columns]) "confidence")))
        (is (not (contains-value? (get-in resolved [:article-aliases :default-visible-columns]) "confidence")))
        (is (not (contains-value? (get-in resolved [:article-aliases :filterable-columns]) "confidence")))
        ;; "avatar-url" should be removed from users
        (is (not (contains-value? (get-in resolved [:users :available-columns]) "avatar-url")))
        (is (not (contains-value? (get-in resolved [:users :default-visible-columns]) "avatar-url")))
        ;; "is_posted" should be removed from expenses
        (is (not (contains-value? (get-in resolved [:expenses :available-columns]) "is_posted")))
        (is (not (contains-value? (get-in resolved [:expenses :default-visible-columns]) "is_posted")))
        (is (not (contains-value? (get-in resolved [:expenses :filterable-columns]) "is_posted")))
        (is (not (contains-value? (get-in resolved [:expenses :sortable-columns]) "is_posted")))
        (is (not (contains-value? (get-in resolved [:expenses :visible-columns]) "is_posted")))
        (is (nil? (get-in resolved [:expenses :column-config :is_posted])))))))

(deftest read-admin-expense-runtime-config-retires-is-posted
  (testing "admin expense runtime config drops retired is_posted field across settings surfaces"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)
      (seed-runtime-config! db "admin" :table-columns
        {:expenses {:available-columns ["purchased_at" "is_posted" "created_at"]
                    :default-visible-columns ["purchased_at" "is_posted"]
                    :filterable-columns ["purchased_at" "is_posted"]
                    :sortable-columns ["purchased_at" "is_posted"]
                    :column-config {:is_posted {:label "Is posted"}}}})
      (seed-runtime-config! db "admin" :form-fields
        {:expenses {:create-fields ["purchased_at" "supplier_id" "is_posted" "notes"]
                    :edit-fields ["is_posted" "notes"]
                    :field-config {:is_posted {:type "checkbox"}
                                   :notes {:type "textarea"}}}})
      (seed-runtime-config! db "admin" :view-options
        {:expenses {:column-defaults {:is_posted true
                                      :purchased_at true}
                    :column-locks {:is_posted false
                                   :created_at true}}})
      (let [table-columns (settings-io/read-table-columns db)
            form-fields (settings-io/read-form-fields db)
            view-options (settings-io/read-view-options db)]
        (is (= ["purchased_at" "created_at"]
              (get-in table-columns [:expenses :available-columns])))
        (is (= ["purchased_at"]
              (get-in table-columns [:expenses :default-visible-columns])))
        (is (= ["purchased_at"]
              (get-in table-columns [:expenses :filterable-columns])))
        (is (= ["purchased_at"]
              (get-in table-columns [:expenses :sortable-columns])))
        (is (nil? (get-in table-columns [:expenses :column-config :is_posted])))
        (is (= ["purchased_at" "supplier_id" "notes"]
              (get-in form-fields [:expenses :create-fields])))
        (is (= ["notes"]
              (get-in form-fields [:expenses :edit-fields])))
        (is (nil? (get-in form-fields [:expenses :field-config :is_posted])))
        (is (= {:purchased_at true}
              (get-in view-options [:expenses :column-defaults])))
        (is (= {:created_at true}
              (get-in view-options [:expenses :column-locks])))))))

(deftest read-and-write-user-expense-runtime-config-retires-is-posted
  (testing "user expense runtime config drops retired is_posted field on read and write"
    (let [db fixtures/*test-db*
          table-columns {:expenses {:available-columns ["purchased_at" "is_posted" "created_at"]
                                    :default-visible-columns ["purchased_at" "is_posted"]
                                    :filterable-columns ["purchased_at" "is_posted"]
                                    :sortable-columns ["purchased_at" "is_posted"]
                                    :column-config {:is_posted {:label "Is posted"}}}}
          form-fields {:expenses {:create-fields ["purchased_at" "is_posted" "notes"]
                                  :edit-fields ["is_posted" "notes"]
                                  :field-config {:is_posted {:type "checkbox"}
                                                 :notes {:type "textarea"}}}}
          view-options {:expenses {:column-defaults {:is_posted true
                                                     :purchased_at true}
                                   :column-locks {:is_posted false
                                                  :created_at true}}}]
      (clear-runtime-overrides! db)
      (seed-runtime-config! db "user" :table-columns table-columns)
      (seed-runtime-config! db "user" :form-fields form-fields)
      (seed-runtime-config! db "user" :view-options view-options)

      (is (= ["purchased_at" "created_at"]
            (get-in (settings-io/read-user-table-columns db) [:expenses :available-columns])))
      (is (= ["purchased_at" "notes"]
            (get-in (settings-io/read-user-form-fields db) [:expenses :create-fields])))
      (is (= {:created_at true}
            (get-in (settings-io/read-user-view-options db) [:expenses :column-locks])))

      (clear-runtime-overrides! db)
      (is (= ["purchased_at" "created_at"]
            (get-in (settings-io/write-user-table-columns! db table-columns)
              [:expenses :available-columns])))
      (is (= ["purchased_at" "notes"]
            (get-in (settings-io/write-user-form-fields! db form-fields)
              [:expenses :create-fields])))
      (is (= {:purchased_at true}
            (get-in (settings-io/write-user-view-options! db view-options)
              [:expenses :column-defaults])))

      (is (= ["purchased_at" "created_at"]
            (get-in (settings-io/read-user-table-columns db) [:expenses :available-columns])))
      (is (= ["purchased_at" "notes"]
            (get-in (settings-io/read-user-form-fields db) [:expenses :create-fields])))
      (is (= {:created_at true}
            (get-in (settings-io/read-user-view-options db) [:expenses :column-locks]))))))

(deftest user-table-columns-normalize-legacy-payer-type-filter
  (testing "legacy user payer type columns are upgraded to a select filter with explicit options"
    (let [db fixtures/*test-db*
          payload {:payers {:available-columns ["label" "payer_type_label" "is_default"]
                            :default-visible-columns ["label" "payer_type_label"]
                            :filterable-columns ["label" "payer_type_label"]
                            :sortable-columns ["label" "payer_type_label"]
                            :computed-fields {:payer_type_label {:type "string"
                                                                 :label "Payer Type"}}}}]
      (clear-runtime-overrides! db)
      (seed-runtime-config! db "user" :table-columns payload)
      (let [resolved (settings-io/read-user-table-columns db)]
        (is (= "select"
              (get-in resolved [:payers :computed-fields :payer_type_label :type])))
        (is (= [{:value "system" :label "system"}
                {:value "custom" :label "custom"}]
              (get-in resolved [:payers :computed-fields :payer_type_label :options]))))

      (clear-runtime-overrides! db)
      (let [written (settings-io/write-user-table-columns! db payload)]
        (is (= "select"
              (get-in written [:payers :computed-fields :payer_type_label :type])))
        (is (= [{:value "system" :label "system"}
                {:value "custom" :label "custom"}]
              (get-in written [:payers :computed-fields :payer_type_label :options])))))))

;; ---------------------------------------------------------------------------
;; Write + read roundtrips via public API
;; ---------------------------------------------------------------------------

(deftest write-and-read-admin-view-options
  (let [db fixtures/*test-db*]
    (clear-runtime-overrides! db)
    (let [payload {:receipts {:display-defaults {:show-highlights? true}
                              :display-locks {:show-edit? false}}}]
      (settings-io/write-view-options! db payload)
      (is (= payload (settings-io/read-view-options db))))))

(deftest write-and-read-admin-form-fields
  (let [db fixtures/*test-db*]
    (clear-runtime-overrides! db)
    (let [payload {:receipts {:create [:date :supplier :amount]
                              :edit [:date :supplier :amount :notes]}}]
      (settings-io/write-form-fields! db payload)
      (is (= payload (settings-io/read-form-fields db))))))

(deftest read-form-fields-sanitizes-legacy-duplicate-runtime-data
  (let [db fixtures/*test-db*]
    (clear-runtime-overrides! db)
    (seed-runtime-config! db "admin" :form-fields
      {:expenses {:edit-fields ["currency"
                                "purchased_at"
                                "purchased-at"
                                "total-amount"
                                "total_amount"]}
       :article-aliases {:edit-fields ["raw_label"
                                       "raw-label"
                                       "unit"]}})
    (is (= {:expenses {:edit-fields ["currency"
                                     "purchased_at"
                                     "total_amount"]}
            :article-aliases {:edit-fields ["raw_label"
                                            "unit"]}}
          (settings-io/read-form-fields db)))))

(deftest write-and-read-user-view-options
  (let [db fixtures/*test-db*]
    (clear-runtime-overrides! db)
    (let [payload {:expenses {:display-defaults {:show-filtering? true}
                              :display-locks {:show-delete? false}}}]
      (settings-io/write-user-view-options! db payload)
      (is (= payload (settings-io/read-user-view-options db))))))

(deftest write-and-read-user-entities
  (let [db fixtures/*test-db*]
    (clear-runtime-overrides! db)
    (let [payload {:expenses {:label "Expenses" :icon "receipt"}}]
      (settings-io/write-user-entities! db payload)
      (is (= payload (settings-io/read-user-entities db))))))
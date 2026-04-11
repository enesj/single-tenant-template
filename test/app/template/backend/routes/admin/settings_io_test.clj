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
    [honey.sql :as sql]
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

;; ---------------------------------------------------------------------------
;; Retired column pruning
;; ---------------------------------------------------------------------------

(deftest read-table-columns-retires-known-columns
  (testing "retired columns (article-aliases/confidence, users/avatar-url) are pruned from reads"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)
      (seed-runtime-config! db "admin" :table-columns
        {:article-aliases {:available-columns ["id" "alias" "confidence" "article-id"]
                           :default-visible-columns ["id" "alias" "confidence"]
                           :filterable-columns ["alias" "confidence"]}
         :users {:available-columns ["id" "email" "avatar-url" "status"]
                 :default-visible-columns ["id" "email" "avatar-url"]}})
      (let [resolved (settings-io/read-table-columns db)]
        ;; "confidence" should be removed from article-aliases
        (is (not (some #{"confidence"} (get-in resolved [:article-aliases :available-columns]))))
        (is (not (some #{"confidence"} (get-in resolved [:article-aliases :default-visible-columns]))))
        (is (not (some #{"confidence"} (get-in resolved [:article-aliases :filterable-columns]))))
        ;; "avatar-url" should be removed from users
        (is (not (some #{"avatar-url"} (get-in resolved [:users :available-columns]))))
        (is (not (some #{"avatar-url"} (get-in resolved [:users :default-visible-columns]))))))))

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

(ns app.template.backend.routes.admin.settings-io-runtime-test
  "Behavior tests for raw runtime-store semantics.

   These tests exercise the `frontend_runtime_configs` storage layer directly,
   verifying:
   - no row -> empty map at the raw storage boundary
   - latest write wins
   - writes replace entire snapshots rather than patching
   - explicit nested-key removals persist in later reads
   - writes stay isolated by scope and config kind

   Bootstrap-driven default availability is covered separately in
   settings-bootstrap-test."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.template.backend.routes.admin.settings-io :as settings-io]
    [clojure.edn :as edn]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(use-fixtures :each fixtures/with-transaction-rollback)

;; ---------------------------------------------------------------------------
;; Direct DB helpers — mirror the private fns so tests don't depend on
;; file-reading public API. After Phase 3 the public API will delegate here.
;; ---------------------------------------------------------------------------

(defn- read-runtime-override
  "Read the runtime snapshot for a given scope + config-key from the DB.
   Returns the parsed EDN map, or {} when no row exists."
  [db scope config-key]
  (let [row (jdbc/execute-one!
              db
              (sql/format {:select [:config_edn]
                           :from [:frontend_runtime_configs]
                           :where [:and
                                   [:= :scope scope]
                                   [:= :config_key (name config-key)]]})
              {:builder-fn rs/as-unqualified-lower-maps})]
    (if-let [config-edn (:config_edn row)]
      (or (edn/read-string config-edn) {})
      {})))

(defn- write-runtime-override!
  "Persist a runtime snapshot via upsert."
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
     (pr-str data)])
  data)

(defn- clear-runtime-overrides!
  "Remove all runtime config rows for test isolation."
  [db]
  (jdbc/execute! db ["DELETE FROM frontend_runtime_configs"]))

;; ---------------------------------------------------------------------------
;; DefaultsWhenUnsaved / FreshEnvironmentOperable
;; ---------------------------------------------------------------------------

(deftest defaults-when-unsaved
  (testing "reading a channel with no runtime snapshot returns {}"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)
      (is (= {} (read-runtime-override db "admin" :view-options)))
      (is (= {} (read-runtime-override db "user" :view-options)))
      (is (= {} (read-runtime-override db "admin" :form-fields)))
      (is (= {} (read-runtime-override db "user" :form-fields)))
      (is (= {} (read-runtime-override db "admin" :table-columns)))
      (is (= {} (read-runtime-override db "user" :table-columns))))))

;; ---------------------------------------------------------------------------
;; LatestSaveWins
;; ---------------------------------------------------------------------------

(deftest latest-save-wins
  (testing "the most recent write is what subsequent reads return"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)
      (write-runtime-override! db "admin" :view-options
        {:receipts {:display-locks {:show-edit? true}}})
      (is (= {:receipts {:display-locks {:show-edit? true}}}
            (read-runtime-override db "admin" :view-options)))

      ;; Second save replaces
      (write-runtime-override! db "admin" :view-options
        {:receipts {:display-locks {:show-edit? false :show-delete? true}}})
      (is (= {:receipts {:display-locks {:show-edit? false :show-delete? true}}}
            (read-runtime-override db "admin" :view-options))))))

;; ---------------------------------------------------------------------------
;; SnapshotReplacement + ExplicitRemovalPersists
;; ---------------------------------------------------------------------------

(deftest snapshot-replacement-not-patch
  (testing "saving a channel replaces the entire snapshot — not a deep merge"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)

      ;; First save with two entity keys
      (write-runtime-override! db "admin" :view-options
        {:receipts {:display-locks {:show-edit? true}}
         :expenses {:display-locks {:show-delete? false}}})

      ;; Second save only mentions :receipts — :expenses should disappear
      (write-runtime-override! db "admin" :view-options
        {:receipts {:display-locks {:show-edit? false}}})

      (let [result (read-runtime-override db "admin" :view-options)]
        (is (= {:receipts {:display-locks {:show-edit? false}}} result))
        (is (nil? (:expenses result))
          "entity keys not in the latest save should be absent")))))

(deftest explicit-removal-persists
  (testing "nested keys removed in a save remain absent on subsequent reads"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)

      ;; Save with nested display-locks
      (write-runtime-override! db "user" :view-options
        {:receipts {:display-defaults {:show-highlights? true}
                    :display-locks {:show-edit? false
                                    :show-delete? false}}})

      ;; Now save with display-locks explicitly empty
      (write-runtime-override! db "user" :view-options
        {:receipts {:display-defaults {:show-highlights? true}
                    :display-locks {}}})

      (let [result (read-runtime-override db "user" :view-options)]
        (is (= {} (get-in result [:receipts :display-locks]))
          "explicitly emptied display-locks should remain empty")
        (is (= {:show-highlights? true}
              (get-in result [:receipts :display-defaults]))
          "untouched sibling keys should be preserved within the snapshot")))))

;; ---------------------------------------------------------------------------
;; ChannelIsolation
;; ---------------------------------------------------------------------------

(deftest channel-isolation-by-scope
  (testing "writing to admin scope does not affect user scope"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)

      (write-runtime-override! db "user" :view-options
        {:receipts {:display-locks {:show-edit? true}}})
      (write-runtime-override! db "admin" :view-options
        {:receipts {:display-locks {:show-edit? false}}})

      (is (= {:receipts {:display-locks {:show-edit? true}}}
            (read-runtime-override db "user" :view-options))
        "user scope should be unchanged after admin write")
      (is (= {:receipts {:display-locks {:show-edit? false}}}
            (read-runtime-override db "admin" :view-options))
        "admin scope should reflect admin write"))))

(deftest channel-isolation-by-kind
  (testing "writing view-options does not affect form-fields or table-columns"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)

      (write-runtime-override! db "admin" :form-fields
        {:receipts {:create [:date :amount]}})
      (write-runtime-override! db "admin" :table-columns
        {:receipts {:available-columns ["id" "date"]}})
      (write-runtime-override! db "admin" :view-options
        {:receipts {:display-locks {:show-edit? true}}})

      (is (= {:receipts {:create [:date :amount]}}
            (read-runtime-override db "admin" :form-fields))
        "form-fields should be unchanged after view-options write")
      (is (= {:receipts {:available-columns ["id" "date"]}}
            (read-runtime-override db "admin" :table-columns))
        "table-columns should be unchanged after view-options write"))))

;; ---------------------------------------------------------------------------
;; Roundtrip: write via public API, read via DB
;; ---------------------------------------------------------------------------

(deftest write-view-options-roundtrip
  (testing "write-view-options! persists and read-runtime-override returns it"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)
      (let [payload {:receipts {:display-locks {:show-edit? true}
                                :display-defaults {:show-highlights? false}}}]
        (settings-io/write-view-options! db payload)
        (is (= payload (read-runtime-override db "admin" :view-options)))))))

(deftest write-form-fields-roundtrip
  (testing "write-form-fields! persists and read-runtime-override returns it"
    (let [db fixtures/*test-db*]
      (clear-runtime-overrides! db)
      (let [payload {:receipts {:create [:date :supplier :amount]
                                :edit [:date :supplier :amount :notes]}}]
        (settings-io/write-form-fields! db payload)
        (is (= payload (read-runtime-override db "admin" :form-fields)))))))

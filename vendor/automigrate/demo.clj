(ns automigrate.demo
  "Demonstration of enhanced automigrate features."
  (:require
   [automigrate.migrations-enhanced :as enhanced]
   [automigrate.templates :as templates]
   [automigrate.util.db :as db-util]
   [automigrate.util.extensions :as extensions]
   [automigrate.util.sql-validation :as sql-validation]))

(defn demo-extension-detection
  "Demonstrate extension detection capabilities"
  [jdbc-url]
  (println "🔌 Extension Detection Demo")
  (println "=" 50)

  (let [db (db-util/db-conn jdbc-url)]
    ;; Show PostgreSQL version
    (let [pg-version (extensions/get-postgresql-version db)]
      (println (str "PostgreSQL Version: " pg-version)))

    ;; List installed extensions
    (println "\n📦 Installed Extensions:")
    (let [installed (extensions/get-installed-extensions db)]
      (doseq [ext (sort installed)]
        (println (str "  ✅ " ext))))

    ;; Show function analysis
    (println "\n🔍 Function Analysis:")
    (let [analysis (extensions/detect-extension-functions db)]
      (println (str "  Extension functions: " (count (:extension-functions analysis))))
      (println (str "  Custom functions: " (count (:custom-functions analysis))))

      (when (seq (:extension-functions analysis))
        (println "  Extension functions found:")
        (doseq [func-name (take 5 (:extension-functions analysis))]
          (println (str "    - " func-name)))))

    ;; Test extension requirements
    (println "\n📋 Extension Requirements:")
    (let [test-functions ["uuid_generate_v4" "hstore_to_json" "calculate_cohost_balance"]]
      (doseq [func-name test-functions]
        (if (extensions/function-requires-extension? func-name)
          (println (str "  " func-name " → requires " (extensions/get-required-extension func-name)))
          (println (str "  " func-name " → custom function")))))))

(defn demo-sql-validation
  "Demonstrate SQL validation and auto-fixing"
  []
  (println "\n🔍 SQL Validation Demo")
  (println "=" 50)

  ;; Test various SQL issues
  (let [test-cases [{:name "JSONB Array Syntax"
                     :sql "CREATE POLICY test ON table USING (current_tenant_id() = ANY(shared_with_tenants));"}

                    {:name "Unquoted Dotted Identifier"
                     :sql "DROP VIEW IF EXISTS v.active.subscriptions.2;"}

                    {:name "Malformed DROP Function"
                     :sql "DROP FUNCTION IF EXISTS test(, text);"}

                    {:name "Valid SQL"
                     :sql "CREATE TABLE test (id UUID PRIMARY KEY);"}]]

    (doseq [{:keys [name sql]} test-cases]
      (println (str "\n📝 Testing: " name))
      (let [result (sql-validation/validate-and-fix-sql sql :verbose? true)]
        (println (str "  Issues found: " (count (:original-issues result))))
        (println (str "  Issues fixed: " (:fixed-count result)))
        (when (not= sql (:sql result))
          (println "  Fixed SQL:")
          (println (str "    " (:sql result))))))))

(defn demo-template-generation
  "Demonstrate enhanced template generation"
  [jdbc-url]
  (println "\n📄 Template Generation Demo")
  (println "=" 50)

  (let [db (db-util/db-conn jdbc-url)
        config (templates/extension-aware-config db {})]

    ;; Test function template
    (println "\n🔧 Function Template:")
    (let [result (templates/apply-template
                   :function
                   "uuid_generate_v4"
                   {:body "CREATE OR REPLACE FUNCTION uuid_generate_v4() RETURNS uuid LANGUAGE c;"}
                   :config config
                   :db-connection db)]
      (println (str "  Type: " (:type result)))
      (println (str "  Extension: " (:extension result)))
      (println (str "  Skip if exists: " (:skip-if-exists result)))
      (when (:extension result)
        (println "  Template preview:")
        (println (str "    " (subs (:content result) 0 100) "..."))))

    ;; Test policy template
    (println "\n📋 Policy Template:")
    (let [result (templates/apply-template
                   :policy
                   "tenant_isolation"
                   {:table "transactions"
                    :sql "CREATE POLICY tenant_isolation ON transactions USING (current_tenant_id() = ANY(shared_with_tenants));"}
                   :config config)]
      (println (str "  Type: " (:type result)))
      (println (str "  Needs JSONB fix: " (:needs-jsonb-fix result)))
      (println (str "  Valid: " (:valid? result))))

    ;; Test view template
    (println "\n👁️ View Template:")
    (let [result (templates/apply-template
                   :view
                   "v.active.subscriptions.2"
                   {:sql "CREATE VIEW v.active.subscriptions.2 AS SELECT * FROM subscriptions;"}
                   :config config)]
      (println (str "  Type: " (:type result)))
      (println (str "  Has dotted name: " (:has-dotted-name result)))
      (println (str "  Needs quoting: " (:needs-quoting result))))))

(defn demo-enhanced-migration
  "Demonstrate enhanced migration with all features"
  [jdbc-url]
  (println "\n🚀 Enhanced Migration Demo")
  (println "=" 50)

  (let [config {:migration-strategy {:functions :use-extensions
                                     :validate-sql true
                                     :auto-fix-common-issues true
                                     :continue-on-error true}
                :extensions {:auto-install true
                             :preferred ["uuid-ossp" "hstore"]}}]

    ;; Show migration health analysis
    (println "\n🏥 Migration Health Analysis:")
    (let [health-results (enhanced/analyze-migration-health
                           "db/migrations"
                           :config config)]
      (println (str "  Total migrations: " (:total health-results)))
      (println (str "  Issues found: " (count (:issues health-results))))
      (println (str "  Suggestions: " (count (:suggestions health-results)))))

    ;; Show extension management
    (println "\n🔌 Extension Management:")
    (enhanced/list-extensions jdbc-url)

    ;; Check extension compatibility
    (println "\n🔍 Extension Compatibility:")
    (enhanced/check-extension-compatibility jdbc-url ["uuid-ossp" "hstore" "btree_gist"])))

(defn demo-migration-recovery
  "Demonstrate migration error recovery"
  []
  (println "\n🚨 Migration Recovery Demo")
  (println "=" 50)

  ;; Simulate different migration scenarios
  (let [test-migrations [{:name "valid-migration"
                          :sql "CREATE TABLE test (id UUID PRIMARY KEY);"
                          :expected-result :success}

                         {:name "extension-function"
                          :sql "CREATE FUNCTION uuid_generate_v4() RETURNS uuid LANGUAGE c AS '$libdir/uuid-ossp';"
                          :expected-result :skipped}

                         {:name "syntax-error"
                          :sql "CREATE POLICY bad ON table USING (current_tenant_id() = ANY(shared_with_tenants));"
                          :expected-result :fixed}

                         {:name "critical-error"
                          :sql "INVALID SQL STATEMENT;"
                          :expected-result :error}]]

    (doseq [{:keys [name sql expected-result]} test-migrations]
      (println (str "\n📋 Testing: " name))
      (println (str "  Expected: " expected-result))

      ;; Validate the SQL
      (let [validation-result (sql-validation/validate-and-fix-sql sql :auto-fix? true)]
        (println (str "  Has errors: " (:has-errors? validation-result)))
        (println (str "  Fixed count: " (:fixed-count validation-result)))

        (when (> (:fixed-count validation-result) 0)
          (println "  Fixed SQL:")
          (println (str "    " (:sql validation-result))))))))

(defn run-complete-demo
  "Run all demos in sequence"
  [jdbc-url]
  (println "🎯 Enhanced Automigrate Library Demo")
  (println "====================================")
  (println "This demo showcases all the enhancements made to the automigrate library.")

  ;; Run all demo functions
  (demo-extension-detection jdbc-url)
  (demo-sql-validation)
  (demo-template-generation jdbc-url)
  (demo-enhanced-migration jdbc-url)
  (demo-migration-recovery)

  (println "\n✨ Demo Complete!")
  (println "\nKey Enhancements Demonstrated:")
  (println "  ✅ PostgreSQL extension detection and management")
  (println "  ✅ SQL syntax validation and auto-fixing")
  (println "  ✅ Enhanced migration templates with intelligence")
  (println "  ✅ Migration strategy selection (extension vs custom)")
  (println "  ✅ Error recovery and continue-on-error options")
  (println "  ✅ PostgreSQL version compatibility checks")
  (println "  ✅ Migration health analysis and suggestions"))

;; Usage examples
(comment
  ;; Run the complete demo
  (run-complete-demo "jdbc:postgresql://localhost:5432/bookkeeping?user=user&password=password")

  ;; Run individual demos
  (demo-extension-detection "jdbc:postgresql://localhost:5432/bookkeeping?user=user&password=password")
  (demo-sql-validation)
  (demo-template-generation "jdbc:postgresql://localhost:5432/bookkeeping?user=user&password=password")

  ;; Use enhanced migration function
  (enhanced/migrate-with-enhancements
    {:jdbc-url "jdbc:postgresql://localhost:5432/bookkeeping?user=user&password=password"
     :migrations-dir "db/migrations"}
    :config {:migration-strategy {:functions :use-extensions
                                  :validate-sql true
                                  :auto-fix-common-issues true}
             :extensions {:auto-install true}})

  ;; Analyze migration health
  (enhanced/analyze-migration-health "db/migrations")

  ;; Install extension manually
  (enhanced/install-extension "jdbc:postgresql://localhost:5432/bookkeeping?user=user&password=password" "uuid-ossp"))

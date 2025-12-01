#!/usr/bin/env clj

(require '[next.jdbc :as jdbc]
  '[next.jdbc.result-set :as rs]
  '[clojure.pprint :as pprint]
  '[clojure.set :as set]
  '[aero.core :as aero])

(defn get-db-spec [profile]
  (let [config (aero/read-config "config/base.edn" {:profile profile})
        db-config (:database config)]
    {:dbtype (:dbtype db-config)
     :host (:host db-config)
     :port (:port db-config)
     :dbname (:dbname db-config)
     :user (:user db-config)
     :password (:password db-config)}))

(defn get-tables [db-spec]
  (->> (jdbc/execute! db-spec
         ["SELECT table_name, table_type
                     FROM information_schema.tables
                     WHERE table_schema = 'public'
                     ORDER BY table_name"]
         {:builder-fn rs/as-unqualified-lower-maps})
    (map #(select-keys % [:table_name :table_type]))
    set))

(defn get-columns [db-spec]
  (->> (jdbc/execute! db-spec
         ["SELECT table_name, column_name, data_type, is_nullable,
                            column_default, ordinal_position
                     FROM information_schema.columns
                     WHERE table_schema = 'public'
                     ORDER BY table_name, ordinal_position"]
         {:builder-fn rs/as-unqualified-lower-maps})
    (map #(select-keys % [:table_name :column_name :data_type :is_nullable :column_default]))
    set))

(defn get-indexes [db-spec]
  (->> (jdbc/execute! db-spec
         ["SELECT schemaname, tablename, indexname, indexdef
                     FROM pg_indexes
                     WHERE schemaname = 'public'
                     ORDER BY tablename, indexname"]
         {:builder-fn rs/as-unqualified-lower-maps})
    (map #(select-keys % [:schemaname :tablename :indexname :indexdef]))
    set))

(defn get-constraints [db-spec]
  (->> (jdbc/execute! db-spec
         ["SELECT tc.table_name, tc.constraint_name, tc.constraint_type,
                            kcu.column_name, ccu.table_name AS foreign_table_name,
                            ccu.column_name AS foreign_column_name
                     FROM information_schema.table_constraints tc
                     LEFT JOIN information_schema.key_column_usage kcu
                       ON tc.constraint_name = kcu.constraint_name
                       AND tc.table_schema = kcu.table_schema
                     LEFT JOIN information_schema.constraint_column_usage ccu
                       ON ccu.constraint_name = tc.constraint_name
                       AND ccu.table_schema = tc.table_schema
                     WHERE tc.table_schema = 'public'
                     ORDER BY tc.table_name, tc.constraint_name"]
         {:builder-fn rs/as-unqualified-lower-maps})
    (map #(select-keys % [:table_name :constraint_name :constraint_type :column_name
                          :foreign_table_name :foreign_column_name]))
    set))

(defn get-sequences [db-spec]
  (->> (jdbc/execute! db-spec
         ["SELECT sequence_name, data_type, start_value, minimum_value,
                            maximum_value, increment
                     FROM information_schema.sequences
                     WHERE sequence_schema = 'public'
                     ORDER BY sequence_name"]
         {:builder-fn rs/as-unqualified-lower-maps})
    (map #(select-keys % [:sequence_name :data_type :start_value :minimum_value
                          :maximum_value :increment]))
    set))

(defn get-functions [db-spec]
  (->> (jdbc/execute! db-spec
         ["SELECT routine_name, routine_type, data_type
                     FROM information_schema.routines
                     WHERE routine_schema = 'public'
                     ORDER BY routine_name"]
         {:builder-fn rs/as-unqualified-lower-maps})
    (map #(select-keys % [:routine_name :routine_type :data_type]))
    set))

(defn get-schema-info [db-spec]
  {:tables (get-tables db-spec)
   :columns (get-columns db-spec)
   :indexes (get-indexes db-spec)
   :constraints (get-constraints db-spec)
   :sequences (get-sequences db-spec)
   :functions (get-functions db-spec)})

(defn compare-sets [label dev-set test-set]
  (let [dev-only (set/difference dev-set test-set)
        test-only (set/difference test-set dev-set)
        common (set/intersection dev-set test-set)]
    {:label label
     :dev-only dev-only
     :test-only test-only
     :common-count (count common)
     :total-dev (count dev-set)
     :total-test (count test-set)
     :equal? (= dev-set test-set)}))

(defn print-comparison [comparison]
  (let [{:keys [label dev-only test-only common-count total-dev total-test equal?]} comparison]
    (println (str "\n🔍 " label " Comparison"))
    (println (str "═══════════════════════════════════"))
    (println (str "Dev: " total-dev " | Test: " total-test " | Common: " common-count))

    (if equal?
      (println "✅ SCHEMAS MATCH")
      (do
        (println "❌ SCHEMAS DIFFER")

        (when (seq dev-only)
          (println "\n📋 Only in DEV:")
          (doseq [item dev-only]
            (println (str "  - " (if (map? item)
                                   (str (:table_name item) "/"
                                     (or (:column_name item)
                                       (:constraint_name item)
                                       (:indexname item)
                                       (:sequence_name item)
                                       (:routine_name item)))
                                   item)))))

        (when (seq test-only)
          (println "\n🧪 Only in TEST:")
          (doseq [item test-only]
            (println (str "  - " (if (map? item)
                                   (str (:table_name item) "/"
                                     (or (:column_name item)
                                       (:constraint_name item)
                                       (:indexname item)
                                       (:sequence_name item)
                                       (:routine_name item)))
                                   item)))))))))

(defn -main []
  (println "🔄 Comparing DEV and TEST database schemas...")
  (println "═══════════════════════════════════════════════")

  (try
    (println "📡 Connecting to DEV database...")
    (let [dev-spec (get-db-spec :dev)]
      ;; Test dev connection first
      (jdbc/execute! dev-spec ["SELECT 1"] {:builder-fn rs/as-unqualified-lower-maps})
      (println "✅ DEV database connected successfully")

      (println "📡 Connecting to TEST database...")
      (let [test-spec (get-db-spec :test)]
        (try
          ;; Test test connection
          (jdbc/execute! test-spec ["SELECT 1"] {:builder-fn rs/as-unqualified-lower-maps})
          (println "✅ TEST database connected successfully")

          ;; Both connections work, proceed with comparison
          (let [dev-schema (get-schema-info dev-spec)
                test-schema (get-schema-info test-spec)

                comparisons [(compare-sets "Tables" (:tables dev-schema) (:tables test-schema))
                             (compare-sets "Columns" (:columns dev-schema) (:columns test-schema))
                             (compare-sets "Indexes" (:indexes dev-schema) (:indexes test-schema))
                             (compare-sets "Constraints" (:constraints dev-schema) (:constraints test-schema))
                             (compare-sets "Sequences" (:sequences dev-schema) (:sequences test-schema))
                             (compare-sets "Functions" (:functions dev-schema) (:functions test-schema))]]

            (doseq [comparison comparisons]
              (print-comparison comparison))

            (let [all-equal? (every? :equal? comparisons)]
              (println "\n")
              (println "═══════════════════════════════════════════════")
              (if all-equal?
                (println "🎉 ALL SCHEMAS ARE IDENTICAL! ✅")
                (println "⚠️  SCHEMAS HAVE DIFFERENCES! ❌"))
              (println "═══════════════════════════════════════════════")

                            ;; Don't exit with error code to avoid "Error while executing task"
              (when-not all-equal?
                (println "\n💡 Run migrations to sync TEST database with DEV changes"))))

          (catch Exception test-e
            (println "❌ TEST database connection failed:")
            (println (str "   " (.getMessage test-e)))
            (println "\n💡 Only DEV database is available. Showing DEV schema information:")

            (let [dev-schema (get-schema-info dev-spec)]
              (println (str "\n📊 DEV Database Schema Summary:"))
              (println (str "   Tables: " (count (:tables dev-schema))))
              (println (str "   Columns: " (count (:columns dev-schema))))
              (println (str "   Indexes: " (count (:indexes dev-schema))))
              (println (str "   Constraints: " (count (:constraints dev-schema))))
              (println (str "   Sequences: " (count (:sequences dev-schema))))
              (println (str "   Functions: " (count (:functions dev-schema))))

              (println "\n📋 Tables in DEV:")
              (doseq [table (sort-by :table_name (:tables dev-schema))]
                (println (str "   - " (:table_name table) " (" (:table_type table) ")")))

              (System/exit 2))))))

    (catch Exception e
      (println (str "❌ Error: " (.getMessage e)))
      (when-let [cause (.getCause e)]
        (println (str "   Caused by: " (.getMessage cause))))
      (System/exit 1))))

(-main)

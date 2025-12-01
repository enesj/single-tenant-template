(ns compare-with-models
  "Compare database schema with models.edn definitions"
  (:require
   [aero.core :as aero]
   [clojure.set :as set]
   [clojure.string :as str]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

;; Configuration and Database Connection
(defn load-models
  "Load models from resources/db/models.edn"
  []
  (try
    (aero/read-config "resources/db/models.edn")
    (catch Exception e
      (throw (ex-info "Failed to load models.edn"
                      {:error (.getMessage e)
                       :file "resources/db/models.edn"})))))

(defn get-db-config
  "Extract database configuration for given profile"
  [profile]
  (try
    (let [config (aero/read-config "config/base.edn" {:profile profile})]
      (:database config))
    (catch Exception e
      (throw (ex-info "Failed to load database config"
                      {:error (.getMessage e)
                       :profile profile
                       :file "config/base.edn"})))))

(defn create-db-spec
  "Create database specification from config"
  [db-config]
  {:dbtype (:dbtype db-config)
   :host (:host db-config)
   :port (:port db-config)
   :dbname (:dbname db-config)
   :user (:user db-config)
   :password "password"})

(defn get-connection
  "Get database connection for given profile"
  [profile]
  (-> profile
      get-db-config
      create-db-spec))

;; Database Schema Extraction Functions
(defn execute-query
  "Execute SQL query with error handling"
  [db-spec query]
  (try
    (jdbc/execute! db-spec
                   query
                   {:builder-fn rs/as-unqualified-lower-maps})
    (catch Exception e
      (throw (ex-info "Database query failed"
                      {:error (.getMessage e)
                       :query query})))))

(defn get-tables
  "Get all tables from the database"
  [db-spec]
  (->> (execute-query db-spec
         ["SELECT table_name, table_type
           FROM information_schema.tables
           WHERE table_schema = 'public'
           ORDER BY table_name"])
       (map #(select-keys % [:table_name :table_type]))
       set))

(defn get-columns
  "Get all columns from the database"
  [db-spec]
  (->> (execute-query db-spec
         ["SELECT table_name, column_name, data_type, is_nullable,
                  column_default, ordinal_position
           FROM information_schema.columns
           WHERE table_schema = 'public'
           ORDER BY table_name, ordinal_position"])
       (map #(select-keys % [:table_name :column_name :data_type :is_nullable :column_default]))
       set))

(defn get-indexes
  "Get all indexes from the database"
  [db-spec]
  (->> (execute-query db-spec
         ["SELECT schemaname, tablename, indexname, indexdef
           FROM pg_indexes
           WHERE schemaname = 'public'
           ORDER BY tablename, indexname"])
       (map #(select-keys % [:schemaname :tablename :indexname :indexdef]))
       set))

(defn get-constraints
  "Get all constraints from the database"
  [db-spec]
  (->> (execute-query db-spec
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
           ORDER BY tc.table_name, tc.constraint_name"])
       (map #(select-keys % [:table_name :constraint_name :constraint_type :column_name
                             :foreign_table_name :foreign_column_name]))
       set))

(defn get-enum-types
  "Get all enum types from the database"
  [db-spec]
  (->> (execute-query db-spec
         ["SELECT t.typname AS type_name, e.enumlabel AS value
           FROM pg_type t
           JOIN pg_enum e ON t.oid = e.enumtypid
           ORDER BY t.typname, e.enumsortorder"])
       (group-by :type_name)
       (map (fn [[type-name values]]
              {:type_name type-name :values (mapv :value values)}))
       set))

(defn get-rls-policies
  "Get all RLS policies from the database"
  [db-spec]
  (->> (execute-query db-spec
         ["SELECT schemaname AS table_schema, tablename AS table_name,
                  policyname AS policy_name, cmd AS policy_type
           FROM pg_policies
           WHERE schemaname = 'public'
           ORDER BY tablename, policyname"])
       (map #(select-keys % [:table_name :policy_name :policy_type]))
       set))

(defn get-actual-schema
  "Get complete actual database schema"
  [db-spec]
  {:tables (get-tables db-spec)
   :columns (get-columns db-spec)
   :indexes (get-indexes db-spec)
   :constraints (get-constraints db-spec)
   :enums (get-enum-types db-spec)
   :rls (get-rls-policies db-spec)})

;; Schema Comparison Functions
(defn print-section
  "Print a formatted section header"
  [title]
  (println (str "\n🔍 " title))
  (println (str (apply str (repeat (+ 4 (count title)) "═")))))

(defn print-difference
  "Print differences between expected and actual sets"
  [label expected actual]
  (let [missing (set/difference expected actual)
        extra (set/difference actual expected)]
    (when (seq missing)
      (println (str "❌ Missing " label ":"))
      (doseq [item missing]
        (println "  -" item)))
    (when (seq extra)
      (println (str "❌ Extra " label ":"))
      (doseq [item extra]
        (println "  +" item)))
    (when (and (empty? missing) (empty? extra))
      (println (str "✅ " label " match")))))

(defn compare-tables
  "Compare expected vs actual tables"
  [models actual-schema]
  (print-section "Tables Comparison")
  (let [expected-tables (set (map name (keys models)))
        actual-tables (set (map :table_name (:tables actual-schema)))]
    (println (str "Expected: " (count expected-tables) " | Actual: " (count actual-tables)))
    (print-difference "tables" expected-tables actual-tables)))

(defn extract-model-columns
  "Extract column definitions from models"
  [models]
  (into #{}
        (for [[table-name model] models
              [field-name field-type field-options] (:fields model)]
          (let [field-options (or field-options {})]
            {:table_name (name table-name)
             :column_name (name field-name)
             :data_type (if (vector? field-type) (first field-type) field-type)
             :is_nullable (if (:null field-options)
                           (if (= false (:null field-options)) "NO" "YES")
                           "YES")}))))

(defn normalize-column
  "Normalize column for comparison"
  [col]
  (select-keys col [:table_name :column_name :data_type :is_nullable]))

(defn compare-columns
  "Compare expected vs actual columns"
  [models actual-schema]
  (print-section "Columns Comparison")
  (let [expected-cols (extract-model-columns models)
        actual-cols (set (map normalize-column (:columns actual-schema)))]
    (println (str "Expected: " (count expected-cols) " | Actual: " (count actual-cols)))

    ;; Group by table for detailed comparison
    (doseq [table-name (set (map :table_name expected-cols))]
      (let [table-expected (filter #(= (:table_name %) table-name) expected-cols)
            table-actual (filter #(= (:table_name %) table-name) actual-cols)]
        (when (not= (set table-expected) (set table-actual))
          (println (str "\n📋 Column differences in table: " table-name))
          (print-difference "columns" (set table-expected) (set table-actual)))))))

(defn compare-indexes
  "Compare expected vs actual indexes"
  [_models actual-schema]
  (print-section "Indexes Comparison")
  (let [actual-indexes (:indexes actual-schema)]
    (println (str "Found " (count actual-indexes) " indexes in database"))
    (when (seq actual-indexes)
      (println "Indexes by table:")
      (doseq [[table indexes] (group-by :tablename actual-indexes)]
        (println (str "  " table ": " (count indexes) " indexes"))))))

(defn compare-constraints
  "Compare expected vs actual constraints"
  [_models actual-schema]
  (print-section "Constraints Comparison")
  (let [actual-constraints (:constraints actual-schema)]
    (println (str "Found " (count actual-constraints) " constraints in database"))
    (when (seq actual-constraints)
      (println "Constraints by type:")
      (doseq [[type constraints] (group-by :constraint_type actual-constraints)]
        (println (str "  " type ": " (count constraints)))))))

(defn compare-enums
  "Compare expected vs actual enum types"
  [_models actual-schema]
  (print-section "Enum Types Comparison")
  (let [actual-enums (:enums actual-schema)]
    (println (str "Found " (count actual-enums) " enum types in database"))
    (doseq [enum actual-enums]
      (println (str "  " (:type_name enum) ": " (str/join ", " (:values enum)))))))

(defn compare-rls-policies
  "Compare expected vs actual RLS policies"
  [_models actual-schema]
  (print-section "RLS Policies Comparison")
  (let [actual-rls (:rls actual-schema)]
    (println (str "Found " (count actual-rls) " RLS policies in database"))
    (when (seq actual-rls)
      (println "Policies by table:")
      (doseq [[table policies] (group-by :table_name actual-rls)]
        (println (str "  " table ": " (count policies) " policies"))))))

;; Main comparison function
(defn compare-models
  "Compare models.edn with actual database schema"
  []
  (try
    (let [models (load-models)
          db-spec (get-connection :dev)
          actual-schema (get-actual-schema db-spec)]

      (println "🚀 Starting Schema Comparison")
      (println "═══════════════════════════════════════════")

      (compare-tables models actual-schema)
      (compare-columns models actual-schema)
      (compare-indexes models actual-schema)
      (compare-constraints models actual-schema)
      (compare-enums models actual-schema)
      (compare-rls-policies models actual-schema)

      (println "\n✅ Schema comparison completed successfully"))

    (catch Exception e
      (println "❌ Error during comparison:")
      (println "  " (.getMessage e))
      (when-let [data (ex-data e)]
        (println "  Details:" data))
      (when-let [cause (.getCause e)]
        (println "  Caused by:" (.getMessage cause)))
      (System/exit 1))))

(defn -main
  "Entry point for the comparison script"
  [& args]
  (let [profile (or (first args) "dev")]
    (println (str "Comparing schema using profile: " profile))
    (compare-models)))

;; Run comparison when script is executed directly
(when (= *file* (System/getProperty "babashka.file"))
  (-main))

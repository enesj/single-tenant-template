(ns automigrate.db.introspection
  "Database schema introspection and querying utilities"
  (:require
   [automigrate.util.db :as db-util]
   [clojure.string :as str]))

(defn get-db-functions
  "Get all user-defined functions from the database"
  [db]
  (db-util/exec! db
    {:select [[[:pg_get_functiondef :p.oid] :definition]
              [:p.proname :function_name]]
     :from [[:pg_proc :p]]
     :join [[:pg_namespace :n] [:= :p.pronamespace :n.oid]]
     :where [:and
             [:= :n.nspname "public"]
             [:= :p.prokind "f"]]
     :order-by [:p.proname]}))

(defn format-function-drop
  "Generate DROP statement for a function"
  [function-name definition]
  ;; Extract parameter types from definition
  (let [params-match (re-find #"\(([^)]*)\)" definition)
        params (when params-match (second params-match))
        param-types (when (and params (not (str/blank? params)))
                      (str/join ", "
                        (map #(-> %
                                (str/split #"\s+")
                                second)
                          (str/split params #","))))]
    (if (and param-types (not (str/blank? param-types)))
      (format "DROP FUNCTION IF EXISTS %s(%s);" function-name param-types)
      (format "DROP FUNCTION IF EXISTS %s();" function-name))))

(defn get-db-triggers
  "Get all triggers from the database"
  [db]
  (db-util/exec! db
    {:select [[:t.tgname :trigger_name]
              [:c.relname :table_name]
              [[:pg_get_triggerdef :t.oid] :definition]]
     :from [[:pg_trigger :t]]
     :join [[:pg_class :c] [:= :t.tgrelid :c.oid]
            [:pg_namespace :n] [:= :c.relnamespace :n.oid]]
     :where [:and
             [:= :n.nspname "public"]
             [:not :t.tgisinternal]]
     :order-by [:c.relname :t.tgname]}))

(defn get-db-policies
  "Get all RLS policies from the database"
  [db]
  (try
    (db-util/exec! db
      {:select [[:pol.polname :policy_name]
                [:c.relname :table_name]
                [:pol.polcmd :command]
                [:pol.polpermissive :permissive]
                [[:pg_get_expr :pol.polqual :pol.polrelid] :using_expression]
                [[:pg_get_expr :pol.polwithcheck :pol.polrelid] :with_check_expression]
                [[:raw "array_to_string(ARRAY(SELECT rolname FROM pg_roles WHERE oid = ANY(pol.polroles)), ', ')"] :roles]]
       :from [[:pg_policy :pol]]
       :join [[:pg_class :c] [:= :pol.polrelid :c.oid]
              [:pg_namespace :n] [:= :c.relnamespace :n.oid]]
       :where [:= :n.nspname "public"]
       :order-by [:c.relname :pol.polname]})
    (catch Exception _
      ;; Return empty if pg_policy doesn't exist (older PostgreSQL versions)
      [])))

(defn get-db-views
  "Get all views from the database"
  [db]
  (db-util/exec! db
    {:select [[:c.relname :view_name]
              [[:pg_get_viewdef :c.oid true] :definition]]
     :from [[:pg_class :c]]
     :join [[:pg_namespace :n] [:= :c.relnamespace :n.oid]]
     :where [:and
             [:= :n.nspname "public"]
             [:= :c.relkind "v"]]
     :order-by [:c.relname]}))

(defn policy-command->sql
  "Convert policy command character to SQL command"
  [cmd]
  (case cmd
    "r" "SELECT"
    "a" "INSERT"
    "w" "UPDATE"
    "d" "DELETE"
    "*" "ALL"))

(defn get-table-constraints
  "Get table constraints from database"
  [db table-name]
  (db-util/exec! db
    {:select [:constraint_name :constraint_type]
     :from [:information_schema.table_constraints]
     :where [:and
             [:= :table_name table-name]
             [:= :table_schema "public"]]}))

(defn get-column-info
  "Get detailed column information"
  [db table-name]
  (db-util/exec! db
    {:select [:column_name :data_type :is_nullable :column_default]
     :from [:information_schema.columns]
     :where [:and
             [:= :table_name table-name]
             [:= :table_schema "public"]]
     :order-by [:ordinal_position]}))

(defn get-table-names
  "Get all table names from database"
  [db]
  (map :table_name
    (db-util/exec! db
      {:select [:table_name]
       :from [:information_schema.tables]
       :where [:and
               [:= :table_schema "public"]
               [:= :table_type "BASE TABLE"]]})))

(defn table-exists?
  "Check if table exists in database"
  [db table-name]
  (-> (first (db-util/exec! db
               {:select [:%count.*]
                :from [:information_schema.tables]
                :where [:and
                        [:= :table_name table-name]
                        [:= :table_schema "public"]]}))
    :count
    (> 0)))

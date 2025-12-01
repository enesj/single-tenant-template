(ns app.shared.adapters.database
  "Shared database adapter for PostgreSQL object conversion.

   ARCHITECTURE NOTE:
   This adapter provides common PostgreSQL object conversion utilities
   that can be used across different domains (admin, frontend APIs, etc.).

   SEPARATION FROM TEMPLATE ADAPTER:
   - app.shared.adapters.database (this file): Common utilities for PG object conversion
   - app.template.backend.db.adapter: Template infrastructure adapter with full DB protocols

   Template infrastructure reuses these conversion utilities to avoid
   duplicating PostgreSQL object handling while still satisfying domain
   boundaries."
  (:require
    [app.shared.model-naming :as model-naming]
    [app.shared.string :as string]
    [cheshire.core :as json]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log])
  (:import
    (org.postgresql.jdbc PgArray)
    (org.postgresql.util PGobject)))

(defn convert-pg-objects
  "Convert PostgreSQL objects to JSON-serializable Clojure data structures.

   This function recursively walks through data structures and converts:
   - PgArray objects to Clojure vectors
   - PGobject objects (JSON/JSONB) to parsed Clojure data
   - Preserves other data types as-is

   Used to ensure database query results can be safely serialized to JSON
   for API responses."
  [data]
  (walk/postwalk
    (fn [x]
      (cond
        (instance? PgArray x)
        (try
          (vec (.getArray x))
          (catch Exception e
            (log/warn "Failed to convert PgArray, using string representation:" (.getMessage e))
            (let [array-str (.toString x)]
              (if (and (> (count array-str) 2)
                    (= (first array-str) \{)
                    (= (last array-str) \}))
                (-> array-str
                  (subs 1 (dec (count array-str)))
                  (str/split #",")
                  (->> (mapv str/trim)))
                []))))

        (instance? PGobject x)
        (try
          (let [type (.getType x)
                value (.getValue x)]
            (case type
              "json" (json/parse-string value true)
              "jsonb" (json/parse-string value true)
              value))
          (catch Exception e
            (log/warn "Failed to convert PGobject, using string value:" (.getMessage e))
            (.getValue x)))

        :else x))
    data))

(defn convert-db-keys->app-keys
  "Convert snake_case database keys to kebab-case for frontend consumption.

   This function recursively walks through data structures and converts:
   - Database keywords (snake_case) to application keywords (kebab-case)
   - Handles maps, vectors, sequences, and nested structures
   - Preserves non-keyword values as-is

   This replaces the duplicate convert-*-data functions found across route handlers.

   Example:
   {:user_name 'John' :created_at timestamp}
   => {:user-name 'John' :created-at timestamp}"
  [data]
  (when data
    (cond
      (map? data)
      (into {}
        (map (fn [[k v]]
               [(model-naming/db-keyword->app k) (convert-db-keys->app-keys v)]))
        data)

      (vector? data)
      (mapv convert-db-keys->app-keys data)

      (seq? data)
      (map convert-db-keys->app-keys data)

      :else data)))

(defn app-keyword->camel
  "Convert a kebab-case keyword into camelCase, dropping any namespace."
  [k]
  (if (keyword? k)
    (keyword (string/camel-case (name k)))
    k))

(defn convert-app-keys->camel-keys
  "Convert kebab-case application keys into camelCase for JSON responses.

   Works recursively over maps, vectors, and sequences while preserving
   non-keyword keys and values."
  [data]
  (when data
    (walk/postwalk
      (fn [x]
        (if (map? x)
          (into (empty x)
            (map (fn [[k v]] [(app-keyword->camel k) v]) x))
          x))
      data)))

;; ============================================================================
;; Admin-specific Database Utilities
;; ============================================================================

(defn db-keyword->app-with-aliases
  "Enhanced keyword normalization for admin services that handles table aliases,
   prefixes, and namespaced keywords from JOIN queries.

   This function addresses the common pattern in admin services where:
   - SQL JOINs create namespaced keywords (:users/id, :tenants/name)
   - Table aliases need stripping (u.id -> id)
   - Entity prefixes need removal (user-id -> id)

   Args:
   - k: The keyword to normalize
   - prefixes: Vector of string prefixes to remove (e.g. [\"users-\" \"user-\"])
   - namespaces: Set of namespace strings to check for prefix removal

   Example:
   (db-keyword->app-with-aliases :users/user-name [\"user-\"] #{\"users\"})
   => :name"
  [k prefixes namespaces]
  (when k
    (let [;; Handle both namespaced and non-namespaced keywords
          base-name (-> (name k)
                      ;; Strip SQL table aliases (e.g. "u.id" -> "id")
                      (as-> s (if (and s (str/includes? s "."))
                                (subs s (inc (str/last-index-of s ".")))
                                s))
                      ;; Convert snake_case to kebab-case
                      (str/replace "_" "-"))
          ;; Remove entity prefixes from the name
          base-name (reduce (fn [acc prefix]
                              (if (str/starts-with? acc prefix)
                                (subs acc (count prefix))
                                acc))
                      base-name
                      prefixes)
          ;; Also check namespaced keywords for prefix removal
          base-name (if (and (namespace k) (namespaces (namespace k)))
                      (reduce (fn [acc prefix]
                                (if (str/starts-with? acc prefix)
                                  (subs acc (count prefix))
                                  acc))
                        base-name
                        prefixes)
                      base-name)]
      (keyword base-name))))

(defn normalize-admin-result
  "Comprehensive normalization for admin database results.

   This replaces the db-[entity]->app functions found across admin services
   with a configurable, reusable implementation.

   Args:
   - data: Database result (map or collection of maps)
   - config: Map with normalization configuration:
     {:prefixes ['user-\" 'users-\"]     ; Entity prefixes to remove
      :namespaces #{'users\" 'user\"}    ; Namespaces that trigger prefix removal
      :id-fields #{:id :tenant-id}       ; Fields to convert to strings
      :transforms {:status keyword}}      ; Additional field transformations

   Example:
   (normalize-admin-result
     {:users/user-id 123 :users/user-name \"John\" :status \"active\"}
     {:prefixes ['user-\"] :namespaces #{'users\"} :id-fields #{:id}})
   => {:id \"123\" :name \"John\" :status \"active\"}"
  [data config]
  (when data
    (let [{:keys [prefixes namespaces id-fields transforms]
           :or {prefixes [] namespaces #{} id-fields #{} transforms {}}} config

          normalize-single (fn [item]
                             (when item
                               (let [normalized (into {}
                                                  (keep (fn [[k v]]
                                                          (when-let [k* (db-keyword->app-with-aliases k prefixes namespaces)]
                                                            [k* v])))
                                                  item)]
                                 (reduce-kv
                                   (fn [acc k v]
                                     (cond
                                      ;; Convert ID fields to strings
                                       (id-fields k)
                                       (assoc acc k (some-> v str))

                                      ;; Apply custom transformations
                                       (transforms k)
                                       (assoc acc k ((transforms k) v))

                                       :else
                                       (assoc acc k v)))
                                   {}
                                   normalized))))]

      (cond
        (map? data) (normalize-single data)
        (vector? data) (mapv normalize-single data)
        (seq? data) (map normalize-single data)
        :else data))))

(defn with-admin-transaction
  "Execute a function within an admin transaction with RLS bypass and audit logging."
  ([db f]
   (with-admin-transaction db f nil))
  ([db f audit-context]
   (jdbc/with-transaction [tx db]
     (try
       (jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true "])
       (let [result (f tx)]
         (when audit-context
           (let [{:keys [admin-id action entity-type entity-id]} audit-context]
             (when (and admin-id action)
               (try
                 (jdbc/execute-one! tx
                   ["INSERT INTO admin_audit_log (admin_id, action, entity_type, entity_id, timestamp)
                    VALUES (?, ?, ?, ?, NOW ()) "
                    admin-id action entity-type entity-id])
                 (catch Exception e
                   (log/warn "Failed to log audit info " (.getMessage e)))))))
         result)
       (catch Exception e
         (log/error e "Admin transaction failed " audit-context)
         (throw e))))))

(defn execute-admin-query
  "Execute a query with admin-specific error handling and result normalization."
  [db query normalization-config & [options]]
  (let [{:keys [single? audit-context bypass-rls?]
         :or {single? false bypass-rls? true}} options

        formatted-query (if (vector? query) query (hsql/format query))

        execute-fn (fn [tx]
                     (when bypass-rls?
                       (jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true "]))

                     (let [raw-result (if single?
                                        (jdbc/execute-one! tx formatted-query)
                                        (jdbc/execute! tx formatted-query))]
                       (-> raw-result
                         convert-pg-objects
                         (normalize-admin-result normalization-config))))]

    (if audit-context
      (with-admin-transaction db execute-fn audit-context)
      (jdbc/with-transaction [tx db]
        (execute-fn tx)))))

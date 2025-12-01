(ns automigrate.util.extensions
  "PostgreSQL extension detection and management utilities."
  (:require
   [automigrate.util.db :as db-util]
   [clojure.string :as str]
   [next.jdbc :as jdbc]))

;; Extension dependency mappings
(def extension-function-mappings
  "Maps function names to their providing extensions"
  {"uuid_generate_v1" "uuid-ossp"
   "uuid_generate_v1mc" "uuid-ossp"
   "uuid_generate_v3" "uuid-ossp"
   "uuid_generate_v4" "uuid-ossp"
   "uuid_generate_v5" "uuid-ossp"
   "uuid_ns_dns" "uuid-ossp"
   "uuid_ns_oid" "uuid-ossp"
   "uuid_ns_url" "uuid-ossp"
   "uuid_ns_x500" "uuid-ossp"
   "uuid_nil" "uuid-ossp"

   ;; hstore functions
   "hstore" "hstore"
   "hstore_to_json" "hstore"
   "hstore_to_jsonb" "hstore"
   "hstore_to_array" "hstore"
   "hstore_to_matrix" "hstore"
   "hstore_to_json_loose" "hstore"
   "hstore_to_jsonb_loose" "hstore"
   "akeys" "hstore"
   "avals" "hstore"
   "skeys" "hstore"
   "svals" "hstore"
   "each" "hstore"
   "exist" "hstore"
   "exists_any" "hstore"
   "exists_all" "hstore"
   "defined" "hstore"
   "isdefined" "hstore"
   "isexists" "hstore"
   "delete" "hstore"
   "slice" "hstore"
   "slice_array" "hstore"
   "fetchval" "hstore"
   "populate_record" "hstore"
   "hs_concat" "hstore"
   "hs_contains" "hstore"
   "hs_contained" "hstore"
   "hstore_gt" "hstore"
   "hstore_ge" "hstore"
   "hstore_lt" "hstore"
   "hstore_le" "hstore"
   "hstore_eq" "hstore"
   "hstore_ne" "hstore"
   "hstore_cmp" "hstore"
   "hstore_hash" "hstore"
   "hstore_hash_extended" "hstore"
   "hstore_in" "hstore"
   "hstore_out" "hstore"
   "hstore_recv" "hstore"
   "hstore_send" "hstore"
   "hstore_subscript_handler" "hstore"
   "hstore_version_diag" "hstore"
   "gin_extract_hstore" "hstore"
   "gin_extract_hstore_query" "hstore"
   "gin_consistent_hstore" "hstore"
   "ghstore_in" "hstore"
   "ghstore_out" "hstore"
   "ghstore_compress" "hstore"
   "ghstore_decompress" "hstore"
   "ghstore_penalty" "hstore"
   "ghstore_picksplit" "hstore"
   "ghstore_union" "hstore"
   "ghstore_same" "hstore"
   "ghstore_consistent" "hstore"
   "ghstore_options" "hstore"
   "tconvert" "hstore"})

(def min-postgresql-versions
  "Minimum PostgreSQL versions for extensions"
  {"uuid-ossp" 9.1
   "hstore" 9.0
   "btree_gist" 9.1
   "btree_gin" 9.4
   "ltree" 8.3
   "pg_trgm" 9.0})

(defn get-postgresql-version
  "Get PostgreSQL version as a float"
  [db]
  (try
    (let [version-str (-> (first (db-util/exec! db
                                   {:select [[[:version]]]}))
                        :version)
          version-parts (str/split version-str #" ")
          version-num (second version-parts)]
      (-> version-num
        (str/split #"\.")
        ((fn [[major minor]]
           (Float/parseFloat (str major "." minor))))))
    (catch Exception e
      (throw (ex-info "Failed to get PostgreSQL version"
               {:error (.getMessage e)})))))

(defn extension-available?
  "Check if extension is available in current PostgreSQL instance"
  [db extension-name]
  (try
    (let [result (first (db-util/exec! db
                          {:select [1]
                           :from [:pg_available_extensions]
                           :where [:= :name extension-name]}))]
      (some? result))
    (catch Exception _
      false)))

(defn extension-installed?
  "Check if extension is currently installed"
  [db extension-name]
  (try
    (let [result (first (db-util/exec! db
                          {:select [1]
                           :from [:pg_extension]
                           :where [:= :extname extension-name]}))]
      (some? result))
    (catch Exception _
      false)))

(defn get-installed-extensions
  "Get list of all installed extensions"
  [db]
  (try
    (->> (db-util/exec! db
           {:select [:extname]
            :from [:pg_extension]})
      (map :extname)
      (set))
    (catch Exception e
      (throw (ex-info "Failed to get installed extensions"
               {:error (.getMessage e)})))))

(defn get-available-extensions
  "Get list of all available extensions"
  [db]
  (try
    (->> (db-util/exec! db
           {:select [:name]
            :from [:pg_available_extensions]})
      (map :name)
      (set))
    (catch Exception e
      (throw (ex-info "Failed to get available extensions"
               {:error (.getMessage e)})))))

(defn detect-extension-functions
  "Detect which functions are provided by extensions vs custom functions"
  [db]
  (try
    (let [functions (db-util/exec! db
                      {:select [:proname :probin :prosrc]
                       :from [:pg_proc]
                       :join [[:pg_namespace :n] [:= :pronamespace :n.oid]]
                       :where [:and
                               [:= :n.nspname "public"]
                               [:= :prokind "f"]]})
          extension-functions (filter #(and (:probin %)
                                         (str/starts-with? (:probin %) "$libdir/"))
                                functions)
          custom-functions (filter #(or (nil? (:probin %))
                                      (not (str/starts-with? (str (:probin %)) "$libdir/")))
                             functions)]
      {:extension-functions (map :proname extension-functions)
       :custom-functions (map :proname custom-functions)
       :all-functions (map :proname functions)})
    (catch Exception e
      (throw (ex-info "Failed to detect extension functions"
               {:error (.getMessage e)})))))

(defn function-requires-extension?
  "Check if a function name requires a specific extension"
  [function-name]
  (contains? extension-function-mappings function-name))

(defn get-required-extension
  "Get the extension required for a function"
  [function-name]
  (get extension-function-mappings function-name))

(defn should-skip-function-migration?
  "Determine if function migration should be skipped because extension provides it"
  [db function-name function-body]
  (let [required-ext (get-required-extension function-name)]
    (and required-ext
      (or (str/includes? function-body (str "$libdir/" (if (= required-ext "uuid-ossp") "uuid-ossp" required-ext)))
        (str/includes? function-body "LANGUAGE c"))
      (extension-installed? db required-ext))))

(defn ensure-extensions!
  "Ensure required extensions are installed"
  [db extension-names]
  (doseq [ext-name extension-names]
    (when (and (extension-available? db ext-name)
            (not (extension-installed? db ext-name)))
      (try
        (jdbc/execute! db [(str "CREATE EXTENSION IF NOT EXISTS \"" ext-name "\";")])
        (println (str "✅ Installed extension: " ext-name))
        (catch Exception e
          (println (str "⚠️  Failed to install extension " ext-name ": " (.getMessage e))))))))

(defn get-required-extensions-for-migrations
  "Analyze migrations and determine required extensions"
  [migrations]
  (->> migrations
    (filter #(contains? #{".fn"} (subs (:file-name %) (str/last-index-of (:file-name %) "."))))
    (map #(-> % :file-name (str/split #"_") second))
    (filter function-requires-extension?)
    (map get-required-extension)
    (remove nil?)
    (distinct)))

(defn extension-compatibility-check
  "Check if extensions are compatible with PostgreSQL version"
  [db extension-names]
  (let [pg-version (get-postgresql-version db)]
    (reduce
      (fn [acc ext-name]
        (let [min-version (get min-postgresql-versions ext-name)]
          (if (and min-version (< pg-version min-version))
            (assoc acc ext-name {:compatible false
                                 :required-version min-version
                                 :current-version pg-version})
            (assoc acc ext-name {:compatible true
                                 :required-version min-version
                                 :current-version pg-version}))))
      {}
      extension-names)))

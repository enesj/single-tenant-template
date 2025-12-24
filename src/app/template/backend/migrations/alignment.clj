(ns app.template.backend.migrations.alignment
  "Check whether DB state is aligned with migration files and source-of-truth EDN.

  This is intended for REPL/CI use.

  What it checks:
  - Migration files under resources/db/migrations are all applied in DB
  - DB-applied migrations that do not exist as files
  - Duplicate migration numbers
  - Basic schema diff between hierarchical models.edn and actual DB (tables/columns/index names/enums)
  - Existence of extended DB objects declared in hierarchical EDN (functions/triggers/views/policies)

  Exit semantics (helper):
  - 0: aligned
  - 1: differences
  - 2: error (DB connection/config/other)"
  (:require
    [aero.core :as aero]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.set :as set]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [app.template.backend.migrations.function-defaults :as fn-defaults]
    [app.template.backend.utils.model-customizations :as model-cust]
    [automigrate.util.db :as db-util]))

(def ^:private default-migrations-dir "resources/db/migrations")
(def ^:private default-db-root "resources/db")

(def ^:private internal-tables
  "Tables that exist for migration bookkeeping and should not be compared to models."
  #{"automigrate_migrations"})

(defn- now-iso []
  (-> (java.time.OffsetDateTime/now) (.toString)))

(defn- normalize-ident
  "Normalize a keyword/string identifier into the DB-friendly form."
  [x]
  (-> (cond
        (keyword? x) (name x)
        (string? x) x
        :else (str x))
    (str/replace "-" "_")
    (str/lower-case)))

(defn- read-edn-file
  "Read an EDN file that may contain comments. Returns {} if it doesn't exist."
  [path]
  (let [f (io/file path)]
    (if (.exists f)
      (read-string (slurp f))
      {})))

(defn- discover-domain-subdirs
  "Return the names of domain subdirectories under resources/db/domain."
  [db-root]
  (let [base (io/file (str db-root "/domain"))]
    (if (and (.exists base) (.isDirectory base))
      (->> (.listFiles base)
        (filter identity)
        (filter #(.isDirectory %))
        (map #(.getName %))
        (remove #(str/starts-with? % "."))
        (sort))
      [])))

(defn- read-hierarchical-edn
  "Merge template + domain (direct + subdirs) + shared for a given file-name.

  Merge order: template < domain < shared (shared wins)."
  [db-root file-name]
  (let [template-path (str db-root "/template/" file-name)
        shared-path (str db-root "/shared/" file-name)
        domain-direct-path (str db-root "/domain/" file-name)
        domain-subdirs (discover-domain-subdirs db-root)
        domain-subdir-data
        (reduce
          (fn [acc d]
            (merge acc (read-edn-file (str db-root "/domain/" d "/" file-name))))
          {}
          domain-subdirs)]
    (merge
      (read-edn-file template-path)
      (read-edn-file domain-direct-path)
      domain-subdir-data
      (read-edn-file shared-path))))

(defn- q
  [db sqlvec]
  (jdbc/execute! db sqlvec {:builder-fn rs/as-unqualified-lower-maps}))

(defn- list-migration-files
  [dir]
  (let [dir (io/file dir)]
    (when-not (.exists dir)
      (throw (ex-info "Migrations directory not found" {:dir (.getPath dir)})))
    (->> (file-seq dir)
      (filter #(.isFile %))
      (map #(.getName %))
      (remove #(or (str/blank? %) (= % ".DS_Store")))
      (sort))))

(defn- parse-migration-filename
  "Parse a migration filename like `0001_schema.edn`.

  Returns a map like:
    {:file 0001_schema.edn :number 1 :name 0001_schema :ext edn}

  If unparseable, returns:
    {:file <name> :unparseable true}"
  [file]
  (if-let [[_ n base ext] (re-matches #"^(\d+)_([^.]+)\.(.+)$" file)]
    (let [number (Integer/parseInt n)]
      {:file file
       :number number
       :name (str n "_" base)
       :ext ext})
    {:file file :unparseable true}))

(defn- migration-file-report
  [{:keys [migrations-dir]}]
  (let [files (list-migration-files migrations-dir)
        parsed (mapv parse-migration-filename files)
        unparseable (->> parsed (filter :unparseable) (map :file) sort)
        by-number (group-by :number (remove :unparseable parsed))
        duplicates (->> by-number
                     (filter (fn [[_ xs]] (> (count xs) 1)))
                     (into (sorted-map)))
        names (->> parsed
                (remove :unparseable)
                (map :name)
                (set))]
    {:files files
     :parsed parsed
     :names names
     :duplicates duplicates
     :unparseable unparseable}))

(defn- db-applied-migrations
  "Return the set of migration names applied in DB.

  If the tracking table does not exist, returns an empty set."
  [db]
  (try
    (->> (q db ["SELECT name FROM automigrate_migrations ORDER BY created_at ASC"])
      (map :name)
      (set))
    (catch Exception e
      (let [msg (ex-message e)]
        (if (re-find #"relation .+ does not exist" msg)
          #{}
          (throw e))))))

(defn- sql-type->expected
  [field-type]
  (cond
    (keyword? field-type)
    (case field-type
      :uuid {:type-kind :scalar :data-type "uuid"}
      :text {:type-kind :scalar :data-type "text"}
      :boolean {:type-kind :scalar :data-type "boolean"}
      :integer {:type-kind :scalar :data-type "integer"}
      :bigint {:type-kind :scalar :data-type "bigint"}
      :jsonb {:type-kind :scalar :data-type "jsonb"}
      :json {:type-kind :scalar :data-type "json"}
      :timestamptz {:type-kind :scalar :data-type "timestamp with time zone"}
      :timestamp {:type-kind :scalar :data-type "timestamp without time zone"}
      :date {:type-kind :scalar :data-type "date"}
      {:type-kind :scalar :data-type (normalize-ident field-type)})

    (and (vector? field-type) (= :varchar (first field-type)))
    {:type-kind :scalar
     :data-type "character varying"
     :char-max (second field-type)}

    (and (vector? field-type) (= :decimal (first field-type)))
    {:type-kind :scalar
     :data-type "numeric"
     :numeric-precision (second field-type)
     :numeric-scale (nth field-type 2 nil)}

    (and (vector? field-type) (= :enum (first field-type)))
    {:type-kind :enum
     :udt-name (normalize-ident (second field-type))}

    :else
    {:type-kind :unknown
     :raw field-type}))

(defn- expected-nullable?
  [field-opts]
  (let [opts (or field-opts {})]
    (cond
      (true? (:primary-key opts)) false
      (contains? opts :null) (not (false? (:null opts)))
      :else true)))

(defn- models->expected
  [models]
  (let [models (or models {})
        expected-tables
        (->> (keys models)
          (map normalize-ident)
          (set))
        expected-columns
        (reduce-kv
          (fn [acc model-k model]
            (let [t (normalize-ident model-k)
                  fields (:fields model)
                  cols
                  (reduce
                    (fn [m [field-k field-type field-opts]]
                      (let [col (normalize-ident field-k)
                            type-exp (sql-type->expected field-type)
                            nullable? (expected-nullable? field-opts)]
                        (assoc m col
                          (merge type-exp
                            {:is-nullable (if nullable? "YES" "NO")}))))
                    {}
                    (or fields []))]
              (assoc acc t cols)))
          {}
          models)
        expected-indexes
        (->> models
          (mapcat (fn [[_ model]]
                    (for [[idx-name _idx-type _idx-opts] (:indexes model)]
                      (normalize-ident idx-name))))
          (set))
        expected-enums
        (->> models
          (mapcat (fn [[_ model]]
                    (for [[type-name type-kind type-opts] (:types model)
                          :when (= type-kind :enum)]
                      [(normalize-ident type-name) (vec (:choices type-opts))])))
          (into {}))]
    {:tables expected-tables
     :columns expected-columns
     :indexes expected-indexes
     :enums expected-enums}))

(defn- fetch-tables
  [db]
  (->> (q db ["SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY table_name"])
    (map :table_name)
    (remove internal-tables)
    (set)))

(defn- fetch-columns
  [db]
  (->> (q db ["SELECT table_name, column_name, data_type, is_nullable, udt_name, character_maximum_length, numeric_precision, numeric_scale\n             FROM information_schema.columns\n             WHERE table_schema='public'\n             ORDER BY table_name, ordinal_position"])
    (remove #(internal-tables (:table_name %)))
    (reduce
      (fn [acc {:keys [table_name column_name data_type is_nullable udt_name character_maximum_length numeric_precision numeric_scale]}]
        (assoc-in acc [table_name column_name]
                  {:data-type (some-> data_type str/lower-case)
                   :is-nullable is_nullable
                   :udt-name (some-> udt_name str/lower-case)
                   :char-max character_maximum_length
                   :numeric-precision numeric_precision
                   :numeric-scale numeric_scale}))
      {})))

(defn- fetch-indexes
  [db]
  (->> (q db ["SELECT tablename, indexname FROM pg_indexes WHERE schemaname='public' ORDER BY tablename, indexname"])
    (remove #(internal-tables (:tablename %)))
    (map :indexname)
    (map str/lower-case)
    (set)))

(defn- fetch-enums
  [db]
  (let [rows (q db ["SELECT t.typname AS type_name, e.enumlabel AS value\n                   FROM pg_type t\n                   JOIN pg_enum e ON t.oid = e.enumtypid\n                   JOIN pg_namespace n ON n.oid = t.typnamespace\n                   WHERE n.nspname='public'\n                   ORDER BY t.typname, e.enumsortorder"])]
    (->> rows
      (group-by :type_name)
      (reduce-kv (fn [acc type-name xs]
                   (assoc acc (str/lower-case type-name)
                     (mapv :value xs)))
        {}))))

(defn- compare-tables
  [{:keys [expected actual]}]
  {:missing (sort (set/difference expected actual))
   :extra (sort (set/difference actual expected))})

(defn- compare-columns
  "Compare expected vs actual columns.

  Returns a map:
  {:missing {table [col...]}
   :extra {table [col...]}
   :mismatched {table [{:column .. :expected .. :actual ..} ...]}}"
  [{:keys [expected actual]}]
  (let [tables (sort (set/union (set (keys expected)) (set (keys actual))))]
    (reduce
      (fn [acc t]
        (let [exp-cols (get expected t {})
              act-cols (get actual t {})
              exp-names (set (keys exp-cols))
              act-names (set (keys act-cols))
              missing (sort (set/difference exp-names act-names))
              extra (sort (set/difference act-names exp-names))
              common (sort (set/intersection exp-names act-names))
              mismatched
              (->> common
                (keep (fn [c]
                        (let [e (get exp-cols c)
                              a (get act-cols c)
                              ok?
                              (case (:type-kind e)
                                :enum
                                (and (= (:is-nullable e) (:is-nullable a))
                                  (= (:udt-name e) (:udt-name a)))

                                :scalar
                                (and (= (:is-nullable e) (:is-nullable a))
                                  (= (:data-type e) (:data-type a))
                                  (or (nil? (:char-max e)) (= (:char-max e) (:char-max a)))
                                  (or (nil? (:numeric-precision e)) (= (:numeric-precision e) (:numeric-precision a)))
                                  (or (nil? (:numeric-scale e)) (= (:numeric-scale e) (:numeric-scale a))))

                                (= (:is-nullable e) (:is-nullable a)))]
                          (when-not ok?
                            {:column c :expected e :actual a}))))
                (vec))]
          (cond-> acc
            (seq missing) (assoc-in [:missing t] missing)
            (seq extra) (assoc-in [:extra t] extra)
            (seq mismatched) (assoc-in [:mismatched t] mismatched))))
      {:missing {} :extra {} :mismatched {}}
      tables)))

(defn- compare-indexes
  [{:keys [expected actual]}]
  {:missing (sort (set/difference expected actual))})

(defn- compare-enums
  [{:keys [expected actual]}]
  (let [expected-types (set (keys expected))
        actual-types (set (keys actual))
        missing-types (sort (set/difference expected-types actual-types))
        mismatched
        (->> (set/intersection expected-types actual-types)
          (keep (fn [t]
                  (let [exp (get expected t)
                        act (get actual t)
                        missing (seq (set/difference (set exp) (set act)))
                        extra (seq (set/difference (set act) (set exp)))]
                    (when (or missing extra)
                      {:type t
                       :missing-values (sort (or missing []))
                       :extra-values (sort (or extra []))}))))
          (sort-by :type)
          (vec))]
    {:missing-types missing-types
     :mismatched mismatched}))

(def ^:private re-create-function
  (re-pattern
    (str "(?is)\\bcreate\\s+(?:or\\s+replace\\s+)?"
      "function\\s+(?:public\\.)?([a-zA-Z0-9_]+)\\s*\\(")))

(def ^:private re-create-trigger
  (re-pattern "(?is)\\bcreate\\s+trigger\\s+([a-zA-Z0-9_]+)\\b"))

(def ^:private re-create-policy
  (re-pattern "(?is)\\bcreate\\s+policy\\s+([a-zA-Z0-9_]+)\\b"))

(def ^:private re-create-view
  (re-pattern
    (str "(?is)\\bcreate\\s+(?:or\\s+replace\\s+)?"
      "view\\s+(?:public\\.)?([a-zA-Z0-9_]+)\\b")))

(defn- extract-sql-object-name
  "Extract an object name from an :up SQL string.

  kind: one of :function :trigger :policy :view

  Returns a normalized name string or nil if it can't parse."
  [kind sql]
  (let [sql (or sql "")
        re (case kind
             :function re-create-function
             :trigger re-create-trigger
             :policy re-create-policy
             :view re-create-view)]
    (when-let [[_ n] (re-find re sql)]
      (str/lower-case n))))

(defn- expected-extended-object-names
  "Return {:expected #{...} :unparseable [{:key k :up ...} ...]}"
  [kind edn-map]
  (reduce-kv
    (fn [{:keys [expected unparseable]} k v]
      (if-let [n (extract-sql-object-name kind (:up v))]
        {:expected (conj expected n)
         :unparseable unparseable}
        {:expected expected
         :unparseable (conj unparseable {:key k :up (or (:up v) "")})}))
    {:expected #{} :unparseable []}
    (or edn-map {})))

(defn- fetch-functions
  [db]
  (->> (q db ["SELECT p.proname AS name\n             FROM pg_proc p\n             JOIN pg_namespace n ON p.pronamespace = n.oid\n             WHERE n.nspname = 'public' AND p.prokind = 'f'\n             ORDER BY p.proname"])
    (map :name)
    (map str/lower-case)
    (set)))

(defn- fetch-triggers
  [db]
  (->> (q db ["SELECT t.tgname AS name\n             FROM pg_trigger t\n             JOIN pg_class c ON t.tgrelid = c.oid\n             JOIN pg_namespace n ON c.relnamespace = n.oid\n             WHERE n.nspname = 'public' AND NOT t.tgisinternal\n             ORDER BY c.relname, t.tgname"])
    (map :name)
    (map str/lower-case)
    (set)))

(defn- fetch-views
  [db]
  (->> (q db ["SELECT table_name AS name\n             FROM information_schema.views\n             WHERE table_schema='public'\n             ORDER BY table_name"])
    (map :name)
    (map str/lower-case)
    (set)))

(defn- fetch-policies
  [db]
  (->> (q db ["SELECT pol.polname AS name\n             FROM pg_policy pol\n             JOIN pg_class c ON pol.polrelid = c.oid\n             JOIN pg_namespace n ON c.relnamespace = n.oid\n             WHERE n.nspname='public'\n             ORDER BY c.relname, pol.polname"])
    (map :name)
    (map str/lower-case)
    (set)))

(defn report
  "Build an alignment report.

  Options:
  - :jdbc-url (required)
  - :migrations-dir (default resources/db/migrations)
  - :db-root (default resources/db)"
  [{:keys [jdbc-url migrations-dir db-root]
    :or {migrations-dir default-migrations-dir
         db-root default-db-root}}]
  (let [db (db-util/db-conn jdbc-url)
        file-report (migration-file-report {:migrations-dir migrations-dir})
        db-migs (db-applied-migrations db)
        file-migs (:names file-report)
        pending (sort (set/difference file-migs db-migs))
        extra (sort (set/difference db-migs file-migs))

        raw-models (read-hierarchical-edn db-root "models.edn")
        stripped-models (model-cust/strip-all-admin-config raw-models)
        processed-models (fn-defaults/preprocess-models stripped-models)
        expected (models->expected processed-models)

        actual-tables (fetch-tables db)
        actual-columns (fetch-columns db)
        actual-indexes (fetch-indexes db)
        actual-enums (fetch-enums db)

        tables-diff (compare-tables {:expected (:tables expected)
                                     :actual actual-tables})
        columns-diff (compare-columns {:expected (:columns expected)
                                       :actual actual-columns})
        indexes-diff (compare-indexes {:expected (:indexes expected)
                                       :actual actual-indexes})
        enums-diff (compare-enums {:expected (:enums expected)
                                   :actual actual-enums})

        functions-edn (read-hierarchical-edn db-root "functions.edn")
        triggers-edn (read-hierarchical-edn db-root "triggers.edn")
        views-edn (read-hierarchical-edn db-root "views.edn")
        policies-edn (read-hierarchical-edn db-root "policies.edn")

        expected-functions (expected-extended-object-names :function functions-edn)
        expected-triggers (expected-extended-object-names :trigger triggers-edn)
        expected-views (expected-extended-object-names :view views-edn)
        expected-policies (expected-extended-object-names :policy policies-edn)

        db-functions (fetch-functions db)
        db-triggers (fetch-triggers db)
        db-views (fetch-views db)
        db-policies (fetch-policies db)

        missing-functions (sort (set/difference (:expected expected-functions) db-functions))
        missing-triggers (sort (set/difference (:expected expected-triggers) db-triggers))
        missing-views (sort (set/difference (:expected expected-views) db-views))
        missing-policies (sort (set/difference (:expected expected-policies) db-policies))]

    {:timestamp (now-iso)
     :config {:jdbc-url jdbc-url
              :migrations-dir migrations-dir
              :db-root db-root}
     :migrations {:files {:count (count (:names file-report))
                          :unparseable (:unparseable file-report)
                          :duplicates (->> (:duplicates file-report)
                                        (map (fn [[n xs]] {:number n :files (mapv :file xs)}))
                                        (vec))
                          :names file-migs}
                  :db {:count (count db-migs)
                       :names db-migs}
                  :pending pending
                  :extra-in-db extra}
     :schema {:tables tables-diff
              :columns columns-diff
              :indexes indexes-diff
              :enums enums-diff}
     :extended {:functions {:missing missing-functions
                            :unparseable (:unparseable expected-functions)}
                :triggers {:missing missing-triggers
                           :unparseable (:unparseable expected-triggers)}
                :views {:missing missing-views
                        :unparseable (:unparseable expected-views)}
                :policies {:missing missing-policies
                           :unparseable (:unparseable expected-policies)}}}))

(defn diff?
  "Return true if a report contains any differences."
  [r]
  (boolean
    (or (seq (get-in r [:migrations :files :duplicates]))
      (seq (get-in r [:migrations :files :unparseable]))
      (seq (get-in r [:migrations :pending]))
      (seq (get-in r [:migrations :extra-in-db]))

      (seq (get-in r [:schema :tables :missing]))
      (seq (get-in r [:schema :tables :extra]))
      (seq (get-in r [:schema :columns :missing]))
      (seq (get-in r [:schema :columns :extra]))
      (seq (get-in r [:schema :columns :mismatched]))
      (seq (get-in r [:schema :indexes :missing]))
      (seq (get-in r [:schema :enums :missing-types]))
      (seq (get-in r [:schema :enums :mismatched]))

      (seq (get-in r [:extended :functions :missing]))
      (seq (get-in r [:extended :triggers :missing]))
      (seq (get-in r [:extended :views :missing]))
      (seq (get-in r [:extended :policies :missing]))

      (seq (get-in r [:extended :functions :unparseable]))
      (seq (get-in r [:extended :triggers :unparseable]))
      (seq (get-in r [:extended :views :unparseable]))
      (seq (get-in r [:extended :policies :unparseable])))))

(defn exit-code
  "Return the exit code for a report (0 aligned, 1 differences)."
  [r]
  (if (diff? r) 1 0))

(declare print-report! report-for-profile)

(defn exit-code-for-profile
  "Run the alignment check for a config profile and return an exit code.

  Exit semantics:
  - 0: aligned
  - 1: differences
  - 2: error (config/DB/other)

  By default this prints the report to stdout."
  ([profile]
   (exit-code-for-profile profile {:print? true}))
  ([profile {:keys [print?] :or {print? true}}]
   (try
     (let [r (report-for-profile profile)]
       (when print?
         (print-report! r))
       (exit-code r))
     (catch Exception e
       (binding [*out* *err*]
         (println "❌ Error while checking DB/migrations alignment")
         (println (ex-message e))
         (when-let [data (ex-data e)]
           (pprint/pprint data)))
       2))))

(defn- print-kv-lines
  [indent label xs]
  (when (seq xs)
    (println (str indent label ":"))
    (doseq [x xs]
      (println (str indent "  - " x)))))

(defn- print-columns-by-table
  [label table->cols]
  (when (seq table->cols)
    (println (str label ":"))
    (doseq [[t cols] (sort-by key table->cols)]
      (println (str "  " t ":"))
      (doseq [c cols]
        (println (str "    - " c))))))

(defn- print-mismatched-columns
  [table->mismatches]
  (when (seq table->mismatches)
    (println "Mismatched column specs:")
    (doseq [[t mismatches] (sort-by key table->mismatches)]
      (println (str "  " t ":"))
      (doseq [{:keys [column expected actual]} mismatches]
        (println (str "    - " column))
        (println (str "      expected: " (pr-str expected)))
        (println (str "      actual:   " (pr-str actual)))))))

(defn print-report!
  "Pretty-print a report to stdout."
  [r]
  (println "Database Migration Alignment Check")
  (println (str "Timestamp: " (:timestamp r)))
  (println "")

  (println "--- Migrations (files vs DB) ---")
  (println (format "Migration files found: %d" (get-in r [:migrations :files :count])))
  (println (format "Migrations applied in DB: %d" (get-in r [:migrations :db :count])))

  (when-let [dups (seq (get-in r [:migrations :files :duplicates]))]
    (println "\n❌ Duplicate migration numbers detected:")
    (doseq [{:keys [number files]} dups]
      (println (format "  %04d:" number))
      (doseq [f files]
        (println (str "    - " f)))))

  (print-kv-lines "" "Unparseable migration files" (get-in r [:migrations :files :unparseable]))
  (print-kv-lines "" "Pending migrations (in files, missing in DB)" (get-in r [:migrations :pending]))
  (print-kv-lines "" "Extra migrations (in DB, missing in files)" (get-in r [:migrations :extra-in-db]))

  (println "\n--- Schema (models.edn vs DB) ---")
  (print-kv-lines "" "Missing tables" (get-in r [:schema :tables :missing]))
  (print-kv-lines "" "Extra tables" (get-in r [:schema :tables :extra]))
  (print-columns-by-table "Missing columns" (get-in r [:schema :columns :missing]))
  (print-columns-by-table "Extra columns" (get-in r [:schema :columns :extra]))
  (print-mismatched-columns (get-in r [:schema :columns :mismatched]))
  (print-kv-lines "" "Missing indexes" (get-in r [:schema :indexes :missing]))

  (print-kv-lines "" "Missing enum types" (get-in r [:schema :enums :missing-types]))
  (when-let [mism (seq (get-in r [:schema :enums :mismatched]))]
    (println "Enum value mismatches:")
    (doseq [{:keys [type missing-values extra-values]} mism]
      (println (str "  " type ":"))
      (print-kv-lines "    " "missing values" missing-values)
      (print-kv-lines "    " "extra values" extra-values)))

  (println "\n--- Extended objects (EDN vs DB) ---")
  (print-kv-lines "" "Missing functions" (get-in r [:extended :functions :missing]))
  (print-kv-lines "" "Missing triggers" (get-in r [:extended :triggers :missing]))
  (print-kv-lines "" "Missing views" (get-in r [:extended :views :missing]))
  (print-kv-lines "" "Missing policies" (get-in r [:extended :policies :missing]))

  (doseq [[k {:keys [unparseable]}] (:extended r)]
    (when (seq unparseable)
      (println (format "\n⚠️  Could not parse %s names from %d EDN entries (first 3 shown):"
                 (name k) (count unparseable)))
      (doseq [{:keys [key up]} (take 3 unparseable)]
        (let [up (str/trim (or up ""))]
          (println (str "  - key: " (pr-str key)))
          (println (str "    up:  " (subs up 0 (min 120 (count up)))))))))

  (println "\n--- Result ---")
  (if (diff? r)
    (println "❌ Differences found.")
    (println "✅ All aligned.")))

(defn report-for-profile
  "Convenience wrapper: load jdbc-url from config/base.edn via aero.

  Intended for REPL usage. Use `app.template.backend.migrations.simple-repl/get-jdbc-url`
  if you already have it loaded."
  ([] (report-for-profile :dev))
  ([profile]
   (let [cfg (aero/read-config (io/file "config/base.edn") {:profile profile})
         db-cfg (:database cfg)
         jdbc-url (or (System/getenv "DATABASE_URL")
                    (:jdbc-url db-cfg)
                    (when (and db-cfg (every? db-cfg [:host :port :dbname :user]))
                      (format "jdbc:postgresql://%s:%s/%s?user=%s%s"
                        (:host db-cfg) (:port db-cfg) (:dbname db-cfg) (:user db-cfg)
                        (if-let [pwd (:password db-cfg)] (str "&password=" pwd) ""))))]
     (when-not jdbc-url
       (throw (ex-info "DATABASE_URL or database config is required" {:profile profile :database db-cfg})))
     (report {:jdbc-url jdbc-url}))))

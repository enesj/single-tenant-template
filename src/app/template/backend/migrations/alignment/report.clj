(ns app.template.backend.migrations.alignment.report
  "Report generation and printing for alignment checks."
  (:require
    [aero.core :as aero]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.set :as set]
    [clojure.string :as str]
    [app.template.backend.migrations.function-defaults :as fn-defaults]
    [app.template.backend.utils.model-customizations :as model-cust]
    [app.template.backend.migrations.alignment.utils :as utils]
    [app.template.backend.migrations.alignment.files :as files]
    [app.template.backend.migrations.alignment.schema :as schema]
    [app.template.backend.migrations.alignment.fetchers :as fetchers]
    [automigrate.util.db :as db-util]))

(defn report
  "Build an alignment report.

  Options:
  - :jdbc-url (required)
  - :migrations-dir (default resources/db/migrations)
  - :db-root (default resources/db)"
  [{:keys [jdbc-url migrations-dir db-root]
    :or {migrations-dir utils/default-migrations-dir
         db-root utils/default-db-root}}]
  (let [db (db-util/db-conn jdbc-url)
        file-report (files/migration-file-report {:migrations-dir migrations-dir})
        db-migs (files/db-applied-migrations db)
        file-migs (:names file-report)
        pending (sort (set/difference file-migs db-migs))
        extra (sort (set/difference db-migs file-migs))

        raw-models (utils/read-hierarchical-edn db-root "models.edn")
        stripped-models (model-cust/strip-all-admin-config raw-models)
        processed-models (fn-defaults/preprocess-models stripped-models)
        expected (schema/models->expected processed-models)

        actual-tables (fetchers/fetch-tables db)
        actual-columns (fetchers/fetch-columns db)
        actual-indexes (fetchers/fetch-indexes db)
        actual-enums (fetchers/fetch-enums db)

        tables-diff (fetchers/compare-tables {:expected (:tables expected)
                                              :actual actual-tables})
        columns-diff (fetchers/compare-columns {:expected (:columns expected)
                                                :actual actual-columns})
        indexes-diff (fetchers/compare-indexes {:expected (:indexes expected)
                                                :actual actual-indexes})
        enums-diff (fetchers/compare-enums {:expected (:enums expected)
                                            :actual actual-enums})

        functions-edn (utils/read-hierarchical-edn db-root "functions.edn")
        triggers-edn (utils/read-hierarchical-edn db-root "triggers.edn")
        views-edn (utils/read-hierarchical-edn db-root "views.edn")
        policies-edn (utils/read-hierarchical-edn db-root "policies.edn")

        expected-functions (fetchers/expected-extended-object-names :function functions-edn)
        expected-triggers (fetchers/expected-extended-object-names :trigger triggers-edn)
        expected-views (fetchers/expected-extended-object-names :view views-edn)
        expected-policies (fetchers/expected-extended-object-names :policy policies-edn)

        db-functions (fetchers/fetch-functions db)
        db-triggers (fetchers/fetch-triggers db)
        db-views (fetchers/fetch-views db)
        db-policies (fetchers/fetch-policies db)

        missing-functions (sort (set/difference (:expected expected-functions) db-functions))
        missing-triggers (sort (set/difference (:expected expected-triggers) db-triggers))
        missing-views (sort (set/difference (:expected expected-views) db-views))
        missing-policies (sort (set/difference (:expected expected-policies) db-policies))]

    {:timestamp (utils/now-iso)
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

#!/usr/bin/env bb

;; Enhanced Clojure project creation script with proper EDN parsing
;; This replaces the bash script with a more robust Babashka implementation

(require '[clojure.edn :as edn]
  '[clojure.java.io :as io]
  '[clojure.string :as str]
  '[clojure.java.shell :as shell]
  '[babashka.fs :as fs]
  '[cheshire.core :as json])

;; Configuration manifest for file updates
(def config-manifest
  {:replacements
   [{:file "config/base.edn"
     :type :edn
     :updates [{:path [:database-name] :key :db-name}
               {:path [:test-database-name] :key :test-db-name}]}
    {:file "docker-compose.yml"
     :type :text
     :patterns [{:pattern #"POSTGRES_DB: bookkeeping"
                 :replacement "POSTGRES_DB: {{db-name}}"}
                {:pattern #"POSTGRES_DB: bookkeeping-test"
                 :replacement "POSTGRES_DB: {{test-db-name}}"}]}
    {:file "package.json"
     :type :json
     :updates [{:path ["name"] :key :package-name}]}
    {:file "resources/public/index.html"
     :type :text
     :patterns [{:pattern #"<title>.*?</title>"
                 :replacement "<title>{{title}}</title>"}]}
    {:file ".secrets.edn"
     :type :edn
     :updates [{:path [:database-name] :key :db-name}
               {:path [:test-database-name] :key :test-db-name}]}
    {:file "deps.edn"
     :type :edn
     :updates [{:path [:aliases :migrations-dev :exec-args :jdbc-url]
                :transform (fn [v db-name]
                             (str/replace v "bookkeeping" db-name))}
               {:path [:aliases :migrations-test :exec-args :jdbc-url]
                :transform (fn [v test-db-name]
                             (str/replace v "bookkeeping-test" test-db-name))}]}
    {:file "src/app/backend/db/init.clj"
     :type :text
     :patterns [{:pattern #"\"bookkeeping\""
                 :replacement "\"{{db-name}}\""}]}]})

(defn expand-home [path]
  (if (str/starts-with? path "~")
    (str (System/getProperty "user.home") (subs path 1))
    path))

(defn to-title-case [s]
  (str/join " " (map str/capitalize (str/split s #"[\s-_]+"))))

(defn to-snake-case [s]
  (-> s
    (str/replace #"[^\w\s-]" "")
    (str/replace #"[\s-]+" "_")
    str/lower-case))

(defn load-config-file [config-path]
  (try
    (let [expanded-path (expand-home config-path)]
      (when (fs/exists? expanded-path)
        (edn/read-string (slurp expanded-path))))
    (catch Exception e
      (println "Error reading config file:" (.getMessage e))
      nil)))

(defn apply-edn-update [content path value]
  (let [data (edn/read-string content)]
    (pr-str (assoc-in data path value))))

(defn apply-json-update [content path value]
  (let [json-data (json/parse-string content true)]
    (json/generate-string (assoc-in json-data path value) {:pretty true})))

(defn apply-text-patterns [content patterns config]
  (reduce (fn [text {:keys [pattern replacement]}]
            (let [expanded-replacement (reduce (fn [s [k v]]
                                                 (str/replace s (str "{{" (name k) "}}") v))
                                         replacement
                                         config)]
              (str/replace text pattern expanded-replacement)))
    content
    patterns))

(defn update-file [{:keys [file type updates patterns]} target-dir config]
  (let [file-path (str target-dir "/" file)]
    (when (fs/exists? file-path)
      (try
        (let [content (slurp file-path)
              updated-content
              (case type
                :edn (reduce (fn [c {:keys [path key transform]}]
                               (if transform
                                 (let [data (edn/read-string c)
                                       current-val (get-in data path)
                                       new-val (transform current-val (get config key))]
                                   (pr-str (assoc-in data path new-val)))
                                 (apply-edn-update c path (get config key))))
                       content
                       updates)
                :json (reduce (fn [c {:keys [path key]}]
                                (apply-json-update c path (get config key)))
                        content
                        updates)
                :text (apply-text-patterns content patterns config)
                content)]
          (spit file-path updated-content)
          (println "✓ Updated" file))
        (catch Exception e
          (println "✗ Error updating" file ":" (.getMessage e)))))))

(defn copy-template-files [source target exclude-patterns]
  (doseq [file (file-seq (io/file source))
          :when (and (.isFile file)
                  (not (some #(re-find % (.getPath file)) exclude-patterns)))]
    (let [relative-path (str/replace (.getPath file) (str source "/") "")
          target-file (io/file target relative-path)]
      (io/make-parents target-file)
      (io/copy file target-file))))

(defn generate-readme [config target-dir]
  (let [readme-content (str "# " (:title config) "\n\n"
                         "A Clojure/ClojureScript application based on the hosting template.\n\n"
                         "## Getting Started\n\n"
                         "### Prerequisites\n"
                         "- Java 11+\n"
                         "- Node.js 20+\n"
                         "- PostgreSQL 12+\n"
                         "- Clojure CLI tools\n\n"
                         "### Database Setup\n"
                         "1. Create databases:\n"
                         "   ```sql\n"
                         "   CREATE DATABASE " (:db-name config) ";\n"
                         "   CREATE DATABASE " (:test-db-name config) ";\n"
                         "   ```\n\n"
                         "2. Update `.secrets.edn` with your database credentials\n\n"
                         "### Development\n"
                         "```bash\n"
                         "# Install dependencies\n"
                         "npm install\n\n"
                         "# Run database migrations\n"
                         "clj -X:migrations-dev\n\n"
                         "# Start the application\n"
                         "./scripts/run-app.sh\n\n"
                         "# In another terminal, start CSS compilation\n"
                         "npm run develop\n"
                         "```\n\n"
                         "### Testing\n"
                         "```bash\n"
                         "# Run backend tests\n"
                         "clj -M:test\n\n"
                         "# Run frontend tests\n"
                         "./cli-tools/test_scripts/full_workflow.sh\n"
                         "```\n")]
    (spit (str target-dir "/README.md") readme-content)
    (println "✓ Generated README.md")))

;; Validation functions (Improvement #2)
(defn validate-edn-file [file-path]
  (try
    (edn/read-string (slurp file-path))
    {:valid true :file file-path}
    (catch Exception e
      {:valid false :file file-path :error (.getMessage e)})))

(defn validate-project [target-dir]
  (println "\n=== Validating generated project ===")
  (let [required-files ["deps.edn" "package.json" "shadow-cljs.edn"
                        "resources/db/models.edn" "config/base.edn"]
        edn-files (filter #(str/ends-with? % ".edn") required-files)
        validation-errors (atom [])]

    ;; Check required files exist
    (doseq [file required-files]
      (let [file-path (str target-dir "/" file)]
        (if (fs/exists? file-path)
          (println "✓" file "exists")
          (do
            (println "✗" file "missing")
            (swap! validation-errors conj {:type :missing-file :file file})))))

    ;; Validate EDN files
    (doseq [file edn-files]
      (let [file-path (str target-dir "/" file)
            result (validate-edn-file file-path)]
        (if (:valid result)
          (println "✓" file "is valid EDN")
          (do
            (println "✗" file "has invalid EDN:" (:error result))
            (swap! validation-errors conj result)))))

    ;; Check directory structure
    (let [required-dirs ["src/app/backend" "src/app/frontend" "src/app/shared"
                         "resources/public" "cli-tools" "scripts" "test"]]
      (doseq [dir required-dirs]
        (let [dir-path (str target-dir "/" dir)]
          (if (fs/directory? dir-path)
            (println "✓ Directory" dir "exists")
            (do
              (println "✗ Directory" dir "missing")
              (swap! validation-errors conj {:type :missing-dir :dir dir}))))))

    ;; Summary
    (let [errors @validation-errors]
      (if (empty? errors)
        (do
          (println "\n✅ Project validation passed!")
          true)
        (do
          (println (str "\n❌ Project validation failed with " (count errors) " errors"))
          false)))))

(defn create-project [project-name config]
  (let [target-dir (or (:target-dir config) project-name)
        source-dir (System/getProperty "user.dir")
        exclude-patterns [#"\.git" #"node_modules" #"target" #"tmp" #"\.cpcache" #"out" #"resources/public/assets/js" #"create-new-app" #"new-app-config\.edn"]]

    (println "\n=== Creating new project:" project-name "===")
    (println "Configuration:")
    (doseq [[k v] config]
      (println (str "  " (name k) ": " v)))

    ;; Create target directory
    (fs/create-dirs target-dir)

    ;; Copy template files
    (println "\nCopying template files...")
    (copy-template-files source-dir target-dir exclude-patterns)

    ;; Update files based on manifest
    (println "\nApplying customizations...")
    (doseq [replacement (:replacements config-manifest)]
      (update-file replacement target-dir config))

    ;; Generate README
    (generate-readme config target-dir)

    ;; Create .gitignore if missing
    (let [gitignore-path (str target-dir "/.gitignore")]
      (when-not (fs/exists? gitignore-path)
        (spit gitignore-path (str/join "\n" ["node_modules/" "target/" ".cpcache/"
                                             "out/" ".nrepl-port" ".lsp/" ".clj-kondo/"
                                             "resources/public/assets/js/"]))
        (println "✓ Created .gitignore")))

    ;; Initialize git repository
    (when-not (fs/exists? (str target-dir "/.git"))
      (shell/sh "git" "init" :dir target-dir)
      (println "✓ Initialized git repository"))

    ;; Validate the generated project
    (validate-project target-dir)

    (println "\n✨ Project created successfully at:" target-dir)
    (println "\nNext steps:")
    (println "  cd" target-dir)
    (println "  npm install")
    (println "  # Update .secrets.edn with your database credentials")
    (println "  clj -X:migrations-dev")
    (println "  ./scripts/run-app.sh")))

(defn prompt-for-value [prompt current-value]
  (print (str prompt " [" current-value "]: "))
  (flush)
  (let [input (read-line)]
    (if (str/blank? input)
      current-value
      input)))

(defn interactive-config [initial-config]
  (println "\n=== Interactive Configuration ===")
  (let [title (prompt-for-value "Project title" (:title initial-config))
        db-name (prompt-for-value "Database name" (:db-name initial-config))
        package-name (prompt-for-value "Package name" (:package-name initial-config))
        target-dir (prompt-for-value "Target directory" (:target-dir initial-config))]
    {:title title
     :db-name db-name
     :test-db-name (str db-name "_test")
     :package-name package-name
     :target-dir target-dir}))

(defn -main [& args]
  (if (empty? args)
    (do
      (println "Usage: bb create-new-app.clj <project-name> [options]")
      (println "\nOptions:")
      (println "  --config <file>     Load configuration from EDN file")
      (println "  --title <title>     Project title for HTML")
      (println "  --db-name <name>    Database name")
      (println "  --package-name <name> NPM package name")
      (println "  --target-dir <dir>  Target directory (default: project-name)")
      (System/exit 1))

    (let [project-name (first args)
          ;; Parse command line arguments
          parsed-args (apply hash-map (rest args))
          config-file (get parsed-args "--config")

          ;; Build initial config
          base-config {:title (to-title-case project-name)
                       :db-name (to-snake-case project-name)
                       :test-db-name (str (to-snake-case project-name) "_test")
                       :package-name (str/lower-case project-name)
                       :target-dir project-name}

          ;; Load from config file if specified
          file-config (when config-file
                        (load-config-file config-file))

          ;; Merge configurations (CLI args override file config)
          initial-config (merge base-config
                           file-config
                           (when (get parsed-args "--title")
                             {:title (get parsed-args "--title")})
                           (when (get parsed-args "--db-name")
                             {:db-name (get parsed-args "--db-name")
                              :test-db-name (str (get parsed-args "--db-name") "_test")})
                           (when (get parsed-args "--package-name")
                             {:package-name (get parsed-args "--package-name")})
                           (when (get parsed-args "--target-dir")
                             {:target-dir (get parsed-args "--target-dir")}))

          ;; Check for auto-config file
          auto-config-exists? (and (not config-file)
                                (fs/exists? "new-app-config.edn"))

          final-config (if auto-config-exists?
                         (do
                           (println "\nFound new-app-config.edn")
                           (println "Current configuration:")
                           (doseq [[k v] initial-config]
                             (println (str "  " (name k) ": " v)))
                           (print "\nUse this configuration? (y/n/m for modify): ")
                           (flush)
                           (let [choice (read-line)]
                             (case (str/lower-case (or choice "y"))
                               "y" initial-config
                               "m" (interactive-config initial-config)
                               "n" (interactive-config base-config)
                               initial-config)))
                         initial-config)]

      (create-project project-name final-config))))

;; For JSON parsing support
 ;; Execute main when run as script
(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

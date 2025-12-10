(ns core
  (:require
   [app.backend.core :as backend]
   [nrepl.server :as nrepl]
   [shadow.cljs.devtools.api :as shadow.api]
   [shadow.cljs.devtools.server :as shadow.server]
   [system.core :refer [restart-system start-system]]
   [system.watchers :as watchers]))

(defn suppress-stderr []
  (let [err (java.io.PrintStream. "/dev/null")]
    (System/setErr err)))

(defn handle-models-change [file-path]
  (println "🔄 Models.edn changed - restarting system...")
  (restart-system file-path))

(defn write-postgres-env-file []
  (let [config (backend/load-config {})
        {:keys [host port dbname user password]} (:database @config)
        env-content (str "POSTGRES_HOST=" host "\n"
                      "POSTGRES_PORT=" port "\n"
                      "POSTGRES_DATABASE=" dbname "\n"
                      "POSTGRES_USER=" user "\n"
                      "POSTGRES_PASSWORD=" password "\n")]
    (spit ".postgres.env" env-content)
    (println "✅ Postgres environment variables written to .postgres.env")))

(defn start-dev []
  (println "START-DEV CALLED!")
  (write-postgres-env-file)
  (suppress-stderr)
  (start-system)
  (watchers/watch-backend restart-system)
  (watchers/watch-models handle-models-change)
  ;;(println "CREATING GO BLOCK FOR POSTCSS WATCH")
  ;;(go (watchers/postcss-watch))
  (shadow.server/start!)
  ;; Start watching builds on main thread
  (shadow.api/watch :app)
  ;;(shadow.api/watch :test)
  ;; Select browser REPL on main thread before starting nREPL
  (shadow.api/nrepl-select :app)
  (nrepl/start-server :port 7888)
  (println "nREPL server started on port 7888")
  (println "Shadow-cljs watching :app and :test builds")
  (println "📋 File watchers active:")
  (println "   • Backend files (triggers system restart)")
  (println "   • Models.edn (notifies about schema changes)"))

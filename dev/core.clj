(ns core
  (:require
    [app.template.backend.core :as backend]
    [clojure.java.io :as io]
    [nrepl.server :as nrepl]
    [shadow.cljs.devtools.api :as shadow.api]
    [shadow.cljs.devtools.server :as shadow.server]
    [system.core :refer [restart-system start-system]]
    [system.state :as system-state]
    [system.watchers :as watchers]
    [taoensso.timbre :as log]))

(defn suppress-stderr []
  (let [env (System/getenv "DEV_SUPPRESS_STDERR")
        suppress? (not (contains? #{"0" "false" "FALSE" "no" "NO"} (or env "")))]
    (if suppress?
      (do
        (log/warn {:event :dev/stderr-suppressed
                   :cwd (System/getProperty "user.dir")
                   :hint "Set DEV_SUPPRESS_STDERR=false to keep stderr visible"})
        (let [err (java.io.PrintStream. "/dev/null")]
          (System/setErr err)))
      (log/info {:event :dev/stderr-not-suppressed
                 :cwd (System/getProperty "user.dir")
                 :DEV_SUPPRESS_STDERR env}))))

(defn handle-models-change [file-path]
  (log/info {:event :models/changed
             :file-path file-path})
  (restart-system file-path))

(defn write-postgres-env-file
  "Write a local Postgres env file for tooling (MCP, scripts, etc).

  Security/UX notes:
  - This file contains credentials; keep it out of git.
  - We intentionally write under `tmp/` to avoid accidental commits."
  []
  (let [config (backend/load-config {})
        {:keys [host port dbname user password]} (:database @config)
        env-file "tmp/.postgres.env"
        env-content (str "POSTGRES_HOST=" host "\n"
                      "POSTGRES_PORT=" port "\n"
                      "POSTGRES_DATABASE=" dbname "\n"
                      "POSTGRES_USER=" user "\n"
                      "POSTGRES_PASSWORD=" password "\n")]
    (io/make-parents env-file)
    (spit env-file env-content)
    (log/info {:event :dev/postgres-env-written
               :file env-file
               :host host
               :port port
               :dbname dbname
               :user user})))

(defn start-dev []
  (log/info {:event :dev/start-called
             :cwd (System/getProperty "user.dir")
             :thread (.getName (Thread/currentThread))})
  (write-postgres-env-file)
  (suppress-stderr)
  (start-system)
  (watchers/watch-backend restart-system)
  (watchers/watch-models handle-models-change)
  (log/info {:event :dev/watchers-state
             :backend-watcher-set? (boolean @system-state/backend-watcher)
             :models-watcher-set? (boolean @system-state/models-watcher)})
  ;;(println "CREATING GO BLOCK FOR POSTCSS WATCH")
  ;;(go (watchers/postcss-watch))
  (shadow.server/start!)
  ;; Start watching builds on main thread
  (shadow.api/watch :app)
  ;;(shadow.api/watch :test)
  ;; Select browser REPL on main thread before starting nREPL
  (shadow.api/nrepl-select :app)
  (nrepl/start-server :port 7888)
  (log/info {:event :dev/nrepl-started
             :port 7888})
  (log/info {:event :dev/shadow-watch-started
             :builds [:app]
             :note "(shadow.api/watch :test) currently commented out"})
  (log/info {:event :dev/start-dev-finished
             :watchers [:backend :models]
             :admin-url "http://localhost:8085"}))

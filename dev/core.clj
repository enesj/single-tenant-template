(ns core
  (:require
    [app.template.backend.core :as backend]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [nrepl.server :as nrepl]
    [shadow.cljs.devtools.api :as shadow.api]
    [shadow.cljs.devtools.server :as shadow.server]
    [system.core :refer [restart-system start-system]]
    [system.state :as system-state]
    [system.watchers :as watchers]
    [taoensso.timbre :as log])
  (:import
    [java.net BindException InetSocketAddress ServerSocket]))

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

(def ^:private dev-nrepl-port 7888)

(defn- port-free?
  [port]
  (try
    (with-open [socket (ServerSocket.)]
      (.setReuseAddress socket true)
      (.bind socket (InetSocketAddress. "127.0.0.1" port))
      true)
    (catch BindException _
      false)
    (catch Exception _
      false)))

(defn- shadow-already-running?
  [e]
  (and (instance? clojure.lang.ExceptionInfo e)
    (str/includes? (or (ex-message e) "")
      "shadow-cljs already running in project")))

(defn- ensure-shadow-watch!
  []
  (try
    (shadow.server/start!)
    (shadow.api/watch :app)
    (shadow.api/nrepl-select :app)
    (log/info {:event :dev/shadow-watch-started
               :builds [:app]
               :status :started
               :note "(shadow.api/watch :test) currently commented out"})
    {:status :started}
    (catch clojure.lang.ExceptionInfo e
      (if (shadow-already-running? e)
        (let [message (ex-message e)]
          (log/warn {:event :dev/shadow-already-running
                     :status :reused
                     :message message
                     :action "Reusing external shadow-cljs instance; skipping local shadow start/watch."
                     :hint "Stop the existing shadow-cljs JVM if you need a clean single-process dev session."})
          {:status :reused
           :message message})
        (throw e)))))

(defn- ensure-dev-nrepl!
  []
  (if (port-free? dev-nrepl-port)
    (do
      (nrepl/start-server :port dev-nrepl-port)
      (log/info {:event :dev/nrepl-started
                 :port dev-nrepl-port
                 :status :started})
      {:status :started
       :port dev-nrepl-port})
    (do
      (log/warn {:event :dev/nrepl-already-running
                 :status :reused
                 :port dev-nrepl-port
                 :action "Skipping duplicate nREPL start."
                 :hint "Connect to the existing dev nREPL or stop it before starting another dev JVM."})
      {:status :reused
       :port dev-nrepl-port})))

(defn start-dev
  []
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
  (let [shadow-status (ensure-shadow-watch!)
        nrepl-status (ensure-dev-nrepl!)]
    (log/info {:event :dev/start-dev-finished
               :watchers [:backend :models]
               :admin-url "http://localhost:8085"
               :shadow-status (:status shadow-status)
               :nrepl-status (:status nrepl-status)})))

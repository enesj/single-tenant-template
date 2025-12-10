(ns app.backend.core
  (:require
    [aero.core :as aero]
    [app.backend.webserver :as webserver]
    [app.shared.json-config :as json-config]
    [app.shared.model-naming :as model-naming]
    [app.template.di.config :as template-di]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.tools.namespace.repl :as tools-repl]
    [hikari-cp.core :as cp]
    [next.jdbc]
    [next.jdbc.date-time]
    [next.jdbc.prepare]
    [taoensso.timbre :as log])
  (:import
    [java.sql PreparedStatement Timestamp]
    [java.time Instant]))

;; Ensures that java.time.Instant objects are correctly handled by the JDBC driver
(extend-protocol next.jdbc.prepare/SettableParameter
  java.time.Instant
  (set-parameter [^Instant v ^PreparedStatement ps ^long i]
    (.setTimestamp ps i (Timestamp/from v))))

(next.jdbc.date-time/read-as-instant)

;; ============================================================================
;; Closeable Wrapper for Data Structures
;; ============================================================================

(defrecord CloseableData [data]
  java.io.Closeable
  (close [_] nil)  ; No-op close for data structures

  ;; Make it behave like the wrapped data for most operations
  clojure.lang.IDeref
  (deref [_] data))

(defn closeable-data
  "Wrap data in a closeable container"
  [data]
  (CloseableData. data))

;; ============================================================================
;; Replacement Functions for Deleted Dependencies
;; ============================================================================

(defn load-config
  "Load configuration using Aero - replacement for components/config-component"
  [config-options]
  (closeable-data
    (try
      (let [profile         (get config-options :profile :dev)        ; Allow profile override, default to :dev
            config-resource (io/resource "base.edn")]
        (if config-resource
          (aero/read-config config-resource {:profile profile})
          ;; Fallback: try direct file path
          (let [config-file (io/file "config/base.edn")]
            (aero/read-config config-file {:profile profile})))) ; Use correct port for tests
      (catch Exception e
        (log/error e "Error loading config with Aero, using fallback")
        (throw e)))))

(defn load-models-data
  "Load models data from resources and attach kebab-case metadata for runtime use."
  []
  (closeable-data
    (try
      (let [models-resource (io/resource "db/models.edn")
            raw-models (if models-resource
                         (-> models-resource slurp edn/read-string)
                         ;; Fallback: try direct file path
                         (let [models-file (io/file "resources/db/models.edn")]
                           (if (.exists models-file)
                             (-> models-file slurp edn/read-string)
                             (do
                               (log/error "Could not find models.edn file at db/template/models.edn!")
                               (throw (ex-info "Models data is required but not found" {}))))))]
        (model-naming/attach-app-models raw-models))
      (catch Exception e
        (log/error e "Error loading models data")
        (throw e)))))

(defn create-conn-pool
  "Create database connection pool - replacement for db/create-conn-pool"
  [config]
  (let [db-config       (:database config)
        hikari-defaults (:hikari-cp config)

        ;; Build JDBC URL strictly from provided config (or environment)
        db-name         (or (:dbname db-config)
                            (throw (ex-info "Database name missing in config" {:database db-config})))
        jdbc-url        (or (:jdbc-url db-config)
                           (when (every? db-config [:host :port :dbname :user])
                             (format "jdbc:postgresql://%s:%s/%s?user=%s%s"
                               (:host db-config) (:port db-config) db-name (:user db-config)
                               (if-let [pwd (:password db-config)] (str "&password=" pwd) "")))
                           (throw (ex-info "Provide :jdbc-url or host/port/dbname/user in :database config"
                                    {:database db-config})))

        ;; Validate that we're not using the system username as database name
        _               (when (re-find #"database.*enes" (str jdbc-url))
                          (throw (ex-info "Configuration error: database name defaulting to system username"
                                   {:jdbc-url jdbc-url
                                    :user     (System/getProperty "user.name")})))

        hikari-config   (merge hikari-defaults
                          {:pool-name     "db-pool"
                           :adapter       "postgresql"
                           :jdbc-url      jdbc-url
                           :database-name db-name
                            :server-name   (:host db-config)
                            :port-number   (:port db-config)
                            :username      (:user db-config)
                            :password      (:password db-config)})]
    (cp/make-datasource hikari-config)))

(defn new-scheduler
  "Create a simple scheduler - replacement for helpers/new-scheduler"
  []
  (reify
    java.io.Closeable
    (close [_])
    Object
    (toString [_] "SimpleScheduler")))

(defn await-finish
  "Await scheduler finish - replacement for .await-finish"
  [_scheduler]
  (try
    ;; Block indefinitely until interrupted
    (loop []
      (Thread/sleep 1000)  ; Sleep for 1 second intervals
      (recur))
    (catch InterruptedException _
      (log/info "System shutdown requested"))))

(defn create-service-container
  "Create the complete service container with all dependencies wired.
   Now uses the DI container for proper lifecycle management."
  [database models-data config]
  (log/info "Creating service container using DI container...")
  ;; Use the new DI-based service container and wrap it for closeable compatibility
  (closeable-data (template-di/create-service-container config database models-data nil)))

(defn my-system [config-options]
  (fn [do-with-state]
    (with-open [config            (load-config config-options)
                models-data       (load-models-data)
                database          (create-conn-pool @config)
                service-container (try
                                    (create-service-container database @models-data @config)
                                    (catch Exception e
                                      (log/error e "Service container creation failed:" (.getMessage e))
                                      nil))
                webserver         (webserver/create-webserver
                                    (-> @config :webserver :host)
                                    (-> @config :webserver :port)
                                    database
                                    @service-container)]

      (log/info "🚀 Starting system - host:" (-> @config :webserver :host) ", port:" (-> @config :webserver :port) "- Auto-restart works!")
      (json-config/init!)
      (do-with-state {:config            @config
                      :database          database
                      :models-data       @models-data
                      :service-container @service-container
                      :ws                webserver}))))

(def with-my-system
  (my-system {:interval "every now and then"}))

(def with-test-system
  "Test system using :test profile (port 8081, test database)"
  (my-system {:profile :test}))

(def scheduler (new-scheduler))

(defn await-scheduler [_state]
  (await-finish scheduler))

(def init (atom #(throw (ex-info "init not set" {}))))

(defn main []
  (reset! init #(with-my-system await-scheduler))
  (future-call @init))

(comment
  (defn publishing-state [do-with-state target-atom]
    #(do (reset! target-atom %)
       (try (do-with-state %)
         (finally (reset! target-atom nil)))))

  (def state* (atom nil))

  @state*

  (reset! init #(with-my-system (-> await-scheduler
                                  (publishing-state state*))))
  ; Run
  (def instance (future-call @init))
  ; Continue using REPL and eventually interrupt
  (future-cancel instance)

  instance

  (future-cancelled? instance)

  (tools-repl/refresh)

  {})

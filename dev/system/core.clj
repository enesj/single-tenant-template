(ns system.core
  (:require
    [app.template.backend.core :refer [await-scheduler init load-config with-my-system]]
    [clojure.stacktrace :as stacktrace]
    [clojure.tools.namespace.repl :refer [refresh refresh-all set-refresh-dirs]]
    [system.state :refer [instance state]]
    [taoensso.timbre :as log])
  (:import
    [java.net BindException InetSocketAddress ServerSocket]))

(defn write-exception
  "Pretty-print an exception stacktrace to stdout (dev convenience)."
  [e]
  (stacktrace/print-stack-trace e))

(set-refresh-dirs "src" "dev" "config")

(log/info {:event :system/refresh-dirs-set
           :refresh-dirs ["src" "dev" "config"]
           :cwd (System/getProperty "user.dir")
           :thread (.getName (Thread/currentThread))})

(defn- lifecycle-lock-object
  []
  (let [lock-var (or (ns-resolve 'system.state 'lifecycle-lock)
                   (intern 'system.state 'lifecycle-lock (Object.)))]
    @lock-var))

(defn publishing-state [do-with-state target-atom]
  #(do (reset! target-atom %)
     (try (do-with-state %)
       (finally
         (reset! target-atom nil)))))

(defn start-system
  []
  (let [lock (lifecycle-lock-object)]
    (locking lock
      (log/info {:event :system/start-requested
                 :cwd (System/getProperty "user.dir")
                 :thread (.getName (Thread/currentThread))
                 :instance-realized? (realized? @instance)})
      (reset! init #(try
                      (with-my-system (-> (fn [state] (await-scheduler state))
                                        (publishing-state state)))
                      (catch Exception e
                        ;; Avoid relying on stderr (dev/core may suppress it).
                        (log/error e {:event :system/start-failed})
                        (write-exception e)
                        (throw e))))
      (try
        (swap! instance #(if (realized? %)
                           (future-call @init)
                           (throw (ex-info "already running" {}))))
        (log/info {:event :system/start-submitted
                   :instance-realized? (realized? @instance)})
        (catch Exception e
          (log/error e {:event :system/start-submit-failed})
          (throw e))))))

(defn- normalize-port [port]
  (cond
    (integer? port) port
    (string? port) (Integer/parseInt port)
    :else nil))

(defn- configured-web-port
  []
  (try
    (with-open [config (load-config {})]
      (some-> @config :webserver :port normalize-port))
    (catch Exception e
      (log/warn e {:event :system/web-port-fallback-failed})
      nil)))

(defn- current-web-port
  []
  (or (some-> @state :config :webserver :port normalize-port)
    (configured-web-port)))

(defn- log-port-not-cleared!
  [shutdown-status]
  (when (and (:port shutdown-status)
          (not (:port-cleared? shutdown-status)))
    (log/warn {:event :system/port-still-bound
               :thread (.getName (Thread/currentThread))
               :shutdown-status shutdown-status})))

(defn- port-free?
  [port]
  (if-not port
    true
    (try
      (with-open [socket (ServerSocket.)]
        (.setReuseAddress socket true)
        (.bind socket (InetSocketAddress. "127.0.0.1" port))
        true)
      (catch BindException _
        false)
      (catch Exception _
        false))))

(defn- await-stop-complete!
  [port]
  (let [deadline (+ (System/currentTimeMillis) 5000)]
    (loop []
      (let [state-cleared? (nil? @state)
            port-cleared? (port-free? port)]
        (if (or (and state-cleared? port-cleared?)
              (>= (System/currentTimeMillis) deadline))
          {:state-cleared? state-cleared?
           :port port
           :port-cleared? port-cleared?}
          (do
            (Thread/sleep 50)
            (recur)))))))

(defn stop-system
  []
  (let [lock (lifecycle-lock-object)]
    (locking lock
      (let [instance-future @instance
            web-port (current-web-port)]
        (log/info {:event :system/stop-requested
                   :instance-realized? (realized? instance-future)
                   :thread (.getName (Thread/currentThread))
                   :web-port web-port})
        (future-cancel instance-future)
        (try
          @instance-future
          (let [shutdown-status (await-stop-complete! web-port)]
            (log-port-not-cleared! shutdown-status)
            (log/info (merge {:event :system/stop-finished}
                        shutdown-status))
            shutdown-status)
          (catch java.util.concurrent.CancellationException _e
            (let [shutdown-status (await-stop-complete! web-port)]
              (log-port-not-cleared! shutdown-status)
              (log/info (merge {:event :system/stopped
                                :reason :future-cancelled}
                          shutdown-status))
              shutdown-status))
          (catch java.util.concurrent.ExecutionException e
            (let [shutdown-status (await-stop-complete! web-port)]
              (log-port-not-cleared! shutdown-status)
              (log/warn e {:event :system/stop-recoverable-error
                           :reason :prior-instance-failed})
              (log/info (merge {:event :system/stop-finished
                                :reason :prior-instance-failed}
                          shutdown-status))
              shutdown-status))
          (catch Exception e
            (log/error e {:event :system/stop-error})
            (throw e)))))))

(defn- code-file? [filename]
  (and filename (re-matches #"[^.].*\.(clj|cljc)$" filename)))

(defn- refresh-namespaces [filename]
  (let [refresh-mode (if (code-file? filename) :refresh :refresh-all)]
    (log/info {:event :system/refresh-requested
               :filename filename
               :refresh-mode refresh-mode
               :thread (.getName (Thread/currentThread))})
    (let [refresh-result (if (code-file? filename)
                           (refresh :after 'system.core/start-system)
                           (refresh-all :after 'system.core/start-system))]
      (log/info {:event :system/refresh-result
                 :filename filename
                 :refresh-mode refresh-mode
                 :result-type (some-> refresh-result class str)})
      (when (instance? Exception refresh-result)
        (throw refresh-result)))))

(defn restart-system
  "Stops system, refreshes changed namespaces in REPL and starts the system again."
  ([]
   (restart-system nil))
  ([filename]
   (let [lock (lifecycle-lock-object)]
     (locking lock
       (log/info {:event :system/restart-requested
                  :filename filename
                  :thread (.getName (Thread/currentThread))})
       (try
         (let [shutdown-status (stop-system)]
           (when (and (:port shutdown-status)
                   (not (:port-cleared? shutdown-status)))
             (throw (ex-info "Dev web port did not clear after stop"
                      {:event :system/port-not-cleared
                       :filename filename
                       :shutdown-status shutdown-status})))
           (refresh-namespaces filename)
           (log/info {:event :system/restart-finished
                      :filename filename}))
         (catch Exception e
           ;; Avoid relying on stderr (dev/core may suppress it).
           (log/error e {:event :system/restart-failed
                         :filename filename})
           (write-exception e)))))))

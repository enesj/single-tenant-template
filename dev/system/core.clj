(ns system.core
  (:require
   [app.template.backend.core :refer [await-scheduler init with-my-system]]
   [clojure.stacktrace :as stacktrace]
   [clojure.tools.namespace.repl :refer [refresh refresh-all set-refresh-dirs]]
   [system.state :refer [instance state]]
   [taoensso.timbre :as log]))

(defn write-exception
  "Pretty-print an exception stacktrace to stdout (dev convenience)."
  [e]
  (stacktrace/print-stack-trace e))

(set-refresh-dirs "src" "dev" "config")

(log/info {:event :system/refresh-dirs-set
           :refresh-dirs ["src" "dev" "config"]
           :cwd (System/getProperty "user.dir")
           :thread (.getName (Thread/currentThread))})

(defn publishing-state [do-with-state target-atom]
  #(do (reset! target-atom %)
       (try (do-with-state %)
            (finally
              (reset! target-atom nil)))))

(defn start-system []
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
      (throw e))))

(defn stop-system []
  (let [instance-future @instance]
    (log/info {:event :system/stop-requested
               :instance-realized? (realized? instance-future)
               :thread (.getName (Thread/currentThread))})
    (future-cancel instance-future)
    (try
      @instance-future
      (log/info {:event :system/stop-finished})
      (catch java.util.concurrent.CancellationException _e
        (log/info {:event :system/stopped
                   :reason :future-cancelled}))
      (catch Exception e
        (log/error e {:event :system/stop-error})
        (throw e)))))

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
   (log/info {:event :system/restart-requested
              :filename filename
              :thread (.getName (Thread/currentThread))})
   (try
     (stop-system)
     (refresh-namespaces filename)
     (log/info {:event :system/restart-finished
                :filename filename})
     (catch Exception e
       ;; Avoid relying on stderr (dev/core may suppress it).
       (log/error e {:event :system/restart-failed
                     :filename filename})
       (write-exception e)))))

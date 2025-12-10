(ns system.core
  (:require
   [app.backend.core :refer [await-scheduler init with-my-system]]
   [clojure.tools.namespace.repl :refer [refresh refresh-all set-refresh-dirs]]
   [io.aviso.exception :refer [write-exception]]
   [system.state :refer [instance state]]
   [taoensso.timbre :as log]))

(set-refresh-dirs "src" "dev" "config")

(defn publishing-state [do-with-state target-atom]
  #(do (reset! target-atom %)
       (try (do-with-state %)
            (finally (reset! target-atom nil)))))

(defn start-system []
  (log/info "Starting system")
  (reset! init #(try
                  (with-my-system (-> (fn [state] (await-scheduler state))
                                    (publishing-state state)))
                  (catch Exception e (do (write-exception e) (throw e)))))
  (swap! instance #(if (realized? %)
                     (future-call @init)
                     (throw (ex-info "already running" {})))))

(defn stop-system []
  (let [instance-future @instance]
    (future-cancel instance-future)
    (try @instance-future
         (catch java.util.concurrent.CancellationException _e (log/info "system stopped")))))

(defn- code-file? [filename]
  (and filename (re-matches #"[^.].*\.(clj|cljc)$" filename)))

(defn- refresh-namespaces [filename]
  (let [refresh-result (if (code-file? filename)
                         (refresh :after 'system.core/start-system)
                         (refresh-all :after 'system.core/start-system))]
    (when (instance? Exception refresh-result)
      (throw refresh-result))))

(defn restart-system
  "Stops system, refreshes changed namespaces in REPL and starts the system again."
  ([]
   (restart-system nil))
  ([filename]
   (try (stop-system)
        ;;(log-title "Reloading namespaces")
        (refresh-namespaces filename)

        (catch Exception e
          (write-exception e)))))

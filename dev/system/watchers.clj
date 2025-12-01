(ns system.watchers
  "Collection of system change watchers for backend, frontend and postcss.
  Every watcher detects changes in their corresponding namespaces and reflect
  changes by restarting/rerendering changed parts on the system."
  (:require
   [clojure.core.async :refer [go]]
   [hawk.core :as hawk]
   [io.aviso.ansi :as ansi]
   [system.state :refer [backend-watcher models-watcher postcss-watcher]]
   [taoensso.timbre :as log])
  (:import
   [java.util Timer TimerTask]))

;;# BACKEND WATCHER
;;# --------------------------------------------------------------------------

(defn- debounce [callback timeout]
  (let [timer (Timer.)
        task (atom nil)]
    (fn [& args]
      (when-let [running-task ^TimerTask @task]
        (.cancel running-task))

      (let [new-task (proxy [TimerTask] []
                       (run []
                         (apply callback args)
                         (reset! task nil)
                         (.purge timer)))]
        (reset! task new-task)
        (.schedule timer new-task timeout))
      (first args))))

(defn- clojure-file? [_ {:keys [file]}]
  (re-matches #"[^.].*(\.clj|\.edn|\.cljc)$" (.getName file)))

(defn watch-handler [context event]
  (binding [*ns* *ns*]
    (let [file-path (.getPath (:file event))
          file-name (.getName (:file event))]
      (println (ansi/yellow (str "Backend watcher triggered by: " file-path)))
      (println (ansi/yellow (str "File name: " file-name)))
      (println (ansi/yellow (str "Event type: " (:kind event))))
      (when (clojure-file? nil event)
        (println (ansi/green "Processing Clojure file change..."))
        ((:fn context) file-path))
      (when-not (clojure-file? nil event)
        (println (ansi/red "Ignoring non-Clojure file"))))
    context))

(defn watch-backend
  "Automatically restarts the system if backend related files are changed."
  [callback]
  (log/info "Starting backend file watcher")
  (let [watcher (hawk/watch! {:watcher :polling}
                  [{:paths ["src/app" "dev" "config" "vendor"]
                    :context (constantly {:fn callback})
                    :filter clojure-file?
                    :handler (debounce watch-handler 50)}])]
    (reset! backend-watcher watcher)))

(defn stop-backend-watcher []
  (hawk/stop! backend-watcher)
  (reset! backend-watcher nil))

;;# POSTCSS WATCHER
;;# --------------------------------------------------------------------------

(defn postcss-watch
  "Runs postcss watcher in parallel thread and redirects std output to main console."
  []
  (println (ansi/cyan "Starting postcss watcher"))
  (when @postcss-watcher
    (println (ansi/cyan "Stopping existing postcss watcher"))
    (.destroyForcibly @postcss-watcher))

  (let [pb (ProcessBuilder. ["npm" "run" "postcss:watch"])
        _ (.directory pb (java.io.File. "."))
        process (.start pb)]

    ;;(println (ansi/cyan (str "PostCSS watcher started with PID:" (.pid process))))
    ;;(reset! postcss-watcher process)

    ;; Return the process but don't wait for it
    process))

(defn reset-postcss-watch
  "Kills current postcss process and start the new one."
  []
  (println (ansi/red "RESET-POSTCSS-WATCH CALLED!"))
  (when @postcss-watcher
    (println (ansi/red "Destroying existing postcss watcher"))
    (.destroyForcibly @postcss-watcher))
  (println (ansi/red "Starting new postcss watcher in go block"))
  (go (postcss-watch)))

;;# MODELS.EDN WATCHER
;;# --------------------------------------------------------------------------

(defn- models-file? [_ {:keys [file]}]
  (= "models.edn" (.getName file)))

(defn models-watch-handler [context event]
  (binding [*ns* *ns*]
    (let [file-path (.getPath (:file event))]
      (when (models-file? nil event)
        ((:fn context) file-path)))
    context))

(defn watch-models
  "Watches for changes to models.edn file and triggers callback.
   Typically used to notify about schema changes that may require migration."
  [callback]
  (log/info "Starting models.edn file watcher")
  (let [watcher (hawk/watch! {:watcher :polling}
                  [{:paths ["resources/db"]
                    :context (constantly {:fn callback})
                    :filter models-file?
                    :handler (debounce models-watch-handler 100)}])]
    (reset! models-watcher watcher)))

(defn stop-models-watcher []
  (when @models-watcher
    (hawk/stop! @models-watcher)
    (reset! models-watcher nil)))

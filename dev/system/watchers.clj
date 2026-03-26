(ns system.watchers
  "Collection of system change watchers for backend, frontend and postcss.
  Every watcher detects changes in their corresponding namespaces and reflect
  changes by restarting/rerendering changed parts on the system."
  (:require
    [clojure.core.async :refer [go]]
    [hawk.core :as hawk]
    [system.state :refer [backend-watcher models-watcher postcss-watcher]]
    [taoensso.timbre :as log])
  (:import
    [java.util Timer TimerTask]))

;;# BACKEND WATCHER
;;# --------------------------------------------------------------------------

(defn- debounce [callback timeout]
  (let [timer (Timer. "dev-system-watchers-debounce" true)
        task (atom nil)]
    (fn [& args]
      (when-let [running-task ^TimerTask @task]
        (.cancel running-task))

      (let [new-task (proxy [TimerTask] []
                       (run []
                         (try
                           (apply callback args)
                           (catch Throwable t
                             ;; A thrown exception inside TimerTask can kill the Timer thread,
                             ;; effectively disabling future debounced tasks.
                             (log/error t {:event :watcher/debounce-callback-error
                                           :timeout-ms timeout
                                           :callback (str callback)}))
                           (finally
                             (reset! task nil)
                             (try
                               (.purge timer)
                               (catch Throwable _t
                                 ;; best-effort cleanup
                                 nil))))))]
        (reset! task new-task)
        (try
          (.schedule timer new-task timeout)
          (catch Throwable t
            (reset! task nil)
            (log/error t {:event :watcher/debounce-schedule-error
                          :timeout-ms timeout
                          :callback (str callback)}))))
      (first args))))

(defn- clojure-file-details [{:keys [file]}]
  (let [file-name (.getName file)
        file-path (.getPath file)
        matches-extension? (boolean (re-matches #"[^.].*(\.clj|\.edn|\.cljc)$" file-name))
  test-file? (boolean (re-find #"(^|/)test/" file-path))
        admin-ui-config-edn? (boolean (re-find #"/src/app/admin/frontend/config/[^/]+\.edn$" file-path))
        domain-ui-config-edn? (boolean (re-find #"/src/app/domain/frontend/.+/config/[^/]+\.edn$" file-path))
        excluded-edn? (or admin-ui-config-edn? domain-ui-config-edn?)
  excluded-path? test-file?
        ;; These EDN files are edited at runtime via /admin/admin-settings and /admin/user-settings;
        ;; restarting the dev system on each save causes disruptive full page reloads.
  ;; Test namespaces are not part of the dev app refresh dirs, so restarting the live
  ;; backend for `test/` edits only creates noisy/pointless restarts.
  passes? (and matches-extension? (not excluded-edn?) (not excluded-path?))]
       {:file-name file-name
        :file-path file-path
        :matches-extension? matches-extension?
  :test-file? test-file?
        :admin-ui-config-edn? admin-ui-config-edn?
        :domain-ui-config-edn? domain-ui-config-edn?
        :excluded-edn? excluded-edn?
  :excluded-path? excluded-path?
        :passes? passes?}))

(defn- clojure-file? [_ event]
  (:passes? (clojure-file-details event)))

(defn watch-handler [context event]
  (binding [*ns* *ns*]
    (let [details (clojure-file-details event)
          file-path (:file-path details)
          file-name (:file-name details)
          event-kind (:kind event)
          passes? (:passes? details)]
      (log/info {:event :watcher/event
                 :watcher :backend
                 :kind event-kind
                 :file-path file-path
                 :file-name file-name
                 :passes-filter? passes?
                 :cwd (System/getProperty "user.dir")
                 :thread (.getName (Thread/currentThread))
                 :details (dissoc details :file-path :file-name :passes?)})
      (when passes?
        (try
          ((:fn context) file-path)
          (catch Throwable t
            (log/error t {:event :watcher/restart-callback-error
                          :watcher :backend
                          :kind event-kind
                          :file-path file-path
                          :file-name file-name}))))
      (when-not passes?
        (log/info {:event :watcher/event-ignored
                   :watcher :backend
                   :kind event-kind
                   :file-path file-path
                   :file-name file-name
                   :details (dissoc details :file-path :file-name)})))
    context))

(defn watch-backend
  "Automatically restarts the system if backend related files are changed."
  [callback]
  (let [paths ["src/app" "dev" "config" "vendor"]
        cwd (System/getProperty "user.dir")
        path-info (mapv (fn [p]
                          (let [f (java.io.File. p)]
                            {:path p
                             :exists (.exists f)
                             :dir? (.isDirectory f)
                             :canonical-path (try (.getCanonicalPath f)
                                               (catch Throwable _t
                                                 (.getAbsolutePath f)))}))
                    paths)]
    (log/info {:event :watcher/start
               :watcher :backend
               :impl :hawk
               :mode :polling
               :cwd cwd
               :paths path-info
               :thread (.getName (Thread/currentThread))})
    (let [watcher (hawk/watch! {:watcher :polling}
                    [{:paths paths
                      :context (constantly {:fn callback})
                      :filter clojure-file?
                      :handler (debounce watch-handler 50)}])]
      (reset! backend-watcher watcher)
      (log/info {:event :watcher/started
                 :watcher :backend
                 :backend-watcher-set? (boolean @backend-watcher)
                 :watcher-class (some-> watcher class str)})
      watcher)))

(defn stop-backend-watcher []
  (when-let [watcher @backend-watcher]
    (try
      (hawk/stop! watcher)
      (catch Throwable t
        (log/error t {:event :watcher/stop-error
                      :watcher :backend}))))
  (reset! backend-watcher nil)
  (log/info {:event :watcher/stopped
             :watcher :backend
             :backend-watcher-set? (boolean @backend-watcher)}))

;;# POSTCSS WATCHER
;;# --------------------------------------------------------------------------

(defn postcss-watch
  "Runs postcss watcher in parallel thread and redirects std output to main console."
  []
  (log/info {:event :watcher/start
             :watcher :postcss
             :impl :process
             :cwd (System/getProperty "user.dir")
             :thread (.getName (Thread/currentThread))})
  (when @postcss-watcher
    (log/info {:event :watcher/stop-requested
               :watcher :postcss
               :reason :restart})
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
  (log/info {:event :watcher/restart-requested
             :watcher :postcss})
  (when @postcss-watcher
    (log/info {:event :watcher/stop-requested
               :watcher :postcss
               :reason :restart})
    (.destroyForcibly @postcss-watcher))
  (log/info {:event :watcher/start-requested
             :watcher :postcss
             :mode :core-async})
  (go (postcss-watch)))

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
  (log/info {:event :watcher/start
             :watcher :models
             :impl :hawk
             :mode :polling
             :cwd (System/getProperty "user.dir")
             :paths [{:path "resources/db"
                      :exists (.exists (java.io.File. "resources/db"))
                      :dir? (.isDirectory (java.io.File. "resources/db"))
                      :canonical-path (try (.getCanonicalPath (java.io.File. "resources/db"))
                                        (catch Throwable _t
                                          (.getAbsolutePath (java.io.File. "resources/db"))))}]
             :thread (.getName (Thread/currentThread))})
  (let [watcher (hawk/watch! {:watcher :polling}
                  [{:paths ["resources/db"]
                    :context (constantly {:fn callback})
                    :filter models-file?
                    :handler (debounce models-watch-handler 100)}])]
    (reset! models-watcher watcher)
    (log/info {:event :watcher/started
               :watcher :models
               :models-watcher-set? (boolean @models-watcher)
               :watcher-class (some-> watcher class str)})
    watcher))

(defn stop-models-watcher []
  (when @models-watcher
    (try
      (hawk/stop! @models-watcher)
      (catch Throwable t
        (log/error t {:event :watcher/stop-error
                      :watcher :models})))
    (reset! models-watcher nil)
    (log/info {:event :watcher/stopped
               :watcher :models
               :models-watcher-set? (boolean @models-watcher)})))

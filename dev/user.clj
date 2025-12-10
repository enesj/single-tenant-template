(ns user
  {:clj-kondo/ignore [:unused-namespace :unused-refer]}
  (:require
   [clojure.string :as str]
   [clojure.tools.namespace.repl :refer [disable-reload! disable-unload!]]
   [core :as core]
   [snitch.core :refer [defn* defmethod* *fn *let]]
   [taoensso.timbre :as log])
  (:import
   (java.time ZoneId)
   (java.time.format DateTimeFormatter)))

(log/merge-config!
  {:appenders
   {:println {:output-fn (fn [{:keys [instant level ?ns-str ?file ?line msg_]}]
                           (let [formatted-time (-> instant
                                                  .toInstant
                                                  (.atZone (ZoneId/systemDefault))
                                                  (.format (DateTimeFormatter/ofPattern "EEE HH:mm:ss")))
                                 level-color (case level
                                               :trace "\033[37m"   ; white
                                               :debug "\033[36m"   ; cyan
                                               :info "\033[32m"   ; green
                                               :warn "\033[33m"   ; yellow
                                               :error "\033[31m"   ; red
                                               :fatal "\033[35m"   ; magenta
                                               "\033[0m")          ; default
                                 reset-color "\033[0m"]
                             (str "\033[90m" formatted-time reset-color " "
                               level-color (-> level name str/upper-case) reset-color
                               " \033[90m[" (or ?ns-str ?file "?") ":" ?line "]\033[0m - "
                               (force msg_))))}}})

(disable-unload!)
(disable-reload!)


;; File watchers state
(defonce file-watchers (atom nil))

(defn start []
  (core/start-dev))

;; Convenience functions
(defn sync-project-deps
  "Manually sync dependencies"
  []
  (if-let [run-sync-deps (try
                           (require 'app.dev.watchers)
                           (resolve 'app.dev.watchers/run-sync-deps!)
                           (catch Exception _ nil))]
    (run-sync-deps)
    (println "Watchers not loaded")))

(comment
  (do
    (def user/portal ((requiring-resolve 'portal.api/open)))
    (add-tap (requiring-resolve 'portal.api/submit))))

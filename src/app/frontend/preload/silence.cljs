(ns app.frontend.preload.silence
  "Preload to quiet noisy console output early in app load. Applies before any app namespaces.

   - Suppresses specific re-frame warnings (e.g., handler overwrite spam)
   - In production, raises Timbre level to :warn and silences console log/info/debug
  "
  (:require
    [re-frame.loggers :as rlog]
    [taoensso.timbre :as log]))

;; Suppress noisy re-frame warnings like handler overwrites
(let [orig-warn (.-warn js/console)
      orig-log (.-log js/console)
      orig-err (.-error js/console)
      orig-group (or (.-group js/console) (.-log js/console))
      orig-groupEnd (or (.-groupEnd js/console) (fn [] nil))
      pass (fn [f & args] (.apply f js/console (to-array args)))
      warn* (fn [& args]
              (let [s (when (seq args) (str (first args)))
                    noisy? (and s (re-find #"re-frame: overwriting" s))]
                (when-not noisy?
                  (apply pass orig-warn args))))
      log* (fn [& args] (apply pass orig-log args))
      err* (fn [& args] (apply pass orig-err args))
      group* (fn [& args] (apply pass orig-group args))
      groupEnd* (fn [& args] (apply pass orig-groupEnd args))]
  (rlog/set-loggers! {:warn warn*
                      :log log*
                      :error err*
                      :group group*
                      :groupEnd groupEnd*}))

;; In production, quiet console and raise timbre level early
(when-not ^boolean goog.DEBUG
  (let [noop (fn [& _] nil)]
    (doseq [m ["log" "debug" "info" "time" "timeEnd" "trace" "group" "groupCollapsed" "groupEnd"]]
      (aset js/console m noop)))
  (log/merge-config! {:min-level :warn}))

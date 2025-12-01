(ns app.template.frontend.utils.debug
  (:require
    [taoensso.timbre :as log]))

;; Simple debug log function that only logs in development mode
(defn debug-log
  "Only logs in development mode"
  [message & args]
  ;; js/goog.DEBUG is a compile-time constant in advanced compilation
  (when ^boolean js/goog.DEBUG
    (if args
      (log/debug message args)
      (log/debug message))))

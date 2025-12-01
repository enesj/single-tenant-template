(ns user
  (:require
    [app.test.repl :refer [watch-backend]]
    [clojure.tools.namespace.repl :refer [disable-reload! disable-unload!]]
    [kaocha.repl :as k]))

(disable-unload!)
(disable-reload!)

;; Only used in commented code
;;(comment
;;  (require '[snitch.core :refer [defn* defmethod* *fn *let]]))

;;(do
;;  (def portal (p/open {:launcher :intellij}))
;;  (def submit (comp p/submit d/datafy))
;;  (add-tap #'submit))

(tap> "Start tests:")
;;(watch-backend)

(comment
  (watch-backend)
  (k/run-all))

(ns app.frontend.karma-test-runner
  (:require [cljs.test :as test]))

(defn ^:export main []
  (test/run-all-tests #".*-test$"))

(defn ^:export run_with_karma [karma]
  (test/run-all-tests #".*-test$")
  (.start karma))

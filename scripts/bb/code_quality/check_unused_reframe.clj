#!/usr/bin/env bb
;; Check unused re-frame subscriptions and events
;; This script analyzes re-frame keywords flagged as unused and checks if they're actually used

(ns code-quality.check-unused-reframe
  (:require
    [code-quality.check-unused-reframe.core :as core]))

(defn -main
  [& args]
  (apply core/-main args))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))

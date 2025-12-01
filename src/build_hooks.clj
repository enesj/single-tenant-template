(ns build-hooks
  (:require
    [clojure.java.shell :as shell]))

(defn build-css
  "Build CSS using PostCSS after ClojureScript compilation"
  {:shadow.build/stage :flush}
  [build-state]
  (println "Building CSS...")
  (let [result (shell/sh "npm" "run" "postcss")]
    (when (not= 0 (:exit result))
      (println "CSS build failed:" (:err result))))
  build-state)

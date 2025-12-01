(ns app.backend.core-load-models-test
  (:require
    [app.backend.core :as core]
    [clojure.test :refer [deftest is testing]]))

(deftest load-models-attaches-app-metadata
  (with-open [models (core/load-models-data)]
    (let [raw @models
          app (:model-naming/app-models (meta raw))]
      (testing "raw models still a map"
        (is (map? raw))
        (is (seq raw)))
      (testing "converted models stored in metadata"
        (is (map? app))
        (is (:model-naming/converted (meta app)))))))

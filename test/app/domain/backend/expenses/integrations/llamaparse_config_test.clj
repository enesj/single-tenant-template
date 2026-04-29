(ns app.domain.backend.expenses.integrations.llamaparse-config-test
  (:require
    [app.domain.backend.expenses.integrations.llamaparse :as llamaparse]
    [clojure.test :refer [deftest is]]))

(deftest build-config-defaults-expand-to-items-markdown-text
  (let [cfg (llamaparse/build-config
              {:llamaparse {:api-key "k"}}
              {:getenv (constantly nil)})]
    (is (= "items,markdown,text" (:expand cfg)))))
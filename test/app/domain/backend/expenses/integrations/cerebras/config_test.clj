(ns app.domain.backend.expenses.integrations.cerebras.config-test
  (:require
    [app.domain.backend.expenses.integrations.cerebras.config :as cerebras-config]
    [clojure.test :refer [deftest is testing]]))

(deftest build-config-enabled-only-when-api-key-present
  (testing "enabled? is false when no API key exists anywhere"
    (let [cfg (cerebras-config/build-config {} {:getenv (constantly nil)})]
      (is (= false (:enabled? cfg)))
      (is (nil? (:api-key cfg)))))

  (testing "enabled? is true when API key is provided via app config"
    (let [cfg (cerebras-config/build-config {:cerebras {:api-key "from-app-config"}}
                                            {:getenv (constantly nil)})]
      (is (= true (:enabled? cfg)))
      (is (= "from-app-config" (:api-key cfg))))))

(deftest build-config-process-env-wins-over-app-config-for-api-key
  (testing "Process env should override app config for API key"
    (let [cfg (cerebras-config/build-config
                {:cerebras {:api-key "from-app-config"}}
                {:getenv (fn [k]
                           (case k
                             "CEREBRAS_API_KEY" "from-process-env"
                             nil))})]
      (is (= true (:enabled? cfg)))
      (is (= "from-process-env" (:api-key cfg))))))

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

(deftest build-config-refine-settings
  (testing "defaults refine settings"
    (let [cfg (cerebras-config/build-config
                {:cerebras {:socket-timeout-ms 12345}}
                {:getenv (constantly nil)})]
      (is (= 5 (:refine-concurrency cfg)))
      (is (= 12345 (:refine-timeout-ms cfg)))))

  (testing "env overrides refine settings"
    (let [cfg (cerebras-config/build-config
                {:cerebras {:refine-concurrency 2
                            :refine-timeout-ms 1111
                            :socket-timeout-ms 2222}}
                {:getenv (fn [k]
                           (case k
                             "CEREBRAS_REFINE_CONCURRENCY" "7"
                             "CEREBRAS_REFINE_TIMEOUT_MS" "3333"
                             nil))})]
      (is (= 7 (:refine-concurrency cfg)))
      (is (= 3333 (:refine-timeout-ms cfg))))))

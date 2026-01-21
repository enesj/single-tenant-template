(ns app.domain.backend.expenses.integrations.cerebras.config-test
  (:require
    [app.domain.backend.expenses.integrations.cerebras.config :as cerebras-config]
    [clojure.test :refer [deftest is testing]]))

(defn- with-temp-dotenv
  [content f]
  (let [path (java.nio.file.Files/createTempFile "cerebras-test" ".env" (make-array java.nio.file.attribute.FileAttribute 0))
        file (.toFile path)]
    (try
      (spit file content)
      (f (.getAbsolutePath file))
      (finally
        (try
          (.delete file)
          (catch Exception _))))))

(deftest build-config-enabled-only-when-api-key-present
  (testing "enabled? is false when no API key exists anywhere"
    (let [cfg (cerebras-config/build-config {} {:getenv (constantly nil) :dotenv-path "does-not-exist.env"})]
      (is (= false (:enabled? cfg)))
      (is (nil? (:api-key cfg)))))

  (testing "enabled? is true when API key is provided via app config"
    (let [cfg (cerebras-config/build-config {:cerebras {:api-key "from-app-config"}}
                                            {:getenv (constantly nil)
                                             :dotenv-path "does-not-exist.env"})]
      (is (= true (:enabled? cfg)))
      (is (= "from-app-config" (:api-key cfg))))))

(deftest build-config-dotenv-fallback-used-for-api-key
  (testing "When env is missing and app-config has no key, .env can supply the API key"
    (with-temp-dotenv
      (str "CEREBRAS_API_KEY=from-dotenv\n")
      (fn [dotenv-path]
        (let [cfg (cerebras-config/build-config {} {:getenv (constantly nil)
                                                   :dotenv-path dotenv-path})]
          (is (= true (:enabled? cfg)))
          (is (= "from-dotenv" (:api-key cfg))))))))

(deftest build-config-process-env-wins-over-dotenv-for-api-key
  (testing "Process env should win over .env for API key"
    (with-temp-dotenv
      (str "CEREBRAS_API_KEY=from-dotenv\n")
      (fn [dotenv-path]
        (let [cfg (cerebras-config/build-config
                    {}
                    {:getenv (fn [k]
                               (case k
                                 "CEREBRAS_API_KEY" "from-process-env"
                                 nil))
                     :dotenv-path dotenv-path})]
          (is (= true (:enabled? cfg)))
          (is (= "from-process-env" (:api-key cfg))))))))

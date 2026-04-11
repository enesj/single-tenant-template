(ns app.migrate-test
  (:require
    [app.migrate :as migrate]
    [clojure.test :refer [deftest is testing]]))

(defn- invoke-main!
  [{:keys [jdbc-url run-migrations-fn arg]
    :or {arg "61"
         run-migrations-fn (fn [& _]
                             {:applied 0
                              :direction :forward})}}]
  (let [exit-codes (atom [])
        stdout-writer (java.io.StringWriter.)
        stderr-writer (java.io.StringWriter.)
        exit-signal ::exit]
    (with-redefs-fn {#'migrate/resolve-jdbc-url (constantly jdbc-url)
                     #'migrate/run-migrations! run-migrations-fn
                     #'migrate/exit! (fn [code]
                                       (swap! exit-codes conj code)
                                       (throw (ex-info "migration runner exited"
                                                {:signal exit-signal
                                                 :code code})))}
      #(binding [*out* stdout-writer
                 *err* stderr-writer]
         (try
           (migrate/-main arg)
           (catch clojure.lang.ExceptionInfo e
             (when-not (= exit-signal (:signal (ex-data e)))
               (throw e))))
         {:stdout (str stdout-writer)
          :stderr (str stderr-writer)
          :exit-codes @exit-codes}))))

(deftest normalize-jdbc-url-test
  (testing "postgres and postgresql URLs are normalized to jdbc form"
    (is (= "jdbc:postgresql://localhost/app"
          (#'migrate/normalize-jdbc-url "postgres://localhost/app")))
    (is (= "jdbc:postgresql://localhost/app"
          (#'migrate/normalize-jdbc-url "postgresql://localhost/app"))))

  (testing "blank and nil values stay nil"
    (is (nil? (#'migrate/normalize-jdbc-url nil)))
    (is (nil? (#'migrate/normalize-jdbc-url "   ")))))

(deftest parse-target-number-test
  (testing "valid integers are parsed"
    (is (= 61 (#'migrate/parse-target-number "61"))))

  (testing "nil stays nil"
    (is (nil? (#'migrate/parse-target-number nil))))

  (testing "invalid input raises a useful exception"
    (let [ex (try
               (#'migrate/parse-target-number "six-one")
               nil
               (catch clojure.lang.ExceptionInfo e
                 e))]
      (is ex)
      (is (re-find #"Target migration number must be an integer"
            (ex-message ex))))))

(deftest redact-jdbc-url-test
  (testing "database credentials are redacted from log output"
    (is (= "jdbc:postgresql://***@db.example.com/railway"
          (#'migrate/redact-jdbc-url "jdbc:postgresql://user:secret@db.example.com/railway"))))

  (testing "urls without credentials are preserved"
    (is (= "jdbc:postgresql://db.example.com/railway"
          (#'migrate/redact-jdbc-url "jdbc:postgresql://db.example.com/railway"))))

  (testing "nil stays nil"
    (is (nil? (#'migrate/redact-jdbc-url nil)))))

(deftest -main-missing-database-url-fails-loudly-test
  (let [{:keys [stderr exit-codes]} (invoke-main! {:jdbc-url nil})]
    (testing "the runner exits with a failure code"
      (is (= [1] exit-codes)))
    (testing "the error is printed to stderr"
      (is (re-find #"DATABASE_URL env var is required" stderr)))))

(deftest -main-success-path-test
  (let [calls (atom [])
        {:keys [stdout stderr exit-codes]}
        (invoke-main! {:jdbc-url "jdbc:postgresql://user:secret@db.example.com/railway"
                       :run-migrations-fn (fn [args]
                                            (swap! calls conj args)
                                            {:applied 8
                                             :direction :forward})
                       :arg "61"})]
    (testing "the migration runner receives the parsed target number"
      (is (= [{:jdbc-url "jdbc:postgresql://user:secret@db.example.com/railway"
               :number 61}]
            @calls)))
    (testing "success does not trigger a forced exit"
      (is (empty? exit-codes)))
    (testing "stdout announces the run and completion"
      (is (re-find #"Running migrations against jdbc:postgresql://\*\*\*@db\.example\.com/railway" stdout))
      (is (re-find #"Migrations complete\." stdout)))
    (testing "stderr stays quiet on success"
      (is (empty? stderr)))))

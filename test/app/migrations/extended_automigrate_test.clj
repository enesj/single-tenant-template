(ns app.migrations.extended-automigrate-test
  "Tests for the extended automigrate implementation"
  (:require
    [app.migrations.converter :as converter]
    [app.migrations.edn-loader :as loader]
    [app.migrations.sql-generators :as generators]
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing]]))

(deftest test-edn-loader
  (testing "Loading valid EDN migration"
    (let [test-edn {:migration-type :functions
                    :functions [{:name "test_func"
                                 :up "CREATE FUNCTION test_func() RETURNS void AS $$ BEGIN END; $$ LANGUAGE plpgsql;"
                                 :down "DROP FUNCTION test_func();"}]}
          temp-file (io/file "test-migration.edn")]
      (spit temp-file (pr-str test-edn))
      (try
        (let [loaded (loader/load-edn-migration temp-file)]
          (is (= :functions (:migration-type loaded)))
          (is (= 1 (count (:functions loaded))))
          (is (= "test_func" (-> loaded :functions first :name))))
        (finally
          (.delete temp-file)))))

  (testing "Invalid EDN structure throws exception"
    (let [invalid-edn {:wrong-key :value}
          temp-file (io/file "invalid-migration.edn")]
      (spit temp-file (pr-str invalid-edn))
      (try
        (is (thrown? Exception (loader/load-edn-migration temp-file)))
        (finally
          (.delete temp-file))))))

(deftest test-sql-generators
  (testing "Generate SQL with delimiters"
    (let [result (generators/generate-delimiter-sql "CREATE TABLE test();" "DROP TABLE test;")]
      (is (re-find #"-- FORWARD" result))
      (is (re-find #"-- BACKWARD" result))
      (is (re-find #"CREATE TABLE test" result))
      (is (re-find #"DROP TABLE test" result))))

  (testing "EDN to SQL conversion for functions"
    (let [migration-data {:migration-type :functions
                          :functions [{:name "my_func"
                                       :up "CREATE FUNCTION"
                                       :down "DROP FUNCTION"}]}
          results (generators/edn->sql migration-data)]
      (is (= 1 (count results)))
      (is (= "my_func" (:name (first results))))
      (is (re-find #"-- FORWARD" (:sql (first results))))))

  (testing "Migration type to extension mapping"
    (is (= "fn" (generators/migration-type->extension :functions)))
    (is (= "trg" (generators/migration-type->extension :triggers)))
    (is (= "pol" (generators/migration-type->extension :policies)))
    (is (= "view" (generators/migration-type->extension :views))))

  (testing "Filename generation"
    (let [filename (generators/generate-migration-filename 5 "My Test Function!" :functions)]
      (is (= "0005_my_test_function.fn" filename)))))

(deftest test-converter
  (testing "Next migration number calculation"
    (let [temp-dir (io/file "temp-migrations")]
      (.mkdirs temp-dir)
      (try
        ;; No files - should start at 1
        (is (= 1 (converter/get-next-migration-number temp-dir)))

        ;; Create some migration files
        (spit (io/file temp-dir "0001_first.sql") "")
        (spit (io/file temp-dir "0003_third.sql") "")
        (spit (io/file temp-dir "0005_fifth.sql") "")

        ;; Should return 6 (next after 5)
        (is (= 6 (converter/get-next-migration-number temp-dir)))

        (finally
          ;; Cleanup
          (doseq [f (.listFiles temp-dir)]
            (.delete f))
          (.delete temp-dir))))))

(deftest test-integration
  (testing "Full conversion flow"
    (let [source-dir (io/file "test-edn-migrations")
          target-dir (io/file "test-sql-migrations")]
      (.mkdirs source-dir)
      (.mkdirs target-dir)

      (try
        ;; Create test EDN file
        (let [test-edn {:migration-type :functions
                        :functions [{:name "integration_test"
                                     :up "CREATE FUNCTION integration_test() RETURNS void AS $$ BEGIN END; $$ LANGUAGE plpgsql;"
                                     :down "DROP FUNCTION integration_test();"}]}]
          (spit (io/file source-dir "test.edn") (pr-str test-edn))

          ;; Run conversion
          (let [results (converter/convert-edn-to-automigrate-sql
                          (.getPath source-dir)
                          (.getPath target-dir))]
            (is (= 1 (count results)))
            (is (= "test.edn" (:source (first results))))
            (is (re-find #"\.fn$" (:target (first results))))

            ;; Check the generated file exists and has correct content
            (let [generated-file (io/file target-dir (:target (first results)))
                  content (slurp generated-file)]
              (is (.exists generated-file))
              (is (re-find #"-- FORWARD" content))
              (is (re-find #"-- BACKWARD" content))
              (is (re-find #"CREATE FUNCTION integration_test" content)))))

        (finally
          ;; Cleanup
          (doseq [f (.listFiles source-dir)]
            (.delete f))
          (doseq [f (.listFiles target-dir)]
            (.delete f))
          (.delete source-dir)
          (.delete target-dir))))))

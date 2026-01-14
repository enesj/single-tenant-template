(ns app.shared.frontend-config.io-test
  (:require
    [app.shared.frontend-config.io :as fc-io]
    [clojure.test :refer [deftest is testing]])
  (:import
    (java.nio.file Files Path)
    ))

(defn- temp-dir
  ^Path
  []
  (Files/createTempDirectory "frontend-config-io-test" (make-array java.nio.file.attribute.FileAttribute 0)))

(defn- path
  ^String
  [^Path dir filename]
  (str (.toString dir) "/" filename))

(deftest read-edn-or-empty-behavior
  (let [dir (temp-dir)
        missing (path dir "missing.edn")
        invalid (path dir "invalid.edn")
        valid (path dir "valid.edn")]

    (testing "missing file returns {}"
      (is (= {} (fc-io/read-edn-or-empty missing)))
      (is (= {} (fc-io/read-edn-or-throw missing))))

    (testing "invalid EDN returns {} for safe read"
      (spit invalid "{:a 1" :encoding "UTF-8")
      (is (= {} (fc-io/read-edn-or-empty invalid))))

    (testing "invalid EDN throws for strict read"
      (is (thrown? Exception (fc-io/read-edn-or-throw invalid))))

    (testing "valid EDN roundtrips via write-edn-pretty!"
      (fc-io/write-edn-pretty! valid {:a 1 :b {:c [1 2 3]}})
      (is (= {:a 1 :b {:c [1 2 3]}}
            (fc-io/read-edn-or-throw valid))))))

(deftest validated-read-does-not-throw-on-invalid-shape
  (let [dir (temp-dir)
        f (path dir "data.edn")
        data {:hello "world"}]
    (spit f (pr-str data) :encoding "UTF-8")

    (testing "safe validated read returns parsed data even if validator rejects"
      (is (= data
            (fc-io/read-edn-or-empty+validate
              {:config-key :example
               :path f
               :validate-fn (fn [_]
                              {:valid? false
                               :errors [{:message "nope"}]})}))))

    (testing "strict validated read returns parsed data even if validator rejects"
      (is (= data
            (fc-io/read-edn-or-throw+validate
              {:config-key :example
               :path f
               :validate-fn (fn [_]
                              {:valid? false
                               :errors [{:message "nope"}]})}))))))

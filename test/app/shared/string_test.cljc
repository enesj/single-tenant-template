(ns app.shared.string-test
  #?(:clj  (:require
            [app.shared.string :as string]
            [clojure.test :refer [deftest is testing]])
     :cljs (:require
             [app.shared.string :as string]
             [cljs.test :refer-macros [deftest is testing]])))

(deftest case-conversion-test
  (testing "kebab-case"
    (is (= "hello-world" (string/kebab-case "Hello World")))
    (is (= "hello-world" (string/kebab-case "hello_world")))
    (is (= "123" (string/kebab-case 123)))
    (is (nil? (string/kebab-case nil))))

  (testing "snake-case"
    (is (= "hello_world" (string/snake-case "Hello World")))
    (is (= "hello_world" (string/snake-case "hello-world")))
    (is (nil? (string/snake-case nil))))

  (testing "camel-case"
    (is (= "helloWorld" (string/camel-case "hello-world")))
    (is (= "helloWorld" (string/camel-case "Hello world")))
    (is (nil? (string/camel-case nil)))))

(deftest cleaning-and-validation-test
  (testing "slugify"
    (is (= "hello-world" (string/slugify "Hello, World!")))
    (is (nil? (string/slugify nil))))

  (testing "clean-whitespace"
    (is (= "a b c" (string/clean-whitespace "  a  b \n c  ")))
    (is (nil? (string/clean-whitespace nil))))

  (testing "blank? / not-blank? / non-empty-string?"
    (is (true? (string/blank? nil)))
    (is (true? (string/blank? "")))
    (is (true? (string/blank? "   ")))
    (is (false? (string/blank? 0)))

    (is (false? (string/not-blank? nil)))
    (is (true? (string/not-blank? "x")))

    (is (true? (string/non-empty-string? "x")))
    (is (false? (string/non-empty-string? "")))
    (is (false? (string/non-empty-string? nil)))
    (is (false? (string/non-empty-string? 1)))))

(deftest parsing-test
  (testing "safe-parse-int"
    (is (= 42 (string/safe-parse-int "42")))
    (is (= 42 (string/safe-parse-int " 42 ")))
    (is (nil? (string/safe-parse-int "")))
    (is (nil? (string/safe-parse-int "nope"))))

  (testing "safe-parse-double"
    (is (= 3.14 (string/safe-parse-double "3.14")))
    (is (nil? (string/safe-parse-double "")))
    (is (nil? (string/safe-parse-double "nope")))))

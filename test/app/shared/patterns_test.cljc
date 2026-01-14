(ns app.shared.patterns-test
  #?(:clj  (:require
            [app.shared.patterns :as patterns]
            [clojure.test :refer [deftest is testing]])
     :cljs (:require
             [app.shared.patterns :as patterns]
             [cljs.test :refer-macros [deftest is testing]])))

(deftest pattern-helpers-test
  (testing "email"
    (is (true? (patterns/valid-email? "a@b.co")))
    (is (false? (patterns/valid-email? "nope"))))

  (testing "url"
    (is (true? (patterns/valid-url? "https://example.com")))
    (is (true? (patterns/valid-url? "ftp://example.com")))
    (is (true? (patterns/valid-http-url? "https://example.com")))
    (is (false? (patterns/valid-http-url? "ftp://example.com"))))

  (testing "dates"
    (is (true? (patterns/valid-iso-date? "2026-01-14")))
    (is (false? (patterns/valid-iso-date? "01/14/2026")))
    (is (true? (patterns/valid-us-date? "1/14/2026")))
    (is (false? (patterns/valid-us-date? "2026-01-14"))))

  (testing "slugs"
    (is (true? (patterns/valid-slug? "hello-world")))
    (is (false? (patterns/valid-slug? "Hello-World")))
    (is (false? (patterns/valid-slug? "hello_world")))
    (is (false? (patterns/valid-slug? "hello--world")))
    (is (false? (patterns/valid-slug? "hello-"))))

  (testing "phones"
    (is (true? (patterns/valid-phone? "(555) 123-4567")))
    (is (false? (patterns/valid-phone? "abc")))
    (is (true? (patterns/valid-phone-e164? "+14155552671")))
    (is (false? (patterns/valid-phone-e164? "+1 (415) 555-2671")))) )

(ns app.shared.type-conversion-test
  #?(:clj  (:require
            [app.shared.type-conversion :as type-conv]
            [clojure.test :refer [deftest is testing]])
     :cljs (:require [cljs.test :refer-macros [deftest is testing]]
                     [app.shared.type-conversion :as type-conv])))

#?(:clj (set! *warn-on-reflection* true))

(def sample-models
  {:expenses {:fields [[:notes :text]
                       [:metadata :jsonb]
                       [:created_at :timestamptz]
                       [:user_id :uuid]
                       [:currency [:enum :currency]]]
              :types []}})

(deftest cast-field-value-jsonb-test
  (testing "JSONB casting wraps value in HoneySQL lift"
    (is (= [:cast [:lift {:foo "bar"}] :jsonb]
          (type-conv/cast-field-value :jsonb {:foo "bar"}))))
  (testing "Enum casting normalizes to raw type name"
    (is (= [:raw "CAST('BAM' AS currency)"]
          (type-conv/cast-field-value [:enum :currency] "BAM")))))

(deftest prepare-data-for-db-basic-test
  (testing "Nil values are dropped by default"
    (let [prepared (type-conv/prepare-data-for-db sample-models :expenses
                     {:notes "Test"
                      :metadata {:foo 1}
                      :user_id nil})]
      (is (= {:notes "Test"
              :metadata [:cast [:lift {:foo 1}] :jsonb]}
            prepared))
      (is (not (contains? prepared :user_id)))))
  (testing "Including nils retains them explicitly"
    (let [prepared (type-conv/prepare-data-for-db sample-models :expenses
                     {:user_id nil}
                     {:include-nils? true})]
      (is (contains? prepared :user_id))
      (is (nil? (:user_id prepared))))))

(deftest convert-to-type-test
  (testing "Boolean parsing supports strings"
    (is (true? (type-conv/convert-to-type "true" :boolean)))
    (is (false? (type-conv/convert-to-type "0" :boolean))))
  (testing "UUID conversion returns uuid? values"
    (let [uuid-str "550e8400-e29b-41d4-a716-446655440000"
          converted (type-conv/convert-to-type uuid-str :uuid)]
      (is (uuid? converted))
      (is (= uuid-str (str converted)))))
  (testing "JSON string conversion produces maps"
    (let [converted (type-conv/convert-to-type "{\"foo\":true}" :json)
          normalized #?(:clj converted
                        :cljs (js->clj converted :keywordize-keys true))]
      (is (= {:foo true} normalized)))))

(deftest try-parse-uuid-test
  (testing "Best-effort UUID parsing returns nil for blank/invalid"
    (let [uuid-str "550e8400-e29b-41d4-a716-446655440000"
          parsed (type-conv/try-parse-uuid uuid-str)]
      (is (uuid? parsed))
      (is (= uuid-str (str parsed))))
    (is (nil? (type-conv/try-parse-uuid nil)))
    (is (nil? (type-conv/try-parse-uuid "")))
    (is (nil? (type-conv/try-parse-uuid "   ")))
    (is (nil? (type-conv/try-parse-uuid "not-a-uuid")))))

(deftest parse-number-test
  (testing "Valid numbers"
    (is (= 42 (type-conv/parse-number "42")))
    (is (= -42 (type-conv/parse-number "-42")))
    (is (= 3.14 (type-conv/parse-number "3.14"))))
  (testing "Invalid numbers return nil instead of NaN"
    (is (nil? (type-conv/parse-number "abc")))
    (is (nil? (type-conv/parse-number "")))
    (is (nil? (type-conv/parse-number " ")))
    (is (nil? (type-conv/parse-number "123.45.67")))))

(deftest convert-to-type-edge-cases-test
  (testing "Decimal conversion with invalid strings throws instead of NaN"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (type-conv/convert-to-type "abc" :decimal))))
  (testing "Integer conversion with invalid strings throws"
    (is (thrown? #?(:clj Exception :cljs js/Error)
          (type-conv/convert-to-type "abc" :integer)))))

(deftest detect-field-type-test
  (is (= :integer (type-conv/detect-field-type "42")))
  (is (= :decimal (type-conv/detect-field-type "3.14")))
  (is (= :json (type-conv/detect-field-type "{\"foo\":1}")))
  (is (= :uuid (type-conv/detect-field-type "550e8400-e29b-41d4-a716-446655440000")))
  (is (= :boolean (type-conv/detect-field-type "false"))))

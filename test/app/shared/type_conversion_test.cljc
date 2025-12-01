(ns app.shared.type-conversion-test
  #?(:clj  (:require
            [app.shared.type-conversion :as type-conv]
            [clojure.test :refer [deftest is testing]])
     :cljs (:require [cljs.test :refer-macros [deftest is testing]]
                     [app.shared.type-conversion :as type-conv])))

#?(:clj (set! *warn-on-reflection* true))

(def sample-models
  {:properties {:fields [[:name :text]
                         [:settings :jsonb]
                         [:created_at :timestamptz]
                         [:tenant_id :uuid]
                         [:status [:enum :property_status]]]
                :types []}})

(deftest cast-field-value-jsonb-test
  (testing "JSONB casting wraps value in HoneySQL lift"
    (is (= [:cast [:lift {:foo "bar"}] :jsonb]
          (type-conv/cast-field-value :jsonb {:foo "bar"}))))
  (testing "Enum casting normalizes to raw type name"
    (is (= [:raw "CAST('active' AS property_status)"]
          (type-conv/cast-field-value [:enum :property-status] "active")))))

(deftest prepare-data-for-db-basic-test
  (testing "Nil values are dropped by default"
    (let [prepared (type-conv/prepare-data-for-db sample-models :properties
                     {:name "Test"
                      :settings {:foo 1}
                      :tenant_id nil})]
      (is (= {:name "Test"
              :settings [:cast [:lift {:foo 1}] :jsonb]}
            prepared))
      (is (not (contains? prepared :tenant_id)))))
  (testing "Including nils retains them explicitly"
    (let [prepared (type-conv/prepare-data-for-db sample-models :properties
                     {:tenant_id nil}
                     {:include-nils? true})]
      (is (contains? prepared :tenant_id))
      (is (nil? (:tenant_id prepared))))))

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

(deftest detect-field-type-test
  (is (= :integer (type-conv/detect-field-type "42")))
  (is (= :decimal (type-conv/detect-field-type "3.14")))
  (is (= :json (type-conv/detect-field-type "{\"foo\":1}")))
  (is (= :uuid (type-conv/detect-field-type "550e8400-e29b-41d4-a716-446655440000")))
  (is (= :boolean (type-conv/detect-field-type "false"))))

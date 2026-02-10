(ns app.domain.backend.expenses.services.places-api-test
  (:require
    [app.domain.backend.expenses.services.places-api :as places-api]
    [clojure.test :refer [deftest is testing]]))

(deftest search-text-field-mask-test
  (testing "explicit :field-mask is passed to X-Goog-FieldMask"
    (let [captured (atom nil)
          cfg {:api-key "test"
               :base-url "https://example.invalid"
               :timeout-ms 1}
          field-mask "places.displayName,places.id,places.formattedAddress"
          res (with-redefs [places-api/http-post!
                            (fn [url opts]
                              (reset! captured {:url url :opts opts})
                              {:status 200 :body "{\"places\":[]}"})]
                (places-api/search-text! cfg "Bingo" {:field-mask field-mask}))
          headers (get-in @captured [:opts :headers])]
      (is (= field-mask (get headers "X-Goog-FieldMask")))
      (is (= {:places [] :error nil} (select-keys res [:places :error])))))

  (testing "default field mask is used when :field-mask is omitted"
    (let [captured (atom nil)
          cfg {:api-key "test"
               :base-url "https://example.invalid"
               :timeout-ms 1}
          res (with-redefs [places-api/http-post!
                            (fn [url opts]
                              (reset! captured {:url url :opts opts})
                              {:status 200 :body "{\"places\":[]}"})]
                (places-api/search-text! cfg "Bingo" {}))
          headers (get-in @captured [:opts :headers])]
      (is (= "places.displayName,places.id" (get headers "X-Goog-FieldMask")))
      (is (= {:places [] :error nil} (select-keys res [:places :error]))))))

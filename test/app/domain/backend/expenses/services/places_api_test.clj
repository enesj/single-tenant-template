(ns app.domain.backend.expenses.services.places-api-test
  (:require
    [app.domain.backend.expenses.services.places-api :as places-api]
    [cheshire.core :as json]
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

(deftest search-text-retries-with-simplified-field-mask-on-5xx-test
  (testing "5xx with addressComponents retries with that field removed"
    (let [field-mask "places.displayName,places.id,places.formattedAddress,places.addressComponents"
          expected-retry-mask "places.displayName,places.id,places.formattedAddress"
          captured-masks (atom [])
          cfg {:api-key "test"
               :base-url "https://example.invalid"
               :timeout-ms 1}
          ok-body (json/generate-string
                    {:places [{:id "place-1"
                               :displayName {:text "Foo"}
                               :formattedAddress "Ulica X, 71000 Sarajevo"}]})
          internal-err-body (json/generate-string
                              {:error {:code 500
                                       :message "Internal server error."
                                       :status "INTERNAL"}})
          res (with-redefs [places-api/http-post!
                            (fn [_url opts]
                              (swap! captured-masks conj (get-in opts [:headers "X-Goog-FieldMask"]))
                              (if (= 1 (count @captured-masks))
                                {:status 500 :body internal-err-body}
                                {:status 200 :body ok-body}))]
                (places-api/search-text! cfg "Q" {:field-mask field-mask
                                                  :retries 1
                                                  :retry-sleep-ms 0}))]
      (is (= [field-mask expected-retry-mask] @captured-masks))
      (is (= ["Foo"] (mapv :name (:places res))))
      (is (nil? (:error res))))))

(deftest search-text-location-bias-radius-validation-test
  (testing "clamps location-bias radius to Places API max (50000m)"
    (let [captured-body (atom nil)
          cfg {:api-key "test"
               :base-url "https://example.invalid"
               :timeout-ms 1}
          _ (with-redefs [places-api/http-post!
                          (fn [_url opts]
                            (reset! captured-body (:body opts))
                            {:status 200 :body "{\"places\":[]}"})]
              (places-api/search-text! cfg
                "Bingo"
                {:location-bias {:circle {:lat 43.8563
                                          :lng 18.4131
                                          :radius-m 150000.0}}}))
          body (json/parse-string @captured-body true)
          radius (get-in body [:locationBias :circle :radius])]
      (is (<= radius 50000.0))
      (is (= 50000.0 radius))))

  (testing "omits locationBias when radius is NaN"
    (let [captured-body (atom nil)
          cfg {:api-key "test"
               :base-url "https://example.invalid"
               :timeout-ms 1}
          _ (with-redefs [places-api/http-post!
                          (fn [_url opts]
                            (reset! captured-body (:body opts))
                            {:status 200 :body "{\"places\":[]}"})]
              (places-api/search-text! cfg
                "Bingo"
                {:location-bias {:circle {:lat 43.8563
                                          :lng 18.4131
                                          :radius-m ##NaN}}}))
          body (json/parse-string @captured-body true)]
      (is (nil? (:locationBias body)))))

  (testing "omits locationBias when radius is not a number"
    (let [captured-body (atom nil)
          cfg {:api-key "test"
               :base-url "https://example.invalid"
               :timeout-ms 1}
          _ (with-redefs [places-api/http-post!
                          (fn [_url opts]
                            (reset! captured-body (:body opts))
                            {:status 200 :body "{\"places\":[]}"})]
              (places-api/search-text! cfg
                "Bingo"
                {:location-bias {:circle {:lat 43.8563
                                          :lng 18.4131
                                          :radius-m "150000"}}}))
          body (json/parse-string @captured-body true)]
      (is (nil? (:locationBias body))))))

(deftest extract-postal-code-from-address-components-test
  (testing "extracts postal_code longText"
    (is (= "71000"
          (places-api/extract-postal-code-from-address-components
            [{:longText "Sarajevo" :types ["locality" "political"]}
             {:longText "71000" :types ["postal_code"]}]))))

  (testing "falls back to shortText"
    (is (= "71120"
          (places-api/extract-postal-code-from-address-components
            [{:longText "Istočna Ilidža" :types ["locality"]}
             {:shortText "71120" :types ["postal_code"]}]))))

  (testing "returns nil when no postal_code component exists"
    (is (nil? (places-api/extract-postal-code-from-address-components
                [{:longText "Sarajevo" :types ["locality"]}
                 {:longText "Federacija BiH" :types ["administrative_area_level_1"]}])))))

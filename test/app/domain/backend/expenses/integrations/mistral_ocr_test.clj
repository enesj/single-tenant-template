(ns app.domain.backend.expenses.integrations.mistral-ocr-test
  (:require
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]]))

(deftest build-config-respects-app-config
  (let [cfg (mistral-ocr/build-config
              {:mistral {:api-key "k"
                         :base-url "https://example"
                         :ocr-model "m"
                         :ocr-enabled? false
                         :conn-timeout-ms 1
                         :socket-timeout-ms 2
                         :max-retries 3
                         :retry-sleep-ms 4}})]
    (is (= false (:enabled? cfg)))
    (is (= "k" (:api-key cfg)))
    (is (= "https://example" (:base-url cfg)))
    (is (= "m" (:model cfg)))
    (is (= 1 (:conn-timeout-ms cfg)))
    (is (= 2 (:socket-timeout-ms cfg)))
    (is (= 3 (:max-retries cfg)))
    (is (= 4 (:retry-sleep-ms cfg)))))

(deftest ocr-parse-joins-markdown-and-sends-multipart
  (let [called (atom nil)
        resp-json {:pages [{:index 1 :markdown "A"}
                           {:index 2 :markdown "B"}]
                   :model "mistral-ocr-2512"
                   :usage_info {:pages_processed 2}}]
    (with-redefs [mistral-ocr/http-post!
                  (fn [url opts]
                    (reset! called {:url url :opts opts})
                    {:status 200 :body (json/generate-string resp-json)})]
      (let [cfg {:api-key "k"
                 :base-url "https://example"
                 :model "mistral-ocr-2512"
                 :document-type "receipt"
                 :conn-timeout-ms 1
                 :socket-timeout-ms 1
                 :max-retries 0
                 :retry-sleep-ms 0}
            result (mistral-ocr/ocr-parse!
                     cfg
                     {:bytes (byte-array [1 2 3])
                      :filename "r.jpg"
                      :content-type "image/jpeg"})
            multipart (get-in @called [:opts :multipart])]
        (is (= "A\n\nB" (:parsed-markdown result)))
        (is (= "https://example/v1/ocr" (:url @called)))
        (is (= "Bearer k" (get-in @called [:opts :headers "Authorization"])))
        (is (vector? multipart))
        (is (some #(= "file" (:name %)) multipart))
        (is (some #(= "document_type" (:name %)) multipart))
        (is (some #(= "model" (:name %)) multipart))))))

(deftest ocr-extract-sends-json-schema-and-detects-extraction
  (testing "when provider returns the structured object at top-level"
    (let [called (atom nil)
          extraction {:merchant {:name "Store"}
                      :totals {:total 10.26}
                      :items [{:raw_label "Coffee" :line_total 6.00}]}
          resp-json extraction]
      (with-redefs [mistral-ocr/http-post!
                    (fn [url opts]
                      (reset! called {:url url :opts opts})
                      {:status 200 :body (json/generate-string resp-json)})]
        (let [cfg {:api-key "k"
                   :base-url "https://example"
                   :model "mistral-ocr-2512"
                   :document-type "receipt"
                   :conn-timeout-ms 1
                   :socket-timeout-ms 1
                   :max-retries 0
                   :retry-sleep-ms 0}
              result (mistral-ocr/ocr-extract! cfg {:bytes (byte-array [1 2])})
              body-map (json/parse-string (get-in @called [:opts :body]) true)]
          (is (= "https://example/v1/ocr" (:url @called)))
          (is (= :json (get-in @called [:opts :content-type])))
          (is (= extraction (:extraction result)))
          (is (= "json_schema" (get-in body-map [:response_format :type])))
          (is (= "receipt_extraction" (get-in body-map [:response_format :json_schema :name])))
          (is (map? (get-in body-map [:response_format :json_schema :schema]))))))))

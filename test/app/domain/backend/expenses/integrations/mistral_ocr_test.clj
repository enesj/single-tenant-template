(ns app.domain.backend.expenses.integrations.mistral-ocr-test
  (:require
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [app.domain.backend.expenses.integrations.mistral-ocr.http :as mistral-http]
    [cheshire.core :as json]
    [clojure.string :as str]
    [clojure.test :refer [deftest is]]))

(deftest build-config-respects-app-config
  (let [cfg (mistral-ocr/build-config
              {:mistral {:api-key "k"
                         :base-url "https://example"
                         :ocr-model "m"
                         :ocr-enabled? false
                         :conn-timeout-ms 1
                         :socket-timeout-ms 2
                         :max-retries 3
                         :retry-sleep-ms 4}}
              ;; Avoid leaking developer machine env vars (e.g. MISTRAL_API_KEY)
              ;; into test expectations.
              {:getenv (constantly nil)})]
    (is (= false (:enabled? cfg)))
    (is (= "k" (:api-key cfg)))
    (is (= "https://example" (:base-url cfg)))
    (is (= "m" (:model cfg)))
    (is (= 1 (:conn-timeout-ms cfg)))
    (is (= 2 (:socket-timeout-ms cfg)))
    (is (= 3 (:max-retries cfg)))
    (is (= 4 (:retry-sleep-ms cfg)))))

(deftest ocr-parse-joins-markdown-and-sends-json
  (let [called (atom nil)
        resp-json {:pages [{:index 1 :markdown "A"}
                           {:index 2 :markdown "B"}]
                   :model "mistral-ocr-2512"
                   :usage_info {:pages_processed 2}}]
    (with-redefs [mistral-http/http-post!
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
            body-map (json/parse-string (get-in @called [:opts :body]) true)
            doc (get body-map :document)]
        (is (= "A\n\nB" (:parsed-markdown result)))
        (is (= "https://example/v1/ocr" (:url @called)))
        (is (= "Bearer k" (get-in @called [:opts :headers "Authorization"])))
        (is (= :json (get-in @called [:opts :content-type])))
        (is (nil? (get-in @called [:opts :multipart])))
        (is (= "mistral-ocr-2512" (get body-map :model)))
        (is (= "image_url" (get doc :type)))
        (is (str/starts-with? (get doc :image_url) "data:image/jpeg;base64,"))))))

(deftest ocr-extract-uses-markdown-only-path
  (let [called (atom nil)
        resp-json {:pages [{:index 1 :markdown "A"}
                           {:index 2 :markdown "B"}]
                   :model "mistral-ocr-2512"
                   :usage_info {:pages_processed 2}}]
    (with-redefs [mistral-http/http-post!
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

        ;; Current implementation: extraction is disabled; we persist markdown-only parse.
        (is (nil? (:extraction result)))
        (is (= "A\n\nB" (:parsed-markdown result)))

        ;; Request should not include structured extraction format.
        (is (nil? (get body-map :document_annotation_format)))))))

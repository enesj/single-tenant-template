(ns app.domain.backend.expenses.integrations.mistral-ocr-test
  (:require
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [app.domain.backend.expenses.integrations.mistral-ocr.http :as mistral-http]
    [cheshire.core :as json]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]))

(deftest build-config-respects-app-config
  (let [cfg (mistral-ocr/build-config
              {:mistral {:api-key "k"
                         :base-url "https://example"
                         :ocr-model "m"
                         :ocr-enabled? false
                         :ocr-batch-enabled? false
                         :ocr-batch-poll-ms 11
                         :ocr-batch-timeout-ms 22
                         :ocr-batch-max-requests 33
                         :conn-timeout-ms 1
                         :socket-timeout-ms 2
                         :max-retries 3
                         :retry-sleep-ms 4}}
              ;; Avoid leaking developer machine env vars (e.g. MISTRAL_API_KEY)
              ;; into test expectations.
              {:getenv (constantly nil)})]
    (is (= false (:enabled? cfg)))
    (is (= false (:batch-enabled? cfg)))
    (is (= 11 (:batch-poll-ms cfg)))
    (is (= 22 (:batch-timeout-ms cfg)))
    (is (= 33 (:batch-max-requests cfg)))
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

(deftest ocr-extract-sends-json-schema-and-detects-extraction
  (testing "when provider returns the structured object at top-level"
    (let [called (atom nil)
          extraction {:merchant {:name "Store"}
                      :totals {:total 10.26}
                      :items [{:raw_label "Coffee" :line_total 6.00}]}
          resp-json extraction]
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
              body-map (json/parse-string (get-in @called [:opts :body]) true)
              schema (get-in body-map [:document_annotation_format :json_schema])]
          (is (= "https://example/v1/ocr" (:url @called)))
          (is (= :json (get-in @called [:opts :content-type])))
          (is (= extraction (:extraction result)))
          (is (= "json_schema" (get-in body-map [:document_annotation_format :type])))
          (is (= "receipt_extraction" (:name schema)))
          (is (map? (:schema schema))))))))

(deftest ocr-extract-batch-parses-results-and-errors
  (let [uploaded-jsonl (atom nil)
        job-polls (atom 0)
        extraction {:merchant {:name "Store"}
                    :totals {:total 10.26}
                    :items [{:raw_label "Coffee" :line_total 10.26}]}
        ok-body {:pages [{:index 1 :markdown "Hello"}]
                 :document_annotation (json/generate-string extraction)}
        out-jsonl (str
                    (json/generate-string {:custom_id "r1"
                                           :response {:status_code 200
                                                      :body ok-body}})
                    "\n"
                    (json/generate-string {:custom_id "r2"
                                           :response {:status_code 400
                                                      :body (json/generate-string {:object "error" :message "bad"})}})
                    "\n")]
    (with-redefs [mistral-http/http-post!
                  (fn [url opts]
                    (cond
                      (str/ends-with? url "/v1/files")
                      (let [file-part (some #(when (= "file" (:name %)) %) (:multipart opts))
                            f (:content file-part)]
                        (reset! uploaded-jsonl (slurp f))
                        {:status 200 :body (json/generate-string {:id "file-1"})})

                      (str/ends-with? url "/v1/batch/jobs")
                      {:status 200 :body (json/generate-string {:id "job-1"})}

                      :else
                      {:status 404 :body (json/generate-string {:error "unexpected url" :url url})}))
                  mistral-http/http-get!
                  (fn [url _opts]
                    (cond
                      (str/ends-with? url "/v1/batch/jobs/job-1")
                      (let [n (swap! job-polls inc)]
                        (if (= 1 n)
                          {:status 200 :body (json/generate-string {:id "job-1" :status "RUNNING"})}
                          {:status 200 :body (json/generate-string {:id "job-1" :status "SUCCESS" :output_file "out-1"})}))

                      (str/ends-with? url "/v1/files/out-1/content")
                      {:status 200 :body out-jsonl}

                      :else
                      {:status 404 :body (json/generate-string {:error "unexpected url" :url url})}))]
      (let [cfg {:api-key "k"
                 :base-url "https://example"
                 :model "mistral-ocr-2512"
                 :batch-enabled? true
                 :batch-poll-ms 1
                 :batch-timeout-ms 5000
                 :batch-max-requests 50
                 :conn-timeout-ms 1
                 :socket-timeout-ms 1
                 :max-retries 0
                 :retry-sleep-ms 0}
            res (mistral-ocr/ocr-extract-batch!
                  cfg
                  [{:custom-id "r1" :bytes (byte-array [1]) :content-type "image/jpeg"}
                   {:custom-id "r2" :bytes (byte-array [2]) :content-type "image/jpeg"}])]
        (is (str/includes? @uploaded-jsonl "\"custom_id\":\"r1\""))
        (is (str/includes? @uploaded-jsonl "\"document_annotation_format\""))
        (is (contains? (:results res) "r1"))
        (is (contains? (:errors res) "r2"))
        (is (= "Hello" (get-in res [:results "r1" :parsed-markdown])))
        (is (= "Store" (get-in res [:results "r1" :extraction :merchant :name])))))))

(ns app.domain.backend.expenses.integrations.llamaparse-test
  (:require
    [app.domain.backend.expenses.integrations.llamaparse :as llamaparse]
    [app.domain.backend.expenses.integrations.llamaparse.http :as llamaparse-http]
    [cheshire.core :as json]
    [clojure.string :as str]
    [clojure.test :refer [deftest is]]))

(deftest build-config-respects-app-config-and-env
  (let [cfg (llamaparse/build-config
              {:llamaparse {:api-key "k"
                            :base-url "https://example"
                            :tier "agentic"
                            :version "latest"
                            :expand "markdown"
                            :agentic-custom-prompt "extract merchant header"
                            :enabled? false
                            :poll-interval-ms 12
                            :poll-timeout-ms 3456
                            :conn-timeout-ms 1
                            :socket-timeout-ms 2
                            :max-retries 3
                            :retry-sleep-ms 4}}
              {:getenv (constantly nil)})]
    (is (= false (:enabled? cfg)))
    (is (= "k" (:api-key cfg)))
    (is (= "https://example" (:base-url cfg)))
    (is (= "agentic" (:tier cfg)))
    (is (= "latest" (:version cfg)))
    (is (= "markdown" (:expand cfg)))
    (is (= "extract merchant header" (:agentic-custom-prompt cfg)))
    (is (= 12 (:poll-interval-ms cfg)))
    (is (= 3456 (:poll-timeout-ms cfg)))
    (is (= 1 (:conn-timeout-ms cfg)))
    (is (= 2 (:socket-timeout-ms cfg)))
    (is (= 3 (:max-retries cfg)))
    (is (= 4 (:retry-sleep-ms cfg)))))

(deftest build-config-supports-agentic-custom-prompt-from-env
  (let [cfg (llamaparse/build-config
              {:llamaparse {:agentic-custom-prompt "from-config"}}
              {:getenv (fn [k]
                         (case k
                           "LLAMAPARSE_AGENTIC_CUSTOM_PROMPT" "from-env"
                           nil))})]
    (is (= "from-env" (:agentic-custom-prompt cfg)))))

(deftest ocr-parse-uploads-and-polls-until-success
  (let [post-call (atom nil)
        get-calls (atom [])
        responses (atom [{:status 200
                          :body (json/generate-string {:id "job-1"
                                                       :status "PENDING"})}
                         {:status 200
                          :body (json/generate-string {:id "job-1"
                                                       :status "COMPLETED"
                                                       :markdown {:pages [{:markdown "A"}
                                                                          {:markdown "B"}]}})}])]
    (with-redefs [llamaparse-http/http-post!
                  (fn [url opts]
                    (reset! post-call {:url url :opts opts})
                    {:status 200
                     :body (json/generate-string {:id "job-1"})})
                  llamaparse-http/http-get!
                  (fn [url opts]
                    (swap! get-calls conj {:url url :opts opts})
                    (let [resp (first @responses)]
                      (swap! responses #(if (next %) (vec (rest %)) %))
                      resp))]
      (let [cfg {:api-key "k"
                 :base-url "https://example"
                 :tier "agentic"
                 :version "latest"
                 :expand "markdown"
                 :agentic-custom-prompt "extract supplier/store/address from header"
                 :poll-interval-ms 1
                 :poll-timeout-ms 5000
                 :conn-timeout-ms 1
                 :socket-timeout-ms 1
                 :max-retries 0
                 :retry-sleep-ms 0}
            result (llamaparse/ocr-parse!
                     cfg
                     {:bytes (.getBytes "hello")
                      :filename "r.jpg"
                      :content-type "image/jpeg"})
            multipart (get-in @post-call [:opts :multipart])
            config-part (first (filter #(= "configuration" (:name %)) multipart))
            config-json (json/parse-string (:content config-part) true)]
        (is (= "llamaparse" (:provider result)))
        (is (= "job-1" (:job-id result)))
        (is (= "A\n\nB" (:parsed-markdown result)))
        (is (= "https://example/api/v2/parse/upload" (:url @post-call)))
        (is (= "Bearer k" (get-in @post-call [:opts :headers "Authorization"])))
        (is (= "agentic" (get config-json :tier)))
        (is (= "latest" (get config-json :version)))
        (is (= "extract supplier/store/address from header"
              (get-in config-json [:agentic_options :custom_prompt])))
        (is (= 2 (count @get-calls)))
        (is (= "https://example/api/v2/parse/job-1" (get-in @get-calls [0 :url])))))))

(deftest ocr-parse-normalizes-items-and-text-into-receipt-markdown
  (let [post-call (atom nil)
        get-calls (atom [])
        responses
        (atom [{:status 200
                :body (json/generate-string {:id "job-1"
                                             :status "PENDING"})}
               {:status 200
                :body (json/generate-string
                        {:id "job-1"
                         :status "COMPLETED"
                         :markdown {:pages [{:markdown "<table><tr><td>RAW</td></tr></table>"}]}
                         :text {:pages [{:text "MY STORE\n13.02.2026. 17:36\nTOTAL: 30,70\n"}]}
                         :items {:pages [{:items [{:type "table"
                                                   :md "| Label | Qty | Price | Total |\n| --- | --- | --- | --- |\n| ITEM A | 1,000x | 10,00 | 10,00 |\n"}]}]}})}])]
    (with-redefs [llamaparse-http/http-post!
                  (fn [url opts]
                    (reset! post-call {:url url :opts opts})
                    {:status 200
                     :body (json/generate-string {:id "job-1"})})
                  llamaparse-http/http-get!
                  (fn [url opts]
                    (swap! get-calls conj {:url url :opts opts})
                    (let [resp (first @responses)]
                      (swap! responses #(if (next %) (vec (rest %)) %))
                      resp))]
      (let [cfg {:api-key "k"
                 :base-url "https://example"
                 :tier "agentic"
                 :version "latest"
                 :expand "items,markdown,text"
                 :poll-interval-ms 1
                 :poll-timeout-ms 5000
                 :conn-timeout-ms 1
                 :socket-timeout-ms 1
                 :max-retries 0
                 :retry-sleep-ms 0}
            result (llamaparse/ocr-parse!
                     cfg
                     {:bytes (.getBytes "hello")
                      :filename "r.jpg"
                      :content-type "image/jpeg"})
            expand (get-in @get-calls [0 :opts :query-params :expand])
            md (:parsed-markdown result)]
        (is (= "items,text" expand))
        (is (string? (:provider-markdown result)))
        (is (string? md))
        (is (str/includes? md "MY STORE"))
        (is (str/includes? md "| ITEM A"))
        (is (str/includes? md "TOTAL:"))))))

(deftest build-config-remaps-agentic-v2-to-latest
  (let [cfg (llamaparse/build-config
              {:llamaparse {:tier "agentic"
                            :version "v2"
                            :api-key "k"}}
              {:getenv (constantly nil)})]
    (is (= "agentic" (:tier cfg)))
    (is (= "latest" (:version cfg)))))


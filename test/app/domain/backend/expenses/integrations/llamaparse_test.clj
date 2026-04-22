(ns app.domain.backend.expenses.integrations.llamaparse-test
  (:require
    [app.domain.backend.expenses.integrations.llamaparse :as llamaparse]
    [app.domain.backend.expenses.integrations.llamaparse.http :as llamaparse-http]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction :as receipt-extract]
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

(deftest build-config-defaults-expand-to-items-markdown-text
  (let [cfg (llamaparse/build-config
              {:llamaparse {:api-key "k"}}
              {:getenv (constantly nil)})]
    (is (= "items,markdown,text" (:expand cfg)))))

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

(deftest receipt-extraction-parses-discount-and-qty-rows
  (let [resp {:text {:pages [{:text (str "====E=EE=5SE\n"
                                      "\"Pepco B-H\" d.o.o.\n"
                                      "2.12.2025. 19:46\n"
                                      "TOTAL: 14,60\n")}]}
              :items {:pages [{:items [{:type "header"
                                        :md (str "====E=EE=5SE\n"
                                              "\"Pepco B-H\" d.o.o.\n"
                                              "2.12.2025. 19:46\n")}
                                       {:type "table"
                                        :rows [["62778401 Item A" "10,00E"]
                                               ["t/pc" ""]
                                               ["-50,00%:" "5,00"]
                                               ["ITEM B" "9,60E"]
                                               ["2,000x" "4,80"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= "Pepco B-H" (get-in extraction [:merchant :name])))
    (is (= "2025-12-02T19:46:00" (:purchased_at extraction)))
    (is (= 14.60M (bigdec (get-in extraction [:totals :total]))))
    (is (= 2 (count items)))
    (is (= "Item A" (:raw_label (first items))))
    (is (= 1M (bigdec (get-in items [0 :qty]))))
    (is (= 5M (bigdec (get-in items [0 :unit_price]))))
    (is (= 5M (bigdec (get-in items [0 :line_total]))))
    (is (= "ITEM B" (:raw_label (second items))))
    (is (= 2M (bigdec (get-in items [1 :qty]))))
    (is (= 4.80M (bigdec (get-in items [1 :unit_price]))))
    (is (= 9.60M (bigdec (get-in items [1 :line_total]))))))

(deftest receipt-extraction-parses-no-header-four-column-table-rows
  (let [resp {:items {:pages [{:items [{:type "table"
                                        :rows [["CIG DUNHILL ESSENCE BRONZE" "3,000x" "6,60" "19,80E"]
                                               ["CHIPSY XCUT SALTED 140G" "" "" "3,60E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 2 (count items)))
    (is (= {:raw_label "CIG DUNHILL ESSENCE BRONZE"
            :qty 3.000M
            :unit_price 6.60M
            :line_total 19.80M}
          (first items)))
    (is (= {:raw_label "CHIPSY XCUT SALTED 140G"
            :qty 1M
            :unit_price 3.60M
            :line_total 3.60M}
          (second items)))
    (is (= 23.40M (bigdec (get-in extraction [:totals :total]))))))

(deftest receipt-extraction-parses-inline-discount-unit-row
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "\"Pepco B-H\" d.o.o.\n"
                                              "Podružnica Sarajevo 2\n"
                                              "ul. Kolodvorska br.12\n"
                                              "71000 Sarajevo\n")}
                                       {:type "table"
                                        :rows [["62778401 Mirisna svijeca u staklu Premium Collec" "10,00E" ""]
                                               ["t/pc" "" "-50,00%: 5,00"]
                                               ["62778401 Mirisna svijeca u staklu Premium Collec" "10,00E" ""]
                                               ["t/pc" "" "-50,00%: 5,00"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 2 (count items)))
    (is (= ["Mirisna svijeca u staklu Premium Collec"
            "Mirisna svijeca u staklu Premium Collec"]
          (mapv :raw_label items)))
    (is (= [5M 5M]
          (mapv (comp bigdec :line_total) items)))
    (is (= [5M 5M]
          (mapv (comp bigdec :unit_price) items)))
    (is (= 10M (bigdec (get-in extraction [:totals :total]))))))

(deftest receipt-extraction-prefers-structured-text-and-rejects-qty-price-fragment-labels
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "APOTEKE SARAJEVO\n"
                                              "12.02.2026. 15:05\n")}
                                       {:type "text"
                                        :value (str "2,000x 7,56 15,12E\n"
                                                 "-100,00%: 0,00\n"
                                                 "GALAS_ČAJ_UROLOŠKI_100_G_210f 7,40E\n"
                                                 "2PVC_KESA_SREDNJA_6f94 0,10E\n"
                                                 "TOTAL: 7,50")}]}]}
              :text {:pages [{:text (str "APOTEKE SARAJEVO\n"
                                      "12.02.2026. 15:05\n"
                                      "2,000x         7,56         15,12E\n"
                                      "-100,00%:      0,00\n"
                                      "GALAS_CAJ_UROLOSKI_100_G_210f7,40E\n"
                                      "2PVC_KESA_SREDNJA_6f94       0,10E\n"
                                      "TOTAL:               7,50\n")}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 2 (count items)))
    (is (= ["GALAS ČAJ UROLOŠKI 100 G 210f"
            "2PVC KESA SREDNJA 6f94"]
          (mapv :raw_label items)))
    (is (= [7.40M 0.10M]
          (mapv (comp bigdec :line_total) items)))
    (is (= 7.50M (bigdec (get-in extraction [:totals :total]))))))

(deftest receipt-extraction-prefers-structured-code-block-over-noisy-page-text
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "\"Pepco B-H\" d.o.o.\n"
                                              "Podružnica Sarajevo 2\n"
                                              "ul. Kolodvorska br.12\n"
                                              "71000 Sarajevo\n")}
                                       {:type "code"
                                        :value (str "62778401 Mirisna svijeca u staklu Premium Collec\n"
                                                 "t/pc                                      10,00E\n"
                                                 "                                          -50,00%:   5,00\n"
                                                 "62778401 Mirisna svijeca u staklu Premium Collec\n"
                                                 "t/pc                                      10,00E\n"
                                                 "                                          -50,00%:   5,00\n"
                                                 "TOTAL:                                    10,00")}]}]}
              :text {:pages [{:text (str "Pepco B-H d.0.o.\n"
                                      "25.12.2025. 19:46\n"
                                      "62778401 Mirisna svijeca u stak lu Premium Collec\n"
                                      "t/pc              -50,00%:                 10,00E\n"
                                      "                                            TyooL\n"
                                      "                                             5,00\n"
                                      "62778401 Mirisna svijeca u staklu Premium Collec\n"
                                      "t/pc              -50,00%:                 10,00E\n"
                                      "                                             5,00\n"
                                      "TOTAL:            10,00\n"
                                      "unaPr             )\n"
                                      "                                             0,00")}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 2 (count items)))
    (is (= ["Mirisna svijeca u staklu Premium Collec"
            "Mirisna svijeca u staklu Premium Collec"]
          (mapv :raw_label items)))
    (is (= [5M 5M]
          (mapv (comp bigdec :line_total) items)))
    (is (= 10M (bigdec (get-in extraction [:totals :total]))))))

(deftest receipt-extraction-parses-structured-text-label-followed-by-qty-line
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "\"KONZUM\" d.o.o. Sarajevo\n"
                                              "Podružnica br. 66\n"
                                              "Prodavnica br. 90 Sarajevo\n"
                                              "Braće Begić 3\n"
                                              "71101 SARAJEVO CENTAR\n")}
                                       {:type "text"
                                        :value (str "BF: 394987\n"
                                                 "06.01.2026. 15:53\n"
                                                 "SECER BRAZILAS 1KG\n"
                                                 "2,000x 1,50 3,00E\n"
                                                 "SECER SMEDI 800G\n"
                                                 "1,000x 3,25 3,25E\n"
                                                 "TOTAL: 6,25")}]}]}
              :text {:pages [{:text (str "KONZUM\n"
                                      "06.01.2026. 15:53\n"
                                      "TOTAL: 6,25\n")}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 2 (count items)))
    (is (= ["SECER BRAZILAS 1KG"
            "SECER SMEDI 800G"]
          (mapv :raw_label items)))
    (is (= [2M 1M]
          (mapv (comp bigdec :qty) items)))
    (is (= [1.50M 3.25M]
          (mapv (comp bigdec :unit_price) items)))
    (is (= [3.00M 3.25M]
          (mapv (comp bigdec :line_total) items)))
    (is (= 6.25M (bigdec (get-in extraction [:totals :total]))))))

(deftest receipt-extraction-parses-structured-text-split-qty-and-price-lines
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "JU \"APOTEKE SARAJEVO\" SARAJEVO\n"
                                              "APOTEKA \"KOSEVSKO BRDO\"\n"
                                              "BRACE BEGIC br.4\n"
                                              "71000 Sarajevo\n")}
                                       {:type "text"
                                        :value (str "BF: 234080\n"
                                                 "06.01.2026. 15:48\n"
                                                 "CASA_ZA_URIN_KLIK_125_ML_ROMED_48d7\n"
                                                 "2,000x\n"
                                                 "0,55 1,10E\n"
                                                 "TOPLOMJER_DIGITALNI_UEBE_TH1_COLOR_CVRST_17e0 7,15E\n"
                                                 "TOTAL: 8,25")}]}]}
              :text {:pages [{:text (str "APOTEKE SARAJEVO\n"
                                      "06.01.2026. 15:48\n"
                                      "TOTAL: 8,25\n")}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 2 (count items)))
    (is (= ["CASA ZA URIN KLIK 125 ML ROMED 48d7"
            "TOPLOMJER DIGITALNI UEBE TH1 COLOR CVRST 17e0"]
          (mapv :raw_label items)))
    (is (= [2M 1M]
          (mapv (comp bigdec :qty) items)))
    (is (= [0.55M 7.15M]
          (mapv (comp bigdec :unit_price) items)))
    (is (= [1.10M 7.15M]
          (mapv (comp bigdec :line_total) items)))
    (is (= 8.25M (bigdec (get-in extraction [:totals :total]))))))

(deftest receipt-extraction-does-not-treat-product-percent-as-discount
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "\"KONZUM\" d.o.o. Sarajevo\n"
                                              "Podružnica br. 66\n"
                                              "Prodavnica br. 90 Sarajevo\n"
                                              "Braće Begić 3\n"
                                              "71101 SARAJEVO CENTAR\n")}
                                       {:type "table"
                                        :rows [["MLIJEKO MEGGLE 3,2% 657" "6,75E" ""]
                                               ["3,000x" "" "2,25"]]}]}]}
              :text {:pages [{:text (str "VE: 17,00%\n"
                                      "OSN. E: 5,77\n"
                                      "PDV E: 0,98\n"
                                      "PDV: 0,98\n")}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 1 (count items)))
    (is (= "MLIJEKO MEGGLE 3,2% 657" (:raw_label (first items))))
    (is (= 3M (bigdec (get-in items [0 :qty]))))
    (is (= 2.25M (bigdec (get-in items [0 :unit_price]))))
    (is (= 6.75M (bigdec (get-in items [0 :line_total]))))
    (is (= 6.75M (bigdec (get-in extraction [:totals :total]))))))

(deftest receipt-extraction-prefers-header-over-body-items
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "\"UNI-EXPERT\" d.o.o.\n"
                                              "Trg Djece Sarajeva broj 1\n"
                                              "71000 Sarajevo\n"
                                              "JIB: 4245018500121\n")}
                                       {:type "table"
                                        :rows [["Logilink Baterije AAA Alkaline" "2,70E"]]}]}]}
              :text {:pages [{:text (str "\"UNI-EXPERT\" d.o.o.\n"
                                      "Trg Djece Sarajeva broj 1\n"
                                      "71000 Sarajevo\n"
                                      "JIB: 4245018500121\n"
                                      "FISKALNI RACUN\n"
                                      "BF: 28933\n"
                                      "24.01.2026. 12:30\n"
                                      "Logilink Baterije AAA Alkaline\n"
                                      "TOTAL: 6,70\n")}]}}
        extraction (receipt-extract/response->extraction resp)]
    (is (= "UNI-EXPERT" (get-in extraction [:merchant :name])))))

(deftest receipt-extraction-prefers-pre-table-heading-over-descriptive-text
  (let [resp {:items {:pages [{:items [{:type "heading"
                                        :value "STEP d.o.o."}
                                       {:type "text"
                                        :value (str "ZA UNUTRAŠNJU I SPOLJNU TRGOVINU,\n"
                                                 "UGOSTITELJSTVO I TURIZAM\n"
                                                 "Mis Irbina 8\n"
                                                 "71000 Sarajevo")}
                                       {:type "text"
                                        :value (str "BF: 126534\n"
                                                 "12.04.2026. 13:48")}
                                       {:type "table"
                                        :rows [["ESPRESSO KAFA/ko" "2,000x" "2,00" "4,00E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        receipt (receipt-extract/response->receipt resp)]
    (is (= "STEP" (get-in extraction [:merchant :name])))
    (is (= "STEP" (first (str/split-lines (:parsed-markdown receipt)))))))

(deftest receipt-extraction-prefers-quoted-heading-over-owner-text
  (let [resp {:items {:pages [{:items [{:type "heading"
                                        :value "TR CVJEĆARA \"BY AJJA\""}
                                       {:type "text"
                                        :value (str "vl. Pećar Jasna\n"
                                                 "ul. Azize Šaćirbegović bb\n"
                                                 "71000 Sarajevo")}
                                       {:type "text"
                                        :value (str "BF: 186\n"
                                                 "17.04.2026. 11:40")}
                                       {:type "table"
                                        :rows [["Orhideja 25/kom" "25,00A"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        receipt (receipt-extract/response->receipt resp)]
    (is (= "BY AJJA" (get-in extraction [:merchant :name])))
    (is (= "BY AJJA" (first (str/split-lines (:parsed-markdown receipt)))))
    (is (= "ul. Azize Šaćirbegović bb, 71000 Sarajevo"
          (get-in extraction [:merchant :address])))))

(deftest receipt-extraction-ignores-generic-heading-when-header-identifies-merchant
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "\"BINGO\" d.o.o. EXPORT-IMPORT TUZLA\n"
                                              "PJ 219 \"Supermarket Alta\" Sarajevo\n"
                                              "Bulevar Franca Lehara br. 2\n"
                                              "71000 Sarajevo\n")}
                                       {:type "text"
                                        :value (str "JIB: 4209253454360\n"
                                                 "PIB: 209253450003")}
                                       {:type "heading"
                                        :value "FISKALNI RAČUN"}
                                       {:type "table"
                                        :rows [["ITEM" "1,00E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        merchant (:merchant extraction)]
    (is (= "BINGO" (:name merchant)))
    (is (= "PJ 219 \"Supermarket Alta\" Sarajevo" (:store_name merchant)))))

(deftest receipt-extraction-ignores-agentic-merchant-information-heading
  (let [resp {:text {:pages [{:text (str "\"Pepco B-H\" d.o.o.\n"
                                      "Podružnica Sarajevo 2\n"
                                      "ul. Kolodvorska br.12\n"
                                      "71000 Sarajevo\n"
                                      "JIB: 4203144510090\n")}]}
              :items {:pages [{:items [{:type "heading"
                                        :value "Merchant Information"}
                                       {:type "list"
                                        :value (str "* **merchant.name**: \"Pepco B-H\" d.o.o.\n"
                                                 "* **merchant.store_name**: Podružnica Sarajevo 2\n"
                                                 "* **merchant.address**: ul. Kolodvorska br.12 71000 Sarajevo")}
                                       {:type "code"
                                        :value (str "=================================================================\n"
                                                 "              \"Pepco B-H\" d.o.o.\n"
                                                 "            Podružnica Sarajevo 2\n"
                                                 "            ul. Kolodvorska br.12\n"
                                                 "            71000 Sarajevo\n")}
                                       {:type "table"
                                        :rows [["ITEM" "1,00E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        receipt (receipt-extract/response->receipt resp)]
    (is (= "Pepco B-H" (get-in extraction [:merchant :name])))
    (is (= "Pepco B-H" (first (str/split-lines (:parsed-markdown receipt)))))))

(deftest receipt-extraction-prefers-quoted-or-legal-over-branch-line
  (let [itx {:items {:pages [{:items [{:type "header"
                                       :md (str "ITX BH d.o.o.\n"
                                             "Podružnica Sarajevo\n"
                                             "Vrbanja br. 1\n"
                                             "Sarajevo City Center\n"
                                             "71000 Sarajevo\n")}]}]}}
        pepco {:items {:pages [{:items [{:type "header"
                                         :md (str "\"Pepco B-H\" d.o.o.\n"
                                               "Podružnica Sarajevo 2\n"
                                               "ul. Kolodvorska br.12\n"
                                               "71000 Sarajevo\n")}]}]}}
        ex1 (receipt-extract/response->extraction itx)
        ex2 (receipt-extract/response->extraction pepco)]
    (is (= "ITX BH" (get-in ex1 [:merchant :name])))
    (is (= "Pepco B-H" (get-in ex2 [:merchant :name])))))

(deftest receipt-extraction-builds-store-context-for-alias-resolution
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "\"LUPRIV PLUS Mostar\" d.o.o.\n"
                                              "Ogranak Sarajevo 1\n"
                                              "Milana Preloga 2 S\n"
                                              "71120 Novo Sarajevo\n"
                                              "JIB: 4245018500121\n")}
                                       {:type "table"
                                        :rows [["ITEM" "1,00E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        merchant (:merchant extraction)]
    (is (= "LUPRIV PLUS Mostar" (:name merchant)))
    (is (= "Ogranak Sarajevo 1" (:store_name merchant)))
    (is (= "Milana Preloga 2 S, 71120 Novo Sarajevo" (:address merchant)))
    (is (= "Ogranak Sarajevo 1, Milana Preloga 2 S, 71120 Novo Sarajevo"
          (:raw_address merchant)))))

(deftest receipt-extraction-ignores-agentic-merchant-field-header-lines
  (let [resp {:text {:pages [{:text (str "Privatna apoteka \"MEDISAN\"\n"
                                      "mr.ph Amela Hodić\n"
                                      "JUKIĆEVA DO BROJA 2\n"
                                      "71103 SARAJEVO CENTAR\n"
                                      "JIB: 4300502820000\n")}]}
              :items {:pages [{:items [{:type "header"
                                        :md (str "merchant.name: Privatna apoteka \"MEDISAN\" mr.ph Amela Hodić\n"
                                              "merchant.store_name: null\n"
                                              "merchant.address: JUKIĆEVA DO BROJA 2, 71103 SARAJEVO CENTAR\n"
                                              "merchant.raw_address: null, JUKIĆEVA DO BROJA 2, 71103 SARAJEVO CENTAR")
                                        :items [{:type "text"
                                                 :value (str "merchant.name: Privatna apoteka \"MEDISAN\" mr.ph Amela Hodić\n"
                                                          "merchant.store_name: null\n"
                                                          "merchant.address: JUKIĆEVA DO BROJA 2, 71103 SARAJEVO CENTAR\n"
                                                          "merchant.raw_address: null, JUKIĆEVA DO BROJA 2, 71103 SARAJEVO CENTAR")}]}
                                       {:type "text"
                                        :md (str "Privatna apoteka \"MEDISAN\"\n"
                                              "mr.ph Amela Hodić\n"
                                              "JUKIĆEVA DO BROJA 2\n"
                                              "71103 SARAJEVO CENTAR")}
                                       {:type "table"
                                        :rows [["ITEM" "17,50E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        merchant (:merchant extraction)]
    (is (= "MEDISAN" (:name merchant)))
    (is (= "JUKIĆEVA DO BROJA 2, 71103 SARAJEVO CENTAR" (:address merchant)))
    (is (not (str/includes? (:address merchant) "merchant.address:")))
    (is (not (str/includes? (:raw_address merchant) "merchant.raw_address:")))))

(deftest receipt-extraction-recognizes-pj-store-line-and-address
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "\"BINGO\" d.o.o. EXPORT-IMPORT TUZLA\n"
                                              "PJ 219 \"Supermarket Alta\" Sarajevo\n"
                                              "Bulevar Franca Lehara br. 2, Alta Shopping Centa\n"
                                              "71000 SARAJEVO\n"
                                              "JIB: 4209253454360\n")}
                                       {:type "table"
                                        :rows [["ITEM" "1,00E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        merchant (:merchant extraction)]
    (is (= "BINGO" (:name merchant)))
    (is (= "PJ 219 \"Supermarket Alta\" Sarajevo" (:store_name merchant)))
    (is (= "Bulevar Franca Lehara br. 2, Alta Shopping Centa, 71000 SARAJEVO"
          (:address merchant)))
    (is (= "PJ 219 \"Supermarket Alta\" Sarajevo, Bulevar Franca Lehara br. 2, Alta Shopping Centa, 71000 SARAJEVO"
          (:raw_address merchant)))))

(deftest receipt-extraction-recognizes-pj-broj-store-line-and-address
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "\"AFRODITA\" d.o.o.\n"
                                              "P.J. BROJ 3 - MESNICA MAŠIĆ\n"
                                              "Alipašina bb\n"
                                              "71000 Sarajevo\n"
                                              "JIB: 4404096370049\n")}
                                       {:type "table"
                                        :rows [["ITEM" "1,00E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        merchant (:merchant extraction)]
    (is (= "AFRODITA" (:name merchant)))
    (is (= "P.J. BROJ 3 - MESNICA MAŠIĆ" (:store_name merchant)))
    (is (= "Alipašina bb, 71000 Sarajevo" (:address merchant)))))

(deftest receipt-extraction-recognizes-apoteka-store-line-and-street-address
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "== == == == == == == == == == == ==\n"
                                              "JU \"APOTEKE SARAJEVO\" SARAJEVO\n"
                                              "APOTEKA \"KOŠEVSKO BRDO\"\n"
                                              "BRAĆE BEGIĆ br.4\n"
                                              "71000 Sarajevo\n")}
                                       {:type "table"
                                        :rows [["ITEM" "1,00E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        merchant (:merchant extraction)]
    (is (= "APOTEKE SARAJEVO" (:name merchant)))
    (is (= "APOTEKA \"KOŠEVSKO BRDO\"" (:store_name merchant)))
    (is (= "BRAĆE BEGIĆ br.4, 71000 Sarajevo" (:address merchant)))))

(deftest receipt-extraction-recognizes-pj-dot-store-line
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "\"ŠAMON PROMET\" doo Sarajevo\n"
                                              "P.J.3 \"HORECA SHOP I MARKET\"\n"
                                              "MARŠALA TITA 7\n"
                                              "71120 SARAJEVO CENTAR\n"
                                              "JIB: 4200397100042\n")}
                                       {:type "table"
                                        :rows [["ITEM" "1,00E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        merchant (:merchant extraction)]
    (is (= "ŠAMON PROMET" (:name merchant)))
    (is (= "P.J.3 \"HORECA SHOP I MARKET\"" (:store_name merchant)))
    (is (= "MARŠALA TITA 7, 71120 SARAJEVO CENTAR" (:address merchant)))))

(deftest receipt-extraction-deduplicates-repeated-header-address-lines
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "JU \"APOTEKE SARAJEVO\" SARAJEVO\n"
                                              "APOTEKA \"KOŠEVSKO BRDO\"\n"
                                              "BRAĆE BEGIĆ br.4\n"
                                              "71000 Sarajevo\n"
                                              "APOTEKA \"KOŠEVSKO BRDO\"\n"
                                              "BRAĆE BEGIĆ br.4\n"
                                              "71000 Sarajevo\n")}
                                       {:type "table"
                                        :rows [["ITEM" "1,00E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        merchant (:merchant extraction)]
    (is (= "APOTEKA \"KOŠEVSKO BRDO\"" (:store_name merchant)))
    (is (= "BRAĆE BEGIĆ br.4, 71000 Sarajevo" (:address merchant)))
    (is (= "APOTEKA \"KOŠEVSKO BRDO\", BRAĆE BEGIĆ br.4, 71000 Sarajevo"
          (:raw_address merchant)))))

(deftest receipt-extraction-strips-leading-artikal-token-from-item-label
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "\"BINGO\" d.o.o. EXPORT-IMPORT TUZLA\n"
                                              "PJ 219 \"Supermarket Alta\" Sarajevo\n"
                                              "Bulevar Franca Lehara br. 2, Alta Shopping Centa\n"
                                              "71000 SARAJEVO\n"
                                              "JIB: 4209253454360\n")}
                                       {:type "table"
                                        :rows [["Artikal" "Količina x Cijena" "Iznos"]
                                               ["Artikal BOMBONJERA 230G RAFFAELLO FER" "1,000x 9,90" "9,90E"]
                                               ["SA9192 ZDJELA SA POKLOPCEM 0 65L FR" "1,000x 1,90" "1,90E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 2 (count items)))
    (is (= "BOMBONJERA 230G RAFFAELLO FER" (:raw_label (first items))))
    (is (= "SA9192 ZDJELA SA POKLOPCEM 0 65L FR" (:raw_label (second items))))
    (is (= 11.80M (bigdec (get-in extraction [:totals :total]))))))

(deftest receipt-extraction-does-not-promote-quoted-store-name-to-supplier
  (let [resp {:items {:pages [{:items [{:type "header"
                                        :md (str "BINGO doo EXPORT-IMPORT TUZLA\n"
                                              "PJ 57, \"HIPERMARKET\" Otoka\n"
                                              "ul. Džemala Bijedića br. 123\n"
                                              "79220 SARAJEVO NOVI GRAD\n"
                                              "JIB: 4209253451751\n")}
                                       {:type "table"
                                        :rows [["ITEM" "1,00E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        merchant (:merchant extraction)]
    (is (= "BINGO" (:name merchant)))
    (is (= "PJ 57, \"HIPERMARKET\" Otoka" (:store_name merchant)))
    (is (= "ul. Džemala Bijedića br. 123, 79220 SARAJEVO NOVI GRAD"
          (:address merchant)))))

(deftest receipt-extraction-handles-multiple-split-label-and-qty-rows
  (let [resp {:text {:pages [{:text "ITX BH\n19.01.2026. 18:22\nTOTAL: 39,90\n"}]}
              :items {:pages [{:items [{:type "table"
                                        :rows [["RWC 0116523953805 SUKNJA" "" ""]
                                               ["1,000x" "19,95" "19,95E"]
                                               ["RWC 0364480986004 HLAČE" "" ""]
                                               ["1,000x" "19,95" "19,95E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 2 (count items)))
    (is (= 39.90M (bigdec (get-in extraction [:totals :total]))))
    (is (= ["RWC 0116523953805 SUKNJA" "RWC 0364480986004 HLAČE"]
          (mapv :raw_label items)))
    (is (= [1M 1M] (mapv (comp bigdec :qty) items)))
    (is (= [19.95M 19.95M] (mapv (comp bigdec :unit_price) items)))
    (is (= [19.95M 19.95M] (mapv (comp bigdec :line_total) items)))))

(deftest receipt-extraction-keeps-single-row-priced-items-around-split-qty-rows
  (let [resp {:text {:pages [{:text "TROPIC MALOPRODAJA\n25.03.2026. 13:29\nTOTAL: 23,58\n"}]}
              :items {:pages [{:items [{:type "table"
                                        :rows [["HLJEB KARINGTON SA Z/KG" "" "3,50" "E"]
                                               ["VEGETARIAN PAELLA /KG" "" "" ""]
                                               ["0,226x" "24,95" "5,64" "E"]
                                               ["TELECA DZIGERICA /KG" "" "" ""]
                                               ["0,353x" "10,95" "3,87" "E"]
                                               ["Salata RK 1kg RK /KG" "" "" ""]
                                               ["0,096x" "22,45" "2,16" "E"]
                                               ["Kukuruza 210 tk RK /KG" "" "1,35" "E"]
                                               ["Krompir kuvani 1kg K/KG" "" "" ""]
                                               ["0,462x" "15,98" "7,06" "E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 6 (count items)))
    (is (= 23.58M (bigdec (get-in extraction [:totals :total]))))
    (is (= ["HLJEB KARINGTON SA Z/KG"
            "VEGETARIAN PAELLA /KG"
            "TELECA DZIGERICA /KG"
            "Salata RK 1kg RK /KG"
            "Kukuruza 210 tk RK /KG"
            "Krompir kuvani 1kg K/KG"]
          (mapv :raw_label items)))
    (is (= [3.50M 5.64M 3.87M 2.16M 1.35M 7.06M]
          (mapv (comp bigdec :line_total) items)))
    (is (= [1M 0.226M 0.353M 0.096M 1M 0.462M]
          (mapv (comp bigdec :qty) items)))
    (is (= [3.50M 24.95M 10.95M 22.45M 1.35M 15.98M]
          (mapv (comp bigdec :unit_price) items)))))

(deftest receipt-extraction-parses-popust-row-with-pct-in-second-cell
  (let [resp {:text {:pages [{:text "MY STORE\n10.01.2026. 12:01\nTOTAL: 6,75\n"}]}
              :items {:pages [{:items [{:type "table"
                                        :rows [["ITEM A" "7,50E" ""]
                                               ["1,000x" "7,50" "7,50E"]
                                               ["POPUST" "-10,00%:" "6,75"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 1 (count items)))
    (is (= "ITEM A" (:raw_label (first items))))
    (is (= 6.75M (bigdec (get-in items [0 :line_total]))))))

(deftest receipt-extraction-handles-split-label-and-qty-rows
  (let [resp {:items {:pages [{:items [{:type "table"
                                        :rows [["Kafa espresso/ko" "" ""]
                                               ["2,000x" "3,00" "6,00E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 1 (count items)))
    (is (= "Kafa espresso/ko" (:raw_label (first items))))
    (is (= 2M (bigdec (get-in items [0 :qty]))))
    (is (= 3.00M (bigdec (get-in items [0 :unit_price]))))
    (is (= 6.00M (bigdec (get-in items [0 :line_total]))))))

(deftest receipt-extraction-keeps-pending-label-across-code-row
  (let [resp {:text {:pages [{:text "APOTEKE SARAJEVO\n06.01.2026. 15:48\nTOTAL: 8,25\n"}]}
              :items {:pages [{:items [{:type "table"
                                        :rows [["CASA_ZA_URIN_KLIK_125_ML" "" ""]
                                               ["7" "" ""]
                                               ["2,000x" "0,55" "1,10E"]
                                               ["TOPLOMJER_DIGITALNI_UEBE_TH1_COLOR" "" ""]
                                               ["_ČVRST_17e0" "7,15E" ""]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 2 (count items)))
    (is (= 8.25M (bigdec (get-in extraction [:totals :total]))))
    (is (= 1.10M (bigdec (get-in items [0 :line_total]))))
    (is (= 2M (bigdec (get-in items [0 :qty]))))
    (is (= 0.55M (bigdec (get-in items [0 :unit_price]))))
    (is (= 7.15M (bigdec (get-in items [1 :line_total]))))
    (is (str/includes? (:raw_label (second items)) "TOPLOMJER"))
    (is (str/includes? (:raw_label (second items)) "ČVRST"))))

(deftest receipt-extraction-does-not-treat-percent-in-label-as-discount
  (let [resp {:items {:pages [{:items [{:type "table"
                                        :rows [["MEGGLE MLIJEKO 3.2%MM 1L 12" "6,30E" ""]
                                               ["3,000x" "2,10" ""]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 1 (count items)))
    (is (= 3M (bigdec (get-in items [0 :qty]))))
    (is (= 2.10M (bigdec (get-in items [0 :unit_price]))))
    (is (= 6.30M (bigdec (get-in items [0 :line_total]))))))

(deftest receipt-extraction-does-not-treat-vegeta-as-summary
  (let [resp {:items {:pages [{:items [{:type "table"
                                        :rows [["VEGETA 250GR PODRAVKA" "3,60E" ""]
                                               ["1,000x" "3,60" "3,60E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)]
    (is (= 1 (count items)))
    (is (= "VEGETA 250GR PODRAVKA" (:raw_label (first items))))
    (is (= 3.60M (bigdec (get-in items [0 :line_total]))))))

(deftest receipt-extraction-infers-3-col-table-mapping-and-embedded-qty
  (let [resp {:items {:pages [{:items [{:type "table"
                                        :rows [["D20720 SUNKA PURECA DELUX VINDON<br/>0,098x" "29,95" "2,94E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        item (first (:items extraction))]
    (is (= 1 (count (:items extraction))))
    (is (= "SUNKA PURECA DELUX VINDON" (:raw_label item)))
    (is (= 0.098M (bigdec (:qty item))))
    (is (= 29.95M (bigdec (:unit_price item))))
    (is (= 2.94M (bigdec (:line_total item))))))

(deftest ocr-extract-returns-structured-extraction
  (with-redefs [llamaparse/ocr-parse!
                (fn [_cfg _req]
                  {:provider "llamaparse"
                   :raw {:ok true}
                   :extraction {:merchant {:name "Pepco"}
                                :purchased_at "2026-01-01"
                                :totals {:total 1}
                                :items []}
                   :parsed-markdown "hello"
                   :received-at "2026-01-01T00:00:00Z"
                   :model "llamaparse/agentic"
                   :job-id "job-9"})]
    (let [res (llamaparse/ocr-extract! {:api-key "k"} {:bytes (.getBytes "x")})]
      (is (= "llamaparse" (:provider res)))
      (is (= "Pepco" (get-in res [:extraction :merchant :name])))
      (is (= 1 (get-in res [:extraction :totals :total])))
      (is (= "hello" (:parsed-markdown res)))
      (is (= "job-9" (:job-id res))))))

(deftest receipt-extraction-falls-back-to-text-items
  (let [resp {:text {:content "Some header\n\nŽENSKA PIDŽAMA 24,00E\nGALAS_... 39,95E\nGALAS_... 7,65E\n\nUKUPNO 71,60E"}}
        ext (receipt-extract/response->extraction resp)]
    (is (= 3 (count (:items ext))))
    (is (= {:raw_label "ŽENSKA PIDŽAMA"
            :qty 1M
            :unit_price 24.00M
            :line_total 24.00M}
          (first (:items ext))))
    (is (= {:raw_label "GALAS ..."
            :qty 1M
            :unit_price 39.95M
            :line_total 39.95M}
          (second (:items ext))))
    (is (= {:raw_label "GALAS ..."
            :qty 1M
            :unit_price 7.65M
            :line_total 7.65M}
          (nth (:items ext) 2)))
    (is (= 71.60M (-> ext :totals :total)))))

(deftest receipt-extraction-falls-back-to-split-text-item-lines
  (let [resp {:text {:content "04.02.2026. 19:41\n357 35924792904 ZENSKA PIDZAMA\n24,00E\nGALAS_HERPEGAL_MAST_10_G_5122\n7,65E\nTOTAL: 31,65"}}
        ext (receipt-extract/response->extraction resp)
        items (:items ext)]
    (is (= 2 (count items)))
    (is (= 24.00M (-> items first :line_total)))
    (is (= 7.65M (-> items second :line_total)))
    (is (= "357 35924792904 ZENSKA PIDZAMA" (-> items first :raw_label)))
    (is (= "GALAS HERPEGAL MAST 10 G 5122" (-> items second :raw_label)))
    (is (= 31.65M (-> ext :totals :total)))))

(deftest receipt-extraction-combines-split-text-labels-and-prefers-fallback-total-consensus
  (let [resp {:text {:pages [{:text (str "\"PENNY PLUS\" d.o.o. Sarajevo\n"
                                      "16.03.2026. 13:06\n"
                                      "VRECA VAKUM ZA ODJECU HENGER XL 70\n"
                                      "x145cm            8,95E\n"
                                      "TOTAL:        ME98,95\n"
                                      "Gotovina:                8,95\n"
                                      "Ukupno:                  8,95\n")}]}}
        ext (receipt-extract/response->extraction resp)
        items (:items ext)]
    (is (= 1 (count items)))
    (is (= "VRECA VAKUM ZA ODJECU HENGER XL 70 x145cm" (-> items first :raw_label)))
    (is (= 8.95M (-> items first :line_total bigdec)))
    (is (= 8.95M (-> items first :unit_price bigdec)))
    (is (= 1M (-> items first :qty bigdec)))
    (is (= 8.95M (-> ext :totals :total bigdec)))))

(deftest receipt-extraction-prefers-items-total-when-line-totals-are-high-reliability
  (let [resp {:items {:pages [{:items [{:type "table"
                                        :md (str "| ITEM A | 1,000x | 10,00 | 10,00E |\n"
                                              "| ITEM B | 1,000x | 10,10 | 10,10E |")
                                        :rows [["ITEM A" "1,000x" "10,00" "10,00E"]
                                               ["ITEM B" "1,000x" "10,10" "10,10E"]]
                                        :bbox [{:confidence 0.40}]}
                                       {:type "table"
                                        :md "| TOTAL: | 20,13 |"
                                        :rows [["TOTAL:" "20,13"]]
                                        :bbox [{:confidence 0.41}]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        provider-confidence (:provider_confidence extraction)]
    (is (= 20.10M (bigdec (get-in extraction [:totals :total]))))
    (is (= 20.10M (bigdec (:items_total provider-confidence))))
    (is (= 1.0 (:line_total_reliability provider-confidence)))
    (is (= 0.41 (:selected_total_confidence provider-confidence)))
    (is (= :items_total_high_line_total_reliability (:reconciliation_basis provider-confidence)))))

(deftest receipt-extraction-keeps-provider-total-when-small-mismatch-is-not-single-digit
  (let [resp {:items {:pages [{:items [{:type "table"
                                        :md (str "| ITEM A | 1,000x | 0,49 | 0,49E |\n"
                                              "| ITEM B | 1,000x | 0,50 | 0,50E |")
                                        :rows [["ITEM A" "1,000x" "0,49" "0,49E"]
                                               ["ITEM B" "1,000x" "0,50" "0,50E"]]
                                        :bbox [{:confidence 0.40}]}
                                       {:type "table"
                                        :md "| TOTAL: | 1,02 |"
                                        :rows [["TOTAL:" "1,02"]]
                                        :bbox [{:confidence 0.41}]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        provider-confidence (:provider_confidence extraction)]
    (is (= 1.02M (bigdec (get-in extraction [:totals :total]))))
    (is (= 0.99M (bigdec (:items_total provider-confidence))))
    (is (= 1.0 (:line_total_reliability provider-confidence)))
    (is (nil? (:reconciliation_basis provider-confidence)))))

(deftest receipt-extraction-fallback-prefers-text-before-combined-to-avoid-duplicates
  (let [resp {:text {:pages [{:text "CORTIX\n04.02.2026. 19:41\n357 35924792904 ZENSKA PIDZAMA\n24,00E\nTOTAL: 24,00"}]}
              :items {:pages [{:items [{:type "header"
                                        :md "357_35924792904_ZENSKA_PIDZAMA 24,00E"}]}]}}
        ext (receipt-extract/response->extraction resp)
        items (:items ext)]
    (is (= 1 (count items)))
    (is (= "357 35924792904 ZENSKA PIDZAMA" (-> items first :raw_label)))
    (is (= 24.00M (-> items first :line_total)))
    (is (= 24.00M (-> ext :totals :total)))))

(ns app.domain.backend.expenses.integrations.llamaparse.item-extraction-test
  (:require
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction :as receipt-extract]
    [clojure.test :refer [deftest is]]))

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

(deftest receipt-extraction-ignores-cyrillic-tax-summary-table
  (let [resp {:items {:pages [{:items [{:type "text"
                                        :value (str "ФИСКАЛНИ РАЧУН\n"
                                                 "PEPCO B-H DOO Подружница Сарајево 4\n"
                                                 "Спасовданска 20\n"
                                                 "Ist. Novo Sarajevo\n")}
                                       {:type "table"
                                        :rows [["Назив" "Цијена" "Кол." "Укупно"]
                                               ["2200663305162 63305105 Mirisna svijeca u staklu Premium Collec (E)" "10,00" "1" "10,00"]
                                               ["2217931679665 31679635 Svijeca \"silver & gold\" s poklopcem 13.5 (E)" "10,00" "1" "10,00"]
                                               ["2200833176462 33176405 jaja 6cm, 12 kom_ONE_Dark beige (E)" "2,50" "1" "2,50"]]}
                                       {:type "table"
                                        :rows [["Ознака" "Назив" "Стопа" "Порез"]
                                               ["E" "ПДВ" "17%" "3,27"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        items (:items extraction)
        egg-item (nth items 2)]
    (is (= 3 (count items)))
    (is (= "PEPCO B-H" (get-in extraction [:merchant :name])))
    (is (= 22.50M (bigdec (get-in extraction [:totals :total]))))
    (is (= [10.00M 10.00M 2.50M]
          (mapv (comp bigdec :line_total) items)))
    (is (= "33176405 jaja 6cm, 12 kom ONE Dark beige (E)" (:raw_label egg-item)))
    (is (= 2.50M (bigdec (:unit_price egg-item))))
    (is (= 2.50M (bigdec (:line_total egg-item))))
    (is (every? (fn [{:keys [unit_price line_total]}]
                  (and (not (neg? (bigdec unit_price)))
                    (not (neg? (bigdec line_total)))))
          items))))

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


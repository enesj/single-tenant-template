(ns app.domain.backend.expenses.integrations.llamaparse.fallback-reconciliation-test
  (:require
    [app.domain.backend.expenses.integrations.llamaparse :as llamaparse]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction :as receipt-extract]
    [clojure.string :as str]
    [clojure.test :refer [deftest is]]))

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

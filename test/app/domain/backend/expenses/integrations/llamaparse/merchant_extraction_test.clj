(ns app.domain.backend.expenses.integrations.llamaparse.merchant-extraction-test
  (:require
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction :as receipt-extract]
    [clojure.string :as str]
    [clojure.test :refer [deftest is]]))

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

(deftest receipt-extraction-ignores-cyrillic-cashier-line-after-merchant-text
  (let [resp {:text {:pages [{:text (str "PEPCO B-H DOO\n"
                                      "Podružnica Sarajevo 4\n"
                                      "Spasovdanska 20\n"
                                      "Ist. Novo Sarajevo\n"
                                      "Касир: Alma Halilovic\n")}]}
              :items {:pages [{:items [{:type "text"
                                        :value (str "FISKALNI RAČUN\n"
                                                 "PEPCO B-H DOO\n"
                                                 "Podružnica Sarajevo 4\n"
                                                 "Spasovdanska 20\n"
                                                 "Ist. Novo Sarajevo")}
                                       {:type "text"
                                        :value (str "Касир: Alma Halilovic\n"
                                                 "ЕСИР број: 77/4.0")}
                                       {:type "table"
                                        :rows [["ITEM" "22,50E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        receipt (receipt-extract/response->receipt resp)
        merchant (:merchant extraction)]
    (is (= "PEPCO B-H" (:name merchant)))
    (is (= "Podružnica Sarajevo 4" (:store_name merchant)))
    (is (not= "Касир: Alma Halilovic" (:name merchant)))
    (is (= "PEPCO B-H" (first (str/split-lines (:parsed-markdown receipt)))))))

(deftest receipt-extraction-parses-merged-merchant-line-with-cyrillic-branch-context
  (let [resp {:items {:pages [{:items [{:type "text"
                                        :value (str "FISKALNI RAČUN\n"
                                                 "4203144510138\n"
                                                 "PEPCO B-H DOO Подружница Сарајево 4\n"
                                                 "10884203144510001 PEPCO B-H DOO\n"
                                                 "Подружница Сарајево 4\n"
                                                 "Спасовданска 20\n"
                                                 "Ist. Novo Sarajevo")}
                                       {:type "text"
                                        :value (str "Касир: Alma Halilovic\n"
                                                 "ЕСИР број: 77/4.0")}
                                       {:type "table"
                                        :rows [["ITEM" "22,50E"]]}]}]}}
        extraction (receipt-extract/response->extraction resp)
        receipt (receipt-extract/response->receipt resp)
        merchant (:merchant extraction)]
    (is (= "PEPCO B-H" (:name merchant)))
    (is (= "Подружница Сарајево 4" (:store_name merchant)))
    (is (= "Спасовданска 20, Ist. Novo Sarajevo" (:address merchant)))
    (is (= "Подружница Сарајево 4, Спасовданска 20, Ist. Novo Sarajevo"
          (:raw_address merchant)))
    (is (= "PEPCO B-H" (first (str/split-lines (:parsed-markdown receipt)))))))

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


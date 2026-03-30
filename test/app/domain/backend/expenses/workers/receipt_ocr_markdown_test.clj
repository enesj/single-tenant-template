(ns app.domain.backend.expenses.workers.receipt-ocr-markdown-test
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.header :as header]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.items :as items]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.totals :as totals]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]))

(deftest markdown-line-item-candidates-supports-qty-lines
  (let [candidates items/candidates
        markdown (str "020327 HLJEB 400G SA SJEMELKA MA\n"
                   "1.000x 2,10 2.10E\n"
                   "B31508 PASTETA 114G KOKOSTJA ARGETA\n"
                   "1.000x 1,85 1.85E\n")
        items (candidates markdown)]
    (is (= 2 (count items)))
    (is (= {:raw_label "HLJEB 400G SA SJEMELKA MA"
            :qty 1.000M
            :unit_price 2.10M
            :line_total 2.10M}
          (first items)))
    (is (= {:raw_label "PASTETA 114G KOKOSTJA ARGETA"
            :qty 1.000M
            :unit_price 1.85M
            :line_total 1.85M}
          (second items)))))

(deftest markdown-line-item-candidates-supports-label-plus-price
  (let [candidates items/candidates
        markdown (str "A10150772 Snala za kosu BH231226\n"
                   "1,95E\n"
                   "VOLTAREN RETARD TABLETE 100 MG A 2\n"
                   "0 SA P 172e 5,85E\n"
                   "ANDOL TABLETE 300 MG A 20 5673\n"
                   "5,70E\n")
        items (candidates markdown)]
    (is (= 3 (count items)))
    (is (= {:raw_label "Snala za kosu BH231226"
            :qty 1M
            :unit_price 1.95M
            :line_total 1.95M}
          (first items)))
    (is (= {:raw_label "VOLTAREN RETARD TABLETE 100 MG A 2 0 SA P 172e"
            :qty 1M
            :unit_price 5.85M
            :line_total 5.85M}
          (second items)))
    (is (= {:raw_label "ANDOL TABLETE 300 MG A 20 5673"
            :qty 1M
            :unit_price 5.70M
            :line_total 5.70M}
          (nth items 2)))))

(deftest markdown-line-item-candidates-supports-price-with-vat-letter-suffix
  (testing "BA receipts with VAT category suffix (e.g. 2,00A)"
    (let [candidates items/candidates
          ;; Real-world example from caffe bar receipts in Bosnia:
          ;; Lines end with X,XXA where A is the VAT category letter.
          markdown (str "FISKALNI RAČUN\n"
                     "ESPRESSO KAFA/co 2,00A\n"
                     "CAJ/co 2,50A\n")
          items (candidates markdown)]
      (is (= 2 (count items)) "Should parse two line items")
      ;; The parser may prepend non-money-prefix lines to the first item label.
      ;; Focus on the key behavior: parsing the price correctly despite the A suffix.
      (is (= 2.00M (:line_total (first items))))
      (is (= 2.50M (:line_total (second items))))
      (is (str/includes? (:raw_label (first items)) "ESPRESSO"))
      (is (str/includes? (:raw_label (second items)) "CAJ")))))

(deftest markdown-line-item-candidates-supports-mixed-qty-and-inline-price
  (let [candidates items/candidates
        markdown (str "TUBORG 0,33 NEPOVRATNI/KO\n"
                   "24,000x 1,55 37,20E\n"
                   "SCHWEPPES TONIC 1L/KO\n"
                   "3,000x 2,00 6,00E\n"
                   "BULLDOG GIN SA CASOM 0,7/KO 42,00E\n")
        items (candidates markdown)]
    (is (= 3 (count items)))
    (is (= {:raw_label "TUBORG 0,33 NEPOVRATNI/KO"
            :qty 24.000M
            :unit_price 1.55M
            :line_total 37.20M}
          (first items)))
    (is (= {:raw_label "SCHWEPPES TONIC 1L/KO"
            :qty 3.000M
            :unit_price 2.00M
            :line_total 6.00M}
          (second items)))
    (is (= {:raw_label "BULLDOG GIN SA CASOM 0,7/KO"
            :qty 1M
            :unit_price 42.00M
            :line_total 42.00M}
          (nth items 2)))))

(deftest markdown-line-item-candidates-does-not-treat-dimensions-as-qty
  (let [candidates items/candidates
        markdown "60963601 Torba papirna velika 32 x 16 x 45 - bez /pc 0,70E\n"
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= {:raw_label "Torba papirna velika 32 x 16 x 45 - bez /pc"
            :qty 1M
            :unit_price 0.70M
            :line_total 0.70M}
          (first items)))))

(deftest markdown-line-item-candidates-applies-discounts
  (let [candidates items/candidates
        markdown (str "62778401 Mirisna svijeca u staklu Premium Collec\n"
                   "t/pc 10,00E\n"
                   "-50,00%: 5,00\n")
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= {:raw_label "Mirisna svijeca u staklu Premium Collec"
            :qty 1M
            :unit_price 5.00M
            :line_total 5M}
          (first items)))))

(deftest markdown-line-item-candidates-applies-discounts-in-pipe-table
  (let [candidates items/candidates
        markdown (str "|  ITEM A | 1,000x | 10,00 | 10,00E  |\n"
                   "| --- | --- | --- | --- |\n"
                   "|  POPUST | -10,00% |  | 9,00  |\n"
                   "|  ITEM B | 1,000x | 5,00 | 5,00E  |\n")
        items (candidates markdown)]
    (is (= 2 (count items)))
    (is (= {:raw_label "ITEM A"
            :qty 1.000M
            :unit_price 9.00M
            :line_total 9.00M}
          (first items)))
    (is (= {:raw_label "ITEM B"
            :qty 1.000M
            :unit_price 5.00M
            :line_total 5.00M}
          (second items)))))

(deftest markdown-line-item-candidates-ignores-tax-like-lines
  (let [candidates items/candidates
        markdown (str "ITEM\n"
                   "1,000x 1,00 1,00E\n"
                   "PDU E: 7,25\n"
                   "PDU: 7,25\n")
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= "ITEM" (:raw_label (first items))))))

(deftest markdown-line-item-candidates-ignores-payment-summary-lines
  (let [candidates items/candidates
        markdown (str "POVRCE MIX\n"
                   "1,00E\n"
                   "POV E: 0,00\n"
                   "POV: 0,00\n"
                   "CEK: 1,00\n"
                   "CEKIC\n"
                   "10,00E\n"
                   "UMLAČENO: KORTICA: 11,00\n")
        items (candidates markdown)]
    (is (= 2 (count items)))
    (is (= "POVRCE MIX" (:raw_label (first items))))
    (is (= "CEKIC" (:raw_label (second items))))))

(deftest markdown-line-item-candidates-supports-markdown-table-rows
  (let [candidates items/candidates
        markdown (str "|  Mivolis flasteri za djecu |  |   |\n"
                   "| --- | --- | --- |\n"
                   "|  1,000x | 1,85 | 1,85E  |\n")
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= {:raw_label "Mivolis flasteri za djecu"
            :qty 1.000M
            :unit_price 1.85M
            :line_total 1.85M}
          (first items)))))

(deftest markdown-line-item-candidates-supports-table-total-in-label-row
  (let [candidates items/candidates
        markdown (str "|  E09438 | BOMBONJERA 230G RAFFAELLO FER | 9,90E  |\n"
                   "| --- | --- | --- |\n"
                   "|  1,000x | 9,90 |   |\n")
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= {:raw_label "BOMBONJERA 230G RAFFAELLO FER"
            :qty 1.000M
            :unit_price 9.90M
            :line_total 9.90M}
          (first items)))))

(deftest markdown-line-item-candidates-supports-html-table-cells
  (let [candidates items/candidates
        markdown (str "<table>\n"
                   "  <tbody>\n"
                   "    <tr>\n"
                   "      <td>CHYMORAL_S_KAPSULE_A_10_7152</td>\n"
                   "      <td>21,45E</td>\n"
                   "    </tr>\n"
                   "    <tr>\n"
                   "      <td>HERBIKO_NATURAL_BOKVICA_SIRUP_125_ML_5bad</td>\n"
                   "      <td>9,20E</td>\n"
                   "    </tr>\n"
                   "  </tbody>\n"
                   "</table>\n")
        items (candidates markdown)]
    (is (= 2 (count items)))
    (is (= {:raw_label "CHYMORAL_S_KAPSULE_A_10_7152"
            :qty 1M
            :unit_price 21.45M
            :line_total 21.45M}
          (first items)))
    (is (= {:raw_label "HERBIKO_NATURAL_BOKVICA_SIRUP_125_ML_5bad"
            :qty 1M
            :unit_price 9.20M
            :line_total 9.20M}
          (second items)))))

(deftest markdown-merchant-header-extracts-quoted-name
  (let [parse-header header/merchant-header
        markdown (str "\"Pepco B-H\" d.o.o.\n"
                   "Podružnica Sarajevo 2\n"
                   "ul. Kolodvorska br.12\n"
                   "71000 Sarajevo\n"
                   "\n"
                   "JIB: 4203144510090\n"
                   "PIB: 203144510006\n")
        result (parse-header markdown)]
    (is (= "Pepco B-H" (:merchant_name result)))
    (is (= "Podružnica Sarajevo 2" (:store_name result)))
    (is (= "ul. Kolodvorska br.12, 71000 Sarajevo" (:address result)))))

(deftest markdown-merchant-header-extracts-unquoted-name
  (let [parse-header header/merchant-header
        markdown (str "KONZUM d.o.o.\n"
                   "Poslovnica Tuzla 5\n"
                   "Trg slobode 10\n"
                   "75000 Tuzla\n"
                   "JIB: 123456789\n")
        result (parse-header markdown)]
    (is (= "KONZUM" (:merchant_name result)))
    (is (= "Poslovnica Tuzla 5" (:store_name result)))
    (is (= "Trg slobode 10, 75000 Tuzla" (:address result)))))

(deftest markdown-merchant-header-handles-minimal-header
  (let [parse-header header/merchant-header
        markdown (str "BINGO d.d.\n"
                   "TC Mercator\n"
                   "JIB: 999\n")
        result (parse-header markdown)]
    (is (= "BINGO" (:merchant_name result)))
    (is (= "TC Mercator" (:store_name result)))
    (is (nil? (:address result)))))

(deftest markdown-merchant-header-handles-no-store-name
  (let [parse-header header/merchant-header
        markdown (str "\"DM\" d.o.o.\n"
                   "ul. Marsala Tita 25\n"
                   "71000 Sarajevo\n"
                   "JIB: 111\n")
        result (parse-header markdown)]
    (is (= "DM" (:merchant_name result)))
    (is (= "ul. Marsala Tita 25, 71000 Sarajevo" (:address result)))))

(deftest markdown-supplier-guess-ignores-date-time-and-table-lines
  (let [markdown (str "# FISKALNI RAČUN\n"
                   "BF: 238900\n"
                   "13.02.2026. 17:36\n"
                   "<table>\n"
                   "  <tr><td>CHYMORAL_S_KAPSULE_A_10_7152</td><td>21,45E</td></tr>\n"
                   "</table>\n")]
    (is (nil? (header/supplier-guess markdown)))))

(deftest markdown-supplier-guess-keeps-valid-merchant-before-table
  (let [markdown (str "LUPRIV PLUS Mostar\n"
                   "13.02.2026. 17:36\n"
                   "<table>\n"
                   "  <tr><td>ITEM</td><td>1,00E</td></tr>\n"
                   "</table>\n")]
    (is (= "LUPRIV PLUS Mostar" (header/supplier-guess markdown)))))

(deftest markdown-supplier-guess-skips-role-labeled-person-lines
  (let [markdown (str "Kasir: Alma Halilovic\n"
                   "\"Pepco B-H\" d.o.o.\n"
                   "13.02.2026. 17:36\n"
                   "<table>\n"
                   "  <tr><td>ITEM</td><td>1,00E</td></tr>\n"
                   "</table>\n")]
    (is (= "\"Pepco B-H\" d.o.o." (header/supplier-guess markdown)))))

(deftest markdown-purchased-at-extracts-ba-datetime-format
  (testing "parses dd.mm.yyyy. hh:mm with trailing dot"
    (let [markdown "UR CAFFE BAR\n29.01.2026. 14:31\nESPRESSO KAFA 2,00"]
      (is (= "2026-01-29T14:31:00" (totals/purchased-at markdown)))))
  (testing "parses dd.mm.yyyy hh:mm without trailing dot"
    (let [markdown "BINGO\n21.01.2026 17:25\nItem 5,00"]
      (is (= "2026-01-21T17:25:00" (totals/purchased-at markdown)))))
  (testing "parses date-only format dd.mm.yyyy"
    (let [markdown "MERCHANT\n15.03.2026\nItem 10,00"]
      (is (= "2026-03-15" (totals/purchased-at markdown)))))
  (testing "returns nil when no date found"
    (is (nil? (totals/purchased-at "No date here")))
    (is (nil? (totals/purchased-at nil)))))

(deftest markdown-total-amount-prefers-total-over-trailing-ukupno-zero
  (let [markdown (str "TOTAL: 19,50\n"
                   "UPLACENO: 19,50\n"
                   "GOTOVINA: 19,50\n"
                   "UKUPNO: 0,00\n"
                   "POVRAT: 0,00\n")]
    (is (= 19.50M (totals/total-amount markdown)))))

(deftest markdown-total-amount-falls-back-to-ukupno-when-total-missing
  (let [markdown (str "UKUPNO: 42,00\n"
                   "POVRAT: 0,00\n")]
    (is (= 42.00M (totals/total-amount markdown)))))

(deftest markdown-total-amount-supports-heading-prefixed-total
  (let [markdown (str "## TOTAL: 30,70\n"
                   "UKUPNO: 0,00\n")]
    (is (= 30.70M (totals/total-amount markdown)))))

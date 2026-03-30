(ns app.domain.backend.expenses.workers.receipt-ocr-extraction-units-test
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.units :as units]
    [clojure.test :refer [deftest is testing]]))

;; ---------------------------------------------------------------------------
;; parse-unit-suffix
;; ---------------------------------------------------------------------------

(deftest parse-unit-suffix-detects-known-suffixes
  (testing "/KO → kom"
    (is (= {:base-label "TUBORG 0,33 NEPOVRATNI" :unit "kom"}
          (units/parse-unit-suffix "TUBORG 0,33 NEPOVRATNI/KO"))))

  (testing "/KOM → kom"
    (is (= {:base-label "PAPIR ZA PAKOVANJE" :unit "kom"}
          (units/parse-unit-suffix "PAPIR ZA PAKOVANJE/KOM"))))

  (testing "/KG → kg"
    (is (= {:base-label "Narandza grcka rinf" :unit "kg"}
          (units/parse-unit-suffix "Narandza grcka rinf /KG"))))

  (testing "/pc → kom"
    (is (= {:base-label "Torba papirna velika 32 x 16 x 45 - bez" :unit "kom"}
          (units/parse-unit-suffix "Torba papirna velika 32 x 16 x 45 - bez /pc"))))

  (testing "/GR → g"
    (is (= {:base-label "Kocka kokosija 40" :unit "g"}
          (units/parse-unit-suffix "Kocka kokosija 40/GR"))))

  (testing "/LIT → l"
    (is (= {:base-label "SCHWEPPES TONIC 1" :unit "l"}
          (units/parse-unit-suffix "SCHWEPPES TONIC 1/LIT"))))

  (testing "/PAK → pak"
    (is (= {:base-label "MLIJEKO 6x1L" :unit "pak"}
          (units/parse-unit-suffix "MLIJEKO 6x1L/PAK")))))

(deftest parse-unit-suffix-detects-single-letter-liter-suffix
  (testing "/L -> l"
    (is (= {:base-label "PREMIUM 95 BAS EN 228" :unit "l"}
          (units/parse-unit-suffix "PREMIUM 95 BAS EN 228/L"))))

  (testing "/L with punctuation in the base label -> l"
    (is (= {:base-label "TAKSA NAF.DER.ČL.25S.GPDV" :unit "l"}
          (units/parse-unit-suffix "TAKSA NAF.DER.ČL.25S.GPDV/L")))))

(deftest parse-unit-suffix-detects-piece-ocr-variant
  (testing "/co -> kom"
    (is (= {:base-label "ESPRESSO KAFA" :unit "kom"}
          (units/parse-unit-suffix "ESPRESSO KAFA/co"))))

  (testing "Unicode labels still strip /co"
    (is (= {:base-label "ČAJ" :unit "kom"}
          (units/parse-unit-suffix "ČAJ/co")))))

(deftest strip-package-count-suffix-removes-trailing-pack-count-noise
  (is (= "SETH CAJ MENTA 30GR"
        (#'units/strip-package-count-suffix "SETH CAJ MENTA 30GR 24/1")))
  (is (= "MLIJEKO 2.8%"
        (#'units/strip-package-count-suffix "MLIJEKO 2.8% 12 / 1")))
  (is (nil? (#'units/strip-package-count-suffix "ESPRESSO KAFA/co"))))

(deftest parse-unit-suffix-handles-tax-marker
  (testing "/KO (E) → strips tax marker"
    (is (= {:base-label "So turisticka 250g T" :unit "kom"}
          (units/parse-unit-suffix "So turisticka 250g T/KO (E)"))))

  (testing "/KO (A) → strips tax marker"
    (is (= {:base-label "KAPSULE INTENSO 112g" :unit "kom"}
          (units/parse-unit-suffix "KAPSULE INTENSO 112g/KO (A)")))))

(deftest parse-unit-suffix-returns-nil-for-no-suffix
  (is (nil? (units/parse-unit-suffix "HLJEB 400G SA SJEMELKA MA")))
  (is (nil? (units/parse-unit-suffix "ESPRESSO KAFA")))
  (is (nil? (units/parse-unit-suffix nil)))
  (is (nil? (units/parse-unit-suffix "")))
  (is (nil? (units/parse-unit-suffix "   "))))

(deftest parse-unit-suffix-case-insensitive
  (is (= "kom" (:unit (units/parse-unit-suffix "ITEM/ko"))))
  (is (= "kom" (:unit (units/parse-unit-suffix "ITEM/KO"))))
  (is (= "kg" (:unit (units/parse-unit-suffix "ITEM/Kg"))))
  (is (= "g" (:unit (units/parse-unit-suffix "ITEM/Gr")))))

;; ---------------------------------------------------------------------------
;; extract-unit (with qty guard)
;; ---------------------------------------------------------------------------

(deftest extract-unit-strips-suffix-with-integer-qty
  (testing "Integer qty + /KO → strips suffix, unit = kom"
    (is (= {:base-label "TUBORG 0,33 NEPOVRATNI" :unit "kom"}
          (units/extract-unit "TUBORG 0,33 NEPOVRATNI/KO" 24))))

  (testing "Integer qty + /KOM → strips suffix, unit = kom"
    (is (= {:base-label "Espresso kafa" :unit "kom"}
          (units/extract-unit "Espresso kafa/kom" 1.000M)))))

(deftest extract-unit-fractional-qty-with-piece-suffix-assumes-kg
  (testing "Fractional qty + /KO → strip suffix, assume kg"
    (is (= {:base-label "ITEM" :unit "kg"}
          (units/extract-unit "ITEM/KO" 0.350M))))

  (testing "Fractional qty + /KOM → strip suffix, assume kg"
    (is (= {:base-label "ITEM" :unit "kg"}
          (units/extract-unit "ITEM/KOM" 1.5)))))

(deftest extract-unit-always-trusts-non-piece-units
  (testing "Fractional qty + /KG → trusts kg"
    (is (= {:base-label "Narandza grcka rinf" :unit "kg"}
          (units/extract-unit "Narandza grcka rinf /KG" 0.350M))))

  (testing "Integer qty + /KG → trusts kg"
    (is (= {:base-label "Suhomesnato 1" :unit "kg"}
          (units/extract-unit "Suhomesnato 1/KG" 2))))

  (testing "Fractional qty + /GR → trusts g"
    (is (= {:base-label "Biber 50" :unit "g"}
          (units/extract-unit "Biber 50/GR" 0.5M)))))

(deftest extract-unit-detects-single-letter-liter-suffix
  (testing "Fractional qty + /L -> trusts liters"
    (is (= {:base-label "PREMIUM 95 BAS EN 228" :unit "l"}
          (units/extract-unit "PREMIUM 95 BAS EN 228/L" 12.340M))))

  (testing "Integer qty + /L -> trusts liters"
    (is (= {:base-label "TAKSA NAF.DER.ČL.25S.GPDV" :unit "l"}
          (units/extract-unit "TAKSA NAF.DER.ČL.25S.GPDV/L" 1M)))))

(deftest extract-unit-detects-piece-ocr-variant-and-pack-count-noise
  (testing "Integer qty + /co -> strips suffix, unit = kom"
    (is (= {:base-label "ESPRESSO KAFA" :unit "kom"}
          (units/extract-unit "ESPRESSO KAFA/co" 1M))))

  (testing "Unicode label + /co -> strips suffix, unit = kom"
    (is (= {:base-label "ČAJ" :unit "kom"}
          (units/extract-unit "ČAJ/co" 1M))))

  (testing "Trailing 24/1 pack count -> strips metadata, unit inferred from qty"
    (is (= {:base-label "SETH CAJ MENTA 30GR" :unit "kom"}
          (units/extract-unit "SETH CAJ MENTA 30GR 24/1" 1M)))))

(deftest extract-unit-defaults-with-no-suffix
  (testing "No suffix + integer qty → default kom"
    (is (= {:base-label "HLJEB 400G SA SJEMELKA MA" :unit "kom"}
          (units/extract-unit "HLJEB 400G SA SJEMELKA MA" 1))))

  (testing "No suffix + nil qty → default kom"
    (is (= {:base-label "ESPRESSO KAFA" :unit "kom"}
          (units/extract-unit "ESPRESSO KAFA" nil))))

  (testing "No suffix + fractional qty → assume kg"
    (is (= {:base-label "JABUKE" :unit "kg"}
          (units/extract-unit "JABUKE" 0.750M)))))

;; ---------------------------------------------------------------------------
;; process-item-unit
;; ---------------------------------------------------------------------------

(deftest process-item-unit-adds-unit-and-strips-label
  (testing "Item with /KG and fractional qty"
    (let [item {:raw_label "Narandza grcka rinf /KG"
                :qty 0.350M
                :unit_price 3.00M
                :line_total 1.05M}
          result (units/process-item-unit item)]
      (is (= "Narandza grcka rinf" (:raw_label result)))
      (is (= "kg" (:unit result)))))

  (testing "Item with /KO and integer qty"
    (let [item {:raw_label "SCHWEPPES TONIC 1L/KO"
                :qty 3.000M
                :unit_price 2.00M
                :line_total 6.00M}
          result (units/process-item-unit item)]
      (is (= "SCHWEPPES TONIC 1L" (:raw_label result)))
      (is (= "kom" (:unit result)))))

  (testing "Item with no suffix and integer qty"
    (let [item {:raw_label "ESPRESSO KAFA"
                :qty 1M
                :unit_price 2.00M
                :line_total 2.00M}
          result (units/process-item-unit item)]
      (is (= "ESPRESSO KAFA" (:raw_label result)))
      (is (= "kom" (:unit result)))))

  (testing "Item with /KOM but fractional qty → strip suffix, assume kg"
    (let [item {:raw_label "ITEM/KOM"
                :qty 0.5M
                :unit_price 10.00M
                :line_total 5.00M}
          result (units/process-item-unit item)]
      (is (= "ITEM" (:raw_label result)))
      (is (= "kg" (:unit result))))))

(deftest process-item-unit-detects-single-letter-liter-suffix
  (let [item {:raw_label "PREMIUM 95 BAS EN 228/L"
              :qty 12.340M
              :unit_price 2.50M
              :line_total 30.85M}
        result (units/process-item-unit item)]
    (is (= "PREMIUM 95 BAS EN 228" (:raw_label result)))
    (is (= "l" (:unit result)))))

(deftest process-item-unit-strips-piece-ocr-variant-and-pack-count-noise
  (let [coffee-result (units/process-item-unit {:raw_label "ESPRESSO KAFA/co"
                                                :qty 1M
                                                :unit_price 2.50M
                                                :line_total 2.50M})
        tea-result (units/process-item-unit {:raw_label "ČAJ/co"
                                             :qty 1M
                                             :unit_price 3.00M
                                             :line_total 3.00M})
        pack-result (units/process-item-unit {:raw_label "SETH CAJ MENTA 30GR 24/1"
                                              :qty 1M
                                              :unit_price 4.00M
                                              :line_total 4.00M})]
    (is (= "ESPRESSO KAFA" (:raw_label coffee-result)))
    (is (= "kom" (:unit coffee-result)))
    (is (= "ČAJ" (:raw_label tea-result)))
    (is (= "kom" (:unit tea-result)))
    (is (= "SETH CAJ MENTA 30GR" (:raw_label pack-result)))
    (is (= "kom" (:unit pack-result)))))

;; ---------------------------------------------------------------------------
;; process-items-units (batch)
;; ---------------------------------------------------------------------------

(deftest process-items-units-processes-all-items
  (let [items [{:raw_label "TUBORG 0,33 NEPOVRATNI/KO"
                :qty 24.000M
                :unit_price 1.55M
                :line_total 37.20M}
               {:raw_label "Narandza grcka rinf /KG"
                :qty 0.350M
                :unit_price 3.00M
                :line_total 1.05M}
               {:raw_label "ESPRESSO KAFA"
                :qty 1M
                :unit_price 2.00M
                :line_total 2.00M}]
        results (units/process-items-units items)]
    (is (= 3 (count results)))

    (is (= "TUBORG 0,33 NEPOVRATNI" (:raw_label (nth results 0))))
    (is (= "kom" (:unit (nth results 0))))

    (is (= "Narandza grcka rinf" (:raw_label (nth results 1))))
    (is (= "kg" (:unit (nth results 1))))

    (is (= "ESPRESSO KAFA" (:raw_label (nth results 2))))
    (is (= "kom" (:unit (nth results 2))))))

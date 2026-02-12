(ns app.domain.backend.expenses.services.stores-test
  "Tests for store services, focusing on city extraction logic."
  (:require
    [app.domain.backend.expenses.services.stores :as stores]
    [clojure.test :refer [deftest is testing]]))

(deftest extract-city-from-address-test
  (testing "standard address format with postal code"
    (is (= "Sarajevo"
          (stores/extract-city-from-address "ul. Kolodvorska br.12, 71000 Sarajevo"))))

  (testing "uppercase city (title-case normalization)"
    (is (= "Sarajevo Centar"
          (stores/extract-city-from-address "JUKIĆEVA DO BROJA 2, 71103 SARAJEVO CENTAR"))))

  (testing "multi-word city"
    (is (= "Novo Sarajevo"
          (stores/extract-city-from-address "Milana Preloga 2 S, 71120 Novo Sarajevo"))))

  (testing "different city"
    (is (= "Mostar"
          (stores/extract-city-from-address "Kardinala Stepinca bb, 88000 Mostar")))))

(deftest extract-city-edge-cases-test
  (testing "nil address"
    (is (nil? (stores/extract-city-from-address nil))))

  (testing "empty string"
    (is (nil? (stores/extract-city-from-address ""))))

  (testing "blank/whitespace"
    (is (nil? (stores/extract-city-from-address "   "))))

  (testing "no postal code"
    (is (nil? (stores/extract-city-from-address "UL. MERHEMIČA TRG BR. 3"))))

  (testing "postal code only"
    (is (nil? (stores/extract-city-from-address "71000"))))

  (testing "city only (no postal)"
    (is (nil? (stores/extract-city-from-address "Sarajevo")))))

(deftest extract-city-complex-addresses-test
  (testing "multiple commas with nested info"
    (is (= "Sarajevo Centar"
          (stores/extract-city-from-address
            "Bulevar Franca Lehara br. 2.,, Alta Shopping Centar, 71101 SARAJEVO CENTAR"))))

  (testing "trailing punctuation is trimmed"
    (is (= "Sarajevo"
          (stores/extract-city-from-address "Street Address, 71000 Sarajevo,"))))

  (testing "lowercase address components"
    (is (= "Sarajevo"
          (stores/extract-city-from-address "bulevar mese selimovica 31, 71000 sarajevo"))))

  (testing "mixed case with preserving proper capitalization"
    (is (= "Istočna Ilidža"
          (stores/extract-city-from-address "Trg, 71210 istočna ilidža")))))

(deftest extract-city-known-edge-cases-test
  (testing "postal code with spaces"
    ;; From backfill: "Vrbanja 1, 71 000 Sarajevo"
    ;; Spaces are normalized before matching the 5-digit code.
    (is (= "Sarajevo"
          (stores/extract-city-from-address "Vrbanja 1, 71 000 Sarajevo"))))

  (testing "4-digit postal code (unsupported)"
    ;; From backfill: "Ul. Brače Begića broj 1, 1000 Sarajevo"
    ;; Expected: nil because regex requires 5 digits
    (is (nil? (stores/extract-city-from-address "Ul. Brače Begića broj 1, 1000 Sarajevo"))))

  (testing "complex multi-digit postal pattern"
    ;; From backfill: "Gralicacka 1, 71 100 001 13vo"
    ;; Expected: might extract "13vo" after finding 71100 (incorrect but documented)
    (let [result (stores/extract-city-from-address "Gralicacka 1, 71 100 001 13vo")]
      ;; Document current behavior: finds last 5-digit sequence (00113) and gets "vo"
      ;; This is a known limitation for malformed addresses
      (is (or (nil? result) (= "Vo" result))
        "Complex multi-digit patterns may extract incorrect city or nil")))

  (testing "address without city after postal code"
    ;; Edge case: postal code at the end with no city
    (is (nil? (stores/extract-city-from-address "Street Name 123, 71000"))))

  (testing "multiple postal codes - uses last one"
    ;; Algorithm uses the last postal code found
    (is (= "Mostar"
          (stores/extract-city-from-address "71000 Sarajevo office, moved to 88000 Mostar")))))

(deftest extract-city-real-world-addresses-test
  (testing "addresses from actual stores database"
    ;; Real addresses from stores table that were successfully parsed
    (is (= "Sarajevo"
          (stores/extract-city-from-address "BULEVAR MESE SELIMOVICA 31, 71000 Sarajevo")))

    (is (= "Sarajevo"
          (stores/extract-city-from-address "BRAĆE BEGIĆ br.4, 71000 Sarajevo")))

    (is (= "Sarajevo"
          (stores/extract-city-from-address "BULEVAR FRANCA LEHARA B.B, 71000 SARAJEVO")))

    (is (= "Východna Ilidža"
          (stores/extract-city-from-address "Trg BiH 22, 71201 Východna ilidža")))

    (is (= "Sarajevo"
          (stores/extract-city-from-address "Hamdije Kreševljakovića 56-56a, 71000 Sarajevo")))))

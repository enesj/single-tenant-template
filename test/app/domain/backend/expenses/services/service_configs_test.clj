(ns app.domain.backend.expenses.services.service-configs-test
  (:require
    [app.domain.backend.expenses.services.service-configs :as service-configs]
    [clojure.test :refer [deftest is testing]]))

(deftest normalization-unescapes-html-entities-test
  (testing "normalize-supplier-key unescapes common HTML entities"
    (is (= "bk"
          (service-configs/normalize-supplier-key "B&K d.o.o. Sarajevo")))
    (is (= "bk"
          (service-configs/normalize-supplier-key "B&amp;K d.o.o. Sarajevo")))
    (is (= (service-configs/normalize-supplier-key "B&K d.o.o. Sarajevo")
          (service-configs/normalize-supplier-key "B&amp;K d.o.o. Sarajevo")))
    (is (= "bk"
          (service-configs/normalize-supplier-key "B&#38;K d.o.o. Sarajevo")))
    (is (= "bk"
          (service-configs/normalize-supplier-key "B&#x26;K d.o.o. Sarajevo"))))

  (testing "normalize-store-key unescapes common HTML entities"
    (is (= "bk-market"
          (service-configs/normalize-store-key "B&K Market")))
    (is (= "bk-market"
          (service-configs/normalize-store-key "B&amp;K Market")))
    (is (= (service-configs/normalize-store-key "B&K Market")
          (service-configs/normalize-store-key "B&amp;K Market")))))

(deftest normalize-store-key-canonicalizes-street-number-abbreviations-test
  (testing "store keys treat compact and spaced street-number abbreviations as the same store"
    (is (= (service-configs/normalize-store-key "APOTEKA \"KOŠEVSKO BRDO\" BRAĆE BEGIĆ br. 4, 71000 Sarajevo")
          (service-configs/normalize-store-key "APOTEKA \"KOŠEVSKO BRDO\" BRAĆE BEGIĆ br.4, 71000 Sarajevo")))
    (is (= (service-configs/normalize-store-key "APOTEKA \"KOŠEVSKO BRDO\" BRAĆE BEGIĆ broj 4, 71000 Sarajevo")
          (service-configs/normalize-store-key "APOTEKA \"KOŠEVSKO BRDO\" BRAĆE BEGIĆ broj4, 71000 Sarajevo")))))

(deftest unescape-html-entities-is-bounded-test
  (testing "double-encoding is handled with bounded repeats"
    (is (= "bk"
          (service-configs/normalize-supplier-key "B&amp;amp;K d.o.o.")))))

(deftest city-config-present-test
  (testing "city config exists in the service registry"
    (let [cfg (service-configs/get-entity-config :city)]
      (is (some? cfg))
      (is (= "cities" (:table-name cfg)))
      (is (= :id (:primary-key cfg))))))

(deftest sort-allowlists-include-user-visible-columns-test
  (testing "stores support sorting by supplier columns"
    (let [cfg (service-configs/get-entity-config :store)]
      (is (= :s/display_name (get-in cfg [:allowed-order-by :supplier-id])))
      (is (= :s/display_name (get-in cfg [:allowed-order-by :supplier-display-name])))))

  (testing "categories support sorting by description"
    (let [cfg (service-configs/get-entity-config :category)]
      (is (= :description (get-in cfg [:allowed-order-by :description]))))))

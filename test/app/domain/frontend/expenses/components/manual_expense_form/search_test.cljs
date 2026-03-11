(ns app.domain.frontend.expenses.components.manual-expense-form.search-test
  (:require
    [app.domain.frontend.expenses.components.manual-expense-form.search :as search]
    [cljs.test :refer [deftest is testing]]))

(deftest merge-search-results-balances-entity-types-after-dedupe
  (testing "combined quick add results keep a visible mix of entity types and preserve richer article data"
    (let [local-results [{:id "supplier-1"
                          :label "BINGO"
                          :entity-type :supplier}
                         {:id "article-1"
                          :label "Caj Menta Bingo 30g"
                          :entity-type :article}]
          backend-results [{:id "supplier-1"
                            :label "BINGO"
                            :entity_type "supplier"}
                           {:id "supplier-2"
                            :label "Biopharm"
                            :entity_type "supplier"}
                           {:id "store-1"
                            :label "PJ 219 \"Supermarket Alta\" Sarajevo"
                            :entity_type "store"}
                           {:id "store-2"
                            :label "PJ 57, \"HIPERMARKET\" Otoka"
                            :entity_type "store"}
                           {:id "article-1"
                            :label "Caj Menta Bingo 30g"
                            :entity_type "article"
                            :last_price 4.25
                            :last_price_source "supplier"
                            :last_price_supplier_display_name "BINGO"}
                           {:id "article-2"
                            :label "Hljeb Ciabatta Bingo 280g"
                            :entity_type "article"
                            :last_price 2.15
                            :last_price_source "global"
                            :last_price_supplier_display_name "KONZUM"}]
          results (search/merge-search-results local-results backend-results 6)]
      (is (= [[:supplier "supplier-1" nil nil]
              [:store "store-1" nil nil]
              [:article "article-1" 4.25 "supplier"]
              [:supplier "supplier-2" nil nil]
              [:store "store-2" nil nil]
              [:article "article-2" 2.15 "global"]]
            (mapv (fn [result]
                    [(:entity-type result)
                     (:id result)
                     (:last_price result)
                     (:last_price_source result)])
              results)))
      (is (= "Last price from KONZUM"
            (search/last-price-tooltip (last results)))))))

(deftest last-price-tooltip-only-applies-to-global-fallbacks
  (is (nil? (search/last-price-tooltip {:last_price 4.25
                                        :last_price_source "supplier"
                                        :last_price_supplier_display_name "BINGO"})))
  (is (= "Last price from KONZUM"
        (search/last-price-tooltip {:last_price 3.10
                                    :last_price_source "global"
                                    :last_price_supplier_display_name "KONZUM"}))))

(deftest filter-results-by-entity-types-keeps-only-allowed-types
  (is (= [{:id "store-1" :entity-type :store}
          {:id "article-1" :entity-type :article}]
        (search/filter-results-by-entity-types
          [{:id "supplier-1" :entity-type :supplier}
           {:id "store-1" :entity-type :store}
           {:id "article-1" :entity-type :article}]
          [:store :article]))))

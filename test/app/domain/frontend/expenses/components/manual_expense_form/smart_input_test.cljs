(ns app.domain.frontend.expenses.components.manual-expense-form.smart-input-test
  (:require
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input :as smart-input]
    [cljs.test :refer [deftest is testing]]))

(deftest focused-search-types-drop-selected-context-types
  (testing "selected context types are removed from the search focus, while article remains"
    (is (= [:supplier :store :category :article]
          (smart-input/focused-search-types {})))
    (is (= [:store :category :article]
          (smart-input/focused-search-types {:supplier {:id "sup-1"}})))
    (is (= [:category :article]
          (smart-input/focused-search-types {:supplier {:id "sup-1"}
                                             :store {:id "store-1"}})))
    (is (= [:article]
          (smart-input/focused-search-types {:supplier {:id "sup-1"}
                                             :store {:id "store-1"}
                                             :category {:id "cat-1"}})))))

(deftest search-placeholder-reflects-remaining-search-targets
  (testing "placeholder text stops mentioning already selected context types"
    (is (= "Start with supplier, store, category, or article..."
          (smart-input/search-placeholder {} false)))
    (is (= "Search store, category, or article..."
          (smart-input/search-placeholder {:supplier {:id "sup-1"}} true)))
    (is (= "Search category or article..."
          (smart-input/search-placeholder {:supplier {:id "sup-1"}
                                           :store {:id "store-1"}} true)))
    (is (= "Search article..."
          (smart-input/search-placeholder {:supplier {:id "sup-1"}
                                           :store {:id "store-1"}
                                           :category {:id "cat-1"}} true)))))

(deftest current-related-context-prefers-store-over-supplier
  (testing "focused quick picks use store-related data when a store is already selected"
    (is (= {:entity-type :supplier :entity-id "sup-1"}
          (smart-input/current-related-context {:supplier {:id "sup-1"}})))
    (is (= {:entity-type :store :entity-id "store-1"}
          (smart-input/current-related-context {:supplier {:id "sup-1"}
                                                :store {:id "store-1"}})))))

(deftest build-quick-pick-groups-uses-top-10-for-single-missing-type
  (testing "a single remaining entity type shows up to 10 local candidates"
    (let [stores (mapv (fn [n]
                         {:id (str "store-" n)
                          :display-name (str "Store " n)
                          :supplier_id "sup-1"})
                   (range 12))
          groups (smart-input/build-quick-pick-groups
                   [:store]
                   []
                   stores
                   []
                   []
                   "sup-1")]
      (is (= 1 (count groups)))
      (is (= :store (:entity-type (first groups))))
      (is (= 10 (count (:items (first groups))))))))

(deftest build-quick-pick-groups-uses-top-5-per-type-and-filters-stores-by-selected-supplier
  (testing "multiple remaining entity types show 5 each and store picks respect selected supplier"
    (let [stores [{:id "store-1" :display-name "Bingo One" :supplier_id "sup-1"}
                  {:id "store-2" :display-name "Bingo Two" :supplier_id "sup-1"}
                  {:id "store-3" :display-name "Other Supplier" :supplier_id "sup-2"}]
          categories (mapv (fn [n]
                             {:id (str "cat-" n)
                              :name (str "Category " n)})
                       (range 7))
          articles (mapv (fn [n]
                           {:id (str "article-" n)
                            :canonical-name (str "Article " n)})
                     (range 7))
          groups (smart-input/build-quick-pick-groups
                   [:store :category :article]
                   []
                   stores
                   categories
                   articles
                   "sup-1")]
      (is (= [:store :category :article] (mapv :entity-type groups)))
      (is (= ["store-1" "store-2"] (mapv :id (get-in groups [0 :items]))))
      (is (= 5 (count (get-in groups [1 :items]))))
      (is (= 5 (count (get-in groups [2 :items])))))))

(ns app.domain.frontend.expenses.components.manual-expense-form.smart-input-test
  (:require
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.components :as smart-input-components]
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.helpers :as smart-input]
    [cljs.test :refer [deftest is testing]]))

(deftest focused-search-types-drop-selected-context-types
  (testing "selected context types are removed from the search focus, while article remains"
    (is (= [:supplier :store :category :article]
          (smart-input/focused-search-types {} false)))
    (is (= [:store :category :article]
          (smart-input/focused-search-types {:supplier {:id "sup-1"}} false)))
    (is (= [:category :article]
          (smart-input/focused-search-types {:supplier {:id "sup-1"}
                                             :store {:id "store-1"}} false)))
    (is (= [:article]
          (smart-input/focused-search-types {:supplier {:id "sup-1"}
                                             :store {:id "store-1"}
                                             :category {:id "cat-1"}} false)))))

(deftest search-placeholder-reflects-remaining-search-targets
  (let [t (fn [k & args]
            (case k
              :smart-expense/article-mode-ph "Search article..."
              :smart-expense/search-prefix "Search "
              :smart-expense/start-with-prefix "Start with "
              :smart-expense/or-connector " or "
              :smart-expense/entity-supplier "supplier"
              :smart-expense/entity-store "store"
              :smart-expense/entity-category "category"
              :smart-expense/entity-article "article"
              (first args)))]
    (testing "placeholder text stops mentioning already selected context types"
      (is (= "Start with supplier, store, category or article..."
            (smart-input/search-placeholder t {} false false)))
      (is (= "Search store, category or article..."
            (smart-input/search-placeholder t {:supplier {:id "sup-1"}} true false)))
      (is (= "Search category or article..."
            (smart-input/search-placeholder t {:supplier {:id "sup-1"}
                                               :store {:id "store-1"}} true false)))
      (is (= "Search article..."
            (smart-input/search-placeholder t {:supplier {:id "sup-1"}
                                               :store {:id "store-1"}
                                               :category {:id "cat-1"}} true false))))))

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
          groups (smart-input-components/build-quick-pick-groups
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
          groups (smart-input-components/build-quick-pick-groups
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

(deftest phase-two-quick-pick-groups-falls-back-per-missing-type
  (testing "history suggestions apply per entity type so categories still fall back to local picks"
    (let [context-suggestions {:suppliers [{:id "sup-9"
                                            :label "Suggested Supplier"}]
                               :stores []
                               :categories []}
          categories (mapv (fn [n]
                             {:id (str "cat-" n)
                              :name (str "Category " n)})
                       (range 7))
          groups (smart-input-components/phase-two-quick-pick-groups
                   [:supplier :category]
                   context-suggestions
                   []
                   []
                   categories
                   []
                   nil)]
      (is (= [:supplier :category] (mapv :entity-type groups)))
      (is (= ["sup-9"] (mapv :id (get-in groups [0 :items]))))
      (is (= 5 (count (get-in groups [1 :items]))))
      (is (= ["cat-0" "cat-1" "cat-2" "cat-3" "cat-4"]
            (mapv :id (get-in groups [1 :items])))))))

(deftest phase-two-quick-pick-groups-filters-store-suggestions-by-selected-supplier
  (testing "store suggestions from article history must respect the selected supplier"
    (let [context-suggestions {:suppliers []
                               :stores [{:id "store-bingo" :label "Bingo Marka" :supplier_id "sup-1"}
                                        {:id "store-petrol" :label "Petrol BH" :supplier_id "sup-2"}]
                               :categories []}
          groups (smart-input-components/phase-two-quick-pick-groups
                   [:store]
                   context-suggestions
                   []
                   []
                   []
                   []
                   "sup-1")]
      (is (= [:store] (mapv :entity-type groups)))
      (is (= ["store-bingo"] (mapv :id (get-in groups [0 :items]))))))
  (testing "when all store suggestions belong to other suppliers, fall back to local stores filtered by supplier"
    (let [context-suggestions {:suppliers []
                               :stores [{:id "store-petrol" :label "Petrol BH" :supplier_id "sup-2"}]
                               :categories []}
          local-stores [{:id "local-bingo-1" :display-name "Bingo PJ 91" :supplier_id "sup-1"}
                        {:id "local-petrol" :display-name "Petrol Local" :supplier_id "sup-2"}]
          groups (smart-input-components/phase-two-quick-pick-groups
                   [:store]
                   context-suggestions
                   []
                   local-stores
                   []
                   []
                   "sup-1")]
      (is (= [:store] (mapv :entity-type groups)))
      (is (= ["local-bingo-1"] (mapv :id (get-in groups [0 :items])))))))

(deftest phase-two-quick-pick-groups-merges-store-history-with-supplier-pool
  (testing "store quick-picks show history-ranked picks first, then fill from the supplier pool"
    (let [context-suggestions {:suppliers []
                               :stores [{:id "store-history" :label "Bingo PJ 91" :supplier_id "sup-1"}]
                               :categories []}
          local-stores [{:id "store-a" :display-name "Bingo Branch A" :supplier_id "sup-1"}
                        {:id "store-history" :display-name "Bingo PJ 91" :supplier_id "sup-1"}
                        {:id "store-b" :display-name "Bingo Branch B" :supplier_id "sup-1"}
                        {:id "store-other" :display-name "Petrol" :supplier_id "sup-2"}]
          groups (smart-input-components/phase-two-quick-pick-groups
                   [:store]
                   context-suggestions
                   []
                   local-stores
                   []
                   []
                   "sup-1")]
      (is (= [:store] (mapv :entity-type groups)))
      (is (= ["store-history" "store-a" "store-b"]
            (mapv :id (get-in groups [0 :items])))
        "history-ranked store appears first; remaining supplier stores fill the rest; non-supplier stores excluded")))
  (testing "merged store list is capped at the limit (10 when store is the only missing type)"
    (let [history (mapv (fn [n]
                          {:id (str "hist-" n)
                           :label (str "History " n)
                           :supplier_id "sup-1"})
                    (range 4))
          local (mapv (fn [n]
                        {:id (str "local-" n)
                         :display-name (str "Local " n)
                         :supplier_id "sup-1"})
                  (range 12))
          context-suggestions {:suppliers [] :stores history :categories []}
          groups (smart-input-components/phase-two-quick-pick-groups
                   [:store]
                   context-suggestions
                   []
                   local
                   []
                   []
                   "sup-1")
          ids (mapv :id (get-in groups [0 :items]))]
      (is (= 10 (count ids)))
      (is (= ["hist-0" "hist-1" "hist-2" "hist-3"] (subvec ids 0 4))
        "history items appear at the head")
      (is (= ["local-0" "local-1" "local-2" "local-3" "local-4" "local-5"] (subvec ids 4 10))
        "remaining slots are filled from the supplier-scoped local pool"))))

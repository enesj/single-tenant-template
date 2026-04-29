(ns app.mobile.frontend.pages.manual-entry-test
  (:require
    [app.domain.frontend.expenses.components.manual-expense-form.smart-input.constants :as smart-input-constants]
    [app.mobile.frontend.pages.manual-entry.components :as components]
    [app.mobile.frontend.pages.manual-entry.events :as events]
    [app.mobile.frontend.pages.manual-entry.helpers :as helpers]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest add-context-entry-keeps-supplier-and-store-in-sync
  (let [add-context-entry helpers/add-context-entry]
    (testing "selecting a supplier drops an existing store from another supplier"
      (is (= {:supplier {:id "sup-2" :label "New Supplier"}}
            (add-context-entry
              {:supplier {:id "sup-1" :label "Old Supplier"}
               :store {:id "store-1" :label "Old Store" :supplier-id "sup-1"}}
              :supplier
              {:id "sup-2" :display_name "New Supplier"}))))
    (testing "selecting a store also backfills the supplier chip when supplier metadata is present"
      (is (= {:store {:id "store-9"
                      :label "Branch 9"
                      :supplier-id "sup-9"
                      :supplier-display-name "Supplier 9"}
              :supplier {:id "sup-9" :label "Supplier 9"}}
            (add-context-entry
              {}
              :store
              {:id "store-9"
               :display_name "Branch 9"
               :supplier_id "sup-9"
               :supplier_display_name "Supplier 9"}))))))

(deftest mobile-submit-error-key-matches-phase-one-save-validation
  (let [submit-error-key helpers/submit-error-key]
    (testing "requires at least one prepared line item"
      (is (= :smart-expense/err-no-items
            (submit-error-key [] "payer-1"))))
    (testing "requires a payer before either footer save action becomes valid"
      (is (= :smart-expense/err-no-payer
            (submit-error-key [{:label "Item"
                                :qty "1"
                                :unit-price "2.50"}]
              nil))))
    (testing "matches the web flow by allowing valid saves without supplier/store context"
      (is (nil? (submit-error-key [{:label "Item"
                                    :qty "1"
                                    :unit-price "2.50"}]
                  "payer-1"))))))

(deftest phase-one-category-picks-stay-visible-when-category-is-missing
  (let [phase-one-category-picks helpers/phase-one-category-picks
        categories (mapv (fn [n]
                           {:id (str "cat-" n)
                            :name (str "Category " n)})
                     (range 7))]
    (testing "blank search returns the first mobile category chips"
      (is (= ["cat-0" "cat-1" "cat-2" "cat-3" "cat-4"]
            (mapv :id (phase-one-category-picks categories {} "")))))
    (testing "an already selected category suppresses the replacement chips"
      (is (empty? (phase-one-category-picks categories {:category {:id "cat-1"}} ""))))
    (testing "active typing hides the pinned replacement chips"
      (is (empty? (phase-one-category-picks categories {} "kup"))))))

(deftest phase-one-history-picks-show-mobile-store-and-article-chips
  (let [phase-one-history-picks helpers/phase-one-history-picks
        history {:stores [{:id "store-1" :display_name "Store 1"}
                          {:id "store-2" :display_name "Store 2"}]
                 :articles [{:id "article-1" :canonical_name "Article 1" :last_price 4.25}
                            {:id "article-2" :canonical_name "Article 2" :last_price 1.75}]}
        picks-with-category (phase-one-history-picks history [] {:category {:id "cat-1"}} "")
        picks-with-item (phase-one-history-picks history [{:article-id "article-1"}] {:category {:id "cat-1"}} "")]
    (testing "blank search with the default category shows both store and article chips"
      (is (= ["store-1" "store-2"]
            (mapv :id (:stores picks-with-category))))
      (is (= [{:id "article-1" :label "Article 1" :entity-type :article :last-price 4.25 :entity {:id "article-1" :canonical_name "Article 1" :last_price 4.25}}
              {:id "article-2" :label "Article 2" :entity-type :article :last-price 1.75 :entity {:id "article-2" :canonical_name "Article 2" :last_price 1.75}}]
            (:articles picks-with-category))))
    (testing "once line items exist, the history chips give way to article-mode suggestions"
      (is (empty? (:stores picks-with-item)))
      (is (empty? (:articles picks-with-item))))))

(deftest mobile-visible-suggestions-dedupe-store-labels-and-backfill-suppliers
  (let [visible-mobile-suggestions @#'components/visible-mobile-suggestions
        suggestions {:suppliers [{:id "sup-bingo" :display_name "BINGO"}]
                     :stores [{:id "store-bingo-1"
                               :label "BINGO Rajlovac"
                               :address "BRAĆE BEGIĆ br.4, 71000 Sarajevo"
                               :supplier_id "sup-bingo"
                               :supplier_display_name "BINGO"}
                              {:id "store-bingo-2"
                               :label "BINGO Rajlovac Duplicate"
                               :address "BRAĆE BEGIĆ br.4, 71000 Sarajevo"
                               :supplier_id "sup-bingo"
                               :supplier_display_name "BINGO"}
                              {:id "store-apo-1"
                               :label "Apoteka 1"
                               :address "Kranjčevićeva-Tepebašina 1, 71000 Sarajevo"
                               :supplier_id "sup-apo"
                               :supplier_display_name "APOTEKE SARAJEVO"}
                              {:id "store-bk-1"
                               :label "B&K 1"
                               :address "Trg solidarnosti 12, 71000 Sarajevo"
                               :supplier_id "sup-bk"
                               :supplier_display_name "B&K"}]
                     :categories []}
        visible (visible-mobile-suggestions suggestions {})]
    (testing "duplicate visible store labels collapse per supplier"
      (is (= ["store-bingo-1" "store-apo-1" "store-bk-1"]
            (mapv :id (:stores visible))))
      (is (= ["BRAĆE BEGIĆ br.4, 71000 Sarajevo"
              "Kranjčevićeva-Tepebašina 1, 71000 Sarajevo"
              "Trg solidarnosti 12, 71000 Sarajevo"]
            (mapv :label (:stores visible)))))
    (testing "store owners backfill supplier chips so paired colors stay aligned"
      (is (= ["sup-bingo" "sup-apo" "sup-bk"]
            (mapv :id (:suppliers visible))))
      (is (= ["BINGO" "APOTEKE SARAJEVO" "B&K"]
            (mapv :label (:suppliers visible)))))))

(deftest mobile-suggestion-color-map-links-suppliers-and-stores-by-slot
  (let [build-suggestion-supplier-color-map @#'components/build-suggestion-supplier-color-map
        suggestion-chip-class @#'components/suggestion-chip-class
        suppliers [{:id "sup-1" :display_name "BINGO"}
                   {:id "sup-2" :display_name "KONZUM"}]
        stores [{:id "store-1" :display_name "PJ 91" :supplier_id "sup-1"}
                {:id "store-2" :display_name "Podružnica br. 47" :supplier_id "sup-2"}]
        color-map (build-suggestion-supplier-color-map suppliers stores)]
    (testing "visible suppliers and their stores share the same palette slot"
      (is (= (first smart-input-constants/supplier-color-palette)
            (get color-map "sup-1")))
      (is (= (second smart-input-constants/supplier-color-palette)
            (get color-map "sup-2")))
      (is (= (:supplier (first smart-input-constants/supplier-color-palette))
            (suggestion-chip-class color-map :supplier (first suppliers))))
      (is (= (:store (first smart-input-constants/supplier-color-palette))
            (suggestion-chip-class color-map :store (first stores)))))))

(deftest mobile-store-suggestions-keep-paired-colors-without-visible-supplier-chips
  (let [build-suggestion-supplier-color-map @#'components/build-suggestion-supplier-color-map
        suggestion-chip-class @#'components/suggestion-chip-class
        stores [{:id "store-9" :display_name "Branch 9" :supplier_id "sup-9"}]
        color-map (build-suggestion-supplier-color-map [] stores)]
    (testing "store-only suggestion sections still assign a supplier-based palette slot"
      (is (= (first smart-input-constants/supplier-color-palette)
            (get color-map "sup-9")))
      (is (= (:store (first smart-input-constants/supplier-color-palette))
            (suggestion-chip-class color-map :store (first stores)))))))

(deftest item-label-edit-clears-stale-article-id
  (let [update-item-label-entry @#'components/update-item-label-entry]
    (is (= {:label "dun" :qty "1" :unit-price "6.90"}
          (update-item-label-entry {:label "Cigarete Dunhill Distinct"
                                    :article-id "article-1"
                                    :qty "1"
                                    :unit-price "6.90"}
            "dun")))))

(deftest apply-item-article-selection-preserves-row-and-fills-price
  (let [apply-item-article-selection @#'components/apply-item-article-selection]
    (testing "selecting a searched article swaps in the canonical label and article id"
      (is (= {:label "Cigarete Dunhill Distinct"
              :article-id "article-9"
              :qty "2"
              :unit-price "6.90"}
            (apply-item-article-selection {:label "dun"
                                           :qty "2"
                                           :unit-price ""}
              {:id "article-9"
               :canonical_name "Cigarete Dunhill Distinct"
               :last_price 6.9}))))
    (testing "missing remote price keeps an already entered unit price"
      (is (= {:label "No Price Article"
              :article-id "article-7"
              :qty "1"
              :unit-price "4.50"}
            (apply-item-article-selection {:label "old"
                                           :qty "1"
                                           :unit-price "4.50"}
              {:id "article-7"
               :canonical_name "No Price Article"}))))))

(deftest mobile-search-events-support-item-row-autocomplete
  (testing "short queries clear stale article results"
    (reset! rf-db/app-db {:mobile {:search-results {:article [{:id "old"}]}}})
    (rf/dispatch-sync [:mobile/search-entities :article "d" nil])
    (is (empty? (get-in @rf-db/app-db [:mobile :search-results :article]))))
  (testing "article quick-add payloads normalize through the shared search-results event"
    (reset! rf-db/app-db {:mobile {}})
    (rf/dispatch-sync [:mobile/search-results :article {:results [{:id "article-1" :label "Article 1"}]}])
    (is (= [{:id "article-1" :label "Article 1"}]
          (get-in @rf-db/app-db [:mobile :search-results :article])))))

(deftest fetch-cooccurring-articles-without-ids-keeps-mobile-shell-state
  (let [initial-db {:locale :bs
                    :current-route {:data {:view :m/upload-manual}}
                    :session {:authenticated? true
                              :loading? false}
                    :mobile {:active-tab :upload}}]
    (reset! rf-db/app-db initial-db)
    (rf/dispatch-sync [:mobile/fetch-cooccurring-articles [] nil])
    (is (= {:data {:view :m/upload-manual}}
          (:current-route @rf-db/app-db)))
    (is (= {:authenticated? true
            :loading? false}
          (:session @rf-db/app-db)))
    (is (= :upload
          (get-in @rf-db/app-db [:mobile :active-tab])))
    (is (= {:loading? false
            :results []}
          (get-in @rf-db/app-db [:mobile :cooccurring-articles])))))

(deftest entity-search-uri-covers-irregular-endpoints
  (let [entity-search-uri @#'events/entity-search-uri]
    (testing "category lookups use the expense-categories endpoint instead of a naive plural"
      (is (= "/api/v1/expenses/expense-categories" (entity-search-uri :category)))
      (is (= "/api/v1/expenses/expense-categories" (entity-search-uri :expense-category))))
    (testing "known standard entities keep their explicit endpoints"
      (is (= "/api/v1/expenses/suppliers" (entity-search-uri :supplier)))
      (is (= "/api/v1/expenses/stores" (entity-search-uri :store)))
      (is (= "/api/v1/expenses/articles" (entity-search-uri :article))))))

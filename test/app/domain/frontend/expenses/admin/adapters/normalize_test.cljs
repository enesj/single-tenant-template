(ns app.domain.frontend.expenses.admin.adapters.normalize-test
  (:require
    [app.domain.frontend.expenses.admin.adapters.normalize :as sut]
    [cljs.test :refer [deftest is testing]]))

(deftest article-alias-normalization-includes-fk-aliases
  (testing "article aliases expose :article-id and :supplier-id for list/table configs"
    (let [alias {:id "00000000-0000-0000-0000-000000000001"
                 :supplier_id "00000000-0000-0000-0000-000000000002"
                 :article_id "00000000-0000-0000-0000-000000000003"
                 :raw_label "RWC 0116523953805 SUKNJA"
                 :raw_label_normalized "rwc-0116523953805-suknja"
                 :supplier_display_name "Test Supplier"
                 :article_canonical_name "Suknja"}
          normalized (sut/article-alias->template-entity alias)]
      (is (= (:supplier_id alias) (:supplier-id normalized)))
      (is (= (:article_id alias) (:article-id normalized)))
      ;; sanity: existing computed/aliased fields remain available
      (is (= "Test Supplier" (:supplier-display-name normalized)))
      (is (= "Suknja" (:article-canonical-name normalized))))))

(deftest payer-normalization-exposes-legacy-payer-type-label-alias
  (testing "payers expose :payer-type-label for stale runtime table-column configs"
    (let [payer {:id "00000000-0000-0000-0000-000000000010"
                 :payer_type "custom"
                 :type "custom"
                 :label "Shared household"
                 :user_email "demo@example.com"}
          normalized (sut/payer->template-entity payer)]
      (is (= "custom" (:payer-type normalized)))
      (is (= "custom" (:payer-type-label normalized)))
      (is (= "demo@example.com" (:user-email normalized))))))

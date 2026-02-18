(ns app.admin.frontend.specs.generic-entity-specs-test
  (:require
    [app.admin.frontend.specs.generic]
    [app.shared.model-naming :as model-naming]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- reset-db!
  [db]
  (reset! rf-db/app-db db)
  (rf/clear-subscription-cache!))

(defn- fields-by-id
  [fields]
  (into {}
    (map (fn [field]
           [(model-naming/ensure-app-keyword (:id field)) field]))
    fields))

(deftest admin-entity-spec-preserves-model-derived-field-types
  (testing ":admin/entity-spec keeps model field typing/options for vector-config columns"
    (reset-db!
      {:admin {:config {:table-columns
                        {:expenses {:available-columns ["purchased_at" "currency" "supplier_display_name"]
                                    :computed-fields {"supplier_display_name" {:type "string" :label "Supplier"}}
                                    :column-config {}
                                    :always-visible []}}}}
       :entities {:specs
                  {:expenses [{:id "purchased-at" :label "Purchased at" :type "input" :input-type "datetime-local"}
                              {:id "currency"
                               :label "Currency"
                               :type "select"
                               :input-type "select"
                               :options [{:value "BAM" :label "BAM"}
                                         {:value "EUR" :label "EUR"}
                                         {:value "USD" :label "USD"}]}]}}})

    (let [spec @(rf/subscribe [:admin/entity-spec :expenses])
          by-id (fields-by-id (:fields spec))
          purchased-at (get by-id :purchased-at)
          currency (get by-id :currency)
          supplier (get by-id :supplier-display-name)]
      (is (= "datetime-local" (:input-type purchased-at))
        "purchased_at should retain datetime input type instead of degrading to text")
      (is (= "select" (:type currency))
        "currency should stay select when model metadata provides enum/select typing")
      (is (= 3 (count (:options currency)))
        "currency should keep enum options so filter UI can render a dropdown")
      (is (= "string" (:type supplier))
        "computed column type should still come from table computed-fields config"))))

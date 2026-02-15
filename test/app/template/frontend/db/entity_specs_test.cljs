(ns app.template.frontend.db.entity-specs-test
  (:require
    ;; In the full test suite, admin registers an override for :form-entity-specs/by-name.
    ;; Require it explicitly so this test is stable and exercises the effective subscription.
    [app.admin.frontend.specs.generic]
    ;; Ensure handlers/subscriptions are registered
    [app.template.frontend.db.entity-specs :as entity-specs]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- reset-db! [db]
  (reset! rf-db/app-db db)
  ;; Ensure we don't reuse cached subscription computations between tests.
  (rf/clear-subscription-cache!))

(def ^:private models-data
  ;; Minimal models metadata keyed in snake_case, as it often arrives from the DB layer.
  ;; We want subscriptions to tolerate callers using snake_case entity identifiers.
  {:price_observations
   {:fields
    [[:id :serial {:null false}]
     [:created_at :timestamptz {:null false}]
     [:foo_bar [:varchar 255] {:null false}]]}})

(def ^:private article-aliases-models-data
  {:article_aliases
   {:fields
    [[:id :uuid {:null false}]
     [:created_at :timestamptz {:null false}]
     [:updated_at :timestamptz {:null false}]
     [:supplier_id :uuid {:null false
                          :foreign-key :suppliers/id}]
     [:article_id :uuid {:null false
                         :foreign-key :articles/id}]
     [:raw_label_normalized [:varchar 255] {:null false}]
     [:confidence :integer {:null false}]]}})

(def ^:private article-aliases-table-columns
  {:available-columns
   ["supplier_display_name"
    "article_canonical_name"
    "raw_label_normalized"
    "confidence"
    "created_at"
    "id"]
   :computed-fields
   {"supplier_display_name" {}
    "article_canonical_name" {}}
   :column-metadata
   {"article_canonical_name" {:label "Article"}}})

(deftest entity-specs-by-name-normalizes-entity-key
  (testing ":entity-specs/by-name resolves the same spec for snake_case and kebab-case entity identifiers"
    (reset-db! {:models-data models-data})
    (rf/dispatch-sync [::entity-specs/initialize-entity-specs])

    (let [spec-kebab @(rf/subscribe [:entity-specs/by-name :price-observations])
          spec-snake @(rf/subscribe [:entity-specs/by-name :price_observations])
          spec-str @(rf/subscribe [:entity-specs/by-name "price_observations"])]
      (is (seq spec-kebab) "Sanity: spec should not be empty")
      (is (= spec-kebab spec-snake) "snake_case keyword should resolve to the same spec")
      (is (= spec-kebab spec-str) "snake_case string should resolve to the same spec")
      (is (some #(= "foo-bar" (:id %)) spec-kebab)
        "Field ids should be normalized to kebab-case"))))

(deftest form-entity-specs-by-name-normalizes-entity-key
  (testing ":form-entity-specs/by-name resolves the same spec for snake_case and kebab-case entity identifiers"
    (reset-db! {:models-data models-data})

    (let [spec-kebab @(rf/subscribe [:form-entity-specs/by-name :price-observations])
          spec-snake @(rf/subscribe [:form-entity-specs/by-name :price_observations])
          spec-str @(rf/subscribe [:form-entity-specs/by-name "price_observations"])]
      (is (seq spec-kebab) "Sanity: form spec should not be empty")
      (is (= spec-kebab spec-snake) "snake_case keyword should resolve to the same form spec")
      (is (= spec-kebab spec-str) "snake_case string should resolve to the same form spec")
      (is (some #(= "foo-bar" (:id %)) spec-kebab)
        "Field ids should be normalized to kebab-case"))))

(deftest entity-specs-by-name-includes-config-computed-fields-and-orders-by-available
  (testing ":entity-specs/by-name merges computed fields from table-columns and filters/orders by :available-columns"
    (reset-db!
      {:models-data article-aliases-models-data
       :domain {:config {:table-columns {:article-aliases article-aliases-table-columns}}}})
    (rf/dispatch-sync [::entity-specs/initialize-entity-specs])

    (let [spec @(rf/subscribe [:entity-specs/by-name :article-aliases])
          ids (mapv :id spec)
          article-field (some (fn [field]
                                (when (= "article-canonical-name" (:id field))
                                  field))
                          spec)]
      ;; Computed fields should exist (data provides these keys even though models metadata doesn't).
      (is (some #{"supplier-display-name"} ids))
      (is (some #{"article-canonical-name"} ids))
      (is (= "Article" (:label article-field)))
      ;; Base foreign-key fields should be filtered out because they're not in :available-columns.
      (is (not (some #{"supplier-id"} ids)))
      (is (not (some #{"article-id"} ids)))
      ;; Ordering should follow :available-columns exactly.
      (is (= ["supplier-display-name"
              "article-canonical-name"
              "raw-label-normalized"
              "confidence"
              "created-at"
              "id"]
            ids)))))

(deftest entity-specs-by-name-blank-column-metadata-label-falls-back
  (testing "blank :column-metadata labels fall back to default naming"
    (reset-db!
      {:models-data article-aliases-models-data
       :domain {:config {:table-columns
                         {:article-aliases (assoc article-aliases-table-columns
                                             :column-metadata {"article_canonical_name" {:label "   "}})}}}})
    (rf/dispatch-sync [::entity-specs/initialize-entity-specs])

    (let [spec @(rf/subscribe [:entity-specs/by-name :article-aliases])
          article-field (some (fn [field]
                                (when (= "article-canonical-name" (:id field))
                                  field))
                          spec)]
      (is (= "Article canonical name" (:label article-field))))))

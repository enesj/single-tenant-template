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

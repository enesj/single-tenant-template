(ns app.template.frontend.interceptors.persistence-test
  (:require
    ;; Reuse the Node test bootstrap to ensure jsdom + localStorage exist.
    [app.admin.frontend.test-setup]
    [app.template.frontend.interceptors.persistence :as persistence]
    [cljs.test :refer [deftest is testing use-fixtures]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(use-fixtures :each
  (fn [t]
    (reset! rf-db/app-db {})
    (when (exists? js/localStorage)
      (.clear js/localStorage))
    (t)))

(deftest load-stored-prefs-normalizes-snake-case-entity-keys-test
  (testing "stored ui-entity-prefs snake_case keys are migrated to kebab-case and deep-merged"
    ;; Pretend we have old persisted prefs under a snake_case entity key.
    (.setItem js/localStorage
      "ui-entity-prefs"
      (pr-str
        {:price_observations {:columns {:visible {:id false}}
                              :filters {:fields {:created_at true}}}}))

    ;; Existing in-memory prefs under the canonical kebab-case key.
    (swap! rf-db/app-db assoc-in
      [:ui :entity-prefs :price-observations :display]
      {:show-edit? true})

    (rf/dispatch-sync [::persistence/load-stored-prefs])

    (let [prefs (get-in @rf-db/app-db [:ui :entity-prefs])]
      (is (contains? prefs :price-observations) "Prefs should exist under canonical kebab-case key")
      (is (not (contains? prefs :price_observations)) "Snake_case key should not remain after normalization")

      ;; Deep merge should preserve both existing in-memory prefs and stored prefs.
      (is (= true (get-in prefs [:price-observations :display :show-edit?])))
      (is (= false (get-in prefs [:price-observations :columns :visible :id])))
      (is (= true (get-in prefs [:price-observations :filters :fields :created-at]))))))

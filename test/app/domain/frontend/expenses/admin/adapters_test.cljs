(ns app.domain.frontend.expenses.admin.adapters-test
  "Tests for domain admin adapters (expenses entities)"
  (:require
    [app.admin.frontend.test-setup :as setup]
    [app.domain.frontend.expenses.admin.adapters.ui-state :as ui-state]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest initialize-expenses-entity-seeds-current-page-without-per-page
  (testing "initialize event seeds :current-page without hardcoding per-page"
    (setup/reset-db!)
    (rf/dispatch-sync [::ui-state/initialize-entity :expenses {:sort-field :purchased-at :sort-direction :desc}])
    (let [db @rf-db/app-db
          base (paths/list-ui-state :expenses)]
      (is (= 1 (get-in db (conj base :pagination :current-page))))
      (is (nil? (get-in db (conj base :pagination :per-page)))
        "per-page should be left unset so list-view can seed it from configured defaults")
      (is (nil? (get-in db (conj base :per-page)))
        "legacy top-level per-page should not be initialized here"))))

(deftest initialize-expenses-entity-preserves-existing-pagination
  (testing "initialize event preserves any existing pagination (incl per-page)"
    (setup/reset-db!)
    (let [base (paths/list-ui-state :expenses)]
      (swap! rf-db/app-db assoc-in base {:pagination {:current-page 4 :per-page 25}})
      (rf/dispatch-sync [::ui-state/initialize-entity :expenses {:sort-field :purchased-at :sort-direction :desc}])
      (let [db @rf-db/app-db]
        (is (= 4 (get-in db (conj base :pagination :current-page)))
          "should not overwrite existing current-page")
        (is (= 25 (get-in db (conj base :pagination :per-page)))
          "should not overwrite existing per-page")))))

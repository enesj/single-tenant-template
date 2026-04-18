(ns app.admin.frontend.adapters.backlog-test
  (:require
    [app.admin.frontend.adapters.backlog :as backlog-adapter]
    [app.admin.frontend.test-setup :as setup]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest initialize-backlog-adapter-seeds-status-first-default-sorts
  (testing "initialize event seeds canonical backlog default sorts to match backend ordering"
    (setup/reset-db!)
    (rf/dispatch-sync [::backlog-adapter/initialize-backlog-adapter-with-config])
    (let [db @rf-db/app-db
          base (paths/list-ui-state :backlog)]
      (is (= {:field :status :direction :asc}
            (get-in db (conj (paths/entity-metadata :backlog) :sort))))
      (is (= {:field :status :direction :asc}
            (get-in db (conj base :sort))))
      (is (= [{:field :status :direction :asc}
              {:field :number :direction :asc}]
            (get-in db (paths/list-sorts :backlog))))
      (is (= 1 (get-in db (conj base :pagination :current-page))))
      (is (nil? (get-in db (conj base :pagination :per-page)))
        "per-page should be left unset so list-view can seed it from configured defaults"))))

(deftest initialize-backlog-adapter-preserves-existing-pagination
  (testing "initialize event preserves existing pagination when seeding backlog sort defaults"
    (setup/reset-db!)
    (let [base (paths/list-ui-state :backlog)]
      (swap! rf-db/app-db assoc-in base {:pagination {:current-page 3 :per-page 77}})
      (rf/dispatch-sync [::backlog-adapter/initialize-backlog-adapter-with-config])
      (let [db @rf-db/app-db]
        (is (= 3 (get-in db (conj base :pagination :current-page))))
        (is (= 77 (get-in db (conj base :pagination :per-page))))))))

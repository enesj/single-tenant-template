(ns app.template.frontend.utils.shared-test
  (:require
    [app.template.frontend.utils.shared :as shared]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]))

(deftest extract-entity-id-test
  (testing "extract-entity-id handles namespaced keys"
    (is (= 1 (shared/extract-entity-id {:id 1})))
    (is (= 2 (shared/extract-entity-id {:users/id 2})))
    (is (= 3 (shared/extract-entity-id {:entity/id 3})))
    (is (nil? (shared/extract-entity-id {}))))

  (testing "extract-entity-ids filters nils"
    (is (= [1 2]
          (shared/extract-entity-ids [{:id 1} {:users/id 2} {:id nil}])))))

(deftest validate-entity-operation-test
  (testing "validate-entity-operation enforces id presence"
    (is (= {:valid? false :error "Entity cannot be nil"}
          (shared/validate-entity-operation nil :create)))
    (is (= {:valid? false :error "Entity must have a valid ID"}
          (shared/validate-entity-operation {:name "No ID"} :update)))
    (is (= {:valid? true}
          (shared/validate-entity-operation {:id 1} :delete)))))

(deftest dispatch-helpers-test
  (testing "dispatch-entity-operation builds expected event"
    (let [events (atom [])]
      (with-redefs [rf/dispatch (fn [event] (swap! events conj event))]
        (shared/dispatch-entity-operation :admin :users :update {:name "Ada"})
        (shared/dispatch-entity-operation :admin :users :delete {:name "Ada"} 1))
      (is (= [[:admin/users-update {:name "Ada"}]
              [:admin/users-delete 1 {:name "Ada"}]]
            @events))))

  (testing "dispatch-batch-operation composes event keyword"
    (let [events (atom [])]
      (with-redefs [rf/dispatch (fn [event] (swap! events conj event))]
        (shared/dispatch-batch-operation :tenant :users :status :active [1 2]))
      (is (= [[:tenant/bulk-update-users-status [1 2] :active]] @events)))))

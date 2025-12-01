(ns app.template.frontend.db.db-test
  "Tests for database path utilities and structure validation"
  (:require
    [app.template.frontend.auto-test-data :as auto-data]
    [app.template.frontend.db.db :as template-db]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [are deftest is run-tests testing]]))

(def simple-models-data
  {:users {:fields [[:id :uuid {:primary-key true}]
                    [:tenant_id :uuid]
                    [:full_name [:varchar 255]]
                    [:role [:enum :user-role]]]
           :types [[:user-role :enum {:choices ["admin" "member"]}]]}
   :properties {:fields [[:id :uuid {:primary-key true}]
                         [:tenant_id :uuid]
                         [:name [:varchar 255]]
                         [:status [:enum :property-status]]]
                :types [[:property-status :enum {:choices ["active" "inactive"]}]]}})

(def simple-models-data-vector
  (mapv (fn [[k v]] [(name k) v]) simple-models-data))

(def complex-models-data auto-data/models)

(def complex-models-data-vector
  (mapv (fn [[k v]] [(name k) v]) complex-models-data))

(def sample-user-form-state
  {:data {:full_name "Ada Lovelace" :role "admin"}
   :dirty-fields #{:full_name}
   :submitting? false
   :submitted? true
   :success {:full_name true}
   :waiting #{:full_name}
   :errors {:role "Required for audit"}
   :server-errors nil})

(defn augment-with-sample-entity-state
  [db entity-key]
  (-> db
    (assoc-in [:entities entity-key :data]
      {1 {:id 1
          :tenant_id "tenant-123"
          :full_name "Ada Lovelace"
          :role "admin"}
       "user-2" {:id "user-2"
                 :tenant_id "tenant-123"
                 :full_name "Grace Hopper"
                 :role "member"}})
    (assoc-in [:entities entity-key :ids] [1 "user-2"])
    (assoc-in [:entities entity-key :metadata]
      {:loading? false
       :success true
       :last-updated 1700000000})
    (assoc-in [:ui :lists entity-key]
      (-> (template-db/make-default-list-state)
        (assoc :selected-ids #{1 "user-2"}
          :filters {:role "admin"}
          :search {:term "Ada"
                   :columns [:full_name]
                   :pending? false})))
    (assoc-in [:forms entity-key] sample-user-form-state)))

(defn build-sample-db
  [models-data]
  (-> (template-db/make-db-with-models-data template-db/default-db models-data)
    (augment-with-sample-entity-state :users)))

(defn build-complex-db
  [models-data]
  (let [txn-id (keyword "txn" "1")]
    (-> (template-db/make-db-with-models-data template-db/default-db models-data)
      (augment-with-sample-entity-state :users)
      (update-in [:entities :properties]
        (fn [store]
          (let [store (or store {:data {} :ids [] :metadata {}})]
            (-> store
              (assoc :data {"prop-1" {:id "prop-1"
                                      :tenant_id "tenant-123"
                                      :name "Riverfront Condo"
                                      :status "active"
                                      :settings {}}})
              (assoc :ids ["prop-1"])
              (assoc-in [:metadata :context] {:source :fixture})))))
      (update-in [:entities :transactions]
        (fn [store]
          (let [store (or store {:data {} :ids [] :metadata {}})]
            (-> store
              (assoc :data {txn-id {:id txn-id
                                    :tenant_id "tenant-123"
                                    :description "First rent payment"
                                    :amount 1200.5
                                    :tags []}})
              (assoc :ids [txn-id])
              (assoc-in [:metadata :stats] {:total 1})))))
      (assoc-in [:ui :entity-configs :properties :visible-columns] {:name true :status true})
      (assoc-in [:ui :lists :properties :selected-ids] #{"prop-1"}))))

(defn throws-ex-info?
  [f]
  (try
    (f)
    false
    (catch cljs.core.ExceptionInfo _
      true)))

(deftest models-data-normalization-test
  (testing "map inputs are returned unchanged"
    (is (= simple-models-data (template-db/models-data->map simple-models-data))))

  (testing "vector inputs are converted to a map of string keys"
    (let [vector-result (template-db/models-data->map simple-models-data-vector)]
      (is (= #{"users" "properties"} (set (keys vector-result))))
      (is (= (:fields (:users simple-models-data))
            (:fields (get vector-result "users"))))))

  (testing "seq inputs are converted to a map"
    (let [seq-result (template-db/models-data->map (seq simple-models-data-vector))]
      (is (= #{"users" "properties"} (set (keys seq-result)))))))

(deftest validate-db-with-simple-models-test
  (testing "dynamic schema validates runtime state with keyword models-data"
    (let [db (build-sample-db simple-models-data)]
      (is (= db (template-db/validate-db db simple-models-data)))))

  (testing "dynamic schema validates runtime state with string-key models-data"
    (let [db (build-sample-db simple-models-data-vector)]
      (is (= db (template-db/validate-db db simple-models-data-vector))))))

(deftest validate-db-with-complex-models-test
  (testing "dynamic schema handles complex models-data map"
    (let [db (build-complex-db complex-models-data)]
      (is (= db (template-db/validate-db db complex-models-data)))))

  (testing "dynamic schema handles complex models-data vector"
    (let [db (build-complex-db complex-models-data-vector)]
      (is (= db (template-db/validate-db db complex-models-data-vector))))))

(deftest dynamic-schema-rejects-invalid-entity-shape-test
  (testing "invalid ids collection is rejected"
    (let [db (build-sample-db simple-models-data)
          invalid-db (assoc-in db [:entities :users :ids] {:oops true})]
      (is (throws-ex-info? #(template-db/validate-db invalid-db simple-models-data)))))

  (testing "invalid form waiting state is rejected"
    (let [db (build-sample-db simple-models-data)
          invalid-db (assoc-in db [:forms :users :waiting] {:field true})]
      (is (throws-ex-info? #(template-db/validate-db invalid-db simple-models-data))))))

(deftest current-route-test
  (testing "current-route returns correct path vector"
    (is (= [:current-route] (paths/current-route))))

  (testing "current-route path structure"
    (let [path (paths/current-route)]
      (is (vector? path))
      (is (= 1 (count path)))
      (is (keyword? (first path))))))

(deftest current-page-test
  (testing "current-page returns correct path vector"
    (is (= [:ui :current-page] (paths/current-page))))

  (testing "current-page path structure"
    (let [path (paths/current-page)]
      (is (vector? path))
      (is (= 2 (count path)))
      (is (every? keyword? path)))))

(deftest entity-paths-test
  (testing "entity-data path generation"
    (are [entity-type expected] (= expected (paths/entity-data entity-type))
      :transactions [:entities :transactions :data]
      :items [:entities :items :data]
      :transaction-types [:entities :transaction-types :data]))

  (testing "entity-ids path generation"
    (are [entity-type expected] (= expected (paths/entity-ids entity-type))
      :transactions [:entities :transactions :ids]
      :items [:entities :items :ids]
      :transaction-types [:entities :transaction-types :ids]))

  (testing "entity-metadata path generation"
    (are [entity-type expected] (= expected (paths/entity-metadata entity-type))
      :transactions [:entities :transactions :metadata]
      :items [:entities :items :metadata]
      :transaction-types [:entities :transaction-types :metadata]))

  (testing "entity-loading? path generation"
    (are [entity-type expected] (= expected (paths/entity-loading? entity-type))
      :transactions [:entities :transactions :metadata :loading?]
      :items [:entities :items :metadata :loading?]
      :transaction-types [:entities :transaction-types :metadata :loading?]))

  (testing "entity-error path generation"
    (are [entity-type expected] (= expected (paths/entity-error entity-type))
      :transactions [:entities :transactions :metadata :error]
      :items [:entities :items :metadata :error]
      :transaction-types [:entities :transaction-types :metadata :error]))

  (testing "entity-last-updated path generation"
    (are [entity-type expected] (= expected (paths/entity-last-updated entity-type))
      :transactions [:entities :transactions :metadata :last-updated]
      :items [:entities :items :metadata :last-updated]
      :transaction-types [:entities :transaction-types :metadata :last-updated]))

  (testing "entity-success path generation"
    (are [entity-type expected] (= expected (paths/entity-success entity-type))
      :transactions [:entities :transactions :metadata :success]
      :items [:entities :items :metadata :success]
      :transaction-types [:entities :transaction-types :metadata :success])))

(deftest form-paths-test
  (testing "form-data path generation"
    (are [entity-type expected] (= expected (paths/form-data entity-type))
      :transactions [:forms :transactions :data]
      :items [:forms :items :data]
      :transaction-types [:forms :transaction-types :data]))

  (testing "form-field path generation"
    (are [entity-type field expected] (= expected (paths/form-field entity-type field))
      :transactions :amount [:forms :transactions :data :amount]
      :items :description [:forms :items :data :description]
      :transaction-types :name [:forms :transaction-types :data :name]))

  (testing "form-errors path generation"
    (are [entity-type expected] (= expected (paths/form-errors entity-type))
      :transactions [:forms :transactions :errors]
      :items [:forms :items :errors]
      :transaction-types [:forms :transaction-types :errors]))

  (testing "form-field-error path generation"
    (are [entity-type field expected] (= expected (paths/form-field-error entity-type field))
      :transactions :amount [:forms :transactions :errors :amount]
      :items :description [:forms :items :errors :description]
      :transaction-types :name [:forms :transaction-types :errors :name]))

  (testing "form-submitting? path generation"
    (are [entity-type expected] (= expected (paths/form-submitting? entity-type))
      :transactions [:forms :transactions :submitting?]
      :items [:forms :items :submitting?]
      :transaction-types [:forms :transaction-types :submitting?]))

  (testing "form-submitted? path generation"
    (are [entity-type expected] (= expected (paths/form-submitted? entity-type))
      :transactions [:forms :transactions :submitted?]
      :items [:forms :items :submitted?]
      :transaction-types [:forms :transaction-types :submitted?]))

  (testing "form-dirty-fields path generation"
    (are [entity-type expected] (= expected (paths/form-dirty-fields entity-type))
      :transactions [:forms :transactions :dirty-fields]
      :items [:forms :items :dirty-fields]
      :transaction-types [:forms :transaction-types :dirty-fields]))

  (testing "form-server-errors paths"
    (are [entity-type expected] (= expected (paths/form-server-errors-all entity-type))
      :transactions [:forms :transactions :server-errors]
      :items [:forms :items :server-errors])

    (are [entity-type field expected] (= expected (paths/form-server-errors entity-type field))
      :transactions :amount [:forms :transactions :server-errors :amount]
      :items :description [:forms :items :server-errors :description]))

  (testing "form-success paths"
    (are [entity-type expected] (= expected (paths/form-success-all entity-type))
      :transactions [:forms :transactions :success]
      :items [:forms :items :success])

    (are [entity-type field expected] (= expected (paths/form-success entity-type field))
      :transactions :amount [:forms :transactions :success :amount]
      :items :description [:forms :items :success :description]))

  (testing "form-waiting path generation"
    (are [entity-type expected] (= expected (paths/form-waiting entity-type))
      :transactions [:forms :transactions :waiting]
      :items [:forms :items :waiting]
      :transaction-types [:forms :transaction-types :waiting])))

(deftest list-ui-paths-test
  (testing "list-ui-state path generation"
    (are [entity-type expected] (= expected (paths/list-ui-state entity-type))
      :transactions [:ui :lists :transactions]
      :items [:ui :lists :items]
      :transaction-types [:ui :lists :transaction-types]))

  (testing "list-sort-config path generation"
    (are [entity-type expected] (= expected (paths/list-sort-config entity-type))
      :transactions [:ui :lists :transactions :sort]
      :items [:ui :lists :items :sort]
      :transaction-types [:ui :lists :transaction-types :sort]))

  (testing "list-current-page path generation"
    (are [entity-type expected] (= expected (paths/list-current-page entity-type))
      :transactions [:ui :lists :transactions :pagination :current-page]
      :items [:ui :lists :items :pagination :current-page]
      :transaction-types [:ui :lists :transaction-types :pagination :current-page]))

  (testing "list-total-items path generation"
    (are [entity-type expected] (= expected (paths/list-total-items entity-type))
      :transactions [:ui :lists :transactions :total-items]
      :items [:ui :lists :items :total-items]
      :transaction-types [:ui :lists :transaction-types :total-items]))

  (testing "list-per-page path generation"
    (are [entity-type expected] (= expected (paths/list-per-page entity-type))
      :transactions [:ui :lists :transactions :per-page]
      :items [:ui :lists :items :per-page]
      :transaction-types [:ui :lists :transaction-types :per-page]))

  (testing "entity-selected-ids path generation"
    (are [entity-type expected] (= expected (paths/entity-selected-ids entity-type))
      :transactions [:ui :lists :transactions :selected-ids]
      :items [:ui :lists :items :selected-ids]
      :transaction-types [:ui :lists :transaction-types :selected-ids]))

  (testing "entity-display-settings path generation"
    (are [entity-name expected] (= expected (paths/entity-display-settings entity-name))
      :transactions [:ui :entity-configs :transactions]
      :items [:ui :entity-configs :items]
      :transaction-types [:ui :entity-configs :transaction-types])))

(deftest path-consistency-test
  (testing "All paths follow consistent naming convention"
    (let [entity-types [:transactions :items :transaction-types]]
      (doseq [entity-type entity-types]
        (testing (str "Paths for " entity-type " are consistent")
          ;; Entity paths
          (is (= [:entities entity-type] (take 2 (paths/entity-data entity-type))))
          (is (= [:entities entity-type] (take 2 (paths/entity-ids entity-type))))
          (is (= [:entities entity-type] (take 2 (paths/entity-metadata entity-type))))

          ;; Form paths
          (is (= [:forms entity-type] (take 2 (paths/form-data entity-type))))
          (is (= [:forms entity-type] (take 2 (paths/form-errors entity-type))))

          ;; List UI paths
          (is (= [:ui :lists entity-type] (paths/list-ui-state entity-type))))))))

(deftest path-usage-patterns-test
  (testing "Path functions can be composed with get-in"
    (let [sample-db {:entities {:transactions {:data {1 {:id 1 :amount 100}
                                                      2 {:id 2 :amount 200}}
                                               :ids [1 2]
                                               :metadata {:loading? false}}}
                     :forms {:transactions {:data {:amount 150}
                                            :errors {:amount "Must be positive"}}}
                     :ui {:lists {:transactions {:pagination {:current-page 1}
                                                 :per-page 10}}}}]

      ;; Test entity data access
      (is (= {1 {:id 1 :amount 100}
              2 {:id 2 :amount 200}}
            (get-in sample-db (paths/entity-data :transactions))))

      ;; Test form field access
      (is (= 150 (get-in sample-db (paths/form-field :transactions :amount))))

      ;; Test error access
      (is (= "Must be positive" (get-in sample-db (paths/form-field-error :transactions :amount))))

      ;; Test list UI access
      (is (= 1 (get-in sample-db (paths/list-current-page :transactions)))))))

(defn run-all-tests []
  (helpers/log-test-start "DB Path Tests")
  (run-tests))

;; Export for browser testing
(set! js/window.runDbTests run-all-tests)

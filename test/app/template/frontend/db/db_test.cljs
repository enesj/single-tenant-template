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
   :suppliers {:fields [[:id :uuid {:primary-key true}]
                        [:display_name [:varchar 255]]
                        [:normalized_key [:varchar 255]]]
               :types []}})

(def simple-models-data-vector
  (mapv (fn [[k v]] [(name k) v]) simple-models-data))

(def complex-models-data auto-data/models)

(def complex-models-data-vector
  (mapv (fn [[k v]] [(name k) v]) complex-models-data))

(def sample-user-form-state
  {:values {:full_name "Ada Lovelace" :role "admin"}
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
  (let [expense-id (keyword "expense" "1")]
    (-> (template-db/make-db-with-models-data template-db/default-db models-data)
      (augment-with-sample-entity-state :users)
      (update-in [:entities :suppliers]
        (fn [store]
          (let [store (or store {:data {} :ids [] :metadata {}})]
            (-> store
              (assoc :data {"sup-1" {:id "sup-1"
                                     :display_name "Corner Store"
                                     :normalized_key "corner-store"}})
              (assoc :ids ["sup-1"])
              (assoc-in [:metadata :context] {:source :fixture})))))
      (update-in [:entities :expenses]
        (fn [store]
          (let [store (or store {:data {} :ids [] :metadata {}})]
            (-> store
              (assoc :data {expense-id {:id expense-id
                                        :supplier_id "sup-1"
                                        :payer_id "payer-1"
                                        :total_amount 42.50
                                        :notes "Sample expense"}})
              (assoc :ids [expense-id])
              (assoc-in [:metadata :stats] {:total 1})))))
      (assoc-in [:ui :entity-configs :suppliers :visible-columns] {:display_name true :normalized_key true})
      (assoc-in [:ui :lists :suppliers :selected-ids] #{"sup-1"}))))

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
      (is (= #{"users" "suppliers"} (set (keys vector-result))))
      (is (= (:fields (:users simple-models-data))
            (:fields (get vector-result "users"))))))

  (testing "seq inputs are converted to a map"
    (let [seq-result (template-db/models-data->map (seq simple-models-data-vector))]
      (is (= #{"users" "suppliers"} (set (keys seq-result)))))))

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
      :expenses [:entities :expenses :data]
      :suppliers [:entities :suppliers :data]
      :receipts [:entities :receipts :data]))

  (testing "entity-ids path generation"
    (are [entity-type expected] (= expected (paths/entity-ids entity-type))
      :expenses [:entities :expenses :ids]
      :suppliers [:entities :suppliers :ids]
      :receipts [:entities :receipts :ids]))

  (testing "entity-metadata path generation"
    (are [entity-type expected] (= expected (paths/entity-metadata entity-type))
      :expenses [:entities :expenses :metadata]
      :suppliers [:entities :suppliers :metadata]
      :receipts [:entities :receipts :metadata]))

  (testing "entity-loading? path generation"
    (are [entity-type expected] (= expected (paths/entity-loading? entity-type))
      :expenses [:entities :expenses :metadata :loading?]
      :suppliers [:entities :suppliers :metadata :loading?]
      :receipts [:entities :receipts :metadata :loading?]))

  (testing "entity-error path generation"
    (are [entity-type expected] (= expected (paths/entity-error entity-type))
      :expenses [:entities :expenses :metadata :error]
      :suppliers [:entities :suppliers :metadata :error]
      :receipts [:entities :receipts :metadata :error]))

  (testing "entity-last-updated path generation"
    (are [entity-type expected] (= expected (paths/entity-last-updated entity-type))
      :expenses [:entities :expenses :metadata :last-updated]
      :suppliers [:entities :suppliers :metadata :last-updated]
      :receipts [:entities :receipts :metadata :last-updated]))

  (testing "entity-success path generation"
    (are [entity-type expected] (= expected (paths/entity-success entity-type))
      :expenses [:entities :expenses :metadata :success]
      :suppliers [:entities :suppliers :metadata :success]
      :receipts [:entities :receipts :metadata :success])))

(deftest form-paths-test
  (testing "form-data path generation"
    (are [entity-type expected] (= expected (paths/form-data entity-type))
      :expenses [:forms :expenses :values]
      :suppliers [:forms :suppliers :values]
      :receipts [:forms :receipts :values]))

  (testing "form-field path generation"
    (are [entity-type field expected] (= expected (paths/form-field entity-type field))
      :expenses :total_amount [:forms :expenses :values :total_amount]
      :suppliers :display_name [:forms :suppliers :values :display_name]
      :receipts :status [:forms :receipts :values :status]))

  (testing "form-errors path generation"
    (are [entity-type expected] (= expected (paths/form-errors entity-type))
      :expenses [:forms :expenses :errors]
      :suppliers [:forms :suppliers :errors]
      :receipts [:forms :receipts :errors]))

  (testing "form-field-error path generation"
    (are [entity-type field expected] (= expected (paths/form-field-error entity-type field))
      :expenses :total_amount [:forms :expenses :errors :total_amount]
      :suppliers :display_name [:forms :suppliers :errors :display_name]
      :receipts :status [:forms :receipts :errors :status]))

  (testing "form-submitting? path generation"
    (are [entity-type expected] (= expected (paths/form-submitting? entity-type))
      :expenses [:forms :expenses :submitting?]
      :suppliers [:forms :suppliers :submitting?]
      :receipts [:forms :receipts :submitting?]))

  (testing "form-submitted? path generation"
    (are [entity-type expected] (= expected (paths/form-submitted? entity-type))
      :expenses [:forms :expenses :submitted?]
      :suppliers [:forms :suppliers :submitted?]
      :receipts [:forms :receipts :submitted?]))

  (testing "form-dirty-fields path generation"
    (are [entity-type expected] (= expected (paths/form-dirty-fields entity-type))
      :expenses [:forms :expenses :dirty-fields]
      :suppliers [:forms :suppliers :dirty-fields]
      :receipts [:forms :receipts :dirty-fields]))

  (testing "form-server-errors paths"
    (are [entity-type expected] (= expected (paths/form-server-errors-all entity-type))
      :expenses [:forms :expenses :server-errors]
      :suppliers [:forms :suppliers :server-errors])

    (are [entity-type field expected] (= expected (paths/form-server-errors entity-type field))
      :expenses :total_amount [:forms :expenses :server-errors :total_amount]
      :suppliers :display_name [:forms :suppliers :server-errors :display_name]))

  (testing "form-success paths"
    (are [entity-type expected] (= expected (paths/form-success-all entity-type))
      :expenses [:forms :expenses :success]
      :suppliers [:forms :suppliers :success])

    (are [entity-type field expected] (= expected (paths/form-success entity-type field))
      :expenses :total_amount [:forms :expenses :success :total_amount]
      :suppliers :display_name [:forms :suppliers :success :display_name]))

  (testing "form-waiting path generation"
    (are [entity-type expected] (= expected (paths/form-waiting entity-type))
      :expenses [:forms :expenses :waiting]
      :suppliers [:forms :suppliers :waiting]
      :receipts [:forms :receipts :waiting])))

(deftest list-ui-paths-test
  (testing "list-ui-state path generation"
    (are [entity-type expected] (= expected (paths/list-ui-state entity-type))
      :expenses [:ui :lists :expenses]
      :suppliers [:ui :lists :suppliers]
      :receipts [:ui :lists :receipts]))

  (testing "list-sort-config path generation"
    (are [entity-type expected] (= expected (paths/list-sort-config entity-type))
      :expenses [:ui :lists :expenses :sort]
      :suppliers [:ui :lists :suppliers :sort]
      :receipts [:ui :lists :receipts :sort]))

  (testing "list-current-page path generation"
    (are [entity-type expected] (= expected (paths/list-current-page entity-type))
      :expenses [:ui :lists :expenses :current-page]
      :suppliers [:ui :lists :suppliers :current-page]
      :receipts [:ui :lists :receipts :current-page]))

  (testing "list-total-items path generation"
    (are [entity-type expected] (= expected (paths/list-total-items entity-type))
      :expenses [:ui :lists :expenses :total-items]
      :suppliers [:ui :lists :suppliers :total-items]
      :receipts [:ui :lists :receipts :total-items]))

  (testing "list-per-page path generation"
    (are [entity-type expected] (= expected (paths/list-per-page entity-type))
      :expenses [:ui :lists :expenses :per-page]
      :suppliers [:ui :lists :suppliers :per-page]
      :receipts [:ui :lists :receipts :per-page]))

  (testing "entity-selected-ids path generation"
    (are [entity-type expected] (= expected (paths/entity-selected-ids entity-type))
      :expenses [:ui :lists :expenses :selected-ids]
      :suppliers [:ui :lists :suppliers :selected-ids]
      :receipts [:ui :lists :receipts :selected-ids]))

  (testing "entity-display-settings path generation"
    (are [entity-name expected] (= expected (paths/entity-display-settings entity-name))
      :expenses [:ui :entity-configs :expenses]
      :suppliers [:ui :entity-configs :suppliers]
      :receipts [:ui :entity-configs :receipts]))

  (testing "entity-prefs-key scopes admin routes separately"
    (is (= :expenses
          (paths/entity-prefs-key {:current-route {:data {:name :expenses/list}}} :expenses)))
    (is (= :admin/expenses
          (paths/entity-prefs-key {:current-route {:data {:name :admin/expenses}}} :expenses)))))

(deftest path-consistency-test
  (testing "All paths follow consistent naming convention"
    (let [entity-types [:expenses :suppliers :receipts]]
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
    (let [sample-db {:entities {:expenses {:data {1 {:id 1 :total_amount 100}
                                                  2 {:id 2 :total_amount 200}}
                                           :ids [1 2]
                                           :metadata {:loading? false}}}
                     :forms {:expenses {:values {:total_amount 150}
                                        :errors {:total_amount "Must be positive"}}}
                     :ui {:lists {:expenses {:current-page 1
                                             :pagination {:current-page 1}
                                             :per-page 10}}}}]

      ;; Test entity data access
      (is (= {1 {:id 1 :total_amount 100}
              2 {:id 2 :total_amount 200}}
            (get-in sample-db (paths/entity-data :expenses))))

      ;; Test form field access
      (is (= 150 (get-in sample-db (paths/form-field :expenses :total_amount))))

      ;; Test error access
      (is (= "Must be positive" (get-in sample-db (paths/form-field-error :expenses :total_amount))))

      ;; Test list UI access
      (is (= 1 (get-in sample-db (paths/list-current-page :expenses)))))))

(defn run-all-tests []
  (helpers/log-test-start "DB Path Tests")
  (run-tests))

;; Export for browser testing
(set! js/window.runDbTests run-all-tests)

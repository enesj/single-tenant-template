(ns app.template.frontend.events.form-events-test
  "Tests for template form event handlers"
  (:require
    [app.template.frontend.api.http :as http-api]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is testing]]
    [day8.re-frame.http-fx :as http-fx]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defonce form-test-events-registered
  (do
    (rf/reg-event-db
      ::test-initialize-db
      (fn [_ _]
        helpers/valid-test-db-state))
    true))

(defn- reset-db!
  []
  (reset! rf-db/app-db {})
  (rf/dispatch-sync [::test-initialize-db]))

(deftest submit-form-tests
  (testing "Create submission marks form as submitting and calls create-entity"
    (reset-db!)
    (let [captured (atom nil)]
      (with-redefs [http-api/create-entity (fn [config] (reset! captured config) ::create-request)
                    http-api/update-entity (fn [_] (throw (ex-info "should not hit" {})))
                    http-fx/http-effect (fn [_])]
        (rf/dispatch-sync [::form-events/submit-form
                           {:entity-name :items
                            :values {:name "Test"}
                            :editing false}])
        (is (true? (get-in @rf-db/app-db (paths/form-submitting? :items)))
          "Submitting? flag should be true during request")
        (is (= "items" (:entity-name @captured)) "Should target correct entity")
        (is (= {:name "Test"} (:data @captured)) "Should send full values for create")))

    (testing "Update submission strips :id from payload"
      (reset-db!)
      (let [captured (atom nil)]
        (with-redefs [http-api/update-entity (fn [config] (reset! captured config) ::update-request)
                      http-api/create-entity (fn [_] (throw (ex-info "should not hit" {})))
                      http-fx/http-effect (fn [_])]
          (rf/dispatch-sync [::form-events/submit-form
                             {:entity-name :items
                              :values {:id 55 :name "Updated"}
                              :editing true}])
          (is (true? (get-in @rf-db/app-db (paths/form-submitting? :items))))
          (is (= 55 (:id @captured)) "Should include id in path params")
          (is (= {:name "Updated"} (:data @captured)) "Should drop :id from request data"))))))

(deftest create-success-test
  (testing "Create success clears submitting state and tracks recently created"
    (reset-db!)
    (swap! rf-db/app-db assoc-in [:forms :items] {:submitting? true})
    (rf/dispatch-sync [::form-events/create-success :items {:id 101 :name "Created"}])
    (let [db @rf-db/app-db]
      (is (false? (get-in db (paths/form-submitting? :items))) "Submitting? should be false")
      (is (= #{101} (get-in db [:ui :recently-created :items])) "Should record recently created id")
      (is (= true (get-in db [:forms :items :success])) "Form success flag set"))))

(deftest update-success-test
  (testing "Update success tracks recently updated ids"
    (reset-db!)
    (swap! rf-db/app-db assoc-in [:forms :items] {:submitting? true})
    (rf/dispatch-sync [::form-events/update-success :items {:id "abc"}])
    (let [db @rf-db/app-db]
      (is (false? (get-in db (paths/form-submitting? :items))))
      (is (= #{"abc"} (get-in db [:ui :recently-updated :items])))
      (is (true? (get-in db [:forms :items :success])))
      (is (nil? (get-in db [:forms :items :errors]))))))

(deftest failure-tests
  (testing "Create failure surfaces error message and clears submitting"
    (reset-db!)
    (swap! rf-db/app-db assoc-in [:forms :items] {:submitting? true})
    (rf/dispatch-sync [::form-events/create-failure :items {:error "Boom"}])
    (let [db @rf-db/app-db]
      (is (false? (get-in db (paths/form-submitting? :items))))
      (is (= "Boom" (get-in db [:forms :items :errors :form :message]))))

    (testing "Update failure falls back to response error"
      (reset-db!)
      (swap! rf-db/app-db assoc-in [:forms :items] {:submitting? true})
      (rf/dispatch-sync [::form-events/update-failure :items {:response {:error "Server"}}])
      (is (= "Server" (get-in @rf-db/app-db [:forms :items :errors :form :message]))))))

(deftest server-validation-tests
  (testing "set-server-field-error invokes validation endpoint and marks waiting"
    (reset-db!)
    (let [captured (atom nil)]
      (with-redefs [http-api/api-request (fn [config] (reset! captured config) ::validation)
                    http-fx/http-effect (fn [_])]
        (rf/dispatch-sync [::form-events/set-server-field-error
                           {:entity-name :items
                            :dirty {:name "Value"}}])
        (is (= #{:name} (get-in @rf-db/app-db (paths/form-waiting :items)))
          "Waiting set should include dirty field")
        (is (= :post (:method @captured)))
        (is (= "/api/v1/entities/items/validate" (:uri @captured)))
        (is (= {:name "Value"} (:params @captured))))))

  (testing "Server validation success clears waiting and errors"
    (reset-db!)
    (swap! rf-db/app-db assoc-in (paths/form-waiting :items) #{:name})
    (swap! rf-db/app-db assoc-in (paths/form-server-errors :items :name) {:message "old"})
    (rf/dispatch-sync [::form-events/set-server-field-error-success :items :name {:valid? true}])
    (let [db @rf-db/app-db]
      (is (nil? (get-in db (paths/form-waiting :items))))
      (is (nil? (get-in db (paths/form-server-errors :items :name))))
      (is (true? (get-in db (paths/form-success :items :name))))))

  (testing "Server validation failure preserves waiting minus processed field"
    (reset-db!)
    (swap! rf-db/app-db assoc-in (paths/form-waiting :items) #{:name :other})
    (rf/dispatch-sync [::form-events/set-server-field-error-success :items :name
                       {:valid? false :error "Invalid" :color "#f00" :other-field :foo}])
    (let [db @rf-db/app-db]
      (is (= #{:other} (get-in db (paths/form-waiting :items))))
      (is (= {:message "Invalid" :color "#f00"}
            (get-in db (paths/form-server-errors :items :name))))
      (is (false? (get-in db (paths/form-success :items :name))))))

  (testing "Server validation failure event clears waiting set"
    (reset-db!)
    (swap! rf-db/app-db assoc-in (paths/form-waiting :items) #{:name})
    (rf/dispatch-sync [::form-events/set-server-field-error-failure :items nil])
    (is (= #{} (get-in @rf-db/app-db (paths/form-waiting :items))))))

(deftest dirty-and-cancel-tests
  (testing "set-dirty-fields merges into existing set"
    (reset-db!)
    (swap! rf-db/app-db assoc-in (paths/form-dirty-fields :items) #{:name})
    (rf/dispatch-sync [::form-events/set-dirty-fields :items [:email :name]])
    (is (= #{:name :email} (get-in @rf-db/app-db (paths/form-dirty-fields :items))))

    (testing "cancel-form clears values and editing when not batch editing"
      (reset-db!)
      (swap! rf-db/app-db assoc-in [:forms :items] {:values {:name "X"}
                                                    :server-errors {:name "err"}})
      (swap! rf-db/app-db assoc-in [:ui :editing] 10)
      (rf/dispatch-sync [::form-events/cancel-form :items])
      (let [db @rf-db/app-db]
        (is (= {} (get-in db [:forms :items :values])))
        (is (nil? (get-in db [:forms :items :server-errors])))
        (is (nil? (get-in db [:ui :editing])))))

    (testing "clear-form resets success and errors"
      (reset-db!)
      (swap! rf-db/app-db assoc-in [:forms :items]
        {:values {:name "Keep"} :errors {:form {:message "bad"}} :success true})
      (rf/dispatch-sync [::form-events/clear-form :items])
      (is (= {} (get-in @rf-db/app-db [:forms :items :values])))
      (is (nil? (get-in @rf-db/app-db [:forms :items :errors])))
      (is (false? (get-in @rf-db/app-db [:forms :items :success]))))))

(ns app.template.frontend.components.form.validation-test
  "Tests for form validation logic"
  (:require
    [app.shared.validation.core :as validation-core]
    [app.shared.validation.fork :as validation-fork]
    [app.template.frontend.components.form.validation :as validation]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is run-tests testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

;; Mock parse-field-value tests
(deftest parse-field-value-test
  (testing "Parsing number input type"
    (is (= 123.45 (#'validation/parse-field-value "number" "123.45")))
    (is (= 42 (#'validation/parse-field-value "number" "42")))
    (is (= -10.5 (#'validation/parse-field-value "number" "-10.5")))
    (is (nil? (#'validation/parse-field-value "number" "")))
    (is (nil? (#'validation/parse-field-value "number" "   ")))
    (is (js/isNaN (#'validation/parse-field-value "number" "not-a-number"))))

  (testing "Parsing object input type"
    (is (= {:key "value"} (#'validation/parse-field-value "object" "{:key \"value\"}")))
    (is (= [1 2 3] (#'validation/parse-field-value "object" "[1 2 3]")))
    ;; When parsing fails, it returns the symbol 'invalid-edn, not the string
    (is (= 'invalid-edn (#'validation/parse-field-value "object" "invalid-edn")))
    (is (= "{malformed" (#'validation/parse-field-value "object" "{malformed"))))

  (testing "Parsing other input types (default)"
    (is (= "text value" (#'validation/parse-field-value "text" "text value")))
    (is (= "123" (#'validation/parse-field-value "text" "123")))
    (is (= "" (#'validation/parse-field-value "text" "")))
    (is (= "special chars !@#" (#'validation/parse-field-value "email" "special chars !@#")))))

;; Test for set-handle-value-change function
(deftest set-handle-value-change-test
  (testing "Handle value change function creation"
    (let [field {:id "amount"
                 :input-type "number"}
          form-props {:set-handle-change (fn [{:keys [value path]}]
                                           {:value value :path path})
                      :entity-name :transactions
                      :state (atom {:initial-values {:amount 100}})
                      :editing true}
          handler (validation/set-handle-value-change field form-props nil)]

      (testing "Handler is created when set-handle-change is provided"
        (is (fn? handler)))

      (testing "Handler returns nil when set-handle-change is not provided"
        (let [props-without-handler (dissoc form-props :set-handle-change)
              nil-handler (validation/set-handle-value-change field props-without-handler nil)]
          (is (nil? nil-handler))))))

  (testing "Value parsing in handler"
    (let [parsed-values (atom [])
          field {:id "test-field"
                 :input-type "number"}
          form-props {:set-handle-change (fn [{:keys [value path]}]
                                           (swap! parsed-values conj value))
                      :entity-name :test-entity
                      :state (atom {:initial-values {:test-field nil}})
                      :editing true}
          handler (validation/set-handle-value-change field form-props nil)]

      ;; Mock event target
      (let [mock-event #js {:target #js {:value "123.45"}}]
        (handler mock-event)
        (is (= [123.45] @parsed-values)))

      ;; Direct value (not event)
      (handler "67.89")
      (is (= [123.45 67.89] @parsed-values)))))

;; Test helper functions
(deftest get-initial-value-test
  (testing "Getting initial value from state"
    ;; Test with atom state
    (let [state (atom {:initial-values {:field1 "value1"
                                        :field2 42}})]
      (is (= "value1" (#'validation/get-initial-value state :field1)))
      (is (= 42 (#'validation/get-initial-value state :field2)))
      (is (nil? (#'validation/get-initial-value state :non-existent))))

    ;; Test with plain map state
    (let [state {:initial-values {:field1 "value1"
                                  :field2 42}}]
      (is (= "value1" (#'validation/get-initial-value state :field1)))
      (is (= 42 (#'validation/get-initial-value state :field2)))
      (is (nil? (#'validation/get-initial-value state :non-existent))))

    ;; Test with nil state
    (is (nil? (#'validation/get-initial-value nil :field1)))

    ;; Test with nil field-id
    (is (nil? (#'validation/get-initial-value {:initial-values {:field1 "value"}} nil)))))

(deftest get-field-errors-test
  (testing "Getting field errors from errors map"
    (let [errors {:field1 "Error message 1"
                  :field2 ["Error 1" "Error 2"]
                  :field3 {:message "Complex error"
                           :code "ERR001"}}]

      (is (= "Error message 1" (validation/get-field-errors errors :field1)))
      (is (= ["Error 1" "Error 2"] (validation/get-field-errors errors :field2)))
      (is (= {:message "Complex error" :code "ERR001"}
            (validation/get-field-errors errors :field3)))
      (is (nil? (validation/get-field-errors errors :non-existent)))
      (is (nil? (validation/get-field-errors {} :field1)))
      (is (nil? (validation/get-field-errors nil :field1))))))

;; Integration test with re-frame dispatch
(deftest validation-integration-test
  (testing "Integration with re-frame events"
    ;; Mock validation function
    (with-redefs [validation-core/should-validate-field? (constantly true)
                  validation-fork/validate-single-field
                  (fn [_entity-name field-id value]
                    (when (and (= field-id :amount) (< value 0))
                      "Amount must be positive"))]

      (reset! rf-db/app-db {:entities {}
                            :forms {}
                            :ui {:current-page nil
                                 :lists {:transaction-types {}
                                         :transactions {}
                                         :items {}}
                                 :recently-updated {}
                                 :recently-created {}
                                 :show-timestamps? false
                                 :show-edit? true
                                 :show-delete? true
                                 :show-highlights? true
                                 :entity-configs {}}
                            :current-route nil})

      (let [field {:id "amount" :input-type "number"}
            set-handle-change-calls (atom [])
            form-props {:set-handle-change (fn [params]
                                             (swap! set-handle-change-calls conj params))
                        :entity-name :transactions
                        :state (atom {:initial-values {:amount 0}})
                        :editing true
                        :server-validation? false}
            handler (validation/set-handle-value-change field form-props nil)]

        ;; Test valid value
        (handler "100")
        (is (= 1 (count @set-handle-change-calls)))
        (is (= {:value 100 :path :amount} (last @set-handle-change-calls)))

        ;; Test invalid value
        (handler "-50")
        (is (= 2 (count @set-handle-change-calls)))
        (is (= {:value -50 :path :amount} (last @set-handle-change-calls)))))))

(deftest server-validation-test
  (testing "Server validation triggering"
    (let [server-requests (atom [])
          mock-send-request (fn [params callback]
                              (swap! server-requests conj params)
                              ;; Simulate server response
                              (callback))

          field {:id "email"
                 :input-type "email"
                 :validate-server? true}

          form-props {:set-handle-change (fn [_])
                      :send-server-request mock-send-request
                      :entity-name :users
                      :state (atom {:initial-values {:email "old@example.com"}})
                      :editing true
                      :server-validation? true}

          handler (validation/set-handle-value-change field form-props nil)]

      ;; Test that server validation is triggered for changed values
      (with-redefs [validation-core/should-validate-field? (constantly true)
                    validation-fork/validate-single-field (constantly nil)]

        ;; Change value
        (handler "new@example.com")

        ;; Check that server request was made
        (is (= 1 (count @server-requests)))
        (let [request (first @server-requests)]
          (is (= "email" (:name request)))
          (is (= "new@example.com" (:value request)))
          (is (= true (:set-waiting? request)))
          (is (= ["email"] (:clean-on-refetch request)))
          (is (= 500 (:debounce request))))))))

;; Register test events only once to avoid re-frame warnings
;; Event handlers are already registered in the main application - no need to register them in tests

(deftest dirty-fields-tracking-test
  (testing "Dirty fields are tracked correctly"
    (with-redefs [validation-core/should-validate-field? (constantly false)]

      (reset! rf-db/app-db {:entities {}
                            :forms {}
                            :ui {:current-page nil
                                 :lists {:transaction-types {}
                                         :transactions {}
                                         :items {}}
                                 :recently-updated {}
                                 :recently-created {}
                                 :show-timestamps? false
                                 :show-edit? true
                                 :show-delete? true
                                 :show-highlights? true
                                 :entity-configs {}}
                            :current-route nil})

      (let [field {:id "description" :input-type "text"}
            set-dirty-calls (atom [])
            form-props {:set-handle-change (fn [_])
                        :set-dirty (fn [field-id value]
                                     (swap! set-dirty-calls conj [field-id value]))
                        :entity-name :items
                        :state (atom {:initial-values {:description "Original"}})
                        :editing true
                        :dirty {:amount 100}}               ; Existing dirty fields
            handler (validation/set-handle-value-change field form-props nil)]

        ;; Change value
        (handler "Modified description")

        ;; Check set-dirty was called
        (is (= 1 (count @set-dirty-calls)))
        (is (= [:description "Modified description"] (first @set-dirty-calls)))

        ;; Also dispatch the event for existing dirty fields
        (rf/dispatch-sync [::form-events/set-dirty-fields :items (keys {:amount 100})])

        ;; Check re-frame event was dispatched for existing dirty fields
        (is (= #{:amount} (get-in @rf-db/app-db [:forms :items :dirty-fields])))))))
(defn run-all-tests []
  (helpers/log-test-start "Form Validation Tests")
  (run-tests))

;; Export for browser testing
(set! js/window.runFormValidationTests run-all-tests)

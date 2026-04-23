(ns app.domain.backend.expenses.routes.routes-factory-test
  (:require
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.template.backend.routes.admin.utils :as utils]
    [clojure.test :refer [deftest is testing]]))

(defn- build-test-handler
  [config]
  ((factory/build-list-handler (merge {:service 'test.expenses.service
                                       :entity-plural :suppliers
                                       :default-limit 50
                                       :default-order-by "display_name"
                                       :default-order-dir "asc"
                                       :has-count? false}
                                 config))
   :db))

(deftest build-list-handler-forwards-canonical-sort-param
  (testing "generic admin list handlers honor the canonical sort query param"
    (let [captured-opts (atom nil)
          handler (build-test-handler {})]
      (with-redefs-fn {#'app.domain.backend.expenses.routes.routes-factory/resolve-service-op-fn
                       (fn [_service _legacy-sym service-op]
                         (is (= :list service-op))
                         (fn [_db opts]
                           (reset! captured-opts opts)
                           [{:id "supplier-1"}]))
                       #'factory/to-app identity
                       #'utils/success-response (fn [body & _]
                                                  {:status 200
                                                   :body body})}
        #(let [response (handler {:query-params {"limit" "25"
                                                 "offset" "50"
                                                 "sort" "display-name:asc,created-at:desc"}})]
           (is (= 200 (:status response)))
           (is (= {:suppliers [{:id "supplier-1"}]}
                 (:body response)))
           (is (= {:limit 25
                   :offset 50
                   :sorts [{:field :display-name :direction :asc}
                           {:field :created-at :direction :desc}]
                   :order-by :display-name
                   :order-dir :asc}
                 @captured-opts)))))))

(deftest build-list-handler-falls-back-to-default-sort-when-canonical-sort-invalid
  (testing "invalid canonical sort input falls back to configured default ordering"
    (let [captured-opts (atom nil)
          handler (build-test-handler {:default-order-by "created_at"
                                       :default-order-dir :desc})]
      (with-redefs-fn {#'app.domain.backend.expenses.routes.routes-factory/resolve-service-op-fn
                       (fn [_service _legacy-sym service-op]
                         (is (= :list service-op))
                         (fn [_db opts]
                           (reset! captured-opts opts)
                           []))
                       #'factory/to-app identity
                       #'utils/success-response (fn [body & _]
                                                  {:status 200
                                                   :body body})}
        #(let [response (handler {:query-params {"sort" "display-name:sideways"}})]
           (is (= 200 (:status response)))
           (is (= {:suppliers []}
                 (:body response)))
           (is (= {:limit 50
                   :offset 0
                   :order-by :created-at
                   :order-dir :desc}
                 @captured-opts)))))))
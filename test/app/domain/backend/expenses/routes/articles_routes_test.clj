(ns app.domain.backend.expenses.routes.articles-routes-test
  (:require
    [app.domain.backend.expenses.routes.articles :as articles-routes]
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.services.article-aliases :as aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.template.backend.routes.admin.utils :as utils]
    [clojure.test :refer [deftest is testing]]))

(defn- unmapped-aliases-handler
  []
  (->> (rest (articles-routes/routes :mock-db))
    (some (fn [route]
            (when (= "/unmapped-aliases" (first route))
              (get-in route [1 :get]))))))

(defn- batch-create-aliases-handler
  []
  (->> (rest (articles-routes/routes :mock-db))
    (some (fn [route]
            (when (= "/:id/aliases" (first route))
              (get-in route [1 :post]))))))

(deftest unmapped-aliases-admin-route-includes-pagination-envelope
  (testing "admin unmapped aliases route returns rows plus total/limit/offset"
    (let [list-opts (atom nil)
          count-opts (atom nil)
          handler (unmapped-aliases-handler)]
      (with-redefs [aliases/list-unmapped-aliases (fn [_db opts]
                                                    (reset! list-opts opts)
                                                    [{:id "alias-1"}])
                    aliases/count-unmapped-aliases (fn [_db opts]
                                                     (reset! count-opts opts)
                                                     193)
                    factory/to-app identity
                    utils/success-response (fn [body & [_status]]
                                             {:status 200
                                              :body body})]
        (let [response (handler {:query-params {"limit" "25"
                                                "offset" "50"}})]
          (is (= 200 (:status response)))
          (is (= {:unmapped-aliases [{:id "alias-1"}]
                  :total 193
                  :limit 25
                  :offset 50}
                (:body response)))
          (is (= {:limit 25 :offset 50} @list-opts))
          (is (= @list-opts @count-opts)))))))

(deftest unmapped-aliases-admin-route-forwards-sort-and-filter-params
  (testing "admin unmapped aliases route forwards supported sort and filter params"
    (let [list-opts (atom nil)
          count-opts (atom nil)
          handler (unmapped-aliases-handler)]
      (with-redefs [aliases/list-unmapped-aliases (fn [_db opts]
                                                    (reset! list-opts opts)
                                                    [])
                    aliases/count-unmapped-aliases (fn [_db opts]
                                                     (reset! count-opts opts)
                                                     0)
                    factory/to-app identity
                    utils/success-response (fn [body & [_status]]
                                             {:status 200
                                              :body body})]
        (let [response (handler {:query-params {"limit" "25"
                                                "offset" "50"
                                                "order-by" "raw-label"
                                                "order-dir" "asc"
                                                "supplier-name" "Acme"
                                                "raw-label" "Tea"
                                                "raw-label-normalized" "tea"
                                                "unit" "kg"
                                                "occurrence-count-min" "2"
                                                "occurrence-count-max" "5"}})]
          (is (= 200 (:status response)))
          (is (= {:limit 25
                  :offset 50
                  :order-by :raw-label
                  :order-dir :asc
                  :sorts [{:field :raw-label
                           :direction :asc}]
                  :supplier-name "Acme"
                  :raw-label "Tea"
                  :raw-label-normalized "tea"
                  :unit "kg"
                  :occurrence-count-min 2
                  :occurrence-count-max 5}
                @list-opts))
          (is (= @list-opts @count-opts)))))))

(deftest batch-create-aliases-admin-route-forwards-unit
  (testing "admin batch create aliases route forwards unit in the request body"
    (let [captured (atom nil)
          handler (batch-create-aliases-handler)
          article-id (str (java.util.UUID/randomUUID))
          supplier-id (str (java.util.UUID/randomUUID))]
      (with-redefs [articles/batch-create-aliases!
                    (fn [_db opts]
                      (reset! captured opts)
                      {:created [] :skipped [] :conflicts [] :reassigned []})
                    factory/read-json-body (fn [req] (:body-params req))
                    factory/to-app identity
                    utils/success-response (fn [body & [_status]]
                                             {:status 200
                                              :body body})]
        (let [response (handler {:path-params {:id article-id}
                                 :body-params {:supplier-id supplier-id
                                               :raw-labels ["JABUKE" "BANANE"]
                                               :unit "kg"
                                               :allow-reassign? true}})]
          (is (= 200 (:status response)))
          (is (= {:supplier-id (java.util.UUID/fromString supplier-id)
                  :article-id (java.util.UUID/fromString article-id)
                  :raw-labels ["JABUKE" "BANANE"]
                  :unit "kg"
                  :allow-reassign? true}
                @captured)))))))

(ns app.domain.backend.expenses.handlers.search-test
  (:require
    [app.domain.backend.expenses.handlers.search :as search]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [clojure.test :refer [deftest is testing]]))

(deftest admin-related-handler-uses-global-scope
  (let [db :mock-db
        entity-id (java.util.UUID/randomUUID)
        handler (search/admin-related-handler db)]
    (with-redefs [h/try-parse-uuid (fn [value]
                                     (is (= (str entity-id) value))
                                     entity-id)
                  h/json-response (fn [body & [status]]
                                    {:status (or status 200)
                                     :body body})
                  search/related-for-article (fn [db* id limit tenant-id]
                                               (is (= db db*))
                                               (is (= entity-id id))
                                               (is (= 8 limit))
                                               (is (nil? tenant-id))
                                               {:detail {:canonical_name "Coffee"}})]
      (let [response (handler {:query-params {"type" "articles"
                                              "id" (str entity-id)}})]
        (is (= 200 (:status response)))
        (is (= {:related {:detail {:canonical_name "Coffee"}}
                :type "articles"
                :id (str entity-id)}
              (:body response)))))))

(deftest user-related-handler-passes-tenant-scope
  (let [db :mock-db
        user-id (java.util.UUID/randomUUID)
        tenant-id (java.util.UUID/randomUUID)
        supplier-id (java.util.UUID/randomUUID)
        handler (search/user-related-handler db)]
    (with-redefs [h/get-user-id (constantly user-id)
                  h/ensure-role (constantly nil)
                  h/get-tenant-id (constantly tenant-id)
                  h/try-parse-uuid (constantly supplier-id)
                  h/json-response (fn [body & [status]]
                                    {:status (or status 200)
                                     :body body})
                  search/related-for-supplier (fn [db* id limit tenant-id*]
                                                (is (= db db*))
                                                (is (= supplier-id id))
                                                (is (= 8 limit))
                                                (is (= tenant-id tenant-id*))
                                                {:stores []
                                                 :articles []})]
      (let [response (handler {:query-params {"type" "suppliers"
                                              "id" (str supplier-id)}})]
        (is (= 200 (:status response)))
        (is (= {:related {:stores []
                          :articles []}
                :type "suppliers"
                :id (str supplier-id)}
              (:body response)))))))

(deftest related-handler-rejects-invalid-id
  (testing "admin related handler returns 400 for invalid ids"
    (let [handler (search/admin-related-handler :mock-db)]
      (with-redefs [h/try-parse-uuid (constantly nil)
                    h/json-response (fn [body & [status]]
                                      {:status (or status 200)
                                       :body body})]
        (is (= {:status 400
                :body {:error "Missing or invalid id"}}
              (handler {:query-params {"type" "articles"
                                       "id" "not-a-uuid"}})))))))
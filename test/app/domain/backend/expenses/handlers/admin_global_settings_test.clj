(ns app.domain.backend.expenses.handlers.admin-global-settings-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.backend.test-helpers :as h]
    [app.domain.backend.expenses.handlers.admin-global-settings :as handlers]
    [app.domain.backend.expenses.services.global-settings :as global-settings]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is use-fixtures]])
  (:import
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

(def mock-admin
  {:id (UUID/randomUUID)
   :email "admin@example.com"
   :full_name "Admin User"
   :role "owner"
   :status "active"})

(defn- parse-body [resp]
  (json/parse-string (:body resp) true))

(deftest global-settings-get-and-update-roundtrip
  (when-let [db fixtures/*test-db*]
    (let [get-handler (handlers/get-global-settings-handler db)
          update-handler (handlers/update-global-settings-handler db)
          get-response (get-handler (h/mock-admin-request :get "/admin/api/expenses/global-settings" mock-admin {}))
          update-response (update-handler
                            (h/mock-admin-request :put "/admin/api/expenses/global-settings" mock-admin
                              {:body {:default-currency "EUR"
                                      :default-note "Admin note"
                                      :auto-publish-after-upload true
                                      :ai-receipt-enhancement true}}))
          get-body (parse-body get-response)
          update-body (parse-body update-response)]
      (is (= 200 (:status get-response)))
      (is (= true (:success get-body)))
      (is (= 200 (:status update-response)))
      (is (= "EUR" (get-in update-body [:settings :default-currency])))
      (is (= "Admin note" (get-in update-body [:settings :default-note])))
      (is (= true (get-in update-body [:settings :auto-publish-after-upload])))
      (is (= true (get-in update-body [:settings :ai-receipt-enhancement]))))))

(deftest enabled-currency-lifecycle-roundtrip
  (when-let [db fixtures/*test-db*]
    (let [list-handler (handlers/list-enabled-currencies-handler db)
          add-handler (handlers/add-enabled-currency-handler db)
          remove-handler (handlers/remove-enabled-currency-handler db)
          add-response (add-handler
                         (h/mock-admin-request :post "/admin/api/expenses/enabled-currencies" mock-admin
                           {:body {:code "AUD" :name "Australian Dollar"}}))
          list-response (list-handler (h/mock-admin-request :get "/admin/api/expenses/enabled-currencies" mock-admin {}))
          remove-response (remove-handler
                            (h/mock-admin-request :delete "/admin/api/expenses/enabled-currencies/AUD" mock-admin
                              {:path-params {:code "AUD"}}))
          add-body (parse-body add-response)
          list-body (parse-body list-response)
          remove-body (parse-body remove-response)]
      (is (= 200 (:status add-response)))
      (is (= "AUD" (get-in add-body [:currency :code])))
      (is (= 200 (:status list-response)))
      (is (some #(= "AUD" (:code %)) (:currencies list-body)))
      (is (= 200 (:status remove-response)))
      (is (= "AUD" (:removed remove-body)))
      (is (not (some #(= "AUD" (:code %)) (global-settings/get-enabled-currencies db)))))))

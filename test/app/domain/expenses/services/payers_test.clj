(ns app.domain.expenses.services.payers-test
  "Integration tests for payers service."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.payers :as payers]
    [clojure.test :refer [deftest is use-fixtures]]))

(use-fixtures :each fixtures/with-transaction-rollback)

(deftest payers-default-per-type-test
  (when-let [db fixtures/*test-db*]
    (let [get-payer (:get payers/service)
          p1 (payers/create-payer! db {:type "cash" :label "Cash Wallet" :is_default true})
          p2 (payers/create-payer! db {:type "cash" :label "Cash Jar" :is_default false})
          _ (payers/set-default-payer! db (:id p2))
          p1* (get-payer db (:id p1))
          p2* (get-payer db (:id p2))
          default (payers/get-default-payer db)]
      (is (false? (:is_default p1*)))
      (is (true? (:is_default p2*)))
      (is (= (:id p2) (:id default))))))

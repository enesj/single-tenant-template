(ns app.domain.backend.expenses.services.payers-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.services.payers :as payers]
    [app.domain.expenses.test-helpers :as th]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- count-default-payers
  [db tenant-id]
  (let [row (jdbc/execute-one!
              db
              ["select count(*) as n from payers where is_default = true and tenant_id = ?" tenant-id]
              {:builder-fn rs/as-unqualified-lower-maps})]
    (long (:n row))))

(deftest only-one-default-payer-on-create
  (testing "creating a payer with is_default=true unsets any previous default"
    (let [db fixtures/*test-db*
          user (th/ensure-test-user! db)
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          get-payer (:get payers/service)
          p1 (th/create-payer!
               db
               {:type "cash"
                :label (str "cash-" (UUID/randomUUID))
                :is_default true
                :tenant_id tenant-id})
          p2 (th/create-payer!
               db
               {:type "card"
                :label (str "card-" (UUID/randomUUID))
                :is_default true
                :tenant_id tenant-id})
          p1* (get-payer db (:id p1))
          p2* (get-payer db (:id p2))
          default (payers/get-default-payer db tenant-id)]
      (is (= 1 (count-default-payers db tenant-id))
        "There should be at most one default payer")
      (is (= (:id p2) (:id default))
        "The most recently set default should be the current default")
      (is (false? (:is_default p1*))
        "Previous default should be cleared")
      (is (true? (:is_default p2*))
        "New default should be set"))))

(deftest only-one-default-payer-on-update
  (testing "updating a payer to is_default=true unsets any previous default"
    (let [db fixtures/*test-db*
          user (th/ensure-test-user! db {:email "payer-update@example.com"})
          {:keys [tenant-id]} (th/ensure-test-tenant! db user)
          get-payer (:get payers/service)
          p1 (th/create-payer!
               db
               {:type "cash"
                :label (str "cash-" (UUID/randomUUID))
                :is_default true
                :tenant_id tenant-id})
          p2 (th/create-payer!
               db
               {:type "account"
                :label (str "acct-" (UUID/randomUUID))
                :is_default false
                :tenant_id tenant-id})
          _updated (payers/update-payer!
                     db
                     (:id p2)
                     {:label (str "acct-updated-" (UUID/randomUUID))
                      :is_default true}
                     {:tenant-id tenant-id})
          p1* (get-payer db (:id p1))
          p2* (get-payer db (:id p2))
          default (payers/get-default-payer db tenant-id)]
      (is (= 1 (count-default-payers db tenant-id))
        "There should be at most one default payer")
      (is (= (:id p2) (:id default))
        "Updated payer should become the current default")
      (is (false? (:is_default p1*))
        "Previous default should be cleared")
      (is (true? (:is_default p2*))
        "Updated payer should be set as default"))))
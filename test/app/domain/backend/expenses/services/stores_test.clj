(ns app.domain.backend.expenses.services.stores-test
  "Focused tests for ZIP-only city_id behavior in store flows."
  (:require
    [app.domain.backend.expenses.services.cities :as cities]
    [app.domain.backend.expenses.services.places-api :as places-api]
    [app.domain.backend.expenses.services.stores :as stores]
    [app.domain.backend.expenses.services.stores.repo :as store-repo]
    [app.domain.backend.expenses.services.stores.upsert :as store-upsert]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]
    [next.jdbc :as jdbc])
  (:import
    [java.util UUID]))

(deftest stores-list-sorts-by-supplier-column-test
  (testing "stores list supports supplier sorting by supplier display name"
    (let [captured-sql (atom nil)]
      (with-redefs [jdbc/execute! (fn [_db sql-params _opts]
                                    (reset! captured-sql sql-params)
                                    [])]
        ((:list stores/service) :db {:limit 10
                                     :offset 0
                                     :order-by :supplier-id
                                     :order-dir :asc})
        (let [sql-lc (some-> @captured-sql first str str/lower-case)]
          (is (string? sql-lc))
          (is (re-find #"join\s+suppliers" sql-lc))
          (is (re-find #"order by s\.display_name asc" sql-lc)))))))

(deftest find-or-create-store-resolves-city-id-via-zip-only-test
  (testing "create flow resolves city_id via cities/resolve-city-id-from-text!"
    (let [resolved-city-id (UUID/randomUUID)
          supplier-id (UUID/randomUUID)
          captured-sql (atom nil)
          opts {:places-cfg {:api-key "x"}
                :user-region "BA"}]
      (with-redefs [cities/resolve-city-id-from-text! (fn [_db text resolver-opts]
                                                        (is (= "Test Store\nVrbanja 1, 71 000 Sarajevo" text))
                                                        (is (= opts resolver-opts))
                                                        resolved-city-id)
                    stores/extract-city-from-address (fn [& _]
                                                       (throw (ex-info "city-name extraction must not be used" {})))
                    jdbc/execute-one! (fn [_db sql-params _opts]
                                        (reset! captured-sql sql-params)
                                        {:id (UUID/randomUUID)
                                         :supplier_id supplier-id
                                         :city_id resolved-city-id})]
        (let [row (stores/find-or-create-store!
                    :db
                    {:supplier_id supplier-id
                     :display_name "Test Store"
                     :address "Vrbanja 1, 71 000 Sarajevo"}
                    opts)]
          (is (= resolved-city-id (:city_id row)))
          (is (vector? @captured-sql))
          (is (str/includes? (str/lower-case (first @captured-sql)) "insert into stores")))))))

(deftest find-or-create-store-does-not-infer-city-without-resolvable-zip-test
  (testing "create flow keeps city_id nil when ZIP is missing/invalid/unresolvable"
    (let [supplier-id (UUID/randomUUID)]
      (with-redefs [cities/resolve-city-id-from-text! (fn [_db _text _opts] nil)
                    stores/extract-city-from-address (fn [& _]
                                                       (throw (ex-info "city-name extraction must not be used" {})))
                    jdbc/execute-one! (fn [_db _sql-params _opts]
                                        {:id (UUID/randomUUID)
                                         :supplier_id supplier-id
                                         :city_id nil})]
        (let [row (stores/find-or-create-store!
                    :db
                    {:supplier_id supplier-id
                     :display_name "No Zip Store"
                     :address "No valid postal code here"})]
          (is (nil? (:city_id row))))))))

(deftest update-store-address-unresolvable-zip-sets-city-id-nil-test
  (testing "update flow explicitly sets city_id to nil when address is provided but ZIP is unresolved"
    (let [store-id (UUID/randomUUID)
          captured-sql (atom nil)
          opts {:places-cfg {:api-key "x"}}]
      (with-redefs [cities/resolve-city-id-from-text! (fn [_db text resolver-opts]
                                                        (is (= "No valid ZIP in this address" text))
                                                        (is (= opts resolver-opts))
                                                        nil)
                    stores/extract-city-from-address (fn [& _]
                                                       (throw (ex-info "city-name extraction must not be used" {})))
                    jdbc/execute-one! (fn [_db sql-params _opts]
                                        (reset! captured-sql sql-params)
                                        {:id store-id
                                         :city_id nil})]
        (let [row (store-upsert/update-store! :db store-id {:address "No valid ZIP in this address"} opts)
              sql-lc (str/lower-case (first @captured-sql))]
          (is (nil? (:city_id row)))
          (is (str/includes? sql-lc "city_id"))
          (is (str/includes? sql-lc "null")))))))

(deftest update-store-without-address-does-not-touch-city-id-test
  (testing "update flow does not resolve or set city_id when address is absent"
    (let [store-id (UUID/randomUUID)
          captured-sql (atom nil)]
      (with-redefs [cities/resolve-city-id-from-text! (fn [& _]
                                                        (throw (ex-info "ZIP resolver must not run without address" {})))
                    jdbc/execute-one! (fn [_db sql-params _opts]
                                        (reset! captured-sql sql-params)
                                        {:id store-id
                                         :display_name "Renamed"
                                         :city_id (UUID/randomUUID)})]
        (store-upsert/update-store! :db store-id {:display_name "Renamed"})
        (is (not (str/includes? (str/lower-case (first @captured-sql)) "city_id")))))))

(deftest resolve-store-from-merchant-threads-opts-to-store-writes-test
  (testing "resolve-store-from-merchant passes opts into update-store! when Places resolves place-id"
    (let [supplier-id (UUID/randomUUID)
          existing-store-id (UUID/randomUUID)
          passed-opts (atom nil)
          called-key (atom nil)
          merchant {:name "Bingo"
                    :store_name "Bingo Store"
                    :address "Vrbanja 1, 71000 Sarajevo"}
          opts {:places-cfg {:api-key "x"
                             :region-code "BA"}
                :user-region "BA"}]
      (with-redefs [places-api/search-text! (fn [_cfg _query _places-opts]
                                              {:places [{:name "Bingo Store"
                                                         :raw {:id "place-123"
                                                               :formattedAddress "Vrbanja 1, 71000 Sarajevo"}}]})
                    store-repo/find-by-supplier-and-normalized-key
                    (fn [_db _supplier-id normalized-key]
                      (reset! called-key normalized-key)
                      {:id existing-store-id})
                    store-upsert/update-store! (fn [_db store-id patch resolver-opts]
                                                 (is (= existing-store-id store-id))
                                                 (is (= "Vrbanja 1, 71000 Sarajevo" (:address patch)))
                                                 (is (string? (:normalized_key patch)))
                                                 (reset! passed-opts resolver-opts)
                                                 {:id existing-store-id
                                                  :supplier_id supplier-id})
                    store-upsert/merge-duplicate-stores-by-place-id! (fn [& _] {:kept existing-store-id :merged 0})]
        (let [res (stores/resolve-store-from-merchant :db supplier-id merchant opts)]
          (is (= existing-store-id (:store-id res)))
          (is (= opts @passed-opts))
          (is (string? @called-key))
          (is (str/includes? @called-key "place")))))))
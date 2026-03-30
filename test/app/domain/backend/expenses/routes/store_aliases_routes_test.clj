(ns app.domain.backend.expenses.routes.store-aliases-routes-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.routes.store-aliases :as store-aliases-routes]
    [app.domain.backend.expenses.services.store-aliases :as store-aliases]
    [app.domain.backend.expenses.services.stores :as stores]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.template.backend.routes.admin.utils :as utils]
    [clojure.test :refer [deftest is use-fixtures]]
    [next.jdbc :as jdbc])
  (:import
    [java.time Instant]
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- list-handler
  [db]
  (get-in (store-aliases-routes/routes db) [1 1 :get]))

(defn- set-created-at!
  [db alias-id instant]
  (jdbc/execute-one!
    db
    ["update store_aliases set created_at = ? where id = ?" instant alias-id]))

(deftest store-aliases-admin-route-supports-visible-column-filters
  (when-let [db fixtures/*test-db*]
    (let [token (str (UUID/randomUUID))
          supplier-a-name (str "Store Alias Route Supplier A " token)
          supplier-b-name (str "Store Alias Route Supplier B " token)
          store-a-name (str "Store Alias Route Store A " token)
          store-b-name (str "Store Alias Route Store B " token)
          address-a (str "Alpha Avenue 1 " token)
          address-b (str "Omega Boulevard 9 " token)
          supplier-a (:supplier (suppliers/find-or-create-supplier! db supplier-a-name {}))
          supplier-b (:supplier (suppliers/find-or-create-supplier! db supplier-b-name {}))
          store-a (stores/find-or-create-store!
                    db
                    {:supplier_id (:id supplier-a)
                     :display_name store-a-name
                     :address address-a})
          store-b (stores/find-or-create-store!
                    db
                    {:supplier_id (:id supplier-b)
                     :display_name store-b-name
                     :address address-b})
          alias-a (store-aliases/find-or-create-alias! db (str token " Alpha Market"))
          alias-b (store-aliases/find-or-create-alias! db (str token " Omega Market"))
          alias-a (store-aliases/map-alias-to-store! db (:id alias-a) (:id store-a) 12)
          alias-b (store-aliases/map-alias-to-store! db (:id alias-b) (:id store-b) 88)
          _ (set-created-at! db (:id alias-a) (Instant/parse "2024-01-15T10:00:00Z"))
          _ (set-created-at! db (:id alias-b) (Instant/parse "2024-06-15T10:00:00Z"))
          handler (list-handler db)]
      (with-redefs [utils/success-response (fn [body & _] body)]
        (let [base-params {"limit" "50" "offset" "0"}
              by-supplier (handler {:query-params (assoc base-params "supplier-display-name" supplier-a-name)})
              by-store (handler {:query-params (assoc base-params "store-display-name" store-b-name)})
              by-address (handler {:query-params (assoc base-params "store-address" "Omega Boulevard")})
              by-raw-label (handler {:query-params (assoc base-params "raw-label" "Alpha Market")})
              by-normalized (handler {:query-params (assoc base-params "raw-label-normalized" (:raw_label_normalized alias-b))})
              by-confidence (handler {:query-params (-> base-params
                                                      (assoc "raw-label-normalized" (:raw_label_normalized alias-b))
                                                      (assoc "confidence-min" "80"))})
              by-created-at (handler {:query-params (-> base-params
                                                      (assoc "raw-label-normalized" (:raw_label_normalized alias-b))
                                                      (assoc "created-at-from" "2024-03-01T00:00:00Z"))})]
          (is (= [(:id alias-a)] (mapv :id (:store-aliases by-supplier))))
          (is (= [(:id alias-b)] (mapv :id (:store-aliases by-store))))
          (is (= [(:id alias-b)] (mapv :id (:store-aliases by-address))))
          (is (= [(:id alias-a)] (mapv :id (:store-aliases by-raw-label))))
          (is (= [(:id alias-b)] (mapv :id (:store-aliases by-normalized))))
          (is (= [(:id alias-b)] (mapv :id (:store-aliases by-confidence))))
          (is (= [(:id alias-b)] (mapv :id (:store-aliases by-created-at))))
          (is (= 1 (:total by-supplier)))
          (is (= 1 (:total by-confidence)))
          (is (= 1 (:total by-created-at))))))))

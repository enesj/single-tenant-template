(ns app.domain.backend.expenses.handlers.user-expenses.settings-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.handlers.user-expenses.settings :as settings]
    [app.domain.expenses.test-helpers :as th]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc])
  (:import
    (java.time Instant)
    (java.util UUID)))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- now [] (Instant/now))

(defn- create-test-context!
  "Create a minimal user plus tenant context for handler tests."
  [db]
  (let [user (th/ensure-test-user! db {:email (str "settings-user-" (UUID/randomUUID) "@test.com")
                                       :name "Settings Test User"})
        {:keys [tenant-id]} (th/ensure-test-tenant! db user)]
    {:user-id (:id user)
     :tenant-id tenant-id}))

(defn- req
  [{:keys [user-id tenant-id role body]}]
  (cond-> {:session {:auth-session {:user {:id user-id
                                           :role role}
                                    :membership {:id (UUID/randomUUID)
                                                 :role role}
                                    :tenant {:id tenant-id}}}}
    (some? body) (assoc :body (json/generate-string body))))

(defn- parse-body [resp]
  (json/parse-string (:body resp) true))

(deftest settings-get-returns-defaults-when-missing
  (when-let [db fixtures/*test-db*]
    (let [{:keys [user-id tenant-id]} (create-test-context! db)
          handler (settings/get-settings-handler db)
          resp (handler (req {:user-id user-id
                              :tenant-id tenant-id
                              :role "member"}))
          parsed (parse-body resp)
          body (:data parsed)]
      (is (= 200 (:status resp)))
      (is (= "BAM" (:default-currency body)))
      (is (nil? (:default-payer-id body)))
      (is (= true (:notifications-enabled body)))
      (is (= false (:receipt-refine-enabled body))))))

(deftest settings-put-persists-and-get-reflects
  (when-let [db fixtures/*test-db*]
    (let [{:keys [user-id tenant-id]} (create-test-context! db)
          payer (th/create-payer! db {:type "cash"
                                      :label "Cash"
                                      :tenant_id tenant-id})
          payer-id (:id payer)
          get-handler (settings/get-settings-handler db)
          put-handler (settings/update-settings-handler db)
          put-resp (put-handler
                     (req {:user-id user-id
                           :tenant-id tenant-id
                           :role "member"
                           :body {:default-currency "EUR"
                                  :default-payer-id (str payer-id)
                                  :notifications-enabled false
                                  :receipt-refine-enabled true}}))
          put-parsed (parse-body put-resp)
          put-body (:data put-parsed)
          get-resp (get-handler (req {:user-id user-id
                                      :tenant-id tenant-id
                                      :role "member"}))
          get-parsed (parse-body get-resp)
          get-body (:data get-parsed)]
      (is (= 200 (:status put-resp)))
      (is (= "EUR" (:default-currency put-body)))
      (is (= (str payer-id) (:default-payer-id put-body)))
      (is (= false (:notifications-enabled put-body)))
      (is (= true (:receipt-refine-enabled put-body)))

      (is (= 200 (:status get-resp)))
      (is (= "EUR" (:default-currency get-body)))
      (is (= (str payer-id) (:default-payer-id get-body)))
      (is (= false (:notifications-enabled get-body)))
      (is (= true (:receipt-refine-enabled get-body))))))

(deftest settings-put-blank-payer-clears
  (when-let [db fixtures/*test-db*]
    (let [{:keys [user-id tenant-id]} (create-test-context! db)
          payer (th/create-payer! db {:type "cash"
                                      :label "Cash"
                                      :tenant_id tenant-id})
          payer-id (:id payer)
          put-handler (settings/update-settings-handler db)
          get-handler (settings/get-settings-handler db)
          _ (put-handler
              (req {:user-id user-id
                    :tenant-id tenant-id
                    :role "member"
                    :body {:default-currency "EUR"
                           :default-payer-id (str payer-id)
                           :notifications-enabled true}}))
          clear-resp (put-handler
                       (req {:user-id user-id
                             :tenant-id tenant-id
                             :role "member"
                             :body {:default-payer-id ""}}))
          clear-parsed (parse-body clear-resp)
          clear-body (:data clear-parsed)
          get-parsed (parse-body (get-handler (req {:user-id user-id
                                                    :tenant-id tenant-id
                                                    :role "member"})))
          get-body (:data get-parsed)]
      (is (= 200 (:status clear-resp)))
      (is (nil? (:default-payer-id clear-body)))
      (is (nil? (:default-payer-id get-body))))))

(deftest settings-put-forbidden-for-viewer
  (when-let [db fixtures/*test-db*]
    (let [{:keys [user-id tenant-id]} (create-test-context! db)
          put-handler (settings/update-settings-handler db)
          resp (put-handler
                 (req {:user-id user-id
                       :tenant-id tenant-id
                       :role "viewer"
                       :body {:default-currency "EUR"}}))]
      (is (= 403 (:status resp))))))

(deftest settings-requires-authentication
  (when-let [db fixtures/*test-db*]
    (let [get-handler (settings/get-settings-handler db)
          put-handler (settings/update-settings-handler db)
          get-resp (get-handler {})
          put-resp (put-handler {})]
      (is (= 401 (:status get-resp)))
      (is (= 401 (:status put-resp))))))

(deftest settings-put-validates-currency
  (when-let [db fixtures/*test-db*]
    (let [{:keys [user-id tenant-id]} (create-test-context! db)
          put-handler (settings/update-settings-handler db)
          resp (put-handler
                 (req {:user-id user-id
                       :tenant-id tenant-id
                       :role "member"
                       :body {:default-currency "GBP"}}))
          body (parse-body resp)]
      (is (= 400 (:status resp)))
      (is (= "Unsupported currency" (:error body))))))

(ns app.domain.backend.expenses.handlers.user-expenses.settings-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.handlers.user-expenses.settings :as settings]
    [app.domain.backend.expenses.services.payers :as payers]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc])
  (:import
    (java.time Instant)
    (java.util UUID)))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- now [] (Instant/now))

(defn- create-test-user!
  "Insert a minimal user row and return its UUID id."
  [db role]
  (let [user-id (UUID/randomUUID)
        t (now)
        email (str "user-" (UUID/randomUUID) "@test.com")]
    (jdbc/execute!
      db
      (hsql/format {:insert-into :users
                    :values [{:id user-id
                              :email email
                              :full_name "Test User"
                              :password_hash "test-hash-not-real"
                              :role [:cast (or role "member") :user_role]
                              :status [:cast "active" :user_status]
                              :email_verified false
                              :auth_provider "password"
                              :created_at t
                              :updated_at t}]}))
    user-id))

(defn- req
  [{:keys [user-id role body]}]
  (cond-> {:session {:auth-session {:user {:id user-id
                                          :role role}}}}
    (some? body) (assoc :body (json/generate-string body))))

(defn- parse-body [resp]
  (json/parse-string (:body resp) true))

(deftest settings-get-returns-defaults-when-missing
  (when-let [db fixtures/*test-db*]
    (let [user-id (create-test-user! db "member")
          handler (settings/get-settings-handler db)
          resp (handler (req {:user-id user-id :role "member"}))
          body (parse-body resp)]
      (is (= 200 (:status resp)))
      (is (= "BAM" (:default_currency body)))
      (is (nil? (:default_payer_id body)))
      (is (= true (:notifications_enabled body))))))

(deftest settings-put-persists-and-get-reflects
  (when-let [db fixtures/*test-db*]
    (let [user-id (create-test-user! db "member")
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
          payer-id (:id payer)
          get-handler (settings/get-settings-handler db)
          put-handler (settings/update-settings-handler db)
          put-resp (put-handler
                     (req {:user-id user-id
                           :role "member"
                           :body {:default_currency "EUR"
                                  :default_payer_id (str payer-id)
                                  :notifications_enabled false}}))
          put-body (parse-body put-resp)
          get-resp (get-handler (req {:user-id user-id :role "member"}))
          get-body (parse-body get-resp)]
      (is (= 200 (:status put-resp)))
      (is (= "EUR" (:default_currency put-body)))
      (is (= (str payer-id) (:default_payer_id put-body)))
      (is (= false (:notifications_enabled put-body)))

      (is (= 200 (:status get-resp)))
      (is (= "EUR" (:default_currency get-body)))
      (is (= (str payer-id) (:default_payer_id get-body)))
      (is (= false (:notifications_enabled get-body))))))

(deftest settings-put-blank-payer-clears
  (when-let [db fixtures/*test-db*]
    (let [user-id (create-test-user! db "member")
          payer (payers/create-payer! db {:type "cash" :label "Cash"})
          payer-id (:id payer)
          put-handler (settings/update-settings-handler db)
          get-handler (settings/get-settings-handler db)
          _ (put-handler
              (req {:user-id user-id
                    :role "member"
                    :body {:default_currency "EUR"
                           :default_payer_id (str payer-id)
                           :notifications_enabled true}}))
          clear-resp (put-handler
                       (req {:user-id user-id
                             :role "member"
                             :body {:default_payer_id ""}}))
          clear-body (parse-body clear-resp)
          get-body (parse-body (get-handler (req {:user-id user-id :role "member"})))]
      (is (= 200 (:status clear-resp)))
      (is (nil? (:default_payer_id clear-body)))
      (is (nil? (:default_payer_id get-body))))))

(deftest settings-put-forbidden-for-viewer
  (when-let [db fixtures/*test-db*]
    (let [user-id (create-test-user! db "viewer")
          put-handler (settings/update-settings-handler db)
          resp (put-handler
                 (req {:user-id user-id
                       :role "viewer"
                       :body {:default_currency "EUR"}}))]
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
    (let [user-id (create-test-user! db "member")
          put-handler (settings/update-settings-handler db)
          resp (put-handler
                 (req {:user-id user-id
                       :role "member"
                       :body {:default_currency "GBP"}}))
          body (parse-body resp)]
      (is (= 400 (:status resp)))
      (is (= "Unsupported currency" (:error body))))))

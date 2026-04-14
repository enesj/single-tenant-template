(ns app.domain.backend.expenses.handlers.user-expenses.profile-test
  (:require
    [app.backend.fixtures :as fixtures]
    [app.domain.backend.expenses.handlers.user-expenses.profile :as profile]
    [app.domain.backend.expenses.services.global-settings :as global-settings]
    [app.domain.backend.expenses.services.tenant-settings :as tenant-settings]
    [app.domain.backend.expenses.services.user-expense-settings :as user-settings]
    [app.domain.expenses.test-helpers :as th]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    )
  (:import
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

(defn- create-test-context!
  [db]
  (let [email (str "profile-user-" (UUID/randomUUID) "@test.com")
        user (th/ensure-test-user! db {:email email :name "Profile Test User"})
        {:keys [tenant-id]} (th/ensure-test-tenant! db user)]
    {:user user
     :user-id (:id user)
     :tenant-id tenant-id}))

(defn- req
  [{:keys [user tenant-id role body-params]}]
  (cond-> {:session {:auth-session {:user {:id (:id user)
                                           :email (:email user)
                                           :full_name (or (:full_name user) (:full-name user))}
                                    :membership {:id (UUID/randomUUID)
                                                 :role role}
                                    :tenant {:id tenant-id}}}}
    body-params (assoc :body-params body-params)))

(defn- parse-body [resp]
  (json/parse-string (:body resp) true))

(deftest get-profile-owner-includes-tenant-settings-and-effective-settings
  (when-let [db fixtures/*test-db*]
    (let [{:keys [user user-id tenant-id]} (create-test-context! db)
          persisted (user-settings/get-user-expense-settings db tenant-id user-id)
          _ (global-settings/update-global-settings! db {:default-currency "EUR"
                                                         :default-note "Team default note"
                                                         :auto-publish-after-upload true
                                                         :ai-receipt-enhancement true})
          _ (tenant-settings/update-tenant-settings! db tenant-id {:email-notifications false})
          handler (profile/get-profile-handler db)
          response (handler (req {:user user :tenant-id tenant-id :role "owner"}))
          body (parse-body response)
          data (:data body)]
      (is (= 200 (:status response)))
      (is (= (:email user) (get-in data [:user :email])))
      (is (= "EUR" (get-in data [:settings :default-currency])))
      (is (= "Team default note" (get-in data [:settings :default-note])))
      (is (= true (get-in data [:settings :auto-publish-after-upload])))
      (is (= true (get-in data [:settings :ai-receipt-enhancement])))
      (is (= (str (:default-payer-id persisted)) (str (get-in data [:settings :default-payer-id]))))
      (is (nil? (get-in data [:settings :default-expense-category-id])))
      (is (= false (get-in data [:tenant-settings :email-notifications])))
      (is (seq (:enabled-currencies data))))))

(deftest get-profile-member-omits-tenant-settings
  (when-let [db fixtures/*test-db*]
    (let [{:keys [user tenant-id]} (create-test-context! db)
          handler (profile/get-profile-handler db)
          response (handler (req {:user user :tenant-id tenant-id :role "member"}))
          body (parse-body response)]
      (is (= 200 (:status response)))
      (is (nil? (get-in body [:data :tenant-settings]))))))

(deftest update-profile-defaults-validates-and-persists
  (when-let [db fixtures/*test-db*]
    (let [{:keys [user user-id tenant-id]} (create-test-context! db)
          payer (th/create-payer! db {:type "cash"
                                      :label "Office payer"
                                      :is_default false
                                      :tenant_id tenant-id})
          payer-id (:id payer)
          handler (profile/update-profile-defaults-handler db)
          invalid-response (handler (req {:user user
                                          :tenant-id tenant-id
                                          :role "member"
                                          :body-params {:default-payer-id "not-a-uuid"}}))
          valid-response (handler (req {:user user
                                        :tenant-id tenant-id
                                        :role "member"
                                        :body-params {:default-payer-id (str payer-id)}}))
          cleared-response (handler (req {:user user
                                          :tenant-id tenant-id
                                          :role "member"
                                          :body-params {:default-payer-id ""}}))
          invalid-body (parse-body invalid-response)
          valid-body (parse-body valid-response)
          cleared-body (parse-body cleared-response)
          persisted (user-settings/get-user-expense-settings db tenant-id user-id)]
      (is (= 400 (:status invalid-response)))
      (is (= "default-payer-id must be a UUID or blank to clear" (:error invalid-body)))
      (is (= 200 (:status valid-response)))
      (is (= (str payer-id) (str (get-in valid-body [:data :default-payer-id]))))
      (is (= 200 (:status cleared-response)))
      (is (nil? (get-in cleared-body [:data :default-payer-id])))
      (is (nil? (:default-payer-id persisted))))))

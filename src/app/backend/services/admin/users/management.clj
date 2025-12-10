(ns app.backend.services.admin.users.management
  "User management CRUD operations (single-tenant)."
  (:require
   [app.backend.services.admin.users.validation :as validation]
   [app.shared.type-conversion :as tc]
   [honey.sql :as hsql]
   [java-time.api :as time]
   [next.jdbc :as jdbc])
  (:import
   [java.util UUID]))

(defn update-user!
  "Update user information as admin with proper casting and validation (single-tenant)."
  [db user-id updates _admin-id _ip-address _user-agent]
  (let [validated-updates (validation/validate-user-updates updates)]
    (jdbc/with-transaction [tx db]
      (let [old-user (jdbc/execute-one! tx
                       (hsql/format {:select [:*]
                                     :from [:users]
                                     :where [:= :id user-id]}))]
        (when-not old-user
          (throw (ex-info (str "User not found with ID: " user-id)
                   {:status 404 :user-id user-id})))
        (let [base-updates (-> validated-updates
                             (assoc :updated_at (time/instant)))
              set-map (reduce-kv
                        (fn [m k v]
                          (assoc m k
                            (case k
                              :role (tc/cast-for-database :user-role (str v))
                              :status (tc/cast-for-database :user-status (str v))
                              :auth_provider (tc/cast-for-database :text (str v))
                              :last_login_at (tc/cast-for-database :timestamptz v)
                              v)))
                        {}
                        base-updates)]
          (jdbc/execute-one! tx (hsql/format {:update :users :set set-map :where [:= :id user-id]}))
          (jdbc/execute-one! tx
            (hsql/format {:select [:*]
                          :from [:users]
                          :where [:= :id user-id]})))))))

(defn update-user-role!
  "Update user role as admin (single-tenant)."
  [db user-id new-role _admin-id _ip-address _user-agent]
  (jdbc/with-transaction [tx db]
    (let [old-user (jdbc/execute-one! tx
                     (hsql/format {:select [:*]
                                   :from [:users]
                                   :where [:= :id user-id]}))]
      (when-not old-user
        (throw (ex-info (str "User not found with ID: " user-id)
                 {:status 404 :user-id user-id})))
      (jdbc/execute-one! tx
        (hsql/format {:update :users
                      :set {:role (tc/cast-for-database :user-role new-role)
                            :updated_at (time/instant)}
                      :where [:= :id user-id]})))))

(defn create-user!
  "Create a new user (single-tenant)."
  [db {:keys [email full_name role status auth_provider provider_user_id]} _admin-id _ip-address _user-agent]
  (let [user-id (UUID/randomUUID)
        now (time/instant)]
    (jdbc/with-transaction [tx db]
      (jdbc/execute! tx
        (hsql/format {:insert-into :users
                      :values [{:id user-id
                                :email email
                                :full_name full_name
                                :role (tc/cast-for-database :user-role (or role "member"))
                              :status (tc/cast-for-database :user-status (or status "active"))
                                :auth_provider (tc/cast-for-database :text (or auth_provider "email"))
                                :provider_user_id provider_user_id
                                :created_at now
                                :updated_at now}]}))
      (first (jdbc/execute! tx
               (hsql/format {:select [:*]
                             :from [:users]
                             :where [:= :id user-id]}))))))

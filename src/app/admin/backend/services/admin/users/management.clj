(ns app.admin.backend.services.admin.users.management
  "User management helpers (roles/status); reuse in admin flows."
  (:require
    [app.admin.backend.services.admin.users.validation :as validation]
    [app.template.backend.security.email :as email-privacy]
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
        (let [normalized-email (some-> (:email validated-updates) email-privacy/normalize-email)
              base-updates (cond-> (dissoc validated-updates :email)
                             normalized-email (merge (email-privacy/email-storage normalized-email)))
              set-map (reduce-kv
                        (fn [m k v]
                          (assoc m k
                            (case k
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

(defn create-user!
  "Create a new user (single-tenant)."
  [db {:keys [email full_name status auth_provider]} _admin-id _ip-address _user-agent]
  (let [user-id (UUID/randomUUID)
        now (time/instant)
        normalized-email (email-privacy/normalize-email email)]
    (jdbc/with-transaction [tx db]
      (jdbc/execute! tx
        (hsql/format {:insert-into :users
                      :values [(email-privacy/merge-email-storage
                                 {:id user-id
                                  :full_name full_name
                                  :status (tc/cast-for-database :user-status (or status "active"))
                                  :auth_provider (tc/cast-for-database :text (or auth_provider "email"))
                                  :created_at now}
                                 normalized-email)]}))
      (first (jdbc/execute! tx
               (hsql/format {:select [:*]
                             :from [:users]
                             :where [:= :id user-id]}))))))

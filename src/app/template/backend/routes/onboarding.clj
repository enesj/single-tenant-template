(ns app.template.backend.routes.onboarding
  "Onboarding API routes for tenant setup flow"
  (:require
    [app.template.backend.db.protocols :as db-protocols]
    [app.template.backend.routes.utils :as route-utils]
    [cheshire.core :as json]
    [taoensso.timbre :as log]))

(defn complete-onboarding-handler
  "Handle onboarding completion - mark tenant as fully onboarded"
  [db]
  (fn [req]
    (route-utils/with-error-handling "complete-onboarding"
      (let [auth-session (get-in req [:session :auth-session])
            tenant (:tenant auth-session)
            user (:user auth-session)]

        (if (not auth-session)
          {:status 401
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Not authenticated"})}

          (let [tenant-id (:id tenant)]
            (log/info "Completing onboarding for tenant:" tenant-id "user:" (:email user))

            ;; Update tenant to mark onboarding as completed
            (db-protocols/execute! db
              "UPDATE tenants
               SET onboarding_completed = true,
                   onboarding_step = 'completed',
                   updated_at = NOW()
               WHERE id = ?"
              [tenant-id])

            (log/info "Onboarding completed for tenant:" tenant-id)

            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string {:success true
                                          :message "Onboarding completed successfully"
                                          :redirect-url "/"})}))))))

(defn get-onboarding-status-handler
  "Get current onboarding status for authenticated tenant"
  [db]
  (fn [req]
    (route-utils/with-error-handling "get-onboarding-status"
      (let [auth-session (get-in req [:session :auth-session])
            tenant (:tenant auth-session)]

        (if (not auth-session)
          {:status 401
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Not authenticated"})}

          (let [tenant-id (:id tenant)
                ;; Fetch current onboarding status
                result (first (db-protocols/execute! db
                                "SELECT onboarding_completed, onboarding_step,
                                        created_at, updated_at
                                 FROM tenants WHERE id = ?"
                                [tenant-id]))]

            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string
                     {:onboarding-completed (:onboarding_completed result)
                      :onboarding-step (:onboarding_step result)
                      :tenant-created (:created_at result)
                      :last-updated (:updated_at result)})}))))))

(defn update-onboarding-step-handler
  "Update current onboarding step for tenant"
  [db]
  (fn [req]
    (route-utils/with-error-handling "update-onboarding-step"
      (let [auth-session (get-in req [:session :auth-session])
            tenant (:tenant auth-session)
            body (json/parse-string (slurp (:body req)) true)
            new-step (:step body)]

        (cond
          (not auth-session)
          {:status 401
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Not authenticated"})}

          (not (contains? #{"email_verification" "profile_setup" "property_setup" "completed"} new-step))
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Invalid onboarding step"})}

          :else
          (let [tenant-id (:id tenant)]
            (log/info "Updating onboarding step for tenant:" tenant-id "to:" new-step)

            ;; Update tenant onboarding step
            (db-protocols/execute! db
              "UPDATE tenants
               SET onboarding_step = ?,
                   onboarding_completed = ?,
                   updated_at = NOW()
               WHERE id = ?"
              [new-step
               (= new-step "completed")
               tenant-id])

            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string {:success true
                                          :step new-step
                                          :completed (= new-step "completed")})}))))))

(defn save-profile-handler
  "Save user profile information during onboarding"
  [db]
  (fn [req]
    (route-utils/with-error-handling "save-profile"
      (let [auth-session (get-in req [:session :auth-session])
            user (:user auth-session)
            body (json/parse-string (slurp (:body req)) true)
            {:keys [full_name phone timezone]} body]

        (if (not auth-session)
          {:status 401
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Not authenticated"})}

          (let [user-id (:id user)]
            (log/info "Saving profile for user:" user-id "name:" full_name)

            ;; Update user profile
            (db-protocols/execute! db
              "UPDATE users
               SET full_name = ?,
                   phone = ?,
                   timezone = ?,
                   updated_at = NOW()
               WHERE id = ?"
              [full_name phone timezone user-id])

            (log/info "Profile saved for user:" user-id)

            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string {:success true
                                          :message "Profile saved successfully"})}))))))

(defn create-onboarding-routes
  "Create onboarding routes"
  [db]
  {:complete-onboarding-handler (complete-onboarding-handler db)
   :get-onboarding-status-handler (get-onboarding-status-handler db)
   :update-onboarding-step-handler (update-onboarding-step-handler db)
   :save-profile-handler (save-profile-handler db)})

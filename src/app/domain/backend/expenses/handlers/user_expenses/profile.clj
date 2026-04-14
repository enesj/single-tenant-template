(ns app.domain.backend.expenses.handlers.user-expenses.profile
  "User profile handlers for the new settings hierarchy.

   GET  /api/v1/profile          — user info + effective settings + owner section
  PUT  /api/v1/profile/defaults — update per-user defaults (payer)

   These are mounted at the template API level (not under /expenses)
   since profile is cross-domain."
  (:require
    [app.domain.backend.expenses.services.global-settings :as global-settings]
    [app.domain.backend.expenses.services.tenant-settings :as tenant-settings]
    [app.domain.backend.expenses.services.user-expense-settings :as user-settings]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

;; ---------------------------------------------------------------------------
;; GET /api/v1/profile
;; ---------------------------------------------------------------------------

(defn get-profile-handler
  "Return the user's profile: account info, effective settings (merged from
   global + per-user), enabled currencies, and for owners/admins the tenant
   settings."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles "Role assignment required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              user (h/get-user request)]
          (try
            (let [global (global-settings/get-global-settings db)
                  persisted (user-settings/get-user-expense-settings db tenant-id user-id)
                  effective (user-settings/effective-settings-with-global global persisted)
                  currencies (global-settings/get-enabled-currencies db)
                  role (h/get-user-role request)
                  ;; Build base profile
                  profile {:user {:id (str user-id)
                                  :email (or (:email user) (:users/email user))
                                  :full-name (or (:full_name user) (:full-name user) (:users/full_name user))}
                           :settings effective
                           :enabled-currencies currencies}
                  ;; Owner/admin: include tenant settings
                  profile (if (contains? #{"owner" "admin"} role)
                            (let [ts (when tenant-id
                                       (tenant-settings/get-tenant-settings db tenant-id))]
                              (assoc profile :tenant-settings ts))
                            profile)]
              (h/json-response {:data profile}))
            (catch Exception e
              (log/error e "Failed to load profile" {:user-id user-id})
              (h/json-response {:error "Failed to load profile"} 500)))))
      (h/unauthorized-response))))

;; ---------------------------------------------------------------------------
;; PUT /api/v1/profile/defaults
;; ---------------------------------------------------------------------------

(defn update-profile-defaults-handler
  "Update per-user defaults. Supports:
   - :default-payer-id (UUID or nil to clear)"
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-write-roles
                           "Only members, admins, and owners can update defaults")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)]
          (try
            (let [body (h/read-body-params request)
                  ;; Parse payer ID
                  payer-id-raw (or (:default-payer-id body)
                                 (:default_payer_id body)
                                 (get body "default-payer-id")
                                 (get body "default_payer_id"))
                  payer-id (cond
                             (nil? payer-id-raw) nil
                             (= "" payer-id-raw) nil
                             (instance? UUID payer-id-raw) payer-id-raw
                             :else (h/try-parse-uuid payer-id-raw))]
              ;; Validate
              (when (and (some? payer-id-raw) (not= "" payer-id-raw) (nil? payer-id))
                (throw (ex-info "default-payer-id must be a UUID or blank to clear"
                         {:status 400})))
              (let [result (user-settings/update-user-defaults! db tenant-id user-id
                             {:default-payer-id payer-id})]
                ;; Auto-complete setup_profile step for all active onboarding records
                (try
                  (let [complete-all! (requiring-resolve
                                        'app.template.backend.services.onboarding.core/complete-step-for-all-roles!)]
                    (complete-all! db user-id "setup_profile"))
                  (catch Exception e
                    (log/debug "Onboarding setup_profile completion skipped" {:error (.getMessage e)})))
                (log/info "Updated profile defaults" {:user-id user-id
                                                      :payer-id payer-id})
                (h/json-response {:data result})))
            (catch clojure.lang.ExceptionInfo e
              (let [status (or (:status (ex-data e)) 500)]
                (if (<= 400 status 499)
                  (h/json-response {:error (.getMessage e)} status)
                  (do
                    (log/error e "Failed to update profile defaults" {:user-id user-id})
                    (h/json-response {:error "Failed to update defaults"} 500)))))
            (catch Exception e
              (log/error e "Failed to update profile defaults" {:user-id user-id})
              (h/json-response {:error "Failed to update defaults"} 500)))))
      (h/unauthorized-response))))

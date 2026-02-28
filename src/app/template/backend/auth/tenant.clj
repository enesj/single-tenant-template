(ns app.template.backend.auth.tenant
  "Tenant context resolution for auth flows.

   Encapsulates the 'check memberships → provision or set context' logic so
   that login/register/OAuth handlers stay lean."
  (:require
    [app.template.backend.services.tenant :as tenant-svc]
    [taoensso.timbre :as log]))

(defn resolve-tenant-context
  "Decide what to do after a user authenticates, based on membership count:
     0  → provision a new tenant (auto-creates owner membership + seed data)
     1  → auto-select that tenant
     >1 → return the list so the frontend can ask the user to choose

   Returns a map with :action plus relevant data."
  [db config user]
  (let [user-id     (or (:id user) (:users/id user))
        memberships (tenant-svc/get-user-memberships db user-id)]
    (case (count memberships)
      0 (do
          (log/info "No memberships for user" (or (:email user) (:users/email user)) "— provisioning tenant")
          (let [{:keys [tenant membership]} (tenant-svc/provision-tenant! db config user)]
            {:tenant     tenant
             :membership membership
             :action     :provisioned}))

      1 (let [m      (first memberships)
              t-id   (or (:tenant_id m) (:tenant_memberships/tenant_id m))
              tenant (tenant-svc/find-tenant-by-id db t-id)]
          {:tenant     tenant
           :membership m
           :action     :auto-set})

      ;; >1
      {:tenants memberships
       :action  :selection-required})))

(defn build-auth-session
  "Merge tenant context into the auth-session map returned to Ring.

   `base-session` is typically `{:user <sanitized-user>}`.
   `tenant-ctx`   is the return value of `resolve-tenant-context`."
  [base-session tenant-ctx]
  (case (:action tenant-ctx)
    :provisioned
    (assoc base-session
      :tenant     (:tenant tenant-ctx)
      :membership {:id   (or (get-in tenant-ctx [:membership :id])
                           (get-in tenant-ctx [:membership :tenant_memberships/id]))
                   :role (or (get-in tenant-ctx [:membership :role])
                           (get-in tenant-ctx [:membership :tenant_memberships/role]))})

    :auto-set
    (assoc base-session
      :tenant     (:tenant tenant-ctx)
      :membership {:id   (or (get-in tenant-ctx [:membership :id])
                           (get-in tenant-ctx [:membership :tenant_memberships/id]))
                   :role (or (get-in tenant-ctx [:membership :role])
                           (get-in tenant-ctx [:membership :tenant_memberships/role]))})

    :selection-required
    (assoc base-session
      :tenant-selection-required true
      :available-tenants (mapv (fn [m]
                                 {:tenant-id   (or (:tenant_id m) (:tenant_memberships/tenant_id m))
                                  :tenant-name (or (:tenant_name m) (:tenant_memberships/tenant_name m))
                                  :tenant-slug (or (:tenant_slug m) (:tenant_memberships/tenant_slug m))
                                  :role        (or (:role m) (:tenant_memberships/role m))})
                           (:tenants tenant-ctx)))

    ;; Fallback — should not happen
    base-session))

(comment
  ;; (require 'app.template.backend.auth.tenant :reload)
  :rcf)

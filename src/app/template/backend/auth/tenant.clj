(ns app.template.backend.auth.tenant
  "Tenant context resolution for auth flows.

   Encapsulates the 'check memberships → provision or set context' logic so
   that login/register/OAuth handlers stay lean."
  (:require
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.services.tenant :as tenant-svc]
    [taoensso.timbre :as log]))

(defn resolve-tenant-context
  "Decide what to do after a user authenticates, based on membership count:
     0  → :no-tenant (user must accept an invitation or request admin approval)
     1  → auto-select that tenant
     >1 → return the list so the frontend can ask the user to choose

   Returns a map with :action plus relevant data."
  ([db config user] (resolve-tenant-context db config user {}))
  ([db config user _opts]
   (let [user-id (or (:id user) (:users/id user))
         memberships (tenant-svc/get-user-memberships db user-id)]
     (case (count memberships)
       0 (do
           (log/info "No memberships for user"
             {:user-id user-id
              :user-ref (email-privacy/user-ref user-id)})
           {:action :no-tenant})

       1 (let [m (first memberships)
               t-id (or (:tenant_id m) (:tenant_memberships/tenant_id m))
               tenant (tenant-svc/find-tenant-by-id db t-id)]
           {:tenant tenant
            :membership m
            :action :auto-set})

       {:tenants memberships
        :action :selection-required}))))

(defn- normalize-tenant
  "Normalize a tenant map from next.jdbc namespaced keys to plain keys.
   Ensures downstream code can always read :id, :name, etc. without
   needing (or (:id t) (:tenants/id t)) fallbacks everywhere."
  [t]
  (when t
    {:id         (or (:id t) (:tenants/id t))
     :name       (or (:name t) (:tenants/name t))
     :slug       (or (:slug t) (:tenants/slug t))
     :status     (or (:status t) (:tenants/status t))
     :created_at (or (:created_at t) (:tenants/created_at t))
     :updated_at (or (:updated_at t) (:tenants/updated_at t))}))

(defn build-auth-session
  "Merge tenant context into the auth-session map returned to Ring.

   `base-session` is typically `{:user <sanitized-user>}`.
   `tenant-ctx`   is the return value of `resolve-tenant-context`."
  [base-session tenant-ctx]
  (let [session-user (some-> (:user base-session) email-privacy/user-session-payload)
        base-session* (cond-> base-session
                        session-user (assoc :user session-user))]
    (case (:action tenant-ctx)
      :no-tenant
      (assoc base-session* :no-tenant true)

      :provisioned
      (assoc base-session*
        :tenant (normalize-tenant (:tenant tenant-ctx))
        :membership {:id (or (get-in tenant-ctx [:membership :id])
                           (get-in tenant-ctx [:membership :tenant_memberships/id]))
                     :role (or (get-in tenant-ctx [:membership :role])
                             (get-in tenant-ctx [:membership :tenant_memberships/role]))})

      :auto-set
      (assoc base-session*
        :tenant (normalize-tenant (:tenant tenant-ctx))
        :membership {:id (or (get-in tenant-ctx [:membership :id])
                           (get-in tenant-ctx [:membership :tenant_memberships/id]))
                     :role (or (get-in tenant-ctx [:membership :role])
                             (get-in tenant-ctx [:membership :tenant_memberships/role]))})

      :selection-required
      (assoc base-session*
        :tenant-selection-required true
        :available-tenants (mapv (fn [m]
                                   (let [tenant-id (or (:tenant_id m) (:tenant_memberships/tenant_id m))]
                                     {:tenant-id tenant-id
                                      :tenant-ref (email-privacy/tenant-ref tenant-id)
                                      :tenant-name (or (:tenant_name m) (:tenant_memberships/tenant_name m))
                                      :tenant-slug (or (:tenant_slug m) (:tenant_memberships/tenant_slug m))
                                      :role (or (:role m) (:tenant_memberships/role m))}))
                             (:tenants tenant-ctx)))

      base-session*)))

(comment
  ;; (require 'app.template.backend.auth.tenant :reload)
  :rcf)

(ns app.template.backend.routes.admin.tenants
  "Admin tenant management routes — superpower CRUD on tenants and memberships.

   Platform admins can browse all tenants and manage memberships across tenants,
   bypassing tenant-level role chains. Write operations require admin `owner` role.

   Routes:
   - GET  /admin/api/tenants             - List all tenants
   - GET  /admin/api/tenants/:id         - Get tenant details
   - GET  /admin/api/tenants/:id/members - List tenant members
   - PUT  /admin/api/tenants/:id/members/:member-id/role   - Change member role
   - DELETE /admin/api/tenants/:id/members/:member-id       - Remove member"
  (:require
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [app.template.backend.middleware.admin :as admin-mw]
    [app.template.backend.routes.admin.utils :as utils]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Query Helpers
;; ============================================================================

(defn- uuid-cast [id]
  (if (string? id) [:cast id :uuid] id))

(defn- list-tenants
  "List tenants with pagination, search, and status filters."
  [db {:keys [search status limit offset]}]
  (let [base-where (cond-> []
                     status (conj [:= :t.status [:cast status :tenant_status]])
                     search (conj [:or
                                   [:ilike :t.name (str "%" search "%")]
                                   [:ilike :t.slug (str "%" search "%")]]))
        where-clause (when (seq base-where) (into [:and] base-where))
        query (cond-> {:select [:t.*
                                [[:raw "COALESCE(mc.member_count, 0)"] :member_count]
                                [:owner_u.email :owner_email]
                                [:owner_u.full_name :owner_name]]
                       :from   [[:tenants :t]]
                       :left-join [[{:select   [:tenant_id [[:count :*] :member_count]]
                                     :from     [:tenant_memberships]
                                     :where    [:= :status [:cast "active" :membership_status]]
                                     :group-by [:tenant_id]} :mc]
                                   [:= :mc.tenant_id :t.id]

                                   [:tenant_memberships :om]
                                   [:and
                                    [:= :om.tenant_id :t.id]
                                    [:= :om.role [:cast "owner" :membership_role]]
                                    [:= :om.status [:cast "active" :membership_status]]]

                                   [:users :owner_u]
                                   [:= :owner_u.id :om.user_id]]
                       :order-by [[:t.created_at :desc]]}
                where-clause (assoc :where where-clause)
                limit (assoc :limit limit)
                offset (assoc :offset offset))]
    (mapv convert-pg-objects
      (jdbc/execute! db (sql/format query)
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn- count-tenants
  "Count tenants matching filters."
  [db {:keys [search status]}]
  (let [base-where (cond-> []
                     status (conj [:= :status [:cast status :tenant_status]])
                     search (conj [:or
                                   [:ilike :name (str "%" search "%")]
                                   [:ilike :slug (str "%" search "%")]]))
        where-clause (when (seq base-where) (into [:and] base-where))
        query (cond-> {:select [[[:count :*] :total]]
                       :from   [:tenants]}
                where-clause (assoc :where where-clause))
        row (jdbc/execute-one! db (sql/format query))]
    (or (:total row) 0)))

(defn- get-tenant-detail
  "Get tenant by ID with member count and owner info."
  [db tenant-id]
  (let [query {:select [:t.*
                        [[:raw "COALESCE(mc.member_count, 0)"] :member_count]
                        [:owner_u.email :owner_email]
                        [:owner_u.full_name :owner_name]]
               :from   [[:tenants :t]]
               :left-join [[{:select   [:tenant_id [[:count :*] :member_count]]
                             :from     [:tenant_memberships]
                             :where    [:and
                                        [:= :tenant_id (uuid-cast tenant-id)]
                                        [:= :status [:cast "active" :membership_status]]]
                             :group-by [:tenant_id]} :mc]
                           [:= :mc.tenant_id :t.id]

                           [:tenant_memberships :om]
                           [:and
                            [:= :om.tenant_id :t.id]
                            [:= :om.role [:cast "owner" :membership_role]]
                            [:= :om.status [:cast "active" :membership_status]]]

                           [:users :owner_u]
                           [:= :owner_u.id :om.user_id]]
               :where [:= :t.id (uuid-cast tenant-id)]}]
    (some-> (jdbc/execute-one! db (sql/format query)
              {:builder-fn rs/as-unqualified-lower-maps})
      convert-pg-objects)))

(defn- list-tenant-members
  "List all members of a tenant with user info."
  [db tenant-id]
  (mapv convert-pg-objects
    (jdbc/execute! db
      (sql/format {:select [:tm.* [:u.email :user_email] [:u.full_name :user_full_name]]
                   :from   [[:tenant_memberships :tm]]
                   :join   [[:users :u] [:= :tm.user_id :u.id]]
                   :where  [:and
                            [:= :tm.tenant_id (uuid-cast tenant-id)]
                            [:= :tm.status [:cast "active" :membership_status]]]
                   :order-by [[:tm.created_at :asc]]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- get-membership-by-id
  "Get a single membership by ID."
  [db membership-id]
  (some-> (jdbc/execute-one! db
            (sql/format {:select [:*]
                         :from   [:tenant_memberships]
                         :where  [:= :id (uuid-cast membership-id)]})
            {:builder-fn rs/as-unqualified-lower-maps})
    convert-pg-objects))

;; ============================================================================
;; Handlers
;; ============================================================================

(defn- list-tenants-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            pagination (utils/extract-pagination-params params)
            filters {:search (:search params)
                     :status (:status params)}
            tenants (list-tenants db (merge filters pagination))
            total   (count-tenants db filters)]
        (log/info "Admin list-tenants returned" (count tenants) "tenants"
          {:filters filters :total total})
        (utils/json-response {:tenants tenants
                              :total   total
                              :limit   (:limit pagination)
                              :offset  (:offset pagination)})))
    "Failed to retrieve tenants"))

(defn- get-tenant-handler [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [tenant-id _request]
          (if-let [tenant (get-tenant-detail db tenant-id)]
            (utils/json-response {:tenant tenant})
            (utils/error-response "Tenant not found" :status 404)))))
    "Failed to retrieve tenant details"))

(defn- list-members-handler [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [tenant-id _request]
          (let [members (list-tenant-members db tenant-id)]
            (utils/json-response {:members members
                                  :total   (count members)})))))
    "Failed to retrieve tenant members"))

(defn- change-member-role-handler
  "Superpower: change a member's role, bypassing tenant role chains.
   Guards: cannot change owner's role, valid role only."
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [tenant-id  (utils/extract-uuid-param request :id)
            member-id  (utils/extract-uuid-param request :member-id)]
        (when-not (and tenant-id member-id)
          (throw (ex-info "Invalid tenant or member ID" {:status 400})))
        (let [new-role (get-in request [:body :role])
              admin-id (utils/get-admin-id request)]
          (when-not new-role
            (throw (ex-info "Role is required" {:status 400})))
          (when-not (#{"admin" "member" "viewer"} new-role)
            (throw (ex-info "Invalid role. Must be one of: admin, member, viewer"
                     {:status 400 :allowed ["admin" "member" "viewer"]})))
          (let [membership (get-membership-by-id db member-id)]
            (when-not membership
              (throw (ex-info "Membership not found" {:status 404})))
            (when-not (= (str (:tenant_id membership)) (str tenant-id))
              (throw (ex-info "Membership does not belong to this tenant" {:status 400})))
            ;; Guard: cannot change owner's role
            (when (= "owner" (str (:role membership)))
              (throw (ex-info "Cannot change the owner's role — use transfer-ownership"
                       {:status 403})))
            (let [now     (java.time.LocalDateTime/now)
                  updated (convert-pg-objects
                            (jdbc/execute-one! db
                              (sql/format {:update [:tenant_memberships]
                                           :set    {:role       [:cast new-role :membership_role]
                                                    :updated_at now}
                                           :where  [:= :id (uuid-cast member-id)]
                                           :returning [:*]})
                              {:builder-fn rs/as-unqualified-lower-maps}))]
              (utils/log-admin-action "superpower_change_role" admin-id
                "tenant_membership" member-id
                {:tenant-id  (str tenant-id)
                 :new-role   new-role
                 :old-role   (str (:role membership))})
              (utils/json-response {:membership updated}))))))
    "Failed to change member role"))

(defn- remove-member-handler
  "Superpower: remove a member from tenant, bypassing tenant role chains.
   Guards: cannot remove the owner."
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [tenant-id  (utils/extract-uuid-param request :id)
            member-id  (utils/extract-uuid-param request :member-id)]
        (when-not (and tenant-id member-id)
          (throw (ex-info "Invalid tenant or member ID" {:status 400})))
        (let [admin-id   (utils/get-admin-id request)
              membership (get-membership-by-id db member-id)]
          (when-not membership
            (throw (ex-info "Membership not found" {:status 404})))
          (when-not (= (str (:tenant_id membership)) (str tenant-id))
            (throw (ex-info "Membership does not belong to this tenant" {:status 400})))
          ;; Guard: cannot remove owner
          (when (= "owner" (str (:role membership)))
            (throw (ex-info "Cannot remove the tenant owner" {:status 403})))
          (let [now     (java.time.LocalDateTime/now)
                updated (convert-pg-objects
                          (jdbc/execute-one! db
                            (sql/format {:update [:tenant_memberships]
                                         :set    {:status     [:cast "suspended" :membership_status]
                                                  :updated_at now}
                                         :where  [:= :id (uuid-cast member-id)]
                                         :returning [:*]})
                            {:builder-fn rs/as-unqualified-lower-maps}))]
            (utils/log-admin-action "superpower_remove_member" admin-id
              "tenant_membership" member-id
              {:tenant-id (str tenant-id)
               :user-id   (str (:user_id membership))
               :role      (str (:role membership))})
            (utils/json-response {:membership updated})))))
    "Failed to remove member"))

;; ============================================================================
;; Route Tree
;; ============================================================================

(defn routes
  "Admin tenant management routes.
   Read operations: any authenticated admin.
   Write operations (role change, member removal): admin `owner` role required."
  [db]
  ["/tenants"
   ["" {:get {:handler (list-tenants-handler db)}}]
   ["/:id" {:get {:handler (get-tenant-handler db)}}]
   ["/:id/members"
    ["" {:get {:handler (list-members-handler db)}}]
    ["/:member-id"
     {:middleware [#(admin-mw/wrap-admin-role % :owner)]}
     ["/role" {:put {:handler (change-member-role-handler db)}}]
     ["" {:delete {:handler (remove-member-handler db)}}]]]])

(comment
  ;; (require 'app.template.backend.routes.admin.tenants :reload)
  :rcf)

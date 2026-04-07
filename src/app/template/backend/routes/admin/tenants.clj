(ns app.template.backend.routes.admin.tenants
  "Admin tenant management routes — superpower CRUD on tenants and memberships.

   Platform admins can browse all tenants and manage memberships across tenants,
   bypassing tenant-level role chains. Write operations require admin `owner` role.

   Routes:
   - GET  /admin/api/tenants             - List all tenants
   - GET  /admin/api/tenants/:id         - Get tenant details
   - PUT  /admin/api/tenants/:id         - Update tenant name/slug/status
   - DELETE /admin/api/tenants/batch     - Delete tenants in bulk
   - GET  /admin/api/tenants/:id/members - List tenant members
   - PUT  /admin/api/tenants/:id/members/:member-id/role   - Change member role
   - DELETE /admin/api/tenants/:id/members/:member-id       - Remove member"
  (:require
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [app.template.backend.middleware.admin :as admin-mw]
    [app.template.backend.routes.admin.utils :as utils]
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.services.member :as member-svc]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Query Helpers
;; ============================================================================

(defn- uuid-cast [id]
  (if (string? id) [:cast id :uuid] id))

(def ^:private allowed-tenant-order-by
  {:created-at :t/created_at
   :updated-at :t/updated_at
   :name :t/name
   :slug :t/slug
   :status :t/status
   :member-count :member_count
   :owner-name :owner_u/full_name})

(defn- normalize-order-dir [order-dir]
  (if (= :asc order-dir) :asc :desc))

(defn- list-tenants
  "List tenants with pagination, search, status filters, and sorting."
  [db {:keys [search status limit offset order-by order-dir]}]
  (let [base-where (cond-> []
                     status (conj [:= :t.status [:cast status :tenant_status]])
                     search (conj [:or
                                   [:ilike :t.name (str "%" search "%")]
                                   [:ilike :t.slug (str "%" search "%")]]))
        where-clause (when (seq base-where) (into [:and] base-where))
        order-column (get allowed-tenant-order-by order-by :t/created_at)
        order-direction (normalize-order-dir order-dir)
        query (cond-> {:select [:t.*
                                [[:raw "COALESCE(mc.member_count, 0)"] :member_count]
                                [:owner_u.id :owner_user_id]
                                [:owner_u.email_ciphertext :owner_email_ciphertext]
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
                       :order-by [[order-column order-direction]]}
                where-clause (assoc :where where-clause)
                limit (assoc :limit limit)
                offset (assoc :offset offset))]
    (->> (jdbc/execute! db (sql/format query)
           {:builder-fn rs/as-unqualified-lower-maps})
      (mapv convert-pg-objects)
      (mapv email-privacy/routine-tenant-view))))

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
                        [:owner_u.id :owner_user_id]
                        [:owner_u.email_ciphertext :owner_email_ciphertext]
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
      convert-pg-objects
      email-privacy/routine-tenant-view)))

(defn- list-tenant-members
  "List all members of a tenant with user info."
  [db tenant-id]
  (->> (jdbc/execute! db
      (sql/format {:select [:tm.* [:u.email_ciphertext :user_email_ciphertext] [:u.full_name :user_full_name]]
                      :from   [[:tenant_memberships :tm]]
                      :join   [[:users :u] [:= :tm.user_id :u.id]]
                      :where  [:and
                               [:= :tm.tenant_id (uuid-cast tenant-id)]
                               [:= :tm.status [:cast "active" :membership_status]]]
                      :order-by [[:tm.created_at :asc]]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (mapv convert-pg-objects)
    (mapv email-privacy/routine-membership-view)))

(defn- get-membership-by-id
  "Get a single membership by ID."
  [db membership-id]
  (some-> (jdbc/execute-one! db
            (sql/format {:select [:*]
                         :from   [:tenant_memberships]
                         :where  [:= :id (uuid-cast membership-id)]})
            {:builder-fn rs/as-unqualified-lower-maps})
    convert-pg-objects))

(def ^:private tenant-statuses
  #{"active" "suspended" "archived"})

(defn- request-field-present?
  [body field-key]
  (or (contains? body field-key)
    (contains? body (keyword "tenants" (name field-key)))))

(defn- request-field-value
  [body field-key]
  (or (get body field-key)
    (get body (keyword "tenants" (name field-key)))))

(defn- normalize-tenant-updates
  [body]
  (let [name-present? (request-field-present? body :name)
        slug-present? (request-field-present? body :slug)
        status-present? (request-field-present? body :status)
        name (some-> (request-field-value body :name) str str/trim)
        slug (some-> (request-field-value body :slug) str str/trim)
        status (some-> (request-field-value body :status) str str/trim)
        updates (cond-> {}
                  name-present? (assoc :name name)
                  slug-present? (assoc :slug slug)
                  status-present? (assoc :status [:cast status :tenant_status]))]
    (when (and name-present? (str/blank? name))
      (throw (ex-info "Tenant name cannot be blank" {:status 400 :field :name})))
    (when (and slug-present? (str/blank? slug))
      (throw (ex-info "Tenant slug cannot be blank" {:status 400 :field :slug})))
    (when (and status-present?
            (not (contains? tenant-statuses status)))
      (throw (ex-info "Invalid tenant status" {:status 400
                                               :field :status
                                               :allowed tenant-statuses
                                               :value status})))
    (when (empty? updates)
      (throw (ex-info "No editable tenant fields provided" {:status 400})))
    (assoc updates :updated_at (java.time.LocalDateTime/now))))

(defn- update-tenant!
  [db tenant-id updates]
  (some-> (jdbc/execute-one! db
            (sql/format {:update [:tenants]
                         :set updates
                         :where [:= :id (uuid-cast tenant-id)]
                         :returning [:*]})
            {:builder-fn rs/as-unqualified-lower-maps})
    convert-pg-objects))

(defn- ensure-all-tenants-exist!
  [db tenant-ids]
  (let [requested-ids (mapv str tenant-ids)
        existing-ids (->> (jdbc/execute! db
                            (sql/format {:select [:id]
                                         :from [:tenants]
                                         :where [:in :id tenant-ids]})
                            {:builder-fn rs/as-unqualified-lower-maps})
                       (map (comp str :id))
                       set)
        missing-ids (vec (remove existing-ids requested-ids))]
    (when (seq missing-ids)
      (throw (ex-info "One or more tenants were not found" {:status 404
                                                            :missing-ids missing-ids})))
    tenant-ids))

(defn- delete-tenants!
  [db tenant-ids]
  (jdbc/with-transaction [tx db]
    (let [tenant-ids (ensure-all-tenants-exist! tx tenant-ids)]
      (->> (jdbc/execute! tx
             (sql/format {:delete-from [:tenants]
                          :where [:in :id tenant-ids]
                          :returning [:id]})
             {:builder-fn rs/as-unqualified-lower-maps})
        (mapv (comp str :id))))))

;; ============================================================================
;; Handlers
;; ============================================================================

(defn- list-tenants-handler [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            pagination (utils/extract-pagination-params params)
            sort (utils/extract-sort-params params)
            filters {:search (:search params)
                     :status (:status params)}
            tenants (list-tenants db (merge filters pagination sort))
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

(defn- update-tenant-handler
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [admin-id (utils/get-admin-id request)
            ip-address (utils/get-client-ip request)
            user-agent (get-in request [:headers "user-agent"])
            updates (normalize-tenant-updates (:body request))]
        (utils/handle-uuid-request request :id
          (fn [tenant-id _request]
            (if-let [tenant (update-tenant! db tenant-id updates)]
              (do
                (utils/log-admin-action-with-context
                  "update_tenant"
                  admin-id
                  "tenant"
                  tenant-id
                  {:updates updates}
                  ip-address
                  user-agent)
                (utils/json-response {:tenant tenant}))
              (utils/error-response "Tenant not found" :status 404))))))
    "Failed to update tenant"))

(defn- batch-delete-tenants-handler
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [admin-id (utils/get-admin-id request)
            ip-address (utils/get-client-ip request)
            user-agent (get-in request [:headers "user-agent"])
            raw-ids (vec (or (get-in request [:body :ids]) []))
            tenant-ids (->> raw-ids
                         (map utils/parse-uuid-custom)
                         (filter some?)
                         distinct
                         vec)]
        (when (empty? raw-ids)
          (throw (ex-info "No tenant ids provided" {:status 400})))
        (when (not= (count raw-ids) (count tenant-ids))
          (throw (ex-info "One or more tenant ids are invalid" {:status 400})))
        (let [deleted-ids (delete-tenants! db tenant-ids)]
          (utils/log-admin-action-with-context
            "batch_delete_tenants"
            admin-id
            "tenant"
            (first deleted-ids)
            {:deleted-ids deleted-ids
             :deleted-count (count deleted-ids)}
            ip-address
            user-agent)
          (utils/json-response {:deleted-count (count deleted-ids)
                                :deleted-ids deleted-ids}))))
    "Failed to delete tenants"))

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
          (let [membership (get-membership-by-id db member-id)]
            (when-not membership
              (throw (ex-info "Membership not found" {:status 404})))
            (when-not (= (str (:tenant_id membership)) (str tenant-id))
              (throw (ex-info "Membership does not belong to this tenant" {:status 400})))
            (let [updated (member-svc/superpower-change-role!
                            db
                            {:target-membership membership
                             :new-role          new-role})]
              (utils/log-admin-action "superpower_change_role" admin-id
                "tenant_membership" member-id
                {:tenant-id (str tenant-id)
                 :new-role  new-role
                 :old-role  (str (:role membership))})
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
          (let [updated (member-svc/superpower-remove-member!
                          db
                          {:target-membership membership})]
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
   Write operations (tenant update/delete, role change, member removal): admin `owner` role required."
  [db]
  ["/tenants"
   ["" {:get {:handler (list-tenants-handler db)}}]
   ["/batch" {:delete {:middleware [#(admin-mw/wrap-admin-role % :owner)]
                       :handler (batch-delete-tenants-handler db)}}]
   ["/:id" {:get {:handler (get-tenant-handler db)}
            :put {:middleware [#(admin-mw/wrap-admin-role % :owner)]
                  :handler (update-tenant-handler db)}}]
   ["/:id/members"
    ["" {:get {:handler (list-members-handler db)}}]
    ["/:member-id"
     {:middleware [#(admin-mw/wrap-admin-role % :owner)]}
     ["/role" {:put {:handler (change-member-role-handler db)}}]
     ["" {:delete {:handler (remove-member-handler db)}}]]]])

(comment
  ;; (require 'app.template.backend.routes.admin.tenants :reload)
  :rcf)

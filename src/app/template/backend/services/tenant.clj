(ns app.template.backend.services.tenant
  "Tenant lifecycle service — provisioning, lookup, membership queries.

   Uses raw next.jdbc + honey.sql (not the db-protocols adapter) because
   these queries are multi-table JOINs / transactions that don't need the
   generic CRUD type-casting pipeline."
  (:require
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [app.template.backend.auth.service :as auth-service]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Slug Generation
;; ============================================================================

(defn generate-slug
  "Derive a URL-safe slug from an email address.
   Takes the prefix before @, lowercases, replaces non-alphanum with hyphens,
   and trims leading/trailing hyphens."
  [email]
  (-> email
    (str/split #"@")
    first
    str/lower-case
    (str/replace #"[^a-z0-9]+" "-")
    (str/replace #"^-+|-+$" "")))

(defn- ->jdbc-conn
  "Accept either a raw next.jdbc connectable or the template db-adapter record."
  [db]
  (or (when (map? db) (:connection db))
    db))

(defn ensure-unique-slug
  "Return `base-slug` if available, otherwise append -2, -3, … until unique."
  [db base-slug]
  (let [db (->jdbc-conn db)
        taken? (fn [slug]
                 (some? (jdbc/execute-one! db
                          (sql/format {:select [[:id]]
                                       :from   [:tenants]
                                       :where  [:= :slug slug]
                                       :limit  1}))))]
    (if-not (taken? base-slug)
      base-slug
      (loop [n 2]
        (let [candidate (str base-slug "-" n)]
          (if-not (taken? candidate)
            candidate
            (recur (inc n))))))))

;; ============================================================================
;; Tenant Provisioning
;; ============================================================================

(defn generate-tenant-name
  "Derive a human-friendly workspace name from a user record."
  [user]
  (let [full-name (some-> (or (:full_name user) (:users/full_name user)) str/trim not-empty)
        email     (or (:email user) (:users/email user))]
    (if full-name
      (str full-name "'s workspace")
      (str (first (str/split email #"@")) "'s workspace"))))

(defn- user-email [user]
  (or (:email user) (:users/email user)))

(defn- user-full-name [user]
  (or (:full_name user) (:users/full_name user)))

(defn- user-id [user]
  (let [id (or (:id user) (:users/id user))]
    (if (string? id) (java.util.UUID/fromString id) id)))

(defn- resolve-label
  "Return a display string from a seed label. Accepts either a plain string
  or a locale map {:en \"...\" :bs \"...\"}. Falls back to :en, then string repr."
  [label-or-map locale]
  (if (map? label-or-map)
    (or (get label-or-map locale)
      (get label-or-map :en)
      (str label-or-map))
    label-or-map))

(defn provision-tenant!
  "Create a new tenant + owner membership + seed lookup tables, all inside a
   single transaction. Returns {:tenant <row> :membership <row>}."
  [db config user]
  (let [db         (->jdbc-conn db)
        base-slug  (generate-slug (user-email user))
        slug       (ensure-unique-slug db base-slug)
        name       (generate-tenant-name user)
        tenant-id  (java.util.UUID/randomUUID)
        member-id  (java.util.UUID/randomUUID)
        now        (java.time.LocalDateTime/now)
        defaults   (:tenant-defaults config)
        locale     (or (:default-locale config) :bs)]
    (jdbc/with-transaction [tx db]
      ;; 1) Create tenant
      (let [tenant (convert-pg-objects
                     (jdbc/execute-one! tx
                       (sql/format {:insert-into [:tenants]
                                    :values [{:id         tenant-id
                                              :name       name
                                              :slug       slug
                                              :status     [:cast "active" :tenant_status]
                                              :created_at now
                                              :updated_at now}]
                                    :returning [:*]})))
            ;; 2) Create owner membership
            membership (convert-pg-objects
                         (jdbc/execute-one! tx
                           (sql/format {:insert-into [:tenant_memberships]
                                        :values [{:id         member-id
                                                  :tenant_id  tenant-id
                                                  :user_id    (user-id user)
                                                  :role       [:cast "owner" :membership_role]
                                                  :status     [:cast "active" :membership_status]
                                                  :created_at now
                                                  :updated_at now}]
                                        :returning [:*]})))]

        ;; 3) Seed payer_types — capture default type id for owner payer
        (let [pt-rows (mapv
                        (fn [pt]
                          (jdbc/execute-one! tx
                            (sql/format {:insert-into [:payer_types]
                                         :values [{:id         (java.util.UUID/randomUUID)
                                                   :tenant_id  tenant-id
                                                   :label      (resolve-label (:label pt) locale)
                                                   :is_default (boolean (:is-default pt))
                                                   :created_at now
                                                   :updated_at now}]
                                         :returning [:id :is_default]})
                            {:builder-fn rs/as-unqualified-lower-maps}))
                        (:payer-types defaults))
              default-pt-id (->> pt-rows (filter :is_default) first :id)]

          ;; 4) Seed owner payer (linked to the default payer type)
          (when default-pt-id
            (let [owner-label (or (some-> (user-full-name user) str/trim not-empty)
                                (first (str/split (user-email user) #"@")))]
              (jdbc/execute-one! tx
                (sql/format {:insert-into [:payers]
                             :values [{:id            (java.util.UUID/randomUUID)
                                       :tenant_id     tenant-id
                                       :payer_type_id default-pt-id
                                       :label         owner-label
                                       :is_default    true
                                       :created_at    now
                                       :updated_at    now}]}))))

          ;; 5) Seed expense_categories
          (doseq [cat (:expense-categories defaults)]
            (jdbc/execute-one! tx
              (sql/format {:insert-into [:expense_categories]
                           :values [{:id         (java.util.UUID/randomUUID)
                                     :tenant_id  tenant-id
                                     :name       (resolve-label (:name cat) locale)
                                     :created_at now
                                     :updated_at now}]})))

          (log/info "Provisioned tenant" slug "for user" (user-email user)
            "with" (count (:payer-types defaults)) "payer types,"
            (count (:expense-categories defaults)) "expense categories, and owner payer")

          {:tenant tenant :membership membership})))))

;; ============================================================================
;; Lookup
;; ============================================================================

(defn find-tenant-by-slug
  "Return the active tenant with the given slug, or nil."
  [db slug]
  (convert-pg-objects
    (jdbc/execute-one! (->jdbc-conn db)
      (sql/format {:select [:*]
                   :from   [:tenants]
                   :where  [:and
                            [:= :slug slug]
                            [:= :status [:cast "active" :tenant_status]]]}))))

(defn find-tenant-by-id
  "Return the tenant with the given id, or nil."
  [db id]
  (let [uuid-id (if (string? id) [:cast id :uuid] id)]
    (convert-pg-objects
      (jdbc/execute-one! (->jdbc-conn db)
        (sql/format {:select [:*]
                     :from   [:tenants]
                     :where  [:= :id uuid-id]})))))

;; ============================================================================
;; Membership Queries
;; ============================================================================

(defn get-user-memberships
  "Return all active memberships for a user, joined with tenant info."
  [db user-id]
  (let [db      (->jdbc-conn db)
        uuid-id (if (string? user-id) [:cast user-id :uuid] user-id)]
    (let [opts {:builder-fn rs/as-unqualified-maps}]
      (mapv convert-pg-objects
        (jdbc/execute! db
          (sql/format {:select [:tm.*
                                [:t.name :tenant_name]
                                [:t.slug :tenant_slug]]
                       :from   [[:tenant_memberships :tm]]
                       :join   [[:tenants :t] [:= :tm.tenant_id :t.id]]
                       :where  [:and
                                [:= :tm.user_id uuid-id]
                                [:= :tm.status [:cast "active" :membership_status]]
                                [:= :t.status [:cast "active" :tenant_status]]]})
          opts)))))

(defn get-tenant-members
  "Return members of a tenant, joined with user info.

   Options:
   - :include-suspended? When true, include suspended memberships (default false)."
  ([db tenant-id]
   (get-tenant-members db tenant-id {}))
  ([db tenant-id {:keys [include-suspended?]
                  :or {include-suspended? false}}]
   (let [db (->jdbc-conn db)
         uuid-id (if (string? tenant-id) [:cast tenant-id :uuid] tenant-id)
         opts {:builder-fn rs/as-unqualified-maps}
         where-clause (cond-> [:and
                               [:= :tm.tenant_id uuid-id]]
                        (not include-suspended?)
                        (conj [:= :tm.status [:cast "active" :membership_status]]))
         query {:select [:tm.*
                         [:u.email :user_email]
                         [:u.full_name :user_full_name]
                         [:u.status :user_status]]
                :from [[:tenant_memberships :tm]]
                :join [[:users :u] [:= :tm.user_id :u.id]]
                :where where-clause
                :order-by [[:tm.created_at :asc]]}]
     (mapv convert-pg-objects
       (jdbc/execute! db (sql/format query) opts)))))

(defn get-membership
  "Return the single active membership for a user in a tenant, or nil."
  [db tenant-id user-id]
  (let [db (->jdbc-conn db)
        t-id (if (string? tenant-id) [:cast tenant-id :uuid] tenant-id)
        u-id (if (string? user-id) [:cast user-id :uuid] user-id)]
    (convert-pg-objects
      (jdbc/execute-one! db
        (sql/format {:select [:*]
                     :from   [:tenant_memberships]
                     :where  [:and
                              [:= :tenant_id t-id]
                              [:= :user_id u-id]
                              [:= :status [:cast "active" :membership_status]]]})))))

(comment
  ;; REPL helpers
  ;; (require 'app.template.backend.services.tenant :reload)
  ;; (generate-slug "enes.bajric@example.com")
  ;; => "enes-bajric"
  :rcf)

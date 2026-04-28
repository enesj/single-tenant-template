(ns app.template.backend.services.tenant
  "Tenant lifecycle service — provisioning, lookup, membership queries.

   Uses raw next.jdbc + honey.sql (not the db-protocols adapter) because
   these queries are multi-table JOINs / transactions that don't need the
   generic CRUD type-casting pipeline."
  (:require
    [app.shared.adapters.database :refer [convert-pg-objects]]
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.security.privacy-subject :as privacy-subject]
    [app.template.backend.services.onboarding.core :as onboarding]
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
        email     (or (:email user) (:users/email user) (email-privacy/resolve-email user))]
    (if full-name
      (str full-name "'s workspace")
      (str (first (str/split email #"@")) "'s workspace"))))

(defn- user-email [user]
  (or (:email user) (:users/email user) (email-privacy/resolve-email user)))

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

(def ^:private default-expense-category-template
  {:name {:en "Default" :bs "Podrazumijevano"}
   :is-default true})

(defn- same-expense-category-name?
  [locale category-a category-b]
  (= (some-> (resolve-label (:name category-a) locale) str/trim str/lower-case)
    (some-> (resolve-label (:name category-b) locale) str/trim str/lower-case)))

(defn- normalize-expense-category-defaults
  "Provision exactly one default expense category for every new tenant.

   We always seed a dedicated placeholder default category so owner onboarding
   can guide the workspace-specific rename without leaving the tenant in a
   zero-default state."
  [defaults locale]
  (let [configured (vec (or (:expense-categories defaults) []))
        default-category (or (some #(when (same-expense-category-name? locale % default-expense-category-template)
                                      %)
                               configured)
                           default-expense-category-template)
        remaining (remove #(same-expense-category-name? locale % default-category)
                    configured)]
    (vec (cons (assoc default-category :is-default true)
           (map #(assoc % :is-default false) remaining)))))

(defn provision-tenant!
  "Create a new tenant + owner membership + seed lookup tables, all inside a
   single transaction. Returns {:tenant <row> :membership <row>}."
  [db config user]
  (let [db (->jdbc-conn db)
        base-slug (generate-slug (user-email user))
        slug (ensure-unique-slug db base-slug)
        name (generate-tenant-name user)
        tenant-id (java.util.UUID/randomUUID)
        member-id (java.util.UUID/randomUUID)
        now (java.time.LocalDateTime/now)
        defaults (:tenant-defaults config)
        locale (or (:default-locale config) :bs)
        expense-category-defaults (normalize-expense-category-defaults defaults locale)
        payer-defaults (vec (or (:payers defaults) []))]
    (jdbc/with-transaction [tx db]
      ;; 1) Create tenant
      (let [tenant (convert-pg-objects
                     (jdbc/execute-one! tx
                       (sql/format {:insert-into [:tenants]
                                    :values [{:id tenant-id
                                              :name name
                                              :slug slug
                                              :status [:cast "active" :tenant_status]
                                              :created_at now
                                              :updated_at now}]
                                    :returning [:*]})))
            ;; 2) Create owner membership
            membership (convert-pg-objects
                         (jdbc/execute-one! tx
                           (sql/format {:insert-into [:tenant_memberships]
                                        :values [{:id member-id
                                                  :tenant_id tenant-id
                                                  :user_id (user-id user)
                                                  :role [:cast "owner" :membership_role]
                                                  :status [:cast "active" :membership_status]
                                                  :created_at now
                                                  :updated_at now}]
                                        :returning [:*]})))
            owner-label (or (some-> (user-full-name user) str/trim not-empty)
                          (first (str/split (user-email user) #"@")))
            owner-payer-id (java.util.UUID/randomUUID)]

        ;; 3) Seed starter payers configured for the tenant.
        (doseq [payer payer-defaults]
          (jdbc/execute-one! tx
            (sql/format {:insert-into [:payers]
                         :values [{:id (java.util.UUID/randomUUID)
                                   :tenant_id tenant-id
                                   :type [:cast "custom" :payer_type]
                                   :label (resolve-label (:label payer) locale)
                                   :is_default false
                                   :is_active true
                                   :created_at now
                                   :updated_at now}]})))

        ;; 4) Create owner payer (system-provisioned)
        (jdbc/execute-one! tx
          (sql/format {:insert-into [:payers]
                       :values [{:id owner-payer-id
                                 :tenant_id tenant-id
                                 :type [:cast "system" :payer_type]
                                 :label owner-label
                                 :is_default true
                                 :is_active true
                                 :created_at now
                                 :updated_at now}]}))

        ;; 4b) Create user_expense_settings for the owner
        (jdbc/execute-one! tx
          (sql/format {:insert-into [:user_expense_settings]
                       :values [{:id (java.util.UUID/randomUUID)
                                 :tenant_id tenant-id
                                 :subject_ref (privacy-subject/user-subject-ref (user-id user))
                                 :default_payer_id owner-payer-id
                                 :created_at now
                                 :updated_at now}]}))

        ;; 4c) Provision tenant_settings for the new tenant (Phase 2 — settings hierarchy)
        (jdbc/execute-one! tx
          (sql/format {:insert-into [:tenant_settings]
                       :values [{:id (java.util.UUID/randomUUID)
                                 :tenant_id tenant-id
                                 :email_notifications true
                                 :created_at now
                                 :updated_at now}]
                       :on-conflict [:tenant_id]
                       :do-nothing true}))

        ;; 5) Initialise onboarding for owner role
        (onboarding/initialise-onboarding! tx (user-id user) "owner")

        ;; 6) Seed expense_categories
        (doseq [cat expense-category-defaults]
          (jdbc/execute-one! tx
            (sql/format {:insert-into [:expense_categories]
                         :values [{:id (java.util.UUID/randomUUID)
                                   :tenant_id tenant-id
                                   :name (resolve-label (:name cat) locale)
                                   :exclude_from_reports (boolean (:exclude-from-reports cat))
                                   :is_default (boolean (:is-default cat))
                                   :created_at now
                                   :updated_at now}]})))

        (log/info "Provisioned tenant"
          {:tenant-slug slug
           :user-id (user-id user)
           :user-ref (email-privacy/user-ref (user-id user))
           :email-masked (email-privacy/mask-email (user-email user))
           :payer-count (inc (count payer-defaults))
           :expense-category-count (count expense-category-defaults)})

        {:tenant tenant :membership membership}))))

;; ============================================================================
;; User Payer Provisioning
;; ============================================================================

(defn provision-user-payer!
  "Create a system payer for a new tenant member.
   Label = full name or email prefix (before @).
   Also upserts user_expense_settings with the new payer as default.

   `db-or-tx` can be a raw JDBC connectable or a transaction connection.
   Returns the created payer row."
  [db-or-tx tenant-id user-id user-email & {:keys [full-name]}]
  (let [t-id (if (string? tenant-id) (java.util.UUID/fromString tenant-id) tenant-id)
        u-id (if (string? user-id) (java.util.UUID/fromString user-id) user-id)
        now (java.time.LocalDateTime/now)
        label (or (some-> full-name str/trim not-empty)
                (first (str/split (str user-email) #"@")))
        payer-id (java.util.UUID/randomUUID)
        payer (jdbc/execute-one! db-or-tx
                (sql/format {:insert-into [:payers]
                             :values [{:id payer-id
                                       :tenant_id t-id
                                       :type [:cast "system" :payer_type]
                                       :label label
                                       :is_default false
                                       :is_active true
                                       :created_at now
                                       :updated_at now}]
                             :returning [:*]})
                {:builder-fn rs/as-unqualified-lower-maps})]
    ;; Upsert user_expense_settings with this payer as default
    (jdbc/execute-one! db-or-tx
      (sql/format {:insert-into [:user_expense_settings]
                   :values [{:id (java.util.UUID/randomUUID)
                             :tenant_id t-id
                             :subject_ref (privacy-subject/user-subject-ref u-id)
                             :default_payer_id payer-id
                             :created_at now
                             :updated_at now}]
                   :on-conflict [:tenant_id :subject_ref]
                   :do-update-set {:default_payer_id payer-id
                                   :updated_at now}}))
    (log/info "Provisioned user payer"
      {:label label
       :tenant-id t-id
       :tenant-ref (email-privacy/tenant-ref t-id)
       :user-id u-id
       :user-ref (email-privacy/user-ref u-id)
       :email-masked (email-privacy/mask-email user-email)})
    payer))

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

(defn update-tenant!
  "Update a tenant's mutable fields. Currently supports :name.
   Returns the updated tenant row."
  [db tenant-id updates]
  (let [uuid-id (if (string? tenant-id) [:cast tenant-id :uuid] tenant-id)
        set-map (cond-> {:updated_at [:now]}
                  (contains? updates :name)
                  (assoc :name (:name updates)))]
    (convert-pg-objects
      (jdbc/execute-one! (->jdbc-conn db)
        (sql/format {:update :tenants
                     :set set-map
                     :where [:= :id uuid-id]
                     :returning [:*]})))))

;; ============================================================================
;; Membership Queries
;; ============================================================================

(defn get-user-memberships
  "Return all active memberships for a user, joined with tenant info."
  [db user-id]
  (let [db      (->jdbc-conn db)
        uuid-id (if (string? user-id) [:cast user-id :uuid] user-id)
        opts {:builder-fn rs/as-unqualified-maps}]
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
        opts))))

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
                         [:u.email_ciphertext :user_email_ciphertext]
                         [:u.full_name :user_full_name]
                         [:u.status :user_status]]
                :from [[:tenant_memberships :tm]]
                :join [[:users :u] [:= :tm.user_id :u.id]]
                :where where-clause
                :order-by [[:tm.created_at :asc]]}]
     (->> (jdbc/execute! db (sql/format query) opts)
       (mapv convert-pg-objects)
       (mapv (fn [row]
               (let [email (email-privacy/resolve-email row)]
                 (cond-> row
                   email (assoc :user_email email)))))))))

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

(ns app.admin.backend.services.admin.billing
  "Admin billing helpers for generic payment-provider account links."
  (:require
    [app.admin.backend.services.admin.audit :as audit]
    [app.shared.adapters.database :as shared-db]
    [app.shared.query-builders :as shared-qb]
    [app.template.backend.security.email :as email-privacy]
    [clojure.string :as str]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(def ^:private valid-account-kinds
  #{"user" "tenant"})

(def ^:private valid-link-statuses
  #{"active" "inactive" "disabled" "revoked"})

(def ^:private allowed-order-by
  {:created-at :ppl/created_at
   :provider :ppl/provider
   :provider-customer-ref :ppl/provider_customer_ref
   :status :ppl/status
   :account-kind :ppl/account_kind
   :account-ref :ppl/account_id})

(defn- normalize-string
  [value]
  (some-> value str str/trim not-empty))

(defn- normalize-account-kind
  [account-kind]
  (some-> account-kind normalize-string str/lower-case))

(defn- normalize-provider
  [provider]
  (some-> provider normalize-string str/lower-case))

(defn- normalize-status
  [status]
  (some-> status normalize-string str/lower-case))

(defn- account-table
  [account-kind]
  (case account-kind
    "user" :users
    "tenant" :tenants
    nil))

(defn- account-ref
  [account-kind account-id]
  (case account-kind
    "user" (email-privacy/user-ref account-id)
    "tenant" (email-privacy/tenant-ref account-id)
    nil))

(defn- account-exists?
  [db account-kind account-id]
  (when-let [table (account-table account-kind)]
    (some?
      (jdbc/execute-one! db
        (hsql/format {:select [:id]
                      :from [table]
                      :where [:= :id account-id]
                      :limit 1})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn- validate-account-kind!
  [account-kind]
  (let [account-kind* (normalize-account-kind account-kind)]
    (when-not (contains? valid-account-kinds account-kind*)
      (throw (ex-info "Account kind must be one of: user, tenant"
               {:status 400
                :field :account-kind
                :allowed (sort valid-account-kinds)})))
    account-kind*))

(defn- validate-provider!
  [provider]
  (let [provider* (normalize-provider provider)]
    (when-not provider*
      (throw (ex-info "Provider is required"
               {:status 400
                :field :provider})))
    provider*))

(defn- validate-provider-customer-ref!
  [provider-customer-ref]
  (let [provider-customer-ref* (normalize-string provider-customer-ref)]
    (when-not provider-customer-ref*
      (throw (ex-info "Provider customer ref is required"
               {:status 400
                :field :provider-customer-ref})))
    provider-customer-ref*))

(defn- validate-status!
  [status]
  (let [status* (normalize-status status)]
    (when-not (contains? valid-link-statuses status*)
      (throw (ex-info "Status must be one of: active, inactive, disabled, revoked"
               {:status 400
                :field :status
                :allowed (sort valid-link-statuses)})))
    status*))

(defn- validate-account-id!
  [db account-kind account-id]
  (when-not account-id
    (throw (ex-info "Account ID is required"
             {:status 400
              :field :account-id})))
  (when-not (account-exists? db account-kind account-id)
    (throw (ex-info "Account not found"
             {:status 404
              :field :account-id
              :account-kind account-kind
              :account-id account-id})))
  account-id)

(defn- normalize-link-row
  [row]
  (let [converted (shared-db/to-app row)
        account-kind (normalize-account-kind (:account-kind converted))
        account-id (:account-id converted)
        provider (normalize-provider (:provider converted))
        status (normalize-status (:status converted))]
    (cond-> (-> converted
              (assoc :account-kind account-kind
                :provider provider
                :status status)
              (dissoc :account-id))
      account-id (assoc :account-ref (account-ref account-kind account-id)))))

(defn- build-list-conditions
  [{:keys [account-kind provider status search]}]
  (let [account-kind* (some-> account-kind normalize-account-kind)
        provider* (some-> provider normalize-provider)
        status* (some-> status normalize-status)
        search* (some-> search normalize-string)]
    (cond-> []
      account-kind* (conj [:= :ppl.account_kind [:cast account-kind* :payment_provider_account_kind]])
      provider* (conj [:= :ppl.provider provider*])
      status* (conj [:= :ppl.status status*])
      search* (conj [:or
                     [:ilike :ppl.provider_customer_ref (str "%" search* "%")]
                     [:ilike :ppl.provider (str "%" search* "%")]]))))

(defn- build-list-query
  [{:keys [limit offset sorts order-by order-dir] :as opts}]
  (let [conditions (build-list-conditions opts)
        order-clauses (shared-qb/resolve-order-by-clauses
                        {:sorts sorts
                         :order-by order-by
                         :order-dir order-dir
                         :allowed-order-by allowed-order-by
                         :default-order-by :ppl/created_at
                         :default-order-dir :desc
                         :tie-breaker [:ppl/id :asc]})]
    (cond-> {:select [:ppl.*]
             :from [[:payment_provider_account_links :ppl]]
             :order-by order-clauses
             :limit (or limit 50)
             :offset (or offset 0)}
      (seq conditions) (assoc :where (into [:and] conditions)))))

(defn- build-count-query
  [opts]
  (let [conditions (build-list-conditions opts)]
    (cond-> {:select [[[:count :*] :total]]
             :from [[:payment_provider_account_links :ppl]]}
      (seq conditions) (assoc :where (into [:and] conditions)))))

(defn list-provider-links
  "List payment-provider account links for admin billing operations."
  [db opts]
  (->> (jdbc/execute! db
         (hsql/format (build-list-query opts))
         {:builder-fn rs/as-unqualified-lower-maps})
    (mapv normalize-link-row)))

(defn count-provider-links
  "Count payment-provider account links using the same filters as `list-provider-links`."
  [db opts]
  (let [row (jdbc/execute-one! db (hsql/format (build-count-query opts)))
        total (or (:total row) (some-> row vals first) 0)]
    (long total)))

(defn list-provider-links-page
  "List payment-provider account links and include pagination metadata."
  [db {:keys [limit offset] :as opts}]
  (let [limit* (or limit 50)
        offset* (or offset 0)]
    {:links (list-provider-links db (assoc opts :limit limit* :offset offset*))
     :total (count-provider-links db opts)
     :limit limit*
     :offset offset*}))

(defn create-provider-link!
  "Create a payment-provider link keyed by internal account identity, never email."
  [db {:keys [account-kind account-id provider provider-customer-ref status]}
   admin-id ip-address user-agent]
  (let [account-kind* (validate-account-kind! account-kind)
        account-id* (validate-account-id! db account-kind* account-id)
        provider* (validate-provider! provider)
        provider-customer-ref* (validate-provider-customer-ref! provider-customer-ref)
        status* (if status
                  (validate-status! status)
                  "active")
        now (time/instant)]
    (try
      (let [result (jdbc/execute-one! db
                     (hsql/format
                       {:insert-into :payment_provider_account_links
                        :values [{:id (random-uuid)
                                  :account_kind [:cast account-kind* :payment_provider_account_kind]
                                  :account_id account-id*
                                  :provider provider*
                                  :provider_customer_ref provider-customer-ref*
                                  :status status*
                                  :created_at now
                                  :updated_at now}]
                        :returning [:*]})
                     {:builder-fn rs/as-unqualified-lower-maps})
            normalized (normalize-link-row result)]
        (audit/log-audit! db
          {:admin_id admin-id
           :action "link_payment_provider_account"
           :entity-type "payment_provider_account_link"
           :entity-id (:id result)
           :changes {:account_kind account-kind*
                     :account_ref (:account-ref normalized)
                     :provider provider*
                     :provider_customer_ref provider-customer-ref*
                     :status status*}
           :ip-address ip-address
           :user-agent user-agent})
        normalized)
      (catch org.postgresql.util.PSQLException e
        (if (= "23505" (.getSQLState e))
          (throw (ex-info "A provider link already exists for this account/provider combination"
                   {:status 409
                    :field :provider-customer-ref
                    :provider provider*}))
          (throw e))))))

(defn update-provider-link-status!
  "Update the status of an existing payment-provider account link."
  [db link-id status admin-id ip-address user-agent]
  (let [status* (validate-status! status)
        existing (jdbc/execute-one! db
                   (hsql/format {:select [:*]
                                 :from [:payment_provider_account_links]
                                 :where [:= :id link-id]
                                 :limit 1})
                   {:builder-fn rs/as-unqualified-lower-maps})]
    (when-not existing
      (throw (ex-info "Payment provider link not found"
               {:status 404
                :field :id
                :id link-id})))
    (let [updated (jdbc/execute-one! db
                    (hsql/format {:update :payment_provider_account_links
                                  :set {:status status*
                                        :updated_at (time/instant)}
                                  :where [:= :id link-id]
                                  :returning [:*]})
                    {:builder-fn rs/as-unqualified-lower-maps})
          normalized-existing (normalize-link-row existing)
          normalized-updated (normalize-link-row updated)]
      (audit/log-audit! db
        {:admin_id admin-id
         :action "update_payment_provider_status"
         :entity-type "payment_provider_account_link"
         :entity-id link-id
         :changes {:account_kind (:account-kind normalized-updated)
                   :account_ref (:account-ref normalized-updated)
                   :provider (:provider normalized-updated)
                   :provider_customer_ref (:provider-customer-ref normalized-updated)
                   :status_from (:status normalized-existing)
                   :status_to (:status normalized-updated)}
         :ip-address ip-address
         :user-agent user-agent})
      normalized-updated)))

(comment
  ;; (require 'app.admin.backend.services.admin.billing :reload)
  :rcf)

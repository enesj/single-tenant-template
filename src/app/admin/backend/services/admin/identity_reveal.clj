(ns app.admin.backend.services.admin.identity-reveal
  "Exceptional, audited identity reveal helpers for admin support workflows."
  (:require
    [app.admin.backend.services.admin.audit :as audit]
    [app.template.backend.security.email :as email-privacy]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(def ^:private reveal-targets
  {:user {:table :users
          :entity-type "user"
          :action "reveal_user_email"}
   :admin {:table :admins
           :entity-type "admin"
           :action "reveal_admin_email"}})

(def ^:private min-reason-length 20)

(def ^:private reveal-reason-codes
  {:account-security "Account security investigation"
   :legal-request "Legal or compliance request"
   :data-subject-request "Data subject access request"
   :identity-management "Identity management correction"
   :production-incident "Production incident response"})

(defn- normalize-reason
  [reason]
  (some-> reason str str/trim not-empty))

(defn- normalize-reason-code
  [reason-code]
  (some-> reason-code
    (cond-> (keyword? reason-code) name)
    str
    str/trim
    str/lower-case
    (str/replace "_" "-")
    not-empty
    keyword))

(defn- validate-reason-code!
  [reason-code]
  (let [reason-code* (normalize-reason-code reason-code)]
    (when-not reason-code*
      (throw (ex-info "Break-glass reason code is required"
               {:status 400
                :field :reason-code
                :allowed (-> reveal-reason-codes keys sort vec)})))
    (when-not (contains? reveal-reason-codes reason-code*)
      (throw (ex-info "Unsupported break-glass reason code"
               {:status 400
                :field :reason-code
                :value reason-code
                :allowed (-> reveal-reason-codes keys sort vec)})))
    reason-code*))

(defn- validate-reason!
  [reason]
  (let [reason* (normalize-reason reason)]
    (when-not reason*
      (throw (ex-info "Break-glass reason details are required"
               {:status 400
                :field :reason})))
    (when (< (count reason*) min-reason-length)
      (throw (ex-info "Break-glass reason details must be at least 20 characters"
               {:status 400
                :field :reason
                :min-length min-reason-length})))
    reason*))

(defn- fetch-entity-row
  [db table entity-id]
  (jdbc/execute-one! db
    (sql/format {:select [:id :email_ciphertext :email_lookup_hash :email_key_version]
                 :from [table]
                 :where [:= :id entity-id]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn reveal-email!
  "Reveal a user's or admin's email for exceptional break-glass flows.

   Requires a structured reason code plus non-blank details and always emits an
   audit event. Returns {:reveal {...} :reason-code <code> :reason <details>}
   with both masked and unmasked email. Routine identity-management pages should
   be preferred over this endpoint whenever possible."
  [db target-kind entity-id {:keys [admin-id reason reason-code reason_code ip-address user-agent]}]
  (let [{:keys [table entity-type action]} (or (get reveal-targets target-kind)
                                             (throw (ex-info "Unsupported reveal target"
                                                      {:status 400
                                                       :target-kind target-kind})))
        reason-code* (validate-reason-code! (or reason-code reason_code))
        reason* (validate-reason! reason)
        row (fetch-entity-row db table entity-id)]
    (when-not row
      (throw (ex-info (str (str/capitalize entity-type) " not found")
               {:status 404
                :entity-type entity-type
                :entity-id entity-id})))
    (let [payload (email-privacy/reveal-email-payload target-kind row)
          reason-label (get reveal-reason-codes reason-code*)]
      (audit/log-audit! db
        {:admin_id admin-id
         :action action
         :entity-type entity-type
         :entity-id entity-id
         :changes {:reason_code reason-code*
                   :reason_label reason-label
                   :reason reason*
                   :entity_ref (:entity_ref payload)
                   :email_masked (:email_masked payload)
                   :revealed true}
         :ip-address ip-address
         :user-agent user-agent})
      {:reveal payload
       :reason-code reason-code*
       :reason-label reason-label
       :reason reason*})))

(comment
  ;; (require 'app.admin.backend.services.admin.identity-reveal :reload)
  :rcf)

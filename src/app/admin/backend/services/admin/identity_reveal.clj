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

(def ^:private min-reason-length 10)

(defn- normalize-reason
  [reason]
  (some-> reason str str/trim not-empty))

(defn- validate-reason!
  [reason]
  (let [reason* (normalize-reason reason)]
    (when-not reason*
      (throw (ex-info "Support reason is required"
               {:status 400
                :field :reason})))
    (when (< (count reason*) min-reason-length)
      (throw (ex-info "Support reason must be at least 10 characters"
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
  "Reveal a user's or admin's email for exceptional support flows.

   Requires a non-blank support reason and always emits an audit event.
   Returns {:reveal {...} :reason <reason>} with both masked and unmasked email."
  [db target-kind entity-id {:keys [admin-id reason ip-address user-agent]}]
  (let [{:keys [table entity-type action]} (or (get reveal-targets target-kind)
                                             (throw (ex-info "Unsupported reveal target"
                                                      {:status 400
                                                       :target-kind target-kind})))
        reason* (validate-reason! reason)
        row (fetch-entity-row db table entity-id)]
    (when-not row
      (throw (ex-info (str (str/capitalize entity-type) " not found")
               {:status 404
                :entity-type entity-type
                :entity-id entity-id})))
    (let [payload (email-privacy/reveal-email-payload target-kind row)]
      (audit/log-audit! db
        {:admin_id admin-id
         :action action
         :entity-type entity-type
         :entity-id entity-id
         :changes {:reason reason*
                   :entity_ref (:entity_ref payload)
                   :email_masked (:email_masked payload)
                   :revealed true}
         :ip-address ip-address
         :user-agent user-agent})
      {:reveal payload
       :reason reason*})))

(comment
  ;; (require 'app.admin.backend.services.admin.identity-reveal :reload)
  :rcf)

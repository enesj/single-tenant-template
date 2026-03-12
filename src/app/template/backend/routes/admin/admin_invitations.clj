(ns app.template.backend.routes.admin.admin-invitations
  "Admin invitation routes — public (accept/validate) and protected (CRUD).

   Public endpoints live outside the auth middleware because the invitee
   doesn't have an admin account yet."
  (:require
    [app.admin.backend.services.admin.admin-invitation :as inv-svc]
    [app.shared.adapters.database :as shared-db]
    [app.template.backend.routes.admin.utils :as utils]
    [app.template.backend.services.invitation-email :as inv-email]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Public Handlers (no auth required)
;; ============================================================================

(defn validate-invitation-handler
  "GET /admin/api/invitations/validate?token=...
   Returns invitation details if token is valid."
  [db]
  (fn [request]
    (try
      (let [token (or (get-in request [:query-params "token"])
                    (get-in request [:params :token]))]
        (if (empty? token)
          (utils/error-response "Token is required" :status 400)
          (let [invitation (inv-svc/find-invitation-by-token db token)]
            (if-not invitation
              (utils/json-response {:valid false :error "Invalid invitation token"})
              (let [status (str (:status invitation))
                    expired? (try
                               (let [exp (:expires_at invitation)]
                                 (when exp
                                   (let [inst (cond
                                                (instance? java.time.Instant exp) exp
                                                (instance? java.time.OffsetDateTime exp) (.toInstant exp)
                                                :else (.toInstant (java.time.OffsetDateTime/parse (str exp))))]
                                     (.isBefore inst (java.time.Instant/now)))))
                               (catch Exception _ false))
                    valid? (and (= "pending" status) (not expired?))]
                (utils/json-response
                  {:valid         valid?
                   :email         (:email invitation)
                   :role          (str (:role invitation))
                   :inviter-name  (:inviter_name invitation)
                   :inviter-email (:inviter_email invitation)
                   :error         (when-not valid?
                                    (if expired?
                                      "This invitation has expired"
                                      (str "This invitation is " status)))}))))))
      (catch Exception e
        (log/error e "Failed to validate invitation token")
        (utils/error-response "Internal server error" :status 500)))))

(defn accept-invitation-handler
  "POST /admin/api/invitations/accept
   Creates admin account + session from invitation token."
  [db]
  (fn [request]
    (try
      (let [body (:body request)
            token (or (:token body) (get body "token"))
            full-name (or (:full-name body) (:full_name body)
                        (get body "full-name") (get body "full_name") (get body "fullName"))
            password (or (:password body) (get body "password"))
            {:keys [ip-address user-agent]} (utils/extract-request-context request)]
        (cond
          (empty? token)
          (utils/error-response "Invitation token is required" :status 400)

          (empty? full-name)
          (utils/error-response "Full name is required" :status 400)

          (or (empty? password) (< (count password) 10))
          (utils/error-response "Password must be at least 10 characters" :status 400)

          :else
          (let [{:keys [admin session]}
                (inv-svc/accept-invitation! db
                  {:token      token
                   :full-name  full-name
                   :password   password
                   :ip-address ip-address
                   :user-agent user-agent})]
            (utils/json-response
              {:success true
               :admin   {:id        (:id admin)
                         :email     (:email admin)
                         :full_name (:full_name admin)
                         :role      (str (:role admin))}
               :token   (:token session)}
              :status 201))))
      (catch clojure.lang.ExceptionInfo e
        (let [data (ex-data e)]
          (if (= :validation-error (:type data))
            (utils/json-response {:success false
                                  :error   (.getMessage e)
                                  :errors  (:errors data)}
              :status 400)
            (do
              (log/error e "Failed to accept invitation")
              (utils/error-response "Internal server error" :status 500)))))
      (catch Exception e
        (log/error e "Failed to accept invitation")
        (utils/error-response "Internal server error" :status 500)))))

;; ============================================================================
;; Protected Handlers (owner auth required)
;; ============================================================================

(defn- assert-owner-role! [request]
  (let [admin (:admin request)
        role (str (:role admin))]
    (when (not= "owner" role)
      (throw (ex-info "Only owners can manage admin invitations"
               {:status 403})))))

(defn list-invitations-handler
  "GET /admin/api/invitations — list all pending admin invitations."
  [db]
  (utils/with-error-handling
    (fn [request]
      (assert-owner-role! request)
      (let [invitations (inv-svc/list-pending-invitations db)
            converted (shared-db/to-app invitations)]
        (utils/json-response {:invitations converted})))
    "Failed to list admin invitations"))

(defn create-invitation-handler
  "POST /admin/api/invitations — create a new admin invitation and send email."
  [db email-service base-url]
  (utils/with-validation-error-handling
    (fn [request]
      (assert-owner-role! request)
      (let [admin (:admin request)
            body (:body request)
            email (:email body)
            role (or (:role body) "admin")]
        (when (empty? email)
          (throw (ex-info "Email is required" {:status 400 :field :email})))
        (let [invitation (inv-svc/create-invitation! db
                           {:email      email
                            :role       role
                            :invited-by (:id admin)})
              token (str (:token invitation))
              accept-url (str base-url "/admin/invitation/accept?token=" token)]
          ;; Send email asynchronously (fire-and-forget)
          (when email-service
            (future
              (try
                (inv-email/send-admin-invitation-email! email-service
                  {:to-email     email
                   :inviter-name (or (:full_name admin) (:email admin))
                   :accept-url   accept-url
                   :role         role})
                (catch Exception e
                  (log/error e "Failed to send admin invitation email" {:email email})))))
          (let [converted (shared-db/to-app invitation)]
            (utils/json-response {:invitation converted} :status 201)))))
    "Failed to create admin invitation"))

(defn revoke-invitation-handler
  "DELETE /admin/api/invitations/:id — revoke a pending invitation."
  [db]
  (utils/with-error-handling
    (fn [request]
      (assert-owner-role! request)
      (utils/handle-uuid-request request :id
        (fn [inv-id _request]
          (let [admin (:admin request)]
            (inv-svc/revoke-invitation! db inv-id (:id admin))
            (utils/success-response {:message "Invitation revoked"})))))
    "Failed to revoke admin invitation"))

(defn resend-invitation-handler
  "POST /admin/api/invitations/:id/resend — resend invitation email."
  [db email-service base-url]
  (utils/with-error-handling
    (fn [request]
      (assert-owner-role! request)
      (utils/handle-uuid-request request :id
        (fn [inv-id _request]
          (let [admin (:admin request)
                invitation (inv-svc/resend-invitation! db inv-id (:id admin))
                token (str (:token invitation))
                accept-url (str base-url "/admin/invitation/accept?token=" token)]
            ;; Re-send email asynchronously
            (when email-service
              (future
                (try
                  (inv-email/send-admin-invitation-email! email-service
                    {:to-email     (:email invitation)
                     :inviter-name (or (:full_name admin) (:email admin))
                     :accept-url   accept-url
                     :role         (str (:role invitation))})
                  (catch Exception e
                    (log/error e "Failed to resend admin invitation email")))))
            (utils/success-response {:message "Invitation resent"})))))
    "Failed to resend admin invitation"))

;; ============================================================================
;; Route Definitions
;; ============================================================================

(defn public-routes
  "Public admin invitation routes (no authentication required)."
  [db]
  ["/invitations"
   ["/validate" {:get (validate-invitation-handler db)}]
   ["/accept" {:post (accept-invitation-handler db)}]])

(defn protected-routes
  "Protected admin invitation routes (owner authentication required)."
  [db email-service base-url]
  ["/invitations"
   ["" {:get (list-invitations-handler db)
        :post (create-invitation-handler db email-service base-url)}]
   ["/:id" {:delete (revoke-invitation-handler db)}]
   ["/:id/resend" {:post (resend-invitation-handler db email-service base-url)}]])

(ns app.template.backend.routes.onboarding
  "Onboarding API routes mounted at /api/v1/onboarding.
   All routes require user authentication."
  (:require
    [app.template.backend.routes.utils :as route-utils]
    [app.template.backend.services.onboarding.core :as onboarding]
    [app.template.backend.services.onboarding.differences :as differences]
    [clojure.walk :as walk]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- sanitize
  "Walk a data structure and stringify Java types for JSON serialization."
  [obj]
  (walk/postwalk
    (fn [x]
      (cond
        (instance? java.util.UUID x) (str x)
        (instance? java.time.Instant x) (str x)
        (instance? java.time.LocalDateTime x) (str x)
        (instance? java.time.OffsetDateTime x) (str x)
        (instance? java.time.ZonedDateTime x) (str x)
        (instance? java.time.LocalDate x) (str x)
        (instance? java.time.LocalTime x) (str x)
        (instance? java.sql.Timestamp x) (str x)
        (instance? java.sql.Date x) (str x)
        (instance? java.math.BigDecimal x) (str x)
        (instance? java.math.BigInteger x) (str x)
        :else x))
    obj))

(defn- get-user [req]
  (or (get-in req [:session :auth-session :user])
    (get-in req [:session :user])))

(defn- get-membership-role [req]
  (or (get-in req [:session :auth-session :membership :role])
    (get-in req [:session :auth-session :membership-role])))

(defn- get-tenant-id [req]
  (or (get-in req [:session :auth-session :tenant :id])
    (get-in req [:session :auth-session :tenant :tenants/id])
    (get-in req [:session :tenant-id])))

;; ============================================================================
;; Handlers
;; ============================================================================

(defn- get-progress-handler
  "GET /onboarding/progress — full progress + steps + workspace differences."
  [db]
  (fn [req]
    (route-utils/with-error-handling "onboarding-progress"
      (let [user (get-user req)
            role (get-membership-role req)
            tenant-id (get-tenant-id req)]
        (when-not user
          (throw (ex-info "Not authenticated" {:type :unauthorized})))
        (when-not role
          (throw (ex-info "No active membership"
                   {:type :forbidden
                    :errors {:role ["No membership role in session"]}})))
        (let [progress (onboarding/get-progress db (:id user) role)
              diffs    (when (and tenant-id progress)
                         (differences/compute-role-differences db tenant-id role))]
          (response/response
            (sanitize
              (cond-> {:progress progress}
                diffs (assoc :differences diffs)))))))))

(defn- complete-step-handler
  "POST /onboarding/steps/:step-kind/complete — mark a step completed."
  [db]
  (fn [req]
    (route-utils/with-error-handling "onboarding-complete-step"
      (let [user      (get-user req)
            role      (get-membership-role req)
            step-kind (get-in req [:path-params :step-kind])]
        (when-not user
          (throw (ex-info "Not authenticated" {:type :unauthorized})))
        (when-not role
          (throw (ex-info "No active membership" {:type :forbidden})))
        (let [result (onboarding/complete-step! db (:id user) role step-kind)]
          (if result
            (response/response (sanitize {:success true :step result}))
            (response/response {:success false
                                :message "Step not found or already completed"})))))))

(defn- skip-step-handler
  "POST /onboarding/steps/:step-kind/skip — skip a pending step."
  [db]
  (fn [req]
    (route-utils/with-error-handling "onboarding-skip-step"
      (let [user      (get-user req)
            role      (get-membership-role req)
            step-kind (get-in req [:path-params :step-kind])]
        (when-not user
          (throw (ex-info "Not authenticated" {:type :unauthorized})))
        (when-not role
          (throw (ex-info "No active membership" {:type :forbidden})))
        (let [result (onboarding/skip-step! db (:id user) role step-kind)]
          (if result
            (response/response (sanitize {:success true :step result}))
            (response/response {:success false
                                :message "Step not found or not pending"})))))))

(defn- skip-all-handler
  "POST /onboarding/skip-all — skip all pending steps."
  [db]
  (fn [req]
    (route-utils/with-error-handling "onboarding-skip-all"
      (let [user (get-user req)
            role (get-membership-role req)]
        (when-not user
          (throw (ex-info "Not authenticated" {:type :unauthorized})))
        (when-not role
          (throw (ex-info "No active membership" {:type :forbidden})))
        (let [skipped (onboarding/skip-all-pending! db (:id user) role)]
          (response/response {:success true :skipped-count skipped}))))))

(defn- dismiss-handler
  "POST /onboarding/dismiss — dismiss onboarding checklist (final)."
  [db]
  (fn [req]
    (route-utils/with-error-handling "onboarding-dismiss"
      (let [user (get-user req)
            role (get-membership-role req)]
        (when-not user
          (throw (ex-info "Not authenticated" {:type :unauthorized})))
        (when-not role
          (throw (ex-info "No active membership" {:type :forbidden})))
        (let [result (onboarding/dismiss! db (:id user) role)]
          (if result
            (response/response (sanitize {:success true :progress result}))
            (response/response {:success false
                                :message "Already dismissed or not found"})))))))

;; ============================================================================
;; Route Tree
;; ============================================================================

(defn onboarding-routes
  "Create onboarding routes. Protected by `wrap-user-auth`."
  [db wrap-user-auth]
  ["/onboarding"
   {:middleware [wrap-user-auth]}

   ["/progress"                  {:get  {:handler (get-progress-handler db)}}]
   ["/steps/:step-kind/complete" {:post {:handler (complete-step-handler db)}}]
   ["/steps/:step-kind/skip"     {:post {:handler (skip-step-handler db)}}]
   ["/skip-all"                  {:post {:handler (skip-all-handler db)}}]
   ["/dismiss"                   {:post {:handler (dismiss-handler db)}}]])

(comment
  ;; (require 'app.template.backend.routes.onboarding :reload)
  :rcf)

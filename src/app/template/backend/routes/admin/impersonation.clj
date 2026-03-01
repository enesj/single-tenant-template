(ns app.template.backend.routes.admin.impersonation
  "Admin-side impersonation routes for managing impersonation sessions.
   Mounted under /admin/api/impersonation."
  (:require
    [app.template.backend.routes.admin.utils :as utils]
    [app.template.backend.services.impersonation :as impersonation-svc]))

;; ============================================================================
;; Handlers
;; ============================================================================

(defn- status-handler
  "GET /impersonation/status — current impersonation context (or null)."
  []
  (utils/with-error-handling
    (fn [request]
      (utils/success-response
        {:impersonation (or (:impersonation request) nil)}))
    "Failed to get impersonation status"))

(defn- list-grants-handler
  "GET /impersonation/grants — list active grants for current admin."
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [admin-id (get-in request [:admin :id])
            grants   (impersonation-svc/list-active-grants-for-admin db admin-id)]
        (utils/success-response {:grants grants})))
    "Failed to list impersonation grants"))

(defn- activate-handler
  "POST /impersonation/activate — activate impersonation for a grant."
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [admin-id (get-in request [:admin :id])
            grant-id (get-in request [:body :grant-id])]
        (when-not grant-id
          (throw (ex-info "grant-id is required"
                   {:type :validation-error
                    :errors {:grant-id ["grant-id is required"]}})))
        (let [context (impersonation-svc/activate-impersonation! db admin-id grant-id)]
          (utils/success-response {:success true :impersonation context}))))
    "Failed to activate impersonation"))

(defn- deactivate-handler
  "POST /impersonation/deactivate — clear impersonation."
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [admin-id (get-in request [:admin :id])]
        (impersonation-svc/deactivate-impersonation! db admin-id)
        (utils/success-response {:success true :impersonation nil})))
    "Failed to deactivate impersonation"))

(defn- revoke-grant-handler
  "POST /impersonation/grants/:id/revoke — admin revokes own grant."
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [admin-id (get-in request [:admin :id])
            grant-id (get-in request [:path-params :id])]
        (impersonation-svc/revoke-grant-by-admin! db grant-id admin-id)
        (utils/success-response {:success true})))
    "Failed to revoke impersonation grant"))

;; ============================================================================
;; Route Tree
;; ============================================================================

(defn routes
  "Admin impersonation routes. Mounted under /admin/api/impersonation."
  [db]
  ["/impersonation"
   ["/status"     {:get (status-handler)}]
   ["/grants"     {:get (list-grants-handler db)}]
   ["/grants/:id/revoke" {:post (revoke-grant-handler db)}]
   ["/activate"   {:post (activate-handler db)}]
   ["/deactivate" {:post (deactivate-handler db)}]])

(comment
  ;; (require 'app.template.backend.routes.admin.impersonation :reload)
  :rcf)

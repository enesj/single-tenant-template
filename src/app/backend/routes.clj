(ns app.backend.routes
  (:require
    [app.backend.middleware.security :as security]
    [app.backend.routes.admin-api :as admin-api]
    [app.backend.routes.api :as api]
    [app.template.backend.routes.auth :as auth]
    [app.backend.routes.entities :as entities]
    [app.backend.routes.oauth :as oauth]
    [app.backend.services.monitoring.login-events :as login-monitoring]
    [app.template.backend.routes.email-verification :as email-verification]
    [app.template.backend.routes.oauth :as template-oauth]
    [app.template.backend.routes.onboarding :as onboarding]
    [clojure.stacktrace :as stacktrace]
    [clojure.string :as str]
    [reitit.ring :as ring]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [ring.middleware.resource :refer [wrap-resource]]
    [taoensso.timbre :as log]))

(defn- render-page [_]
  (let [;; Render the page with authentication info
        html-content (slurp "resources/public/index.html")
        ;; Only replace CSRF token if it's bound (when anti-forgery is enabled)
        html-content-with-csrf (if (bound? #'*anti-forgery-token*)
                                 (str/replace html-content "{{csrf-token}}" *anti-forgery-token*)
                                 (str/replace html-content "{{csrf-token}}" ""))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body html-content-with-csrf}))

(defn- admin-render-page
  "Render the main HTML page for the admin panel - uses the same app as main"
  [_]
  (let [;; Use the same index.html as the main app
        html-content (slurp "resources/public/index.html")
        ;; Only replace CSRF token if it's bound (when anti-forgery is enabled)
        html-content-with-csrf (if (bound? #'*anti-forgery-token*)
                                 (str/replace html-content "{{csrf-token}}" *anti-forgery-token*)
                                 (str/replace html-content "{{csrf-token}}" ""))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body html-content-with-csrf}))

(defn- generate-frontend-entity-routes
  "Generate frontend routes for available entities from models-data"
  [md]
  (let [frontend-routes (entities/generate-frontend-entity-routes md)]
    (map (fn [[path config]]
           [path (assoc-in config [:get :handler] render-page)])
      frontend-routes)))

(defn- generate-app-frontend-routes
  "Single-tenant template: no /app tenant aliases."
  [_md]
  [])

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn app-routes [db service-container]
  ;; Extract models-data from service container instead of taking it as separate parameter
  (let [md (:models-data service-container)

        ;; Global request debugging middleware
        global-debug-middleware
        (fn [handler]
          (fn [request]
            ;; Log ALL requests, especially admin API ones
            (when (or (str/includes? (:uri request) "/admin/api")
                    (str/includes? (:uri request) "/api"))
              (log/info "🌐 GLOBAL REQUEST DEBUG:"
                {:method (:request-method request)
                 :uri (:uri request)
                 :admin-token-present (boolean (get-in request [:headers "x-admin-token"]))
                 :content-type (get-in request [:headers "content-type"])}))
            (handler request)))

        static-routes
        [["/" {:get {:handler render-page}}]

         ;; Add explicit /home route to match frontend routing
         ["/home" {:get {:handler render-page}}]

         ;; Add a test route to debug if new routes work at all
         ["/test-route" {:get {:handler (fn [_] {:status 200 :headers {"Content-Type" "text/plain"} :body "Test route works!"})}}]

;; Authentication frontend routes
         ["/login" {:get {:handler render-page}}]
         ["/register" {:get {:handler render-page}}]
         ["/forgot-password" {:get {:handler render-page}}]
         ["/reset-password" {:get {:handler render-page}}]
         ["/change-password" {:get {:handler render-page}}]
         ["/onboarding" {:get {:handler render-page}}]
         ["/onboarding/welcome" {:get {:handler render-page}}]
         ["/onboarding/verify-email" {:get {:handler render-page}}]
         ["/onboarding/profile" {:get {:handler render-page}}]
         ["/onboarding/property" {:get {:handler render-page}}]
         ["/onboarding/complete" {:get {:handler render-page}}]

         ;; OAuth routes (only included when OAuth is enabled)
         ;; (Launch URIs like /login/google are handled directly by wrap-oauth2)
         ;; OAuth routes with CSRF protection
         ;; Custom OAuth launch handlers with state parameter for CSRF protection
         ["/login/google" {:get {:handler (oauth/google-login-handler)}}]
         ["/oauth2/github" {:get {:handler (oauth/github-login-handler)}}]
         ["/oauth/google/callback" {:get {:handler (fn [req]
                                                     (let [auth-service (get service-container :auth-service)
                                                           req-with-container (assoc req :service-container service-container)
                                                           handler (template-oauth/oauth-callback-handler auth-service)
                                                           resp (handler req-with-container)
                                                           user (get-in resp [:session :auth-session :user])
                                                           user-id (:id user)]
                                                       (when user-id
                                                         (login-monitoring/record-login-event! db
                                                           {:principal-type :user
                                                            :principal-id user-id
                                                            :success true
                                                            :reason "oauth-google"
                                                            :ip (or (get-in req [:headers "x-forwarded-for"])
                                                                  (:remote-addr req))
                                                            :user-agent (get-in req [:headers "user-agent"])}))
                                                       resp))}}]
         ["/oauth2/github/callback" {:get {:handler (fn [req]
                                                      (let [auth-service (get service-container :auth-service)
                                                            req-with-container (assoc req :service-container service-container)]
                                                        ((template-oauth/oauth-callback-handler auth-service) req-with-container)))}}]

         ;; Auth status route (as JSON for client)
         ["/auth/status" {:get {:handler auth/auth-status-handler}}]

         ;; Logout routes - both GET and POST for compatibility
         ["/logout" {:get {:handler auth/logout-handler}}]
         ["/auth/logout" {:post {:handler auth/logout-handler}}]

;; Email verification routes
         ["/verify-email" {:get {:handler (fn [req]
                                           ;; Get email service and db adapter from service container
                                            (let [email-service (get service-container :email-service)
                                                  db-adapter    (get service-container :db-adapter)]
                                              ((email-verification/verify-email-handler db-adapter email-service) req)))}}]
         ["/email-verified" {:get {:handler render-page}}]

         ;; Onboarding API routes
         ["/api/onboarding"
          ["/complete" {:post {:handler (fn [req]
                                          (let [db-adapter (get service-container :db-adapter)]
                                            ((onboarding/complete-onboarding-handler db-adapter) req)))}}]
          ["/status" {:get {:handler (fn [req]
                                       (let [db-adapter (get service-container :db-adapter)]
                                         ((onboarding/get-onboarding-status-handler db-adapter) req)))}}]
          ["/step" {:post {:handler (fn [req]
                                      (let [db-adapter (get service-container :db-adapter)]
                                        ((onboarding/update-onboarding-step-handler db-adapter) req)))}}]]

         ;; User profile API routes
         ["/api/user"
          ["/profile" {:post {:handler (fn [req]
                                         (let [db-adapter (get service-container :db-adapter)]
                                           ((onboarding/save-profile-handler db-adapter) req)))}}]]

         ;; API routes with versioning
         ["/api"
          ;; Version 1 API routes - now using service container
          ["/v1" (api/create-versioned-api-routes db md service-container "v1")]]]

         ;; No webhooks in single-tenant template


        ;; CRITICAL: Completely separate admin routes from all other routes
        ;; Admin API routes - Define as standalone routes, not nested under /admin
        admin-api-routes (admin-api/admin-api-routes db service-container)

        ;; Admin frontend routes - Define separately to avoid conflicts
        admin-frontend-routes
        ["/admin"
         ["" {:get {:handler admin-render-page}}]
         ["/login" {:get {:handler admin-render-page}}]
         ["/dashboard" {:get {:handler admin-render-page}}]
         ["/users" {:get {:handler admin-render-page}}]
         ["/admins" {:get {:handler admin-render-page}}]
         ["/audit" {:get {:handler admin-render-page}}]
         ["/login-events" {:get {:handler admin-render-page}}]
         ["/receipts" {:get {:handler admin-render-page}}]
         ["/settings" {:get {:handler admin-render-page}}]
         ;; catch-all for any other admin SPA paths (e.g., /admin/expenses, /admin/suppliers)
         ["/*path" {:get {:handler admin-render-page}}]]

        ;; Additional frontend routes
        frontend-routes
        [["/about" {:get {:handler render-page}}]
         ["/about/" {:get {:handler render-page}}]
         ["/subscription" {:get {:handler render-page}}]
         ["/entities" {:get {:handler render-page}}]
         ["/entities/" {:get {:handler render-page}}]]

;; Combine all routes with proper precedence: API routes FIRST, then frontend
        all-routes (concat static-routes
                     [admin-api-routes]    ; Admin API routes first
                     [admin-frontend-routes]  ; Admin frontend routes second
                     frontend-routes
                     (generate-app-frontend-routes md)      ; NEW: App routes alias to entities
                     (generate-frontend-entity-routes md))

        ;; Filter out nil values from the routes (from conditional OAuth routes)
        routes-filtered (filter identity all-routes)

;; Setup secure session configuration for production
        site-config (-> site-defaults
                      (assoc-in [:security :anti-forgery] false)
                      ;; Secure session configuration
                      (assoc-in [:session :cookie-attrs :same-site] :strict)  ; CSRF protection
                      (assoc-in [:session :cookie-attrs :http-only] true)     ; XSS protection
                      (assoc-in [:session :cookie-attrs :secure] true)        ; HTTPS only (ignored on localhost)
                      (assoc-in [:session :cookie-attrs :max-age] 3600)       ; 1 hour expiration
                      (assoc-in [:session :timeout] 3600)                     ; Server-side timeout
                      ;; Enable secure cookies in production, but allow HTTP in development
                      (assoc-in [:session :cookie-attrs :secure]
                        (not (or (= (System/getenv "ENV") "development")
                               (= (System/getenv "DISABLE_HTTPS_REDIRECT") "true")))))]

    (try
      ;; Create the base handler with conflict resolution
      (let [handler (-> (ring/ring-handler
                          (try
                            ;; Use conflict resolver to handle overlapping routes
                            (let [router (ring/router routes-filtered
                                           {:conflicts (fn [conflicts]
                                                        ;; Prefer literal routes over parameter routes
                                                         (let [literal-routes (filter #(not (str/includes? (first %) ":")) conflicts)
                                                               param-routes (filter #(str/includes? (first %) ":") conflicts)]
                                                           (concat literal-routes param-routes)))})]
                              router)
                            (catch Exception e
                              (log/error "Error creating router:" (.getMessage e))
                              (log/error "Stack trace:" (with-out-str (stacktrace/print-stack-trace e)))
                              (throw e)))
                          (ring/create-default-handler))
;; Apply middleware in correct order (security re-enabled with fixes)
                      global-debug-middleware         ; Add global request debugging FIRST
                      (security/wrap-security)        ; HTTPS + security headers - RE-ENABLED
                      (wrap-resource "public")
                      (wrap-content-type)
                      ;; Removed wrap-oauth2 - using custom handlers with CSRF protection
                      (wrap-defaults site-config))]
        handler)
      (catch Exception e
        (log/error "Error in app-routes:" (.getMessage e))
        (log/error "Stack trace:" (with-out-str (stacktrace/print-stack-trace e)))
        (throw e)))))

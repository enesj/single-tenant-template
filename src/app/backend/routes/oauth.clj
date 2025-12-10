(ns app.backend.routes.oauth
  (:require
    [clj-http.client :as http]
    [clojure.string :as str]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))

;; OAuth2 configuration maps
(defn get-oauth-configs
  "Get OAuth configuration from application config instead of hardcoded values"
  [config]
  (let [oauth-config (:oauth config)]
    {:github (merge (:github oauth-config)
               {:launch-uri "/oauth2/github"
                :basic-auth? false})
     :google (merge (:google oauth-config)
               {:launch-uri "/login/google"
                :basic-auth? false})}))

;; CSRF protection utilities for OAuth flows
(defn generate-oauth-state
  "Generate a cryptographically secure random state for OAuth CSRF protection"
  []
  (str (java.util.UUID/randomUUID)))

(defn build-oauth-authorize-url
  "Build OAuth authorization URL with state parameter for CSRF protection"
  [oauth-configs provider state]
  (let [config (get oauth-configs provider)
        params {"client_id" (:client-id config)
                "redirect_uri" (:redirect-uri config)
                "scope" (clojure.string/join " " (:scopes config))
                "response_type" "code"
                "state" state}
        query-string (->> params
                       (map (fn [[k v]] (str k "=" (java.net.URLEncoder/encode v "UTF-8"))))
                       (clojure.string/join "&"))]
    (str (:authorize-uri config) "?" query-string)))

(defn validate-oauth-state
  "Validate OAuth state parameter against session to prevent CSRF attacks"
  [request-state session-state]
  (log/info "CSRF validation - Request state:" request-state)
  (log/info "CSRF validation - Session state:" session-state)
  (let [valid? (and request-state
                 session-state
                 (= request-state session-state))]
    (log/info "CSRF validation result:" valid?)
    valid?))

;; Function to fetch Google user info using access token
(defn- fetch-google-user-info [access-token]
  (try
    (let [response (http/get "https://www.googleapis.com/oauth2/v3/userinfo"
                     {:headers {"Authorization" (str "Bearer " access-token)}
                      :accept :json
                      :as :json})
          user-info (:body response)]
      user-info)
    (catch Exception e
      (log/error "Failed to fetch Google user info:" (.getMessage e))
      nil)))

;; OAuth token exchange functions

;; Enhanced OAuth callback handler with CSRF protection and manual token exchange
(defn oauth-callback-handler
  "OAuth callback handler is currently disabled in the single-tenant template.
   We keep the route but avoid hitting multi-tenant auth/tenants tables."
  [_db _md]
  (fn [_req]
    {:status 501
     :headers {"Content-Type" "text/html"}
     :body (str "<h1>Authentication Error</h1>"
             "<p>OAuth login is not enabled in the single-tenant template.</p>"
             "<p>Please use the email/password admin login instead.</p>"
             "<p><a href='/'>(Return to Home)</a></p>")}))

(defn get-google-user-info-for-status
  "Public function to fetch Google user info for auth status checks"
  [access-token]
  (fetch-google-user-info access-token))

;; Custom OAuth launch handlers with CSRF protection
(defn oauth-launch-handler
  "Generate OAuth launch handler with CSRF protection via state parameter"
  [provider]
  (fn [req]
    (let [config (get-in req [:service-container :config])
          _ (log/info "OAuth launch - Config found:" (not (nil? config)))
          _ (when config (log/info "OAuth config keys:" (keys (:oauth config))))
          oauth-configs (get-oauth-configs config)
          provider-config (get oauth-configs provider)
          _ (log/info "OAuth configs for provider" provider ":" provider-config)
          _ (log/info "Using redirect-uri:" (:redirect-uri provider-config))
          state (generate-oauth-state)
          auth-url (build-oauth-authorize-url oauth-configs provider state)]
      (log/info "Launching OAuth for provider:" provider "with state:" state)
      (log/info "Generated auth URL:" auth-url)
      (-> (response/redirect auth-url)
        (assoc :session (assoc (:session req) :oauth-state state))))))

(defn google-login-handler
  "Custom Google OAuth login handler with CSRF protection"
  []
  (oauth-launch-handler :google))

(defn github-login-handler
  "Custom GitHub OAuth login handler with CSRF protection"
  []
  (oauth-launch-handler :github))

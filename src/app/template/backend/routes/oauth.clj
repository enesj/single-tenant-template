(ns app.template.backend.routes.oauth
  (:require
    [app.template.shared.auth :as shared-auth]
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.routes.utils :as route-utils :refer [error-response get-oauth-configs]]
    [cheshire.core :as json]
    [clj-http.client :as http]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [ring.util.response :as response]
    [taoensso.timbre :as log])
  (:import
    (java.net URLDecoder URLEncoder)))

;; OAuth Routes
;;
;; This namespace provides route handlers for OAuth authentication.
;; It handles the OAuth callback, token exchange, and user creation/login via Auth Service.

(defn- build-auth-url [base-url params]
  (let [query-string (str/join "&"
                       (map (fn [[k v]]
                              (str (name k) "=" (java.net.URLEncoder/encode (str v) "UTF-8")))
                         params))]
    (str base-url "?" query-string)))

(defn- html-error-response
  "Generate HTML error response"
  [title message]
  {:status 400
   :headers {"Content-Type" "text/html"}
   :body (str "<h1>" title "</h1>"
           "<p>" message "</p>"
           "<p><a href='/'>Return to Home</a></p>")})

(defn- normalize-scopes
  "Normalize OAuth scopes to a provider-compatible string.
   Accepts string or sequential (vector/list) and joins with spaces."
  [scopes]
  (cond
    (nil? scopes) nil
    (string? scopes) scopes
    (sequential? scopes) (str/join " " scopes)
    :else (str scopes)))

(defn google-login-handler
  "Initiate Google OAuth login"
  []
  (fn [req]
    (route-utils/with-error-handling "google-login"
      (let [config (get-in req [:service-container :config])
            oauth-configs (route-utils/get-oauth-configs config)
            provider-config (:google oauth-configs)]
        (if provider-config
          (let [base-url (or (:authorization-uri provider-config)
                           (:authorize-uri provider-config))
                params {:client_id (:client-id provider-config)
                        :redirect_uri (:redirect-uri provider-config)
                        :scope (or (normalize-scopes (:scopes provider-config))
                                 "email profile openid")
                        :response_type "code"
                        :access_type "offline"
                        :prompt "consent"}]
            (if base-url
              (response/redirect (build-auth-url base-url params))
              (do
                (log/error "Google OAuth authorize URI missing in config")
                (html-error-response "Configuration Error" "Google OAuth authorize URI is missing."))))
          (do
            (log/error "Google OAuth configuration missing")
            (html-error-response "Configuration Error" "Google OAuth is not configured.")))))))

(defn github-login-handler
  "Initiate GitHub OAuth login"
  []
  (fn [req]
    (route-utils/with-error-handling "github-login"
      (let [config (get-in req [:service-container :config])
            oauth-configs (route-utils/get-oauth-configs config)
            provider-config (:github oauth-configs)]
        (if provider-config
          (let [base-url (or (:authorization-uri provider-config)
                           (:authorize-uri provider-config))
                params {:client_id (:client-id provider-config)
                        :redirect_uri (:redirect-uri provider-config)
                        :scope (or (normalize-scopes (:scopes provider-config))
                                 "user:email")}]
            (if base-url
              (response/redirect (build-auth-url base-url params))
              (do
                (log/error "GitHub OAuth authorize URI missing in config")
                (html-error-response "Configuration Error" "GitHub OAuth authorize URI is missing."))))
          (do
            (log/error "GitHub OAuth configuration missing")
            (html-error-response "Configuration Error" "GitHub OAuth is not configured.")))))))

(comment "normalize-scopes moved above handlers")

(defn- sanitize-for-serialization
  "Helper function to sanitize objects for JSON/EDN serialization"
  [obj]
  (walk/postwalk
    (fn [x]
      (cond
        ;; Handle all UUID types
        (instance? java.util.UUID x) (str x)
        ;; Handle all time/date types
        (instance? java.time.LocalDateTime x) (str x)
        (instance? java.time.ZonedDateTime x) (str x)
        (instance? java.time.OffsetDateTime x) (str x)
        (instance? java.time.Instant x) (str x)
        (instance? java.time.LocalDate x) (str x)
        (instance? java.time.LocalTime x) (str x)
        ;; Handle SQL types
        (instance? java.sql.Timestamp x) (str x)
        (instance? java.sql.Date x) (str x)
        (instance? java.sql.Time x) (str x)
        ;; Handle numeric types that might not serialize
        (instance? java.math.BigDecimal x) (str x)
        (instance? java.math.BigInteger x) (str x)
        :else x))
    obj))

(defn- fetch-google-user-info [access-token]
  (try
    (let [response (http/get "https://www.googleapis.com/oauth2/v2/userinfo"
                     {:headers {"Authorization" (str "Bearer " access-token)}
                      :accept :json
                      :as :json})
          user-info (:body response)]
      user-info)
    (catch Exception e
      (log/error "Failed to fetch Google user info:" (.getMessage e))
      (throw (ex-info "Failed to fetch Google user info"
               {:type :oauth-error
                :provider :google
                :message (.getMessage e)}
               e)))))

(defn- fetch-github-user-info [access-token]
  (try
    (let [response (http/get "https://api.github.com/user"
                     {:headers {"Authorization" (str "Bearer " access-token)
                                "User-Agent" "hosting-app"}
                      :accept :json
                      :as :json})
          user-info (:body response)]
      user-info)
    (catch Exception e
      (log/error "Failed to fetch GitHub user info:" (.getMessage e))
      (throw (ex-info "Failed to fetch GitHub user info"
               {:type :oauth-error
                :provider :github
                :message (.getMessage e)}
               e)))))

(defn- exchange-code-for-token
  "Exchange OAuth authorization code for access token"
  [oauth-configs provider code redirect-uri]
  (let [config (get oauth-configs provider)
        token-request {:client_id (:client-id config)
                       :client_secret (:client-secret config)
                       :code code
                       :redirect_uri redirect-uri
                       :grant_type "authorization_code"}]
    (log/info "Token exchange request for provider:" provider)
    (try
      (let [response (http/post (:access-token-uri config)
                       {:form-params token-request
                        :accept :json
                        :as :json
                        :throw-exceptions false})]
        (log/info "Token exchange response status:" (:status response))
        (if (= 200 (:status response))
          (do
            (log/info "Token exchange successful")
            (:body response))
          (do
            (log/error "Token exchange failed with status:" (:status response))
            (log/error "Response body:" (:body response))
            nil)))
      (catch Exception e
        (log/error "Failed to exchange OAuth code for token:" (.getMessage e))
        nil))))

;; Enhanced OAuth callback handler with tenant-aware authentication
(defn oauth-callback-handler
  "Create OAuth callback handler using authentication service with onboarding flow support"
  [auth-service]
  (fn [req]
    (route-utils/with-error-handling "oauth-callback"
      (let [config (get-in req [:service-container :config])
            oauth-configs (get-oauth-configs config)
            uri-path (:uri req)
            provider (cond
                       (str/includes? uri-path "google") :google
                       (str/includes? uri-path "github") :github
                       :else nil)
            ;; Parse query parameters
            query-params (or (:query-params req)
                           (when (:query-string req)
                             (into {} (map (fn [param]
                                             (let [[k v] (clojure.string/split param #"=" 2)]
                                               [k (when v (java.net.URLDecoder/decode v "UTF-8"))]))
                                        (clojure.string/split (:query-string req) #"&")))))
            code (get query-params "code")
            redirect-uri (get-in oauth-configs [provider :redirect-uri])]

        (log/info "OAuth callback - Provider:" provider)
        (log/info "OAuth callback - Authorization code received:" (boolean code))

        ;; Manual token exchange instead of expecting pre-exchanged tokens
        (if code
          (try
            ;; Exchange authorization code for access token
            (if-let [token-response (exchange-code-for-token oauth-configs provider code redirect-uri)]
              (if-let [access-token (:access_token token-response)]
                (let [user-info (case provider
                                  :google (fetch-google-user-info access-token)
                                  :github (fetch-github-user-info access-token)
                                  nil)]
                  (if user-info
                    (do
                      (log/info "Processing OAuth callback for provider:" provider "user:" (:email user-info))

                      ;; Use auth service to process OAuth callback (single-tenant)
                      (try
                        (let [session-data (auth-service/process-oauth-callback auth-service user-info provider)
                              user-email (get-in session-data [:user :email])
                              sanitized-user (sanitize-for-serialization (:user session-data))
                              redirect-url "/entities"]

                          (log/info "Authentication successful for:" user-email)
                          (log/info "Redirecting user" user-email "to:" redirect-url)

                          ;; Redirect with session containing only user data
                          (-> (response/redirect redirect-url)
                            (assoc :session {:auth-session {:user sanitized-user}})))

                        (catch clojure.lang.ExceptionInfo e
                          (let [ex-data (ex-data e)]
                            (if (= :account-conflict (:type ex-data))
                              ;; Handle account conflict specifically
                              (html-error-response
                                "Account Already Exists"
                                (str "<p>" (:message ex-data) "</p>"
                                  "<p>To link your " (name provider) " account, please:</p>"
                                  "<ol>"
                                  "<li>Log in with your password</li>"
                                  "<li>Go to account settings to link your " (name provider) " account</li>"
                                  "</ol>"
                                  "<p><a href='/login'>Go to Login Page</a></p>"))
                              ;; Re-throw other ExceptionInfo
                              (throw e))))))

                    ;; Failed to get user info
                    (html-error-response
                      "Authentication Error"
                      (str "Failed to retrieve user information from " (name provider)))))

                ;; No access token in response
                (html-error-response
                  "Authentication Error"
                  (str "No access token received from " (name provider))))

              ;; Token exchange failed
              (html-error-response
                "Authentication Error"
                "Failed to exchange authorization code for access token"))

            (catch Exception e
              (log/error "OAuth callback processing failed:" (.getMessage e))
              (html-error-response
                "Authentication Error"
                (str "An error occurred during authentication: " (.getMessage e)))))

          ;; No authorization code
          (html-error-response
            "OAuth Authentication Error"
            (str "No authentication tokens received.<br/>"
              "Request details:<br/>"
              "URI: " (:uri req) "<br/>"
              "Query string: " (:query-string req) "<br/>"
              "Provider: " (if provider (name provider) "Not detected"))))))))

(defn get-google-user-info-for-status
  "Public function to fetch Google user info for auth status checks"
  [access-token]
  (route-utils/with-error-handling "get-google-user-info"
    (fetch-google-user-info access-token)))

(defn create-oauth-routes
  "Create OAuth routes that use the authentication service"
  [auth-service]
  {:oauth-callback-handler (oauth-callback-handler auth-service)
   :get-google-user-info-for-status get-google-user-info-for-status})

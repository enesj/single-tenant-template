(ns app.template.backend.routes.oauth
  (:require
    [app.domain.backend.registry :as domain-registry]
    [app.template.backend.auth.service :as auth-service]
    [app.template.backend.auth.tenant :as tenant-auth]
    [app.template.backend.routes.utils :as route-utils :refer [get-oauth-configs]]
    [app.template.backend.services.onboarding.core :as onboarding]
    [clj-http.client :as http]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [ring.util.response :as response]
    [taoensso.timbre :as log]
    [app.admin.backend.services.admin.audit :as audit]))

;; OAuth Routes
;;
;; This namespace provides route handlers for OAuth authentication.
;; It handles the OAuth callback, token exchange, and user creation/login via Auth Service.

(defn- safe-return-path
  "Extract a relative path from a return URL to prevent open-redirect attacks.
   Accepts full URLs (strips origin) or relative paths.
   Returns nil for anything that doesn't start with `/`."
  [url]
  (when (and url (not (str/blank? url)))
    (let [path (cond
                 ;; Full URL — extract path + query
                 (str/starts-with? url "http")
                 (try
                   (let [u (java.net.URI. url)]
                     (str (.getPath u)
                       (when-let [q (.getQuery u)] (str "?" q))))
                   (catch Exception _ nil))
                 ;; Already a relative path
                 (str/starts-with? url "/") url
                 :else nil)]
      (when (and path (str/starts-with? path "/"))
        path))))

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
  "Normalize OAuth scopes to a provider-friendly string.
   Accepts string or sequential (vector/list) and joins with spaces."
  [scopes]
  (cond
    (nil? scopes) nil
    (string? scopes) scopes
    (sequential? scopes) (str/join " " scopes)
    :else (str scopes)))

(defn google-login-handler
  "Initiate Google OAuth login.
   Preserves an optional `return` query param via the OAuth `state` parameter
   so the callback can redirect the user back after authentication.
   Using `state` instead of the session avoids SameSite cookie issues on
   cross-origin redirects from Google."
  []
  (fn [req]
    (route-utils/with-error-handling "google-login"
      (let [config (get-in req [:service-container :config])
            oauth-configs (route-utils/get-oauth-configs config)
            provider-config (:google oauth-configs)
            return-url (get-in req [:query-params "return"])
            ;; Encode return URL in the state param (base64 to keep it URL-safe)
            state-value (when return-url
                          (.encodeToString (java.util.Base64/getUrlEncoder)
                            (.getBytes (str "return:" return-url) "UTF-8")))]
        (if provider-config
          (let [base-url (or (:authorization-uri provider-config)
                           (:authorize-uri provider-config))
                params (cond-> {:client_id (:client-id provider-config)
                                :redirect_uri (:redirect-uri provider-config)
                                :scope (or (normalize-scopes (:scopes provider-config))
                                         "email profile openid")
                                :response_type "code"
                                :access_type "offline"
                                :prompt "consent"}
                         state-value (assoc :state state-value))]
            (if base-url
              (response/redirect (build-auth-url base-url params))
              (do
                (log/error "Google OAuth authorize URI missing in config")
                (html-error-response "Configuration Error" "Google OAuth authorize URI is missing."))))
          (do
            (log/error "Google OAuth configuration missing")
            (html-error-response "Configuration Error" "Google OAuth is not configured.")))))))

(defn github-login-handler
  "Initiate GitHub OAuth login.
   Preserves an optional `return` query param via the OAuth `state` parameter."
  []
  (fn [req]
    (route-utils/with-error-handling "github-login"
      (let [config (get-in req [:service-container :config])
            oauth-configs (route-utils/get-oauth-configs config)
            provider-config (:github oauth-configs)
            return-url (get-in req [:query-params "return"])
            state-value (when return-url
                          (.encodeToString (java.util.Base64/getUrlEncoder)
                            (.getBytes (str "return:" return-url) "UTF-8")))]
        (if provider-config
          (let [base-url (or (:authorization-uri provider-config)
                           (:authorize-uri provider-config))
                params (cond-> {:client_id (:client-id provider-config)
                                :redirect_uri (:redirect-uri provider-config)
                                :scope (or (normalize-scopes (:scopes provider-config))
                                         "user:email")}
                         state-value (assoc :state state-value))]
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
  [oauth-configs provider code redirect-uri db]
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
            (when db
              (audit/log-api-failure! db
                {:api-name (keyword (str (name provider) "-oauth")) :operation "token-exchange"
                 :http-status (:status response)
                 :error-message (str "OAuth token exchange non-200: " (:status response))
                 :error-type "http-error"
                 :request-url (:access-token-uri config) :severity :error}))
            nil)))
      (catch Exception e
        (log/error "Failed to exchange OAuth code for token:" (.getMessage e))
        (when db
          (audit/log-api-failure! db
            {:api-name (keyword (str (name provider) "-oauth")) :operation "token-exchange"
             :error-message (.getMessage e)
             :error-type (some-> e class .getName)
             :request-url (:access-token-uri config) :severity :error}))
        nil))))

(defn- ensure-onboarding-summary
  "Return the current onboarding summary for a (user, role) pair.
   If none exists yet, initialise onboarding lazily and re-read the summary."
  [db user-id role]
  (when (and db user-id role)
    (try
      (or (onboarding/get-progress-summary db user-id role)
        (do
          (onboarding/initialise-delta-onboarding! db user-id role)
          (onboarding/get-progress-summary db user-id role)))
      (catch Exception e
        (log/debug "OAuth onboarding summary skipped" {:error (.getMessage e)})
        nil))))

(defn- post-login-redirect-url
  "Choose the post-auth redirect, preferring onboarding when a fresh summary says
   the user should start there."
  [db tenant-ctx auth-session]
  (case (:action tenant-ctx)
    :selection-required "/tenant-select"
    :no-tenant "/tenant-select"
    (let [role (or (get-in auth-session [:membership :role])
                 (:membership-role auth-session))
          user-id (get-in auth-session [:user :id])
          onboarding-summary (ensure-onboarding-summary db user-id role)]
      (if (:redirect-to-onboarding? onboarding-summary)
        "/onboarding"
        (domain-registry/get-post-login-path)))))

;; Enhanced OAuth callback handler with tenant-aware authentication
(defn oauth-callback-handler
  "Create OAuth callback handler using authentication service.
   `db` and `app-config` are passed explicitly so the handler can resolve
   tenant context after authentication."
  [auth-service db app-config]
  (fn [req]
    (route-utils/with-error-handling "oauth-callback"
      (let [config (or app-config (get-in req [:service-container :config]))
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
            ;; Extract return URL from state parameter (base64-encoded "return:<url>")
            state-raw (get query-params "state")
            state-return (when state-raw
                           (try
                             (let [decoded (String. (.decode (java.util.Base64/getUrlDecoder)
                                                      state-raw) "UTF-8")]
                               (when (str/starts-with? decoded "return:")
                                 (safe-return-path (subs decoded 7))))
                             (catch Exception _ nil)))
            redirect-uri (get-in oauth-configs [provider :redirect-uri])]

        (log/info "OAuth callback - Provider:" provider)
        (log/info "OAuth callback - Authorization code received:" (boolean code))

        ;; Manual token exchange instead of expecting pre-exchanged tokens
        (if code
          (try
            ;; Exchange authorization code for access token
            (if-let [token-response (exchange-code-for-token oauth-configs provider code redirect-uri db)]
              (if-let [access-token (:access_token token-response)]
                (let [user-info (case provider
                                  :google (fetch-google-user-info access-token)
                                  :github (fetch-github-user-info access-token)
                                  nil)]
                  (if user-info
                    (do
                      (log/info "Processing OAuth callback for provider:" provider "user:" (:email user-info))

                      ;; Use auth service to process OAuth callback with tenant context
                      (try
                        (let [session-data              (auth-service/process-oauth-callback auth-service user-info provider)
                              user-raw                  (:user session-data)
                              user-email                (:email user-raw)
                              sanitized-user            (sanitize-for-serialization user-raw)
                              new-signup?               (:is-new-signup session-data)
                              verification-required?    (:verification-required session-data)
                              verification-email-sent?  (:verification-email-sent? session-data)
                              verification-email-error  (:verification-email-error session-data)]

                          (if (and new-signup? verification-required?)
                            (if verification-email-sent?
                              ;; New OAuth user: redirect to "check your email" page
                              (do
                                (log/info "New OAuth user" user-email "— verification email sent, redirecting to pending page")
                                (-> (response/redirect "/email-verified?pending=true")
                                  (assoc :session {:auth-session {:user sanitized-user}})))
                              ;; New OAuth user but verification delivery failed: don't pretend email was sent
                              (do
                                (log/warn "New OAuth user verification email delivery failed"
                                  {:email user-email
                                   :provider provider
                                   :error verification-email-error})
                                (-> (response/redirect "/email-verified?error=email-send-failed")
                                  (assoc :session {:auth-session {:user sanitized-user}}))))

                            ;; Existing user: resolve tenant context and redirect normally
                            (let [oauth-db     (or db (get-in req [:service-container :db]))
                                  tenant-ctx   (tenant-auth/resolve-tenant-context oauth-db config user-raw
                                                 {:client-ip (:remote-addr req)})
                                  auth-session (sanitize-for-serialization
                                                 (tenant-auth/build-auth-session {:user sanitized-user} tenant-ctx))
                                  ;; Prefer return URL from OAuth state param (e.g. invitation accept)
                                  ;; over the default post-login redirect.
                                  redirect-url  (or state-return
                                                  (post-login-redirect-url oauth-db tenant-ctx auth-session))]

                              (log/info "Authentication successful for:" user-email)
                              (log/info "Redirecting user" user-email "to:" redirect-url
                                (when state-return "(from OAuth state return URL)"))

                              (-> (response/redirect redirect-url)
                                (assoc :session {:auth-session auth-session})))))

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
              (when db
                (audit/log-api-failure! db
                  {:api-name (keyword (str (when provider (name provider)) "-oauth"))
                   :operation "oauth-callback"
                   :error-message (.getMessage e)
                   :error-type (some-> e class .getName)
                   :severity :error}))
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
  "Create OAuth routes that use the authentication service.
   `db` and `config` are threaded through for tenant resolution in the callback."
  ([auth-service]
   (create-oauth-routes auth-service nil nil))
  ([auth-service db config]
   {:oauth-callback-handler (oauth-callback-handler auth-service db config)
    :get-google-user-info-for-status get-google-user-info-for-status}))

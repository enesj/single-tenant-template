(ns app.template.backend.routes.oauth
  (:require
   [app.template.backend.auth.service :as auth-service]
   [app.template.backend.routes.utils :as route-utils]
   [clj-http.client :as http]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [ring.util.response :as response]
   [taoensso.timbre :as log]))

(defn- sanitize-for-serialization
  "Helper function to sanitize objects for JSON/EDN serialization.
   We keep only simple types and stringify common DB/time types."
  [obj]
  (walk/postwalk
    (fn [x]
      (cond
        (instance? java.util.UUID x) (str x)
        (instance? java.time.LocalDateTime x) (str x)
        (instance? java.time.ZonedDateTime x) (str x)
        (instance? java.time.OffsetDateTime x) (str x)
        (instance? java.time.Instant x) (str x)
        (instance? java.time.LocalDate x) (str x)
        (instance? java.time.LocalTime x) (str x)
        (instance? java.sql.Timestamp x) (str x)
        (instance? java.sql.Date x) (str x)
        (instance? java.sql.Time x) (str x)
        (instance? java.math.BigDecimal x) (str x)
        (instance? java.math.BigInteger x) (str x)
        :else x))
    obj))

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

(defn- html-error-response
  "Generate HTML error response"
  [title message]
  {:status 400
   :headers {"Content-Type" "text/html"}
   :body (str "<h1>" title "</h1>"
           "<p>" message "</p>"
           "<p><a href='/'>Return to Home</a></p>")})

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

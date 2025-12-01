(ns app.template.backend.auth.protocols
  "Authentication service protocols")

(defprotocol OAuthProvider
  "OAuth provider interface"
  (get-auth-url [this redirect-uri]
    "Get authorization URL for OAuth flow
    Returns: authorization URL string")
  (exchange-code [this code redirect-uri]
    "Exchange authorization code for access token
    Returns: {:access-token string :refresh-token string :expires-in int}")
  (get-user-info [this access-token]
    "Get user information from OAuth provider
    Returns: {:email string :name string :avatar-url string :provider-id string}"))

(defprotocol SessionManager
  "Session management interface"
  (create-session [this user-id tenant-id duration]
    "Create new session with expiration
    Returns: {:token string :expires-at instant}")
  (validate-session [this token]
    "Validate session and return context
    Returns: {:valid? boolean :user-id uuid :tenant-id uuid :expires-at instant}")
  (refresh-session [this token]
    "Refresh session expiration
    Returns: {:token string :expires-at instant}")
  (invalidate-session [this token]
    "Invalidate session
    Returns: {:success? boolean}")
  (cleanup-expired-sessions [this]
    "Remove expired sessions
    Returns: {:removed int}"))

(defprotocol PasswordManager
  "Password management interface"
  (hash-password [this password]
    "Hash password securely
    Returns: hashed password string")
  (verify-password [this password hash]
    "Verify password against hash
    Returns: boolean")
  (generate-reset-token [this user-id]
    "Generate password reset token
    Returns: {:token string :expires-at instant}")
  (verify-reset-token [this token]
    "Verify password reset token
    Returns: {:valid? boolean :user-id uuid}"))

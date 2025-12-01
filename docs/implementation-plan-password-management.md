# Password Management Implementation Plan

## Executive Summary

This document outlines the implementation of password change and forgot password (password reset) functionality for both **users** and **admins** in the single-tenant application. The implementation builds upon the existing authentication infrastructure and follows the established patterns.

## Current State Analysis

### ✅ Existing Infrastructure

| Component | Location | Status |
|-----------|----------|--------|
| **Password Hashing** | `src/app/backend/services/admin/auth.clj` | bcrypt with SHA-256 fallback |
| **PasswordManager Protocol** | `src/app/template/backend/auth/protocols.clj` | Protocol defined with `generate-reset-token` / `verify-reset-token` |
| **Email Verification** | `src/app/template/backend/auth/email_verification.clj` | Complete token-based email verification |
| **Email Service** | `src/app/backend/services/gmail_smtp.clj` | Gmail SMTP with verification emails |
| **Admin Auth** | `src/app/backend/routes/admin/auth.clj` | Full admin login/session management |
| **User Auth** | `src/app/template/backend/routes/auth.clj` | Registration and login |
| **Admin Password Reset (by admin)** | `src/app/backend/services/admin/users/security.clj` | Admin triggers reset for users |
| **Audit Logging** | `src/app/backend/services/admin/audit.clj` | Security action logging |

### ❌ Missing Components

| Feature | Description | Priority |
|---------|-------------|----------|
| **Password Reset Tokens Table** | DB table for storing reset tokens | High |
| **Forgot Password Flow (Users)** | User requests reset → email → reset form | High |
| **Forgot Password Flow (Admins)** | Admin requests reset → email → reset form | High |
| **Change Password (Users)** | Authenticated user changes own password | High |
| **Change Password (Admins)** | Authenticated admin changes own password | High |
| **Password Reset UI (Users)** | Frontend pages for forgot/reset | High |
| **Password Reset UI (Admins)** | Admin console password management | High |
| **Rate Limiting** | Prevent brute force on reset endpoints | Medium |
| **Password Strength Meter** | UI feedback on password quality | Low |

---

## Implementation Plan

### Phase 1: Database Schema

#### 1.1 Password Reset Tokens Table

**File**: `resources/db/template/password_reset_tokens/models.edn`

```edn
{:password_reset_tokens
 {:fields
  [[:id :uuid {:primary-key true}]
   [:principal_type [:enum :login-principal-type] {:null false}]
   [:principal_id :uuid {:null false}]
   [:token [:varchar 255] {:null false :unique true}]
   [:expires_at :timestamptz {:null false}]
   [:used_at :timestamptz]
   [:created_at :timestamptz {:default "NOW()"}]]
  :indexes
  [[:idx_password_reset_tokens_token :btree {:fields [:token] :unique true}]
   [:idx_password_reset_tokens_principal :btree {:fields [:principal_type :principal_id]}]
   [:idx_password_reset_tokens_expires :btree {:fields [:expires_at]}]]}}
```

**Migration Steps:**
```clojure
(require '[app.migrations.simple-repl :as mig])
(mig/make-all-migrations!)
(mig/migrate!)
(mig/status)
```

---

### Phase 2: Backend Service Layer

#### 2.1 Password Reset Service

**New File**: `src/app/template/backend/auth/password_reset.clj`

This service handles token generation, validation, and password update logic for both users and admins.

```clojure
(ns app.template.backend.auth.password-reset
  "Password reset service for users and admins"
  (:require
    [app.template.backend.db.protocols :as db-protocols]
    [buddy.hashers :as hashers]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log])
  (:import
    [java.security SecureRandom]
    [java.util Base64 UUID]))

;; ============================================================================
;; Configuration
;; ============================================================================

(def reset-token-length 32)
(def reset-token-expiry-hours 1)  ; Tokens expire in 1 hour
(def min-password-length 10)

;; ============================================================================
;; Token Generation
;; ============================================================================

(defn generate-reset-token
  "Generate a secure random token for password reset"
  []
  (let [random (SecureRandom.)
        bytes (byte-array reset-token-length)]
    (.nextBytes random bytes)
    (-> (Base64/getUrlEncoder)
        (.withoutPadding)
        (.encodeToString bytes))))

;; ============================================================================
;; Password Hashing (reuse from admin/auth)
;; ============================================================================

(defn hash-password
  "Hash password using bcrypt with SHA-512"
  [password]
  (hashers/derive password {:alg :bcrypt+sha512 :iterations 12}))

(defn verify-password
  "Verify password against hash"
  [password hash]
  (try
    (hashers/check password hash)
    (catch Exception _ false)))

;; ============================================================================
;; Reset Token Management
;; ============================================================================

(defn create-reset-token!
  "Create a password reset token for a user or admin.
   
   Args:
     db - database connection
     principal-type - :user or :admin
     principal-id - UUID of the user or admin
   
   Returns: {:token <string> :expires-at <instant>}"
  [db principal-type principal-id]
  (let [token (generate-reset-token)
        expires-at (time/plus (time/instant) (time/hours reset-token-expiry-hours))
        token-id (UUID/randomUUID)]
    
    ;; Invalidate any existing tokens for this principal
    (jdbc/execute! db
      (hsql/format {:delete-from :password_reset_tokens
                    :where [:and
                            [:= :principal_type (name principal-type)]
                            [:= :principal_id principal-id]
                            [:is :used_at nil]]}))
    
    ;; Create new token
    (jdbc/execute! db
      (hsql/format {:insert-into :password_reset_tokens
                    :values [{:id token-id
                              :principal_type (name principal-type)
                              :principal_id principal-id
                              :token token
                              :expires_at expires-at
                              :created_at (time/instant)}]}))
    
    (log/info "Created password reset token" 
              {:principal-type principal-type 
               :principal-id principal-id
               :expires-at expires-at})
    
    {:token token :expires-at expires-at}))

(defn verify-reset-token
  "Verify a password reset token and return principal info.
   
   Returns: {:valid? boolean 
             :principal-type keyword 
             :principal-id uuid 
             :error string (if invalid)}"
  [db token]
  (let [token-record (jdbc/execute-one! db
                       (hsql/format {:select [:*]
                                     :from [:password_reset_tokens]
                                     :where [:= :token token]}))]
    (cond
      ;; Token not found
      (nil? token-record)
      {:valid? false :error "Invalid or expired reset link"}
      
      ;; Token already used
      (some? (:used_at token-record))
      {:valid? false :error "This reset link has already been used"}
      
      ;; Token expired
      (time/before? (time/instant (:expires_at token-record)) (time/instant))
      {:valid? false :error "This reset link has expired"}
      
      ;; Token valid
      :else
      {:valid? true
       :principal-type (keyword (:principal_type token-record))
       :principal-id (:principal_id token-record)})))

(defn mark-token-used!
  "Mark a reset token as used"
  [db token]
  (jdbc/execute! db
    (hsql/format {:update :password_reset_tokens
                  :set {:used_at (time/instant)}
                  :where [:= :token token]})))

;; ============================================================================
;; Password Reset Operations
;; ============================================================================

(defn request-password-reset!
  "Request a password reset for a user or admin.
   
   Args:
     db - database connection
     email - email address
     principal-type - :user or :admin
     send-email-fn - function to send reset email (fn [email token reset-url])
     base-url - base URL for reset links
   
   Returns: {:success boolean :message string}"
  [db email principal-type send-email-fn base-url]
  (let [table (if (= principal-type :admin) :admins :users)
        principal (jdbc/execute-one! db
                    (hsql/format {:select [:id :email :full_name]
                                  :from [table]
                                  :where [:= :email email]}))]
    
    ;; Always return success to prevent email enumeration
    (if principal
      (let [{:keys [token]} (create-reset-token! db principal-type (:id principal))
            reset-url (str base-url "/reset-password?token=" token)]
        
        ;; Send reset email
        (try
          (send-email-fn (:email principal) token reset-url (:full_name principal))
          (log/info "Password reset email sent" {:email email :type principal-type})
          (catch Exception e
            (log/error e "Failed to send reset email" {:email email})))
        
        {:success true 
         :message "If an account exists with this email, you will receive password reset instructions."})
      
      ;; No account found - return same message to prevent enumeration
      (do
        (log/warn "Password reset requested for unknown email" {:email email :type principal-type})
        {:success true 
         :message "If an account exists with this email, you will receive password reset instructions."}))))

(defn reset-password!
  "Reset password using a valid token.
   
   Args:
     db - database connection
     token - reset token from email
     new-password - new password to set
   
   Returns: {:success boolean :error string (if failed)}"
  [db token new-password]
  (let [verification (verify-reset-token db token)]
    (cond
      ;; Token invalid
      (not (:valid? verification))
      {:success false :error (:error verification)}
      
      ;; Password too short
      (< (count new-password) min-password-length)
      {:success false :error (str "Password must be at least " min-password-length " characters")}
      
      ;; Process reset
      :else
      (jdbc/with-transaction [tx db]
        (let [{:keys [principal-type principal-id]} verification
              table (if (= principal-type :admin) :admins :users)
              password-hash (hash-password new-password)]
          
          ;; Update password
          (jdbc/execute! tx
            (hsql/format {:update table
                          :set {:password_hash password-hash
                                :updated_at (time/instant)}
                          :where [:= :id principal-id]}))
          
          ;; Mark token as used
          (mark-token-used! tx token)
          
          (log/info "Password reset successful" 
                    {:principal-type principal-type 
                     :principal-id principal-id})
          
          {:success true})))))

;; ============================================================================
;; Change Password (Authenticated)
;; ============================================================================

(defn change-password!
  "Change password for an authenticated user or admin.
   
   Args:
     db - database connection
     principal-type - :user or :admin
     principal-id - UUID of the user or admin
     current-password - current password for verification
     new-password - new password to set
   
   Returns: {:success boolean :error string (if failed)}"
  [db principal-type principal-id current-password new-password]
  (let [table (if (= principal-type :admin) :admins :users)
        principal (jdbc/execute-one! db
                    (hsql/format {:select [:id :password_hash]
                                  :from [table]
                                  :where [:= :id principal-id]}))]
    (cond
      ;; Principal not found
      (nil? principal)
      {:success false :error "Account not found"}
      
      ;; Current password incorrect
      (not (verify-password current-password (:password_hash principal)))
      {:success false :error "Current password is incorrect"}
      
      ;; New password same as current
      (verify-password new-password (:password_hash principal))
      {:success false :error "New password must be different from current password"}
      
      ;; New password too short
      (< (count new-password) min-password-length)
      {:success false :error (str "Password must be at least " min-password-length " characters")}
      
      ;; Process change
      :else
      (let [password-hash (hash-password new-password)]
        (jdbc/execute! db
          (hsql/format {:update table
                        :set {:password_hash password-hash
                              :updated_at (time/instant)}
                        :where [:= :id principal-id]}))
        
        (log/info "Password changed successfully" 
                  {:principal-type principal-type 
                   :principal-id principal-id})
        
        {:success true}))))
```

#### 2.2 Email Templates for Password Reset

**Extend**: `src/app/backend/services/gmail_smtp.clj`

Add password reset email functions:

```clojure
;; Add to existing namespace

(defn create-password-reset-email-body
  "Create password reset email content"
  [user reset-url]
  (let [user-name (or (:full_name user) "User")
        expiry-hours 1]
    {:text (str "Hello " user-name ",\n\n"
                "You requested to reset your password.\n\n"
                "Click this link to reset your password:\n"
                reset-url "\n\n"
                "This link will expire in " expiry-hours " hour.\n\n"
                "If you didn't request this, you can safely ignore this email.\n\n"
                "Best regards,\nThe Team")
     
     :html (str "<!DOCTYPE html><html><body style='font-family: Arial, sans-serif;'>"
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>"
                "<h2>Password Reset Request</h2>"
                "<p>Hello " user-name ",</p>"
                "<p>You requested to reset your password.</p>"
                "<p style='margin: 20px 0;'>"
                "<a href='" reset-url "' style='background-color: #4F46E5; color: white; "
                "padding: 12px 24px; text-decoration: none; border-radius: 6px;'>"
                "Reset Password</a></p>"
                "<p style='color: #666;'>This link will expire in " expiry-hours " hour.</p>"
                "<p style='color: #666;'>If you didn't request this, you can safely ignore this email.</p>"
                "</div></body></html>")}))

(defn send-password-reset-email
  "Send password reset email via SMTP"
  [smtp-config from-email to-email token reset-url full-name]
  (let [user {:email to-email :full_name full-name}
        {:keys [text html]} (create-password-reset-email-body user reset-url)
        subject "Reset Your Password"]
    (send-smtp-email smtp-config from-email to-email subject text html)))
```

---

### Phase 3: API Routes

#### 3.1 User Password Reset Routes

**New File**: `src/app/template/backend/routes/password_reset.clj`

```clojure
(ns app.template.backend.routes.password-reset
  "Password reset and change API routes for users"
  (:require
    [app.template.backend.auth.password-reset :as pwd-reset]
    [app.template.backend.routes.utils :as route-utils]
    [cheshire.core :as json]
    [taoensso.timbre :as log]))

(defn forgot-password-handler
  "Handle forgot password request for users"
  [db email-service base-url]
  (fn [req]
    (route-utils/with-error-handling "forgot-password"
      (let [{:keys [email]} (:body-params req)]
        (if (empty? email)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Email is required"})}
          
          (let [send-email-fn (fn [to-email token reset-url full-name]
                                (when email-service
                                  ;; Use email service to send reset email
                                  (pwd-reset/send-password-reset-email
                                    (:smtp-config email-service)
                                    (:from-email email-service)
                                    to-email token reset-url full-name)))
                result (pwd-reset/request-password-reset! 
                         db email :user send-email-fn base-url)]
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string result)}))))))

(defn reset-password-handler
  "Handle password reset with token"
  [db]
  (fn [req]
    (route-utils/with-error-handling "reset-password"
      (let [{:keys [token password]} (:body-params req)]
        (cond
          (empty? token)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Reset token is required"})}
          
          (empty? password)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "New password is required"})}
          
          :else
          (let [result (pwd-reset/reset-password! db token password)]
            (if (:success result)
              {:status 200
               :headers {"Content-Type" "application/json"}
               :body (json/generate-string {:success true 
                                            :message "Password reset successfully"})}
              {:status 400
               :headers {"Content-Type" "application/json"}
               :body (json/generate-string {:error (:error result)})})))))))

(defn verify-reset-token-handler
  "Verify reset token is valid (for showing reset form)"
  [db]
  (fn [req]
    (route-utils/with-error-handling "verify-reset-token"
      (let [token (get-in req [:query-params "token"])]
        (if (empty? token)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Token is required"})}
          
          (let [result (pwd-reset/verify-reset-token db token)]
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string {:valid (:valid? result)
                                          :error (:error result)})}))))))

(defn change-password-handler
  "Handle password change for authenticated users"
  [db]
  (fn [req]
    (route-utils/with-error-handling "change-password"
      (let [auth-session (get-in req [:session :auth-session])
            user (:user auth-session)
            {:keys [current-password new-password]} (:body-params req)]
        
        (cond
          (nil? user)
          {:status 401
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Not authenticated"})}
          
          (empty? current-password)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Current password is required"})}
          
          (empty? new-password)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "New password is required"})}
          
          :else
          (let [result (pwd-reset/change-password! 
                         db :user (:id user) current-password new-password)]
            (if (:success result)
              {:status 200
               :headers {"Content-Type" "application/json"}
               :body (json/generate-string {:success true 
                                            :message "Password changed successfully"})}
              {:status 400
               :headers {"Content-Type" "application/json"}
               :body (json/generate-string {:error (:error result)})})))))))

(defn create-password-reset-routes
  "Create password reset/change routes for users"
  [db email-service base-url]
  [["/forgot-password" {:post (forgot-password-handler db email-service base-url)}]
   ["/reset-password" {:post (reset-password-handler db)}]
   ["/verify-reset-token" {:get (verify-reset-token-handler db)}]
   ["/change-password" {:post (change-password-handler db)}]])
```

#### 3.2 Admin Password Routes

**New File**: `src/app/backend/routes/admin/password.clj`

```clojure
(ns app.backend.routes.admin.password
  "Admin password management routes"
  (:require
    [app.template.backend.auth.password-reset :as pwd-reset]
    [app.backend.routes.admin.utils :as utils]
    [cheshire.core :as json]
    [taoensso.timbre :as log]))

(defn admin-forgot-password-handler
  "Handle forgot password request for admins (public endpoint)"
  [db email-service base-url]
  (fn [req]
    (try
      (let [{:keys [email]} (:body-params req)]
        (if (empty? email)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Email is required"})}
          
          (let [send-email-fn (fn [to-email token reset-url full-name]
                                (when email-service
                                  (pwd-reset/send-password-reset-email
                                    (:smtp-config email-service)
                                    (:from-email email-service)
                                    to-email token reset-url full-name)))
                result (pwd-reset/request-password-reset! 
                         db email :admin send-email-fn base-url)]
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body (json/generate-string result)})))
      (catch Exception e
        (log/error e "Admin forgot password error")
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:error "Internal server error"})}))))

(defn admin-reset-password-handler
  "Handle password reset with token for admins (public endpoint)"
  [db]
  (fn [req]
    (try
      (let [{:keys [token password]} (:body-params req)]
        (cond
          (empty? token)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Reset token is required"})}
          
          (empty? password)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "New password is required"})}
          
          :else
          (let [result (pwd-reset/reset-password! db token password)]
            (if (:success result)
              {:status 200
               :headers {"Content-Type" "application/json"}
               :body (json/generate-string {:success true})}
              {:status 400
               :headers {"Content-Type" "application/json"}
               :body (json/generate-string {:error (:error result)})}))))
      (catch Exception e
        (log/error e "Admin reset password error")
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:error "Internal server error"})}))))

(defn admin-change-password-handler
  "Handle password change for authenticated admins"
  [db]
  (fn [req]
    (try
      (let [admin (get-in req [:admin])
            {:keys [current-password new-password]} (:body-params req)]
        
        (cond
          (nil? admin)
          {:status 401
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Not authenticated"})}
          
          (empty? current-password)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Current password is required"})}
          
          (empty? new-password)
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "New password is required"})}
          
          :else
          (let [admin-id (or (:id admin) (:admins/id admin))
                result (pwd-reset/change-password! 
                         db :admin admin-id current-password new-password)]
            (if (:success result)
              (do
                (utils/log-admin-action "change_own_password" admin-id 
                                        {:action "password_changed"})
                {:status 200
                 :headers {"Content-Type" "application/json"}
                 :body (json/generate-string {:success true})})
              {:status 400
               :headers {"Content-Type" "application/json"}
               :body (json/generate-string {:error (:error result)})}))))
      (catch Exception e
        (log/error e "Admin change password error")
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:error "Internal server error"})}))))

(defn create-admin-password-routes
  "Create admin password routes.
   Note: forgot-password and reset-password are PUBLIC (no auth required).
   change-password requires authentication."
  [db email-service base-url]
  {:public-routes
   [["/forgot-password" {:post (admin-forgot-password-handler db email-service base-url)}]
    ["/reset-password" {:post (admin-reset-password-handler db)}]
    ["/verify-reset-token" {:get (fn [req]
                                   (let [token (get-in req [:query-params "token"])
                                         result (pwd-reset/verify-reset-token db token)]
                                     {:status 200
                                      :headers {"Content-Type" "application/json"}
                                      :body (json/generate-string 
                                              {:valid (:valid? result)
                                               :error (:error result)})}))}]]
   
   :protected-routes
   [["/change-password" {:post (admin-change-password-handler db)}]]})
```

#### 3.3 Route Integration

**Update**: `src/app/backend/routes.clj` or equivalent router file

Add the new routes to the API:

```clojure
;; Add to user API routes (public)
["/api/v1/auth" 
 (concat
   existing-auth-routes
   (password-reset/create-password-reset-routes db email-service base-url))]

;; Add to admin routes
;; Public admin routes (no auth)
["/admin/auth"
 (get (admin-password/create-admin-password-routes db email-service base-url) :public-routes)]

;; Protected admin routes (requires auth)  
["/admin/api/auth"
 (get (admin-password/create-admin-password-routes db email-service base-url) :protected-routes)]
```

---

### Phase 4: Frontend Implementation

#### 4.1 User Forgot Password Page

**New File**: `src/app/template/frontend/pages/forgot_password.cljs`

```clojure
(ns app.template.frontend.pages.forgot-password
  "User forgot password page"
  (:require
    [app.template.frontend.components.auth :refer [auth-form-container 
                                                    auth-form-header 
                                                    auth-form-field 
                                                    auth-submit-button
                                                    auth-error-alert]]
    [app.template.frontend.events.auth :as auth-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui forgot-password-page
  []
  (let [[email set-email!] (use-state "")
        [submitted set-submitted!] (use-state false)
        loading? (use-subscribe [:template.auth/forgot-password-loading?])
        error (use-subscribe [:template.auth/forgot-password-error])
        success? (use-subscribe [:template.auth/forgot-password-success?])]
    
    ($ auth-form-container
      (if success?
        ;; Success message
        ($ :div {:class "text-center p-8"}
          ($ :div {:class "w-20 h-20 mx-auto mb-6 bg-success/10 rounded-full flex items-center justify-center"}
            ($ :svg {:class "w-10 h-10 text-success" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"})))
          ($ :h2 {:class "text-2xl font-bold mb-4"} "Check Your Email")
          ($ :p {:class "text-base-content/70 mb-6"}
            "If an account exists with this email, you'll receive password reset instructions.")
          ($ :a {:href "/login" :class "ds-btn ds-btn-primary"} "Back to Login"))
        
        ;; Forgot password form
        ($ :<>
          ($ auth-form-header {:title "Forgot Password"
                               :subtitle "Enter your email to receive reset instructions"})
          
          ($ auth-error-alert {:error error})
          
          ($ :form {:id "forgot-password-form"
                    :on-submit (fn [e]
                                 (.preventDefault e)
                                 (rf/dispatch [::auth-events/forgot-password email]))}
            
            ($ auth-form-field {:label "Email Address"
                                :type "email"
                                :placeholder "Enter your email"
                                :value email
                                :field-id "forgot-email"
                                :required true
                                :on-change #(set-email! (.. % -target -value))})
            
            ($ auth-submit-button {:loading? loading?
                                   :text "Send Reset Link"
                                   :loading-text "Sending..."
                                   :button-id "forgot-submit-btn"})
            
            ($ :div {:class "text-center mt-4"}
              ($ :a {:href "/login" :class "ds-link ds-link-primary text-sm"}
                "Back to Login"))))))))
```

#### 4.2 User Reset Password Page

**New File**: `src/app/template/frontend/pages/reset_password.cljs`

```clojure
(ns app.template.frontend.pages.reset-password
  "User reset password page (with token from email)"
  (:require
    [app.template.frontend.components.auth :refer [auth-form-container 
                                                    auth-form-header 
                                                    auth-form-field 
                                                    auth-submit-button
                                                    auth-error-alert]]
    [app.template.frontend.events.auth :as auth-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui reset-password-page
  []
  (let [[password set-password!] (use-state "")
        [confirm-password set-confirm-password!] (use-state "")
        [form-errors set-form-errors!] (use-state {})
        
        ;; Get token from URL
        token (-> js/window .-location .-search
                  (js/URLSearchParams.)
                  (.get "token"))
        
        loading? (use-subscribe [:template.auth/reset-password-loading?])
        error (use-subscribe [:template.auth/reset-password-error])
        success? (use-subscribe [:template.auth/reset-password-success?])
        token-valid? (use-subscribe [:template.auth/reset-token-valid?])
        token-checking? (use-subscribe [:template.auth/reset-token-checking?])
        
        validate-form
        (fn []
          (cond-> {}
            (< (count password) 10)
            (assoc :password "Password must be at least 10 characters")
            
            (not= password confirm-password)
            (assoc :confirm-password "Passwords do not match")))
        
        handle-submit
        (fn [e]
          (.preventDefault e)
          (let [errors (validate-form)]
            (if (empty? errors)
              (rf/dispatch [::auth-events/reset-password token password])
              (set-form-errors! errors))))]
    
    ;; Verify token on mount
    (use-effect
      (fn []
        (when token
          (rf/dispatch [::auth-events/verify-reset-token token]))
        js/undefined)
      [token])
    
    ($ auth-form-container
      (cond
        ;; Checking token
        token-checking?
        ($ :div {:class "text-center p-8"}
          ($ :div {:class "ds-loading ds-loading-spinner ds-loading-lg"})
          ($ :p {:class "mt-4"} "Verifying reset link..."))
        
        ;; Token invalid
        (and (not token-checking?) (not token-valid?))
        ($ :div {:class "text-center p-8"}
          ($ :div {:class "text-error text-6xl mb-4"} "✕")
          ($ :h2 {:class "text-2xl font-bold mb-4"} "Invalid or Expired Link")
          ($ :p {:class "text-base-content/70 mb-6"}
            "This password reset link is invalid or has expired.")
          ($ :a {:href "/forgot-password" :class "ds-btn ds-btn-primary"}
            "Request New Link"))
        
        ;; Password reset success
        success?
        ($ :div {:class "text-center p-8"}
          ($ :div {:class "text-success text-6xl mb-4"} "✓")
          ($ :h2 {:class "text-2xl font-bold mb-4"} "Password Reset Successfully")
          ($ :p {:class "text-base-content/70 mb-6"}
            "Your password has been updated. You can now sign in with your new password.")
          ($ :a {:href "/login" :class "ds-btn ds-btn-primary"} "Sign In"))
        
        ;; Reset form
        :else
        ($ :<>
          ($ auth-form-header {:title "Reset Password"
                               :subtitle "Enter your new password"})
          
          ($ auth-error-alert {:error error})
          
          ($ :form {:id "reset-password-form"
                    :on-submit handle-submit}
            
            ($ auth-form-field {:label "New Password"
                                :type "password"
                                :placeholder "Enter new password (min. 10 characters)"
                                :value password
                                :field-id "reset-password"
                                :required true
                                :on-change #(set-password! (.. % -target -value))})
            
            (when (:password form-errors)
              ($ :p {:class "text-error text-sm mb-2"} (:password form-errors)))
            
            ($ auth-form-field {:label "Confirm Password"
                                :type "password"
                                :placeholder "Confirm new password"
                                :value confirm-password
                                :field-id "reset-confirm-password"
                                :required true
                                :on-change #(set-confirm-password! (.. % -target -value))})
            
            (when (:confirm-password form-errors)
              ($ :p {:class "text-error text-sm mb-2"} (:confirm-password form-errors)))
            
            ($ auth-submit-button {:loading? loading?
                                   :text "Reset Password"
                                   :loading-text "Resetting..."
                                   :button-id "reset-submit-btn"})))))))
```

#### 4.3 User Change Password Component (Settings)

**New File**: `src/app/template/frontend/components/change_password.cljs`

```clojure
(ns app.template.frontend.components.change-password
  "Change password form component for authenticated users"
  (:require
    [app.template.frontend.components.auth :refer [auth-form-field 
                                                    auth-submit-button
                                                    auth-error-alert]]
    [app.template.frontend.events.auth :as auth-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui change-password-form
  []
  (let [[current-password set-current-password!] (use-state "")
        [new-password set-new-password!] (use-state "")
        [confirm-password set-confirm-password!] (use-state "")
        [form-errors set-form-errors!] (use-state {})
        
        loading? (use-subscribe [:template.auth/change-password-loading?])
        error (use-subscribe [:template.auth/change-password-error])
        success? (use-subscribe [:template.auth/change-password-success?])
        
        validate-form
        (fn []
          (cond-> {}
            (empty? current-password)
            (assoc :current-password "Current password is required")
            
            (< (count new-password) 10)
            (assoc :new-password "Password must be at least 10 characters")
            
            (not= new-password confirm-password)
            (assoc :confirm-password "Passwords do not match")))
        
        handle-submit
        (fn [e]
          (.preventDefault e)
          (let [errors (validate-form)]
            (if (empty? errors)
              (do
                (rf/dispatch [::auth-events/change-password 
                              current-password new-password])
                ;; Clear form on submit
                (set-current-password! "")
                (set-new-password! "")
                (set-confirm-password! ""))
              (set-form-errors! errors))))]
    
    ($ :div {:class "ds-card bg-base-100 shadow-xl"}
      ($ :div {:class "ds-card-body"}
        ($ :h2 {:class "ds-card-title"} "Change Password")
        
        (when success?
          ($ :div {:class "ds-alert ds-alert-success mb-4"}
            ($ :span "Password changed successfully!")))
        
        ($ auth-error-alert {:error error})
        
        ($ :form {:on-submit handle-submit}
          
          ($ auth-form-field {:label "Current Password"
                              :type "password"
                              :placeholder "Enter current password"
                              :value current-password
                              :field-id "current-password"
                              :required true
                              :on-change #(set-current-password! (.. % -target -value))})
          
          (when (:current-password form-errors)
            ($ :p {:class "text-error text-sm mb-2"} (:current-password form-errors)))
          
          ($ auth-form-field {:label "New Password"
                              :type "password"
                              :placeholder "Enter new password (min. 10 characters)"
                              :value new-password
                              :field-id "new-password"
                              :required true
                              :on-change #(set-new-password! (.. % -target -value))})
          
          (when (:new-password form-errors)
            ($ :p {:class "text-error text-sm mb-2"} (:new-password form-errors)))
          
          ($ auth-form-field {:label "Confirm New Password"
                              :type "password"
                              :placeholder "Confirm new password"
                              :value confirm-password
                              :field-id "confirm-new-password"
                              :required true
                              :on-change #(set-confirm-password! (.. % -target -value))})
          
          (when (:confirm-password form-errors)
            ($ :p {:class "text-error text-sm mb-2"} (:confirm-password form-errors)))
          
          ($ :div {:class "ds-card-actions justify-end mt-4"}
            ($ auth-submit-button {:loading? loading?
                                   :text "Change Password"
                                   :loading-text "Changing..."
                                   :button-id "change-password-btn"})))))))
```

#### 4.4 Admin Forgot Password Page

**New File**: `src/app/admin/frontend/pages/forgot_password.cljs`

```clojure
(ns app.admin.frontend.pages.forgot-password
  "Admin forgot password page"
  (:require
    [app.template.frontend.components.auth :refer [auth-form-container 
                                                    auth-form-header 
                                                    auth-form-field 
                                                    auth-submit-button
                                                    auth-error-alert]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-forgot-password-page
  []
  (let [[email set-email!] (use-state "")
        loading? (use-subscribe [:admin/forgot-password-loading?])
        error (use-subscribe [:admin/forgot-password-error])
        success? (use-subscribe [:admin/forgot-password-success?])]
    
    ($ :div {:class "min-h-screen flex items-center justify-center bg-base-200"}
      ($ :div {:class "max-w-md w-full"}
        ($ auth-form-container
          (if success?
            ;; Success message
            ($ :div {:class "text-center p-8"}
              ($ :div {:class "w-20 h-20 mx-auto mb-6 bg-success/10 rounded-full flex items-center justify-center"}
                ($ :svg {:class "w-10 h-10 text-success" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                            :d "M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"})))
              ($ :h2 {:class "text-2xl font-bold mb-4"} "Check Your Email")
              ($ :p {:class "text-base-content/70 mb-6"}
                "If an admin account exists with this email, you'll receive reset instructions.")
              ($ :a {:href "/admin/login" :class "ds-btn ds-btn-primary"} "Back to Login"))
            
            ;; Forgot password form
            ($ :<>
              ($ auth-form-header {:title "Admin Password Reset"
                                   :subtitle "Enter your admin email to receive reset instructions"})
              
              ($ auth-error-alert {:error error})
              
              ($ :form {:id "admin-forgot-password-form"
                        :on-submit (fn [e]
                                     (.preventDefault e)
                                     (rf/dispatch [:admin/forgot-password email]))}
                
                ($ auth-form-field {:label "Admin Email"
                                    :type "email"
                                    :placeholder "admin@company.com"
                                    :value email
                                    :field-id "admin-forgot-email"
                                    :required true
                                    :on-change #(set-email! (.. % -target -value))})
                
                ($ auth-submit-button {:loading? loading?
                                       :text "Send Reset Link"
                                       :loading-text "Sending..."
                                       :button-id "admin-forgot-submit-btn"})
                
                ($ :div {:class "text-center mt-4"}
                  ($ :a {:href "/admin/login" :class "ds-link ds-link-primary text-sm"}
                    "Back to Admin Login"))))))))))
```

#### 4.5 Frontend Events (User)

**Add to**: `src/app/template/frontend/events/auth.cljs`

```clojure
;; ============================================================================
;; Forgot Password Events
;; ============================================================================

(rf/reg-event-fx
  ::forgot-password
  common-interceptors
  (fn [{:keys [db]} [_ email]]
    {:db (-> db
             (assoc-in [:session :forgot-password-loading?] true)
             (assoc-in [:session :forgot-password-error] nil)
             (assoc-in [:session :forgot-password-success?] false))
     :http-xhrio (http/post-request
                   {:uri "/api/v1/auth/forgot-password"
                    :params {:email email}
                    :on-success [::forgot-password-success]
                    :on-failure [::forgot-password-failure]})}))

(rf/reg-event-db
  ::forgot-password-success
  common-interceptors
  (fn [db _]
    (-> db
        (assoc-in [:session :forgot-password-loading?] false)
        (assoc-in [:session :forgot-password-success?] true))))

(rf/reg-event-db
  ::forgot-password-failure
  common-interceptors
  (fn [db [_ error]]
    (-> db
        (assoc-in [:session :forgot-password-loading?] false)
        (assoc-in [:session :forgot-password-error] 
                  (http/extract-error-message error)))))

;; ============================================================================
;; Reset Password Events
;; ============================================================================

(rf/reg-event-fx
  ::verify-reset-token
  common-interceptors
  (fn [{:keys [db]} [_ token]]
    {:db (assoc-in db [:session :reset-token-checking?] true)
     :http-xhrio (http/get-request
                   {:uri (str "/api/v1/auth/verify-reset-token?token=" token)
                    :on-success [::verify-reset-token-success]
                    :on-failure [::verify-reset-token-failure]})}))

(rf/reg-event-db
  ::verify-reset-token-success
  common-interceptors
  (fn [db [_ response]]
    (-> db
        (assoc-in [:session :reset-token-checking?] false)
        (assoc-in [:session :reset-token-valid?] (:valid response)))))

(rf/reg-event-db
  ::verify-reset-token-failure
  common-interceptors
  (fn [db _]
    (-> db
        (assoc-in [:session :reset-token-checking?] false)
        (assoc-in [:session :reset-token-valid?] false))))

(rf/reg-event-fx
  ::reset-password
  common-interceptors
  (fn [{:keys [db]} [_ token password]]
    {:db (-> db
             (assoc-in [:session :reset-password-loading?] true)
             (assoc-in [:session :reset-password-error] nil))
     :http-xhrio (http/post-request
                   {:uri "/api/v1/auth/reset-password"
                    :params {:token token :password password}
                    :on-success [::reset-password-success]
                    :on-failure [::reset-password-failure]})}))

(rf/reg-event-db
  ::reset-password-success
  common-interceptors
  (fn [db _]
    (-> db
        (assoc-in [:session :reset-password-loading?] false)
        (assoc-in [:session :reset-password-success?] true))))

(rf/reg-event-db
  ::reset-password-failure
  common-interceptors
  (fn [db [_ error]]
    (-> db
        (assoc-in [:session :reset-password-loading?] false)
        (assoc-in [:session :reset-password-error] 
                  (http/extract-error-message error)))))

;; ============================================================================
;; Change Password Events
;; ============================================================================

(rf/reg-event-fx
  ::change-password
  common-interceptors
  (fn [{:keys [db]} [_ current-password new-password]]
    {:db (-> db
             (assoc-in [:session :change-password-loading?] true)
             (assoc-in [:session :change-password-error] nil)
             (assoc-in [:session :change-password-success?] false))
     :http-xhrio (http/post-request
                   {:uri "/api/v1/auth/change-password"
                    :params {:current-password current-password
                             :new-password new-password}
                    :on-success [::change-password-success]
                    :on-failure [::change-password-failure]})}))

(rf/reg-event-db
  ::change-password-success
  common-interceptors
  (fn [db _]
    (-> db
        (assoc-in [:session :change-password-loading?] false)
        (assoc-in [:session :change-password-success?] true))))

(rf/reg-event-db
  ::change-password-failure
  common-interceptors
  (fn [db [_ error]]
    (-> db
        (assoc-in [:session :change-password-loading?] false)
        (assoc-in [:session :change-password-error] 
                  (http/extract-error-message error)))))

;; ============================================================================
;; Subscriptions
;; ============================================================================

(rf/reg-sub :template.auth/forgot-password-loading?
  (fn [db] (get-in db [:session :forgot-password-loading?])))

(rf/reg-sub :template.auth/forgot-password-error
  (fn [db] (get-in db [:session :forgot-password-error])))

(rf/reg-sub :template.auth/forgot-password-success?
  (fn [db] (get-in db [:session :forgot-password-success?])))

(rf/reg-sub :template.auth/reset-token-checking?
  (fn [db] (get-in db [:session :reset-token-checking?])))

(rf/reg-sub :template.auth/reset-token-valid?
  (fn [db] (get-in db [:session :reset-token-valid?])))

(rf/reg-sub :template.auth/reset-password-loading?
  (fn [db] (get-in db [:session :reset-password-loading?])))

(rf/reg-sub :template.auth/reset-password-error
  (fn [db] (get-in db [:session :reset-password-error])))

(rf/reg-sub :template.auth/reset-password-success?
  (fn [db] (get-in db [:session :reset-password-success?])))

(rf/reg-sub :template.auth/change-password-loading?
  (fn [db] (get-in db [:session :change-password-loading?])))

(rf/reg-sub :template.auth/change-password-error
  (fn [db] (get-in db [:session :change-password-error])))

(rf/reg-sub :template.auth/change-password-success?
  (fn [db] (get-in db [:session :change-password-success?])))
```

---

### Phase 5: Route Configuration

#### 5.1 Add User Routes

**Update**: `src/app/template/frontend/routes.cljs`

```clojure
;; Add to route definitions
["/forgot-password" {:name :forgot-password
                     :view forgot-password-page}]
["/reset-password" {:name :reset-password
                    :view reset-password-page}]
```

#### 5.2 Add Admin Routes

**Update**: `src/app/admin/frontend/routes.cljs`

```clojure
;; Add public (unauthenticated) admin routes
["/admin/forgot-password" {:name :admin-forgot-password
                           :view admin-forgot-password-page}]
["/admin/reset-password" {:name :admin-reset-password
                          :view admin-reset-password-page}]
```

#### 5.3 Add Login Page Links

**Update**: `src/app/template/frontend/pages/login.cljs`

Add "Forgot Password?" link:

```clojure
;; After password field
($ :div {:class "flex justify-end mb-4"}
  ($ :a {:href "/forgot-password" :class "ds-link ds-link-primary text-sm"}
    "Forgot Password?"))
```

**Update**: `src/app/admin/frontend/pages/login.cljs`

Add "Forgot Password?" link:

```clojure
;; After password field
($ :div {:class "flex justify-end mb-4"}
  ($ :a {:href "/admin/forgot-password" :class "ds-link ds-link-primary text-sm"}
    "Forgot Password?"))
```

---

### Phase 6: Security Enhancements

#### 6.1 Rate Limiting

Add rate limiting to password reset endpoints to prevent abuse:

**Update**: `src/app/backend/middleware/rate_limiting.clj`

```clojure
;; Add specific limits for password reset endpoints
(def password-reset-limits
  {:window-ms (* 15 60 1000)  ; 15 minutes
   :max-requests 5})          ; 5 requests per window

(defn wrap-password-reset-rate-limit
  [handler]
  (wrap-rate-limiting handler password-reset-limits))
```

Apply to routes:

```clojure
["/forgot-password" {:post {:handler (-> forgot-password-handler
                                          wrap-password-reset-rate-limit)}]
```

#### 6.2 Audit Logging

Ensure all password operations are logged:

```clojure
;; Add audit entries for:
;; - password_reset_requested (email entered, don't log if user found)
;; - password_reset_completed
;; - password_changed
```

#### 6.3 Session Invalidation

After password change/reset, invalidate existing sessions:

```clojure
(defn invalidate-user-sessions!
  "Invalidate all sessions for a user after password change"
  [db principal-type principal-id]
  ;; Clear any session tokens for this user
  ;; Force re-authentication
  )
```

---

## Implementation Order

| Order | Phase | Description | Effort |
|-------|-------|-------------|--------|
| 1 | Phase 1 | Database schema (password_reset_tokens) | 1 hour |
| 2 | Phase 2.1 | Password reset service | 3 hours |
| 3 | Phase 2.2 | Email templates | 1 hour |
| 4 | Phase 3.1 | User password routes | 2 hours |
| 5 | Phase 3.2 | Admin password routes | 2 hours |
| 6 | Phase 4.1-4.2 | User forgot/reset pages | 3 hours |
| 7 | Phase 4.3 | User change password component | 1.5 hours |
| 8 | Phase 4.4 | Admin forgot password page | 2 hours |
| 9 | Phase 4.5 | Frontend events/subs | 2 hours |
| 10 | Phase 5 | Route configuration | 1 hour |
| 11 | Phase 6 | Security (rate limiting, audit) | 2 hours |

**Total Estimated Effort**: ~20 hours

---

## API Endpoints Summary

### User Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/auth/forgot-password` | Public | Request password reset |
| GET | `/api/v1/auth/verify-reset-token` | Public | Verify reset token |
| POST | `/api/v1/auth/reset-password` | Public | Reset password with token |
| POST | `/api/v1/auth/change-password` | Required | Change own password |

### Admin Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/admin/auth/forgot-password` | Public | Request admin password reset |
| GET | `/admin/auth/verify-reset-token` | Public | Verify admin reset token |
| POST | `/admin/auth/reset-password` | Public | Reset admin password with token |
| POST | `/admin/api/auth/change-password` | Required | Change own admin password |

---

## Frontend Routes Summary

### User Routes

| Path | Component | Auth |
|------|-----------|------|
| `/forgot-password` | `forgot-password-page` | Public |
| `/reset-password` | `reset-password-page` | Public |
| `/settings` (change password section) | `change-password-form` | Required |

### Admin Routes

| Path | Component | Auth |
|------|-----------|------|
| `/admin/forgot-password` | `admin-forgot-password-page` | Public |
| `/admin/reset-password` | `admin-reset-password-page` | Public |
| `/admin/settings` (change password section) | `admin-change-password-form` | Required |

---

## Security Checklist

- [ ] Password reset tokens expire in 1 hour
- [ ] Tokens are single-use (marked used after reset)
- [ ] Rate limiting on forgot-password endpoints (5 requests/15 min)
- [ ] No email enumeration (same response whether account exists or not)
- [ ] Secure token generation (32 bytes, SecureRandom)
- [ ] Password minimum length enforced (10 characters)
- [ ] Current password required for password change
- [ ] New password cannot match current password
- [ ] All password operations logged to audit log
- [ ] Sessions invalidated after password reset
- [ ] HTTPS enforced for all password endpoints

---

## Testing Strategy

### Backend Tests

1. **Token generation/verification**
   - Valid token creation
   - Token expiration
   - Token reuse prevention

2. **Password reset flow**
   - Successful reset
   - Invalid token handling
   - Weak password rejection

3. **Password change flow**
   - Successful change with correct current password
   - Rejection with wrong current password
   - New password validation

### Frontend Tests

1. **Form validation**
   - Email format
   - Password length
   - Password match

2. **API integration**
   - Success flows
   - Error handling
   - Loading states

### Integration Tests

1. **Full forgot password flow**
   - Request reset → email sent → token valid → password reset → login

2. **Security tests**
   - Rate limiting
   - Token expiration
   - Session invalidation

---

## Migration Notes

1. Run database migration to create `password_reset_tokens` table
2. Deploy backend changes first
3. Deploy frontend changes
4. Verify email service is configured
5. Test full flows in staging before production

---

## Future Enhancements (Out of Scope)

- Password strength meter UI
- "Remember this device" for 2FA bypass
- Password history (prevent reuse of last N passwords)
- Account lockout after failed attempts
- Magic link authentication (passwordless)

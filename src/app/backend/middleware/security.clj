(ns app.backend.middleware.security
  "Security middleware for production-grade HTTP security enforcement.

   This middleware provides HTTPS enforcement while maintaining smooth local development.
   Key features:
   - Automatic HTTP to HTTPS redirection in production
   - Local development exemptions (localhost, 127.0.0.1, private IPs)
   - Environment-based override capabilities
   - Admin route prioritization for enhanced security
   - Rate limiting integration"
  (:require
    [app.backend.middleware.rate-limiting :as rate-limiting]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

(defn force-https-middleware
  "Middleware that enforces HTTPS in production while allowing HTTP in development.

   Redirects HTTP requests to HTTPS except for:
   - localhost (any port)
   - 127.0.0.1 (any port)
   - Private network ranges (192.168.x.x, 10.x.x.x)
   - When DISABLE_HTTPS_REDIRECT environment variable is set

   Admin routes get priority treatment with immediate redirection."
  [handler]
  (fn [request]
    (let [scheme (:scheme request)
          server-name (:server-name request)
          uri (:uri request)
          is-admin-route (str/starts-with? uri "/admin")

          ;; Check if this is a local development environment
          is-local-dev (or (= server-name "localhost")
                         (= server-name "127.0.0.1")
                         (str/starts-with? server-name "192.168.")
                         (str/starts-with? server-name "10.")
                         (str/starts-with? server-name "172.")  ; Docker networks
                         (System/getenv "DISABLE_HTTPS_REDIRECT"))

          should-redirect (and (= scheme :http)
                            (not is-local-dev))]

      (if should-redirect
        (do
          ;; Log security redirection for audit purposes
          (when is-admin-route
            (log/info "HTTPS redirect enforced for admin route"
              {:uri uri
               :server-name server-name
               :user-agent (get-in request [:headers "user-agent"])
               :ip (or (get-in request [:headers "x-forwarded-for"])
                     (get-in request [:headers "x-real-ip"])
                     (:remote-addr request))}))

          ;; Return HTTPS redirect
          {:status 301
           :headers {"Location" (str "https://" server-name uri)
                     "Strict-Transport-Security" "max-age=31536000; includeSubDomains"
                     "Cache-Control" "no-cache, no-store, must-revalidate"}
           :body "Redirecting to secure connection..."})

        ;; Allow request to proceed
        (handler request)))))

(defn security-headers-middleware
  "Middleware that adds security headers to all responses.

   Headers added:
   - X-Frame-Options: Prevents clickjacking
   - X-Content-Type-Options: Prevents MIME sniffing
   - X-XSS-Protection: Enables XSS filtering
   - Referrer-Policy: Controls referrer information
   - Content-Security-Policy: Restricts resource loading (admin routes only)"
  [handler]
  (fn [request]
    (let [response (handler request)
          uri (:uri request)
          is-admin-route (str/starts-with? uri "/admin")

          ;; Base security headers for all routes
          base-headers {"X-Frame-Options" "DENY"
                        "X-Content-Type-Options" "nosniff"
                        "X-XSS-Protection" "1; mode=block"
                        "Referrer-Policy" "strict-origin-when-cross-origin"}

          ;; Enhanced headers for admin routes
          admin-headers (if is-admin-route
                          {"Content-Security-Policy"
                           "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self' ws://localhost:* wss://localhost:*"
                           "X-Frame-Options" "DENY"
                           "Cache-Control" "no-cache, no-store, must-revalidate, private"}
                          {})

          ;; Merge headers with existing response headers
          security-headers (merge base-headers admin-headers)
          existing-headers (or (:headers response) {})
          updated-headers (merge existing-headers security-headers)]

      (assoc response :headers updated-headers))))

(defn wrap-security
  "Convenience function to apply all security middleware in the correct order.

   Order matters:
   1. HTTPS enforcement (must be first to catch HTTP requests)
   2. Rate limiting (with comprehensive error handling)
   3. Security headers (applied to all responses)

   Usage:
   (-> handler
       (wrap-security)
       (other-middleware))"
  [handler]
  (-> handler
    security-headers-middleware
    rate-limiting/wrap-rate-limiting  ; RE-ENABLED with error handling
    force-https-middleware))

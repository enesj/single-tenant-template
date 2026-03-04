(ns app.template.backend.middleware.rate-limiting
  "Rate limiting middleware to prevent brute force attacks and API abuse.

   Features:
   - In-memory rate limiting with configurable windows
   - Different limits for admin vs regular routes
   - IP-based, tenant-based, and user-based limiting
   - Automatic cleanup of expired entries
   - Detailed logging for security monitoring"
  (:require
    [clojure.string :as str]
    [java-time.api :as time]
    [taoensso.timbre :as log])
  (:import
    [java.util.concurrent ConcurrentHashMap]))

;; Global rate limiting storage (in production, consider Redis)
(defonce ^:private rate-limit-storage (ConcurrentHashMap.))

;; Rate limiting configurations - using consistent key names
;; Relaxed settings for development environment
(def ^:private rate-limits
  {:admin-login      {:max-attempts 200 :window-minutes 1 :block-minutes 0.017}  ; ~1 second
   :admin-api        {:max-attempts 1000 :window-minutes 1 :block-minutes 0.017} ; ~1 second
   :regular-api      {:max-attempts 2000 :window-minutes 1 :block-minutes 0.017} ; ~1 second
   :auth-endpoints   {:max-attempts 500 :window-minutes 5 :block-minutes 0.017}  ; ~1 second
   :tenant-provisioning {:max-attempts 5 :window-minutes 60 :block-minutes 60}})

(defn- get-client-ip
  "Extract client IP from request, handling proxies and load balancers."
  [request]
  (or (get-in request [:headers "x-forwarded-for"])
    (get-in request [:headers "x-real-ip"])
    (get-in request [:headers "cf-connecting-ip"])  ; Cloudflare
    (:remote-addr request)
    "unknown"))

(defn- get-rate-limit-key
  "Generate unique key for rate limiting based on IP, route type, tenant, and optional user ID."
  [ip route-type tenant-id user-id]
  (str route-type ":" ip
    (when tenant-id (str ":t:" tenant-id))
    (when user-id (str ":u:" user-id))))

(defn- local-dev-ip?
  "Return true when the client IP represents localhost / private networks.

  Note: Some servers report IPv6 loopback as the expanded form
  `0:0:0:0:0:0:0:1` instead of `::1`."
  [ip]
  (let [ip (-> (str ip)
             (str/split #",")
             first
             str/trim)]
    (or (= ip "127.0.0.1")
      (= ip "localhost")
      (= ip "::1")
      (= ip "0:0:0:0:0:0:0:1")
      (str/starts-with? ip "192.168.")
      (str/starts-with? ip "10.")
      (str/starts-with? ip "172.16.")
      (= ip "146.255.154.27"))))

(defn- rate-limiting-disabled?
  "Return true when rate limiting should be skipped.

  Used for local development + automated tests where we intentionally make many
  provisioning/auth requests quickly."
  [ip]
  (or (local-dev-ip? ip)
    (System/getenv "DISABLE_RATE_LIMITING")
    (System/getProperty "DISABLE_RATE_LIMITING")))

(defn- cleanup-expired-entries!
  "Remove expired entries from rate limiting storage to prevent memory leaks."
  []
  (let [now (time/instant)
        expired-keys (filter
                       (fn [key]
                         (when-let [entry (.get rate-limit-storage key)]
                           (let [expires-at (:expires-at entry)]
                             (and expires-at
                               (try (time/before? expires-at now)
                                 (catch Exception _ false))))))
                       (keys rate-limit-storage))]
    (doseq [key expired-keys]
      (.remove rate-limit-storage key))
    (when (seq expired-keys)
      (log/debug "Cleaned up expired rate limit entries" {:count (count expired-keys)}))))

(defn- get-route-type
  "Determine the rate limiting category based on request URI and method."
  [request]
  (let [uri (:uri request)
        method (:request-method request)]
    (cond
      ;; Admin login endpoint - strictest limits
      (and (= method :post)
        (str/ends-with? uri "/admin/api/login"))
      :admin-login

      ;; Admin API endpoints - moderate limits
      (str/starts-with? uri "/admin/api/")
      :admin-api

      ;; Authentication endpoints - moderate limits
      (or (str/starts-with? uri "/auth/")
        (str/starts-with? uri "/oauth/")
        (str/starts-with? uri "/login"))
      :auth-endpoints

      ;; Regular API endpoints - generous limits
      (str/starts-with? uri "/api/")
      :regular-api

      ;; No rate limiting for other routes
      :else nil)))

(defn- is-rate-limited?
  "Check if client should be rate limited and update counters."
  [ip route-type tenant-id user-id]
  (let [config (get rate-limits route-type)
        key (get-rate-limit-key ip route-type tenant-id user-id)
        now (time/instant)
        ;; Support fractional windows/blocks (we use tiny blocks in dev).
        window-ms (long (Math/ceil (* 60000.0 (double (:window-minutes config)))))
        block-ms  (long (Math/ceil (* 60000.0 (double (:block-minutes config)))))
        window-start (time/minus now (time/millis window-ms))

        ;; Get or create entry
        entry (.computeIfAbsent rate-limit-storage key
                (fn [_] {:attempts []
                         :blocked-until nil
                         :created-at now}))

        ;; Check if currently blocked
        blocked-until (:blocked-until entry)]

    (cond
      ;; Currently blocked
      (and blocked-until
        (time/after? blocked-until now))
      (do
        (log/warn "Rate limit block active"
          {:ip ip :route-type route-type :blocked-until blocked-until})
        true)

      :else
      ;; Check attempt count in current window
      (let [recent-attempts (filter (fn [attempt-time]
                                      (and attempt-time window-start
                                        (time/after? attempt-time window-start)))
                              (:attempts entry))
            attempt-count (count recent-attempts)]

        (if (>= attempt-count (:max-attempts config))
          ;; Block client
          (let [block-until (time/plus now (time/millis block-ms))
                updated-entry (assoc entry
                                :blocked-until block-until
                                :attempts (conj recent-attempts now))]
            (.put rate-limit-storage key updated-entry)
            (log/warn "Rate limit exceeded, blocking client"
              {:ip ip
               :route-type route-type
               :attempts attempt-count
               :max-attempts (:max-attempts config)
               :blocked-until block-until})
            true)

          ;; Allow request and record attempt
          (let [updated-entry (assoc entry
                                :attempts (conj recent-attempts now)
                                :blocked-until nil)]
            (.put rate-limit-storage key updated-entry)
            false))))))

(defn- create-rate-limit-response
  "Create HTTP response for rate limited requests."
  [route-type retry-after-seconds]
  {:status 429
   :headers {"Content-Type" "application/json"
             "Retry-After" (str retry-after-seconds)
             "X-RateLimit-Limit" (str (:max-attempts (get rate-limits route-type)))
             "X-RateLimit-Window" (str (:window-minutes (get rate-limits route-type)) "m")}
   :body "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"})

(defn wrap-rate-limiting
  "Middleware to enforce rate limiting on specified routes.

   Automatically detects route types and applies appropriate limits.
   Cleans up expired entries periodically to prevent memory leaks.

   IMPORTANT: This middleware must never swallow exceptions from downstream
   handlers/middleware. If our internal rate limiter fails, we return 503.
   If the application handler fails, we let it fail normally so the real error
   is visible in logs and error handling upstream can do its job."
  [handler]
  (fn [request]
    (let [route-type (get-route-type request)]

      ;; Periodically cleanup expired entries (every ~100 requests)
      (when (zero? (mod (rand-int 100) 100))
        (try
          (cleanup-expired-entries!)
          (catch Exception _ nil)))

      (if-not route-type
        ;; No rate limiting for this route
        (handler request)

        (let [ip (get-client-ip request)
              user-id (get-in request [:session :user-id])
              ;; Include tenant-id for regular API routes so tenants get independent buckets.
              ;; Admin routes stay global (IP-only) since admins operate cross-tenant.
              tenant-id (when (= route-type :regular-api)
                          (or (get-in request [:session :tenant-id])
                            (get-in request [:session :auth-session :tenant :id])))]

          (if (rate-limiting-disabled? ip)
            (handler request)

            (let [limited?
                  (try
                    (is-rate-limited? ip route-type tenant-id user-id)
                    (catch Exception e
                      (log/error e "Rate limiting middleware failed, returning 503"
                        {:route-type route-type
                         :uri (:uri request)
                         :method (:request-method request)
                         :ip ip
                         :tenant-id tenant-id
                         :user-id user-id})
                      ::rate-limiter-error))]

              (cond
                (= limited? ::rate-limiter-error)
                {:status 503
                 :headers {"Content-Type" "application/json"}
                 :body "{\"error\":\"Service unavailable\",\"message\":\"Rate limiting system failure. Please try again shortly.\"}"}

                limited?
                (let [config (get rate-limits route-type)
                      retry-after (long (Math/ceil (* 60.0 (double (:block-minutes config)))))]
                  (create-rate-limit-response route-type retry-after))

                :else
                (handler request)))))))))

(defn check-provisioning-rate-limit!
  "Check tenant provisioning rate limit for an IP.

  Returns nil if OK, or a 429 response map if rate limited.

  IMPORTANT: For localhost / dev / tests we skip this entirely (otherwise
  running E2E suites quickly will trip the 5-per-hour limit)."
  [ip]
  (when-not (rate-limiting-disabled? ip)
    (when (is-rate-limited? ip :tenant-provisioning nil nil)
      (create-rate-limit-response :tenant-provisioning 3600))))

(defn get-rate-limit-stats
  "Get current rate limiting statistics for monitoring."
  []
  (let [now (time/instant)
        entries (into {} rate-limit-storage)
        active-blocks (count (filter (fn [[_ entry]]
                                       (let [blocked-until (:blocked-until entry)]
                                         (and blocked-until
                                           (try (time/after? blocked-until now)
                                             (catch Exception _ false)))))
                               entries))
        total-entries (count entries)]
    {:active-blocks active-blocks
     :total-entries total-entries
     :storage-size (.size rate-limit-storage)}))

(defn clear-rate-limits!
  "Clear all rate limiting data (for testing or emergency)."
  []
  (.clear rate-limit-storage)
  (log/info "Rate limiting storage cleared"))

(ns app.backend.middleware.rate-limiting
  "Rate limiting middleware to prevent brute force attacks and API abuse.

   Features:
   - In-memory rate limiting with configurable windows
   - Different limits for admin vs regular routes
   - IP-based and user-based limiting
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
(def ^:private rate-limits
  {:admin-login      {:max-attempts 20 :window-minutes 5 :block-minutes 1}
   :admin-api        {:max-attempts 100 :window-minutes 5 :block-minutes 5}
   :regular-api      {:max-attempts 300 :window-minutes 5 :block-minutes 1}
   :auth-endpoints   {:max-attempts 100 :window-minutes 10 :block-minutes 1}})

(defn- get-client-ip
  "Extract client IP from request, handling proxies and load balancers."
  [request]
  (or (get-in request [:headers "x-forwarded-for"])
    (get-in request [:headers "x-real-ip"])
    (get-in request [:headers "cf-connecting-ip"])  ; Cloudflare
    (:remote-addr request)
    "unknown"))

(defn- get-rate-limit-key
  "Generate unique key for rate limiting based on IP, route type, and optional user ID."
  [ip route-type user-id]
  (str route-type ":" ip (when user-id (str ":" user-id))))

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
  [ip route-type user-id]
  (let [config (get rate-limits route-type)
        key (get-rate-limit-key ip route-type user-id)
        now (time/instant)
        window-start (time/minus now (time/minutes (:window-minutes config)))

        ;; Get or create entry
        entry (.computeIfAbsent rate-limit-storage key
                (fn [_] {:attempts []
                         :blocked-until nil
                         :created-at now}))

        ;; Check if currently blocked
        blocked-until (:blocked-until entry)]

    (cond
      ;; Currently blocked - with safer time comparison
      (and blocked-until
        (try
          (time/after? blocked-until now)
          (catch Exception e
            (log/warn "Time comparison failed in rate limiting, allowing request" {:error (.getMessage e)})
            false)))
      (do
        (log/warn "Rate limit block active"
          {:ip ip :route-type route-type :blocked-until blocked-until})
        true)

      :else
      ;; Check attempt count in current window - with safer time comparisons
      (let [recent-attempts (try
                              (filter (fn [attempt-time]
                                        (and attempt-time window-start
                                          (try
                                            (time/after? attempt-time window-start)
                                            (catch Exception e
                                              (log/debug "Time comparison failed for attempt, excluding" {:error (.getMessage e)})
                                              false))))
                                (:attempts entry))
                              (catch Exception e
                                (log/warn "Failed to filter recent attempts, using empty list" {:error (.getMessage e)})
                                []))
            attempt-count (count recent-attempts)]

        (if (>= attempt-count (:max-attempts config))
          ;; Block client
          (let [block-until (try
                              (time/plus now (time/minutes (:block-minutes config)))
                              (catch Exception e
                                (log/error "Failed to calculate block-until time" {:error (.getMessage e)})
                                now))
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
   Cleans up expired entries periodically to prevent memory leaks."
  [handler]
  (fn [request]
    (try
      (let [route-type (get-route-type request)]

        ;; Periodically cleanup expired entries (every ~100 requests)
        (when (zero? (mod (rand-int 100) 100))
          (try (cleanup-expired-entries!) (catch Exception _ nil)))

        (if route-type
          (let [ip (get-client-ip request)
                user-id (get-in request [:session :user-id])  ; Optional user-based limiting

                ;; Skip rate limiting for local development
                is-local-dev (or (= ip "127.0.0.1")
                               (= ip "localhost")
                               (str/starts-with? (str ip) "192.168.")
                               (System/getenv "DISABLE_RATE_LIMITING")
                               (System/getProperty "DISABLE_RATE_LIMITING"))]

            (if (and (not is-local-dev)
                  (try (is-rate-limited? ip route-type user-id)
                    (catch Exception e
                      (log/error e "Rate limiting check failed, allowing request")
                      false)))
              ;; Return rate limit response
              (let [config (get rate-limits route-type)
                    retry-after (* (:block-minutes config) 60)]
                (create-rate-limit-response route-type retry-after))

              ;; Allow request to proceed
              (handler request)))

          ;; No rate limiting for this route
          (handler request)))
      (catch Exception e
        (log/error e "Rate limiting middleware failed, allowing request")
        (handler request)))))

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

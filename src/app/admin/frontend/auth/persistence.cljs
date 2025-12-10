(ns app.admin.frontend.auth.persistence
  "Authentication state persistence that survives hot-reloads"
  (:require
    [taoensso.timbre :as log]
    [clojure.walk :as walk]))

;; Storage keys
(def ^:private token-key "admin-token")
(def ^:private user-key "admin-user")
(def ^:private auth-status-key "admin-auth-status")
(def ^:private timestamp-key "admin-auth-timestamp")

;; Session timeout (24 hours in milliseconds)
(def ^:private session-timeout (* 24 60 60 1000))

(defn- serialize-data
  "Safely serialize data for localStorage"
  [data]
  (try
    (js/JSON.stringify (clj->js data))
    (catch js/Error e
      (log/error e "Failed to serialize data for localStorage")
      nil)))

(defn- deserialize-data
  "Safely deserialize data from localStorage"
  [json-str]
  (try
    (when json-str
      (-> (js->clj (js/JSON.parse json-str))
          (walk/keywordize-keys)))
    (catch js/Error e
      (log/error e "Failed to deserialize data from localStorage")
      nil)))

(defn- is-session-valid?
  "Check if the stored session is still valid based on timestamp"
  [timestamp]
  (and timestamp
       (< (- (js/Date.now) timestamp) session-timeout)))

(defn store-auth-state!
  "Persist authentication state to localStorage"
  [{:keys [token user authenticated?]}]
  (when token
    (.setItem js/localStorage token-key token)
    (.setItem js/localStorage timestamp-key (str (js/Date.now))))
  (when user
    (.setItem js/localStorage user-key (serialize-data user)))
  (.setItem js/localStorage auth-status-key (str (boolean authenticated?)))
  (log/info "Stored auth state in localStorage"
           {:token-present (boolean token)
            :user-present (boolean user)
            :authenticated? authenticated?}))

(defn clear-auth-state!
  "Clear all authentication state from localStorage"
  []
  (.removeItem js/localStorage token-key)
  (.removeItem js/localStorage user-key)
  (.removeItem js/localStorage auth-status-key)
  (.removeItem js/localStorage timestamp-key)
  (log/info "Cleared auth state from localStorage"))

(defn load-auth-state
  "Load authentication state from localStorage if valid"
  []
  (let [token (.getItem js/localStorage token-key)
        timestamp-str (.getItem js/localStorage timestamp-key)
        timestamp (when timestamp-str (js/parseInt timestamp-str))
        auth-status-str (.getItem js/localStorage auth-status-key)
        user-json (.getItem js/localStorage user-key)]

    (if (and token (is-session-valid? timestamp))
      (let [user (deserialize-data user-json)
            authenticated? (and auth-status-str (= "true" auth-status-str))]
        (log/info "Loaded valid auth state from localStorage"
                 {:token-present (boolean token)
                  :user-present (boolean user)
                  :authenticated? authenticated?
                  :session-age-ms (- (js/Date.now) timestamp)})
        {:token token
         :user user
         :authenticated? authenticated?
         :valid? true})
      (do
        (when (or token timestamp)
          (log/warn "Found expired or invalid auth state, clearing"
                   {:token-present (boolean token)
                    :timestamp timestamp
                    :current-time (js/Date.now)}))
        (clear-auth-state!)
        {:valid? false}))))

(defn update-token!
  "Update just the token in storage (e.g., after session refresh)"
  [token]
  (if token
    (do
      (.setItem js/localStorage token-key token)
      (.setItem js/localStorage timestamp-key (str (js/Date.now)))
      (log/info "Updated token in localStorage"))
    (do
      (.removeItem js/localStorage token-key)
      (.removeItem js/localStorage timestamp-key)
      (log/info "Removed token from localStorage"))))

(defn get-persisted-token
  "Get the persisted token if valid"
  []
  (let [token (.getItem js/localStorage token-key)
        timestamp-str (.getItem js/localStorage timestamp-key)
        timestamp (when timestamp-str (js/parseInt timestamp-str))]
    (if (and token (is-session-valid? timestamp))
      (do
        (log/info "Retrieved valid persisted token")
        token)
      (do
        (when token
          (log/warn "Persisted token expired, clearing"))
        (clear-auth-state!)
        nil))))

;; Initialize auth state restoration
(defn init-auth-persistence!
  "Initialize authentication persistence and restore state if available"
  [on-state-restore]
  (let [auth-state (load-auth-state)]
    (when (:valid? auth-state)
      (log/info "Restoring authentication state after hot-reload")
      (on-state-restore auth-state))
    auth-state))

;; Check if we have a valid session on app start
(defn has-valid-session?
  "Check if there's a valid session stored"
  []
  (let [token (.getItem js/localStorage token-key)
        timestamp-str (.getItem js/localStorage timestamp-key)
        timestamp (when timestamp-str (js/parseInt timestamp-str))]
    (and token (is-session-valid? timestamp))))

;; Export for debugging
(defn debug-auth-storage
  "Debug function to inspect auth storage state"
  []
  {:token-present (boolean (.getItem js/localStorage token-key))
   :user-present (boolean (.getItem js/localStorage user-key))
   :auth-status (.getItem js/localStorage auth-status-key)
   :timestamp (.getItem js/localStorage timestamp-key)
   :session-valid? (has-valid-session?)})
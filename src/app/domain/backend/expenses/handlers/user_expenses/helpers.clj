(ns app.domain.backend.expenses.handlers.user-expenses.helpers
  "Common helpers for user expense handlers."
  (:require
    [cheshire.core :as json])
  (:import
    [java.util UUID]))

(defn try-parse-uuid
  "Parse a UUID from string, returns nil if invalid."
  [s]
  (when s
    (try
      (UUID/fromString (str s))
      (catch Exception _ nil))))

(defn parse-boolean-param
  "Parse boolean parameter from query params map (string values)."
  [params k]
  (when-let [val (get params k)]
    (Boolean/parseBoolean (str val))))

(defn get-user-id
  "Extract user-id from request session and normalize to UUID.
   Accepts either UUID objects or string UUIDs; returns nil if missing/invalid."
  [request]
  (let [raw-id (or (get-in request [:session :auth-session :user :id])
                 (get-in request [:session :user :id]))]
    (cond
      (instance? UUID raw-id) raw-id
      :else (try-parse-uuid raw-id))))

(defn json-response
  "Create a JSON response with the given body and status."
  ([body] (json-response body 200))
  ([body status]
   {:status status
    :headers {"Content-Type" "application/json"}
    :body (json/generate-string body)}))

(defn unauthorized-response
  "Return 401 unauthorized response."
  ([] (unauthorized-response "Authentication required"))
  ([message]
   (json-response {:error message} 401)))

(defn not-found-response
  "Return 404 not found response."
  ([] (not-found-response "Resource not found"))
  ([message]
   (json-response {:error message} 404)))

(defn forbidden-response
  "Return 403 forbidden response."
  ([] (forbidden-response "Forbidden"))
  ([message]
   (json-response {:error message} 403)))

(defn- normalize-role
  [role]
  (cond
    (keyword? role) (name role)
    (string? role) role
    :else nil))

(defn get-user-role
  [request]
  (normalize-role
    (or (get-in request [:session :auth-session :user :role])
      (get-in request [:session :user :role]))))

(def reference-data-read-roles
  #{"viewer" "member" "admin" "owner"})

(def reference-data-write-roles
  #{"member" "admin" "owner"})

(defn ensure-role
  [request allowed-roles message]
  (let [role (get-user-role request)]
    (when-not (contains? allowed-roles role)
      (forbidden-response (or message "Forbidden")))))

(defn read-json-body
  [request]
  (or (:body-params request)
    (json/parse-string (slurp (:body request)) true)))

(defn read-body-params
  "Flexible body parsing that handles various input formats."
  [request]
  (or (:body-params request)
    (get-in request [:parameters :body])
    (when-let [b (:body request)]
      (cond
        (map? b) b
        (string? b) (json/parse-string b true)
        :else (json/parse-string (slurp b) true)))
    {}))

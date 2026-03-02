(ns app.domain.backend.expenses.handlers.user-expenses.helpers
  "Common helpers for user-facing expenses handlers.

  Intended to be used by all /api/v1/expenses/** endpoints (not just expenses CRUD),
  so we keep auth/role extraction and JSON responses consistent."
  (:require
    [app.shared.http :as shared-http]
    [app.shared.model-naming :as model-naming]
    [app.shared.type-conversion :as type-conv]
    [cheshire.core :as json]
    [clojure.string :as str])
  (:import
    [java.util UUID]))

(defn get-param
  "Get a parameter from a params map that may have keyword keys, string keys,
  snake_case, or kebab-case.

  Examples:
  - (get-param params :supplier_id) will match :supplier_id, \"supplier_id\",
    :supplier-id, or \"supplier-id\".

  Intended for Ring/Reitit :query-params maps that often contain string keys."
  [params k]
  (when (some? params)
    (let [k-name (when (keyword? k) (name k))
          k-str (when (string? k) k)
          variants (->> [(when (keyword? k) k)
                         k-name
                         (when k-name (str/replace k-name "_" "-"))
                         (when k-name (str/replace k-name "-" "_"))
                         (when k-str (keyword k-str))
                         (when k-str (keyword (str/replace k-str "_" "-")))
                         (when k-str (keyword (str/replace k-str "-" "_")))]
                     (remove nil?))]
      (some #(get params %) variants))))

(defn try-parse-uuid
  "Parse a UUID from string, returns nil if invalid."
  [s]
  (type-conv/try-parse-uuid s))

(defn parse-boolean-param
  "Parse boolean parameter from query params map (string values)."
  [params k]
  (when-let [val (get-param params k)]
    (Boolean/parseBoolean (str val))))

(defn parse-order-by
  "Parse an `order-by` value from params into an app keyword.

  Accepts values like \"display_name\", \"display-name\", :display_name, :display-name.
  Ignores blanks; returns nil when missing."
  [params]
  (when-let [raw (get-param params :order-by)]
    (let [s (some-> raw str str/trim not-empty)
          s (some-> s (str/replace #"^:" ""))]
      (when s
        (model-naming/ensure-app-keyword s)))))

(defn parse-order-dir
  "Parse an `order-dir` value from params into :asc or :desc.

  Accepts \"asc\"/\"desc\" (case-insensitive) and keyword-ish values like :asc.
  Returns nil when missing/invalid so services can apply their default."
  [params]
  (let [s (some-> (get-param params :order-dir) str str/trim str/lower-case)
        s (some-> s (str/replace #"^:" ""))]
    (case s
      "asc" :asc
      "desc" :desc
      nil)))

(def ^:private max-page-limit
  "Maximum rows a paginated endpoint will return."
  500)

(defn parse-page-limit
  "Parse the `limit` query param to a clamped long in [1, 500].

  Falls back to `default-limit` when the param is absent or non-numeric."
  [params default-limit]
  (-> (or (some-> (get-param params :limit) parse-long)
        default-limit)
    long
    (max 1)
    (min max-page-limit)))

(defn parse-page-offset
  "Parse the `offset` query param to a non-negative long."
  [params]
  (max 0 (long (or (some-> (get-param params :offset) parse-long) 0))))

(defn get-user
  "Return the user map from the request (session or identity), or nil if missing.

  Some routes/middleware attach an `:identity` map (e.g. Buddy) while others rely
  on the template session structure."
  [request]
  (or (get-in request [:session :auth-session :user])
    (get-in request [:session :user])
    (:identity request)))

(defn get-user-id
  "Extract user-id from request session and normalize to UUID.
   Accepts either UUID objects or string UUIDs; returns nil if missing/invalid."
  [request]
  (let [raw-id (or (get-in request [:session :auth-session :user :id])
                 (get-in request [:session :user :id])
                 (get-in request [:identity :id]))]
    (cond
      (instance? UUID raw-id) raw-id
      :else (try-parse-uuid raw-id))))

(defn get-tenant-id
  "Extract tenant-id from request session and normalize to UUID.
   Mirrors `get-user-id` but reads from the tenant context populated by
   Phase 1 auth middleware.
   Handles both unqualified (:id) and namespaced (:tenants/id) keys
   since next.jdbc returns qualified keys from queries."
  [request]
  (let [raw-id (or (get-in request [:session :auth-session :tenant :id])
                 (get-in request [:session :auth-session :tenant :tenants/id])
                 (get-in request [:session :tenant-id]))]
    (cond
      (instance? UUID raw-id) raw-id
      :else (try-parse-uuid raw-id))))

(defn json-response
  "Create a JSON response with the given body and status."
  ([body] (json-response body 200))
  ([body status]
   (shared-http/json-string-response status body)))

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
  "Return the effective role string for the current user.

  Priority: tenant membership role > global user role > identity role.
  The membership role is set by `build-auth-session` (Phase 1) and reflects
  the user's role within their current tenant."
  [request]
  (normalize-role
    (or (get-in request [:session :auth-session :membership :role])
      (get-in request [:session :auth-session :user :role])
      (get-in request [:session :user :role])
      (get-in request [:identity :role]))))

(defn tenant-elevated?
  "True when the user has owner or admin membership role in the current tenant."
  [request]
  (let [role (get-user-role request)]
    (contains? #{"admin" "owner"} role)))

(def reference-data-read-roles
  #{"viewer" "member" "admin" "owner"})

(def reference-data-write-roles
  #{"member" "admin" "owner"})

(def expenses-read-roles
  "Roles allowed to read expenses data (summary, list, detail)."
  reference-data-read-roles)

(def expenses-write-roles
  "Roles allowed to create/update/delete expenses."
  reference-data-write-roles)

(def receipts-write-roles
  "Roles allowed to mutate receipts (upload, review, approve, delete, OCR)."
  reference-data-write-roles)

(defn ensure-role
  [request allowed-roles message]
  (let [role (get-user-role request)]
    (when-not (contains? allowed-roles role)
      (forbidden-response (or message "Forbidden")))))

(defn read-json-body
  [request]
  (or
    (:body-params request)
    (when-let [body (:body request)]
      (json/parse-string (slurp body) true))
    {}))

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

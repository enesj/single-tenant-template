(ns app.domain.backend.expenses.handlers.user-expenses.helpers
  "Common helpers for user-facing expenses handlers.

  Intended to be used by all /api/v1/expenses/** endpoints (not just expenses CRUD),
  so we keep auth/role extraction and JSON responses consistent."
  (:require
    [app.shared.adapters.database :as shared-db]
    [app.shared.http :as shared-http]
    [app.shared.model-naming :as model-naming]
    [app.shared.type-conversion :as type-conv]
    [cheshire.core :as json]
    [clojure.string :as str])
  (:import
    [java.util UUID]
    [org.postgresql.util PSQLException]))

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

(defn- normalize-sort-field
  [field]
  (some-> field str str/trim not-empty (str/replace #"^:" "") model-naming/ensure-app-keyword))

(defn- normalize-sort-direction
  [direction]
  (case (some-> direction str str/trim str/lower-case (str/replace #"^:" ""))
    "asc" :asc
    "desc" :desc
    nil))

(defn- parse-sort-entry
  [raw-entry]
  (let [[field direction] (some-> raw-entry str str/trim (str/split #":" 2))
        field* (normalize-sort-field field)
        direction* (normalize-sort-direction direction)]
    (when (and field* direction*)
      {:field field*
       :direction direction*})))

(defn parse-sort-params
  "Parse canonical sort params from user expenses query params.

  Accepts the new `sort` query param (`field:dir,field2:dir`) and also tolerates
  legacy `order-by` / `order-dir` during the cutover.

  Returns:
  - :sorts     ordered sort entries
  - :order-by  first sort field (legacy convenience)
  - :order-dir first sort direction (legacy convenience)"
  [params]
  (let [sorts (or (some-> (get-param params :sort)
                    str
                    (str/split #",")
                    (->> (keep parse-sort-entry)
                      seq
                      vec))
                (let [order-by (normalize-sort-field (get-param params :order-by))
                      order-dir (normalize-sort-direction (get-param params :order-dir))]
                  (when (and order-by order-dir)
                    [{:field order-by :direction order-dir}])))
        primary-sort (first sorts)]
    (cond-> {}
      (seq sorts) (assoc :sorts sorts)
      (:field primary-sort) (assoc :order-by (:field primary-sort))
      (:direction primary-sort) (assoc :order-dir (:direction primary-sort)))))

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

(defn parse-instant-param
  "Parse an instant-like query param.

  Accepts ISO instants or YYYY-MM-DD dates; returns nil when the value is blank
  or invalid. Date-only values are interpreted at UTC start-of-day."
  [raw]
  (when-let [value (some-> raw str str/trim not-empty)]
    (or (try
          (java.time.Instant/parse value)
          (catch Exception _ nil))
      (try
        (-> (java.time.LocalDate/parse value)
          (.atStartOfDay java.time.ZoneOffset/UTC)
          .toInstant)
        (catch Exception _ nil)))))

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

  Priority order:
  1) Tenant membership role (Phase 1 sessions)
  2) Global user role (legacy/pre-tenant sessions)
  3) Identity role (Buddy/OAuth middleware)"
  [request]
  (normalize-role
    (or (get-in request [:session :auth-session :membership :role])
      (get-in request [:session :auth-session :user :role])
      (get-in request [:session :user :role])
      (get-in request [:identity :role]))))

(defn tenant-elevated?
  "True when the user has member, admin, or owner membership role in the current tenant.
   Members and above can see all tenant data (receipts, expenses)."
  [request]
  (let [role (get-user-role request)]
    (contains? #{"member" "admin" "owner"} role)))

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

;; ---------------------------------------------------------------------------
;; Shared handler helpers
;; ---------------------------------------------------------------------------

(def to-app
  "Normalize database result keys to app (kebab-case) format."
  shared-db/to-app)

(def admin-owner-roles
  "Roles with administrative management access (admin and owner only)."
  #{"admin" "owner"})

(defn ensure-admin-or-owner
  "Check that the requesting user has admin or owner role.
  Returns a forbidden response if not authorized, nil if ok."
  ([request]
   (ensure-admin-or-owner request "Only admins and owners can access this resource."))
  ([request message]
   (ensure-role request admin-owner-roles message)))

(defn body-has-any-key?
  "True if `body` contains any of the given keys."
  [body ks]
  (boolean (some #(contains? body %) ks)))

(defn body-get-first
  "Return the value for the first matching key found in `body`."
  [body ks]
  (some #(get body %) ks))

(defn parse-path-id
  "Extract and parse a UUID from the request path parameters."
  ([request] (parse-path-id request :id))
  ([request param-key]
   (try-parse-uuid (or (get-in request [:path-params param-key])
                     (get-in request [:parameters :path param-key])))))

(defn parse-batch-ids
  "Parse batch IDs from request body, trying multiple key variants.
  Returns [raw-ids parsed-ids] where parsed-ids are valid UUIDs."
  [body id-keys]
  (let [raw-ids (or (some #(get body %) id-keys) [])
        ids (->> raw-ids (map try-parse-uuid) (filter some?) vec)]
    [raw-ids ids]))

(defn extract-text-filter-params
  "Extract text filter params from a query-params map given a whitelist of keys.

  `filter-keys` is a collection of keywords (e.g. [:supplier-display-name :notes]).
  Returns a map of {key value} for non-nil values found in params."
  [params filter-keys]
  (reduce
    (fn [acc k]
      (if-let [v (get-param params k)]
        (assoc acc k v)
        acc))
    {}
    filter-keys))

(defn batch-delete-entities
  "Execute batch delete of entities by IDs.
  `delete-one!` is a function of (id) that returns truthy on success.
  Returns a map of {:deleted-count :deleted-ids :errors}."
  [delete-one! ids]
  (let [deleted-ids (atom [])
        errors (atom [])]
    (doseq [id ids]
      (try
        (if (boolean (delete-one! id))
          (swap! deleted-ids conj (str id))
          (swap! errors conj {:id (str id) :error "not found"}))
        (catch PSQLException e
          (let [sql-state (.getSQLState e)]
            (swap! errors conj {:id (str id)
                                :error (if (= "23503" sql-state)
                                         "foreign key constraint"
                                         "database error")
                                :sql-state sql-state})))
        (catch Exception e
          (swap! errors conj {:id (str id) :error (.getMessage e)}))))
    {:deleted-count (count @deleted-ids)
     :deleted-ids (vec @deleted-ids)
     :errors (vec @errors)}))

(ns app.domain.backend.expenses.handlers.user-expenses.reference-data
  "Reference data (suppliers, payers) handlers for user expenses."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [clojure.string :as str]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log]))

(defn- resolve-service-map
  "Resolve the `service` var in a service namespace and return its value."
  [service-ns]
  (when-let [service-var (requiring-resolve (symbol (name service-ns) "service"))]
    (let [m (var-get service-var)]
      (when (map? m)
        m))))

(defn- resolve-service-op-fn
  "Resolve an operation fn for a service.

  Prefers an explicitly named var when provided (so wrappers/overrides still win),
  otherwise falls back to the `service` map key."
  ([service-ns service-op]
   (resolve-service-op-fn service-ns service-op nil))
  ([service-ns service-op legacy-var]
   (or (when legacy-var
         (requiring-resolve (symbol (name service-ns) (name legacy-var))))
     (when-let [m (resolve-service-map service-ns)]
       (get m service-op))
     (throw (ex-info (str "Could not resolve service op " service-op
                       " in namespace " service-ns)
              {:ns service-ns
               :service-op service-op
               :legacy-var legacy-var})))))

(defn- default-payer-type-id
  "Return the current default payer type id or nil."
  [db]
  (some-> ((requiring-resolve 'app.domain.backend.expenses.services.payer-types/get-default-payer-type)
           db)
    :id))

(def ^:private supplier-purge-roles
  "User roles allowed to permanently purge suppliers."
  #{"admin" "owner"})

(defn list-suppliers-handler
  "Handler factory for listing suppliers available to users."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles "Role assignment required")]
        forbidden
        (try
          (let [params (:query-params request)
                limit (or (some-> (:limit params) parse-long) 100)
                offset (or (some-> (:offset params) parse-long) 0)
                include-archived (boolean (h/parse-boolean-param params :include_archived))
                suppliers-svc (requiring-resolve 'app.domain.backend.expenses.services.suppliers/list-suppliers)
                suppliers (vec (suppliers-svc db {:limit limit
                                                  :offset offset
                                                  :include_archived include-archived}))
                role (h/get-user-role request)
                include-active-expenses-count? (contains? supplier-purge-roles role)
                suppliers-with-counts
                (if include-active-expenses-count?
                  (let [supplier-ids (keep :id suppliers)
                        active-counts-fn (requiring-resolve 'app.domain.backend.expenses.services.suppliers/active-expenses-counts-by-supplier)
                        active-counts (active-counts-fn db supplier-ids)]
                    (mapv (fn [s]
                            (assoc s :active_expenses_count (long (get active-counts (:id s) 0))))
                      suppliers))
                  suppliers)]
            (h/json-response {:data suppliers-with-counts}))
          (catch Exception e
            (log/error e "Error listing suppliers")
            (h/json-response {:error "Failed to list suppliers"} 500))))
      (h/unauthorized-response))))

(defn get-supplier-handler
  "Handler factory for getting a single supplier by id.

  NOTE: This is user-facing API (non-admin)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles "Role assignment required")]
        forbidden
        (let [supplier-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
                            (h/try-parse-uuid (get-in request [:parameters :path :id])))]
          (if supplier-id
            (try
              (let [get-supplier (resolve-service-op-fn
                                   'app.domain.backend.expenses.services.suppliers
                                   :get
                                   'get-supplier)
                    supplier (get-supplier db supplier-id)]
                (if supplier
                  (h/json-response {:data supplier})
                  (h/not-found-response "Supplier not found")))
              (catch Exception e
                (log/error e "Error getting supplier" {:supplier-id supplier-id})
                (h/json-response {:error "Failed to get supplier"} 500)))
            (h/json-response {:error "Invalid supplier ID"} 400))))
      (h/unauthorized-response))))

(defn list-payers-handler
  "Handler factory for listing payers available to users."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles "Role assignment required")]
        forbidden
        (try
          (let [params (:query-params request)
                limit (or (some-> (:limit params) parse-long) 100)
                offset (or (some-> (:offset params) parse-long) 0)
                payers-svc (resolve-service-op-fn
                             'app.domain.backend.expenses.services.payers
                             :list
                             'list-payers)
                payers (payers-svc db {:limit limit :offset offset})]
            (h/json-response {:data payers}))
          (catch Exception e
            (log/error e "Error listing payers")
            (h/json-response {:error "Failed to list payers"} 500))))
      (h/unauthorized-response))))

(defn- find-payer-type-id-by-label
  [db label]
  (when (some? label)
    (some-> (jdbc/execute-one!
              db
              (hsql/format {:select [:id]
                            :from [:payer_types]
                            :where [:= [:lower :label] (some-> label str str/lower-case)]
                            :limit 1})
              {:builder-fn rs/as-unqualified-lower-maps})
      :id)))

(defn create-supplier-handler
  "Handler factory for creating a supplier (shared catalog).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-write-roles "Only members and admins can modify suppliers")]
        forbidden
        (try
          (let [body (h/read-json-body request)
                 supplier-data (select-keys body [:display_name :address])
                create-supplier! (resolve-service-op-fn
                                   'app.domain.backend.expenses.services.suppliers
                                   :create!
                                   'create-supplier!)
                supplier (create-supplier! db supplier-data)]
            (h/json-response {:data supplier} 201))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating supplier" {:error (ex-message e) :data (ex-data e)})
            (h/json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Error creating supplier")
            (h/json-response {:error "Failed to create supplier"} 500))))
      (h/unauthorized-response))))

(defn update-supplier-handler
  "Handler factory for updating a supplier (shared catalog).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-write-roles "Only members and admins can modify suppliers")]
        forbidden
        (let [supplier-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
                            (h/try-parse-uuid (get-in request [:parameters :path :id])))]
          (if supplier-id
            (try
              (let [body (h/read-json-body request)
                   updates (select-keys body [:display_name :address])
                    update-supplier! (resolve-service-op-fn
                                       'app.domain.backend.expenses.services.suppliers
                                       :update!
                                       'update-supplier!)
                    supplier (update-supplier! db supplier-id updates)]
                (if supplier
                  (h/json-response {:data supplier})
                  (h/not-found-response "Supplier not found")))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating supplier" {:error (ex-message e) :data (ex-data e)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Error updating supplier" {:supplier-id supplier-id})
                (h/json-response {:error "Failed to update supplier"} 500)))
            (h/json-response {:error "Invalid supplier ID"} 400))))
      (h/unauthorized-response))))

(defn delete-supplier-handler
  "Handler factory for deleting a supplier (shared catalog).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-write-roles "Only members and admins can modify suppliers")]
        forbidden
        (let [supplier-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
                            (h/try-parse-uuid (get-in request [:parameters :path :id])))]
          (if supplier-id
            (try
              (let [delete-supplier! (requiring-resolve 'app.domain.backend.expenses.services.suppliers/delete-supplier!)
                    deleted? (boolean (delete-supplier! db supplier-id))]
                (if deleted?
                  (h/json-response {:success true})
                  (h/not-found-response "Supplier not found")))
              (catch org.postgresql.util.PSQLException e
                (let [sql-state (.getSQLState e)]
                  (if (= "23503" sql-state) ;; foreign_key_violation
                    (do
                      (log/warn "Cannot delete supplier - has related records" {:supplier-id supplier-id})
                      (h/json-response {:error "Cannot delete: record has related data. Remove related records first."} 409))
                    (do
                      (log/error e "Database error deleting supplier" {:supplier-id supplier-id :sql-state sql-state})
                      (h/json-response {:error "Failed to delete supplier"} 500)))))
              (catch Exception e
                (log/error e "Error deleting supplier" {:supplier-id supplier-id})
                (h/json-response {:error "Failed to delete supplier"} 500)))
            (h/json-response {:error "Invalid supplier ID"} 400))))
      (h/unauthorized-response))))

;; ---------------------------------------------------------------------------
;; Suppliers: purge (hard delete)
;; ---------------------------------------------------------------------------

(defn purge-supplier-preview-handler
  "Handler factory for previewing what would be deleted by a supplier purge.

  Allowed roles: admin/owner.

  NOTE: This is user-facing API (non-admin)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request supplier-purge-roles "Only admins and owners can purge suppliers")]
        forbidden
        (let [supplier-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
                            (h/try-parse-uuid (get-in request [:parameters :path :id])))]
          (if supplier-id
            (try
              (let [purge-preview (requiring-resolve 'app.domain.backend.expenses.services.suppliers/purge-supplier-preview)
                    preview (purge-preview db supplier-id)]
                (h/json-response {:preview preview}))
              (catch clojure.lang.ExceptionInfo e
                (let [status (or (:status (ex-data e)) 400)]
                  (h/json-response {:error (ex-message e)} status)))
              (catch Exception e
                (log/error e "Error previewing supplier purge" {:supplier-id supplier-id})
                (h/json-response {:error "Failed to load supplier purge preview"} 500)))
            (h/json-response {:error "Invalid supplier ID"} 400))))
      (h/unauthorized-response))))

(defn purge-supplier-handler
  "Handler factory for permanently purging an archived supplier.

  Allowed roles: admin/owner.

  NOTE: This is user-facing API (non-admin)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request supplier-purge-roles "Only admins and owners can purge suppliers")]
        forbidden
        (let [supplier-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
                            (h/try-parse-uuid (get-in request [:parameters :path :id])))]
          (if supplier-id
            (try
              (let [purge-supplier! (requiring-resolve 'app.domain.backend.expenses.services.suppliers/purge-supplier!)
                    result (purge-supplier! db supplier-id)]
                (h/json-response {:result result}))
              (catch clojure.lang.ExceptionInfo e
                (let [status (or (:status (ex-data e)) 400)]
                  (h/json-response {:error (ex-message e)} status)))
              (catch Exception e
                (log/error e "Error purging supplier" {:supplier-id supplier-id})
                (h/json-response {:error "Failed to purge supplier"} 500)))
            (h/json-response {:error "Invalid supplier ID"} 400))))
      (h/unauthorized-response))))

(defn create-payer-handler
  "Handler factory for creating a payer (shared catalog).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-write-roles "Only members and admins can modify payers")]
        forbidden
        (try
          (let [body (h/read-json-body request)
                ;; Require FK; fall back to default payer type when not provided
                payer-type-id-raw (or (:payer_type_id body) (default-payer-type-id db))
                payer-type-id (cond
                                (instance? java.util.UUID payer-type-id-raw) payer-type-id-raw
                                :else (h/try-parse-uuid payer-type-id-raw))
                _ (when (nil? payer-type-id)
                    (throw (ex-info "payer_type_id is required" {:status 400})))
                payer-data (-> (select-keys body [:label :is_default])
                             (assoc :payer_type_id payer-type-id)
                             (cond-> (contains? body :is_default)
                               (update :is_default boolean)))
                create-payer! (requiring-resolve 'app.domain.backend.expenses.services.payers/create-payer!)
                payer (create-payer! db payer-data)]
            (h/json-response {:data payer} 201))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating payer" {:error (ex-message e) :data (ex-data e)})
            (h/json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Error creating payer")
            (h/json-response {:error "Failed to create payer"} 500))))
      (h/unauthorized-response))))

(defn update-payer-handler
  "Handler factory for updating a payer (shared catalog).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-write-roles "Only members and admins can modify payers")]
        forbidden
        (let [payer-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
                         (h/try-parse-uuid (get-in request [:parameters :path :id])))]
          (if payer-id
            (try
              (let [body (h/read-json-body request)
                    payer-type-id-raw (:payer_type_id body)
                    payer-type-id (when (contains? body :payer_type_id)
                                    (cond
                                      (instance? java.util.UUID payer-type-id-raw) payer-type-id-raw
                                      :else (h/try-parse-uuid payer-type-id-raw)))
                    _ (when (and (contains? body :payer_type_id) (nil? payer-type-id))
                        (throw (ex-info "Invalid payer_type_id" {:status 400})))
                    updates (-> (select-keys body [:label :is_default])
                              (cond-> (contains? body :payer_type_id)
                                (assoc :payer_type_id payer-type-id))
                              (cond-> (contains? body :is_default)
                                (update :is_default boolean)))
                    update-payer! (requiring-resolve 'app.domain.backend.expenses.services.payers/update-payer!)
                    payer (update-payer! db payer-id updates)]
                (if payer
                  (h/json-response {:data payer})
                  (h/not-found-response "Payer not found")))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating payer" {:error (ex-message e) :data (ex-data e)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Error updating payer" {:payer-id payer-id})
                (h/json-response {:error "Failed to update payer"} 500)))
            (h/json-response {:error "Invalid payer ID"} 400))))
      (h/unauthorized-response))))

(defn delete-payer-handler
  "Handler factory for deleting a payer (shared catalog).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-write-roles "Only members and admins can modify payers")]
        forbidden
        (let [payer-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
                         (h/try-parse-uuid (get-in request [:parameters :path :id])))]
          (if payer-id
            (try
              (let [delete-payer! (resolve-service-op-fn
                                    'app.domain.backend.expenses.services.payers
                                    :delete!
                                    'delete-payer!)
                    deleted? (boolean (delete-payer! db payer-id))]
                (if deleted?
                  (h/json-response {:success true})
                  (h/not-found-response "Payer not found")))
              (catch org.postgresql.util.PSQLException e
                (let [sql-state (.getSQLState e)]
                  (if (= "23503" sql-state) ;; foreign_key_violation
                    (do
                      (log/warn "Cannot delete payer - has related records" {:payer-id payer-id})
                      (h/json-response {:error "Cannot delete payer: it has related expenses or other records. Remove related records first."} 409))
                    (do
                      (log/error e "Database error deleting payer" {:payer-id payer-id :sql-state sql-state})
                      (h/json-response {:error "Failed to delete payer"} 500)))))
              (catch Exception e
                (log/error e "Error deleting payer" {:payer-id payer-id})
                (h/json-response {:error "Failed to delete payer"} 500)))
            (h/json-response {:error "Invalid payer ID"} 400))))
      (h/unauthorized-response))))

(def ^:private payer-type-manage-roles
  "User roles allowed to manage payer types."
  #{"admin" "owner"})

(defn list-payer-types-handler
  "Handler factory for listing payer types.

  Allowed roles: viewer/member/admin/owner."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles "Role assignment required")]
        forbidden
        (try
          (let [params (:query-params request)
                limit (or (some-> (:limit params) parse-long) 100)
                offset (or (some-> (:offset params) parse-long) 0)
                list-payer-types (resolve-service-op-fn
                                   'app.domain.backend.expenses.services.payer-types
                                   :list)
                payer-types (vec (list-payer-types db {:limit limit :offset offset}))]
            (h/json-response {:data payer-types}))
          (catch Exception e
            (log/error e "Error listing payer types")
            (h/json-response {:error "Failed to list payer types"} 500))))
      (h/unauthorized-response))))

(defn create-payer-type-handler
  "Handler factory for creating a payer type.

  Allowed roles: admin/owner."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request payer-type-manage-roles "Only admins and owners can manage payer types")]
        forbidden
        (try
          (let [body (h/read-json-body request)
                payer-type-data (-> (select-keys body [:label :is_default])
                                  (cond-> (contains? body :is_default)
                                    (update :is_default boolean)))
                create-payer-type! (requiring-resolve 'app.domain.backend.expenses.services.payer-types/create-payer-type!)
                payer-type (create-payer-type! db payer-type-data)]
            (h/json-response {:data payer-type} 201))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating payer type" {:error (ex-message e) :data (ex-data e)})
            (h/json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Error creating payer type")
            (h/json-response {:error "Failed to create payer type"} 500))))
      (h/unauthorized-response))))

(defn update-payer-type-handler
  "Handler factory for updating a payer type.

  Allowed roles: admin/owner."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request payer-type-manage-roles "Only admins and owners can manage payer types")]
        forbidden
        (let [payer-type-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
                              (h/try-parse-uuid (get-in request [:parameters :path :id])))]
          (if payer-type-id
            (try
              (let [body (h/read-json-body request)
                    updates (-> (select-keys body [:label :is_default])
                              (cond-> (contains? body :is_default)
                                (update :is_default boolean)))
                    update-payer-type! (requiring-resolve 'app.domain.backend.expenses.services.payer-types/update-payer-type!)
                    payer-type (update-payer-type! db payer-type-id updates)]
                (if payer-type
                  (h/json-response {:data payer-type})
                  (h/not-found-response "Payer type not found")))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating payer type" {:error (ex-message e) :data (ex-data e)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Error updating payer type" {:payer-type-id payer-type-id})
                (h/json-response {:error "Failed to update payer type"} 500)))
            (h/json-response {:error "Invalid payer type ID"} 400))))
      (h/unauthorized-response))))

(defn delete-payer-type-handler
  "Handler factory for deleting a payer type.

  Allowed roles: admin/owner."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request payer-type-manage-roles "Only admins and owners can manage payer types")]
        forbidden
        (let [payer-type-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
                              (h/try-parse-uuid (get-in request [:parameters :path :id])))]
          (if payer-type-id
            (try
              (let [delete-payer-type! (resolve-service-op-fn
                                         'app.domain.backend.expenses.services.payer-types
                                         :delete!)
                    deleted? (boolean (delete-payer-type! db payer-type-id))]
                (if deleted?
                  (h/json-response {:success true})
                  (h/not-found-response "Payer type not found")))
              (catch org.postgresql.util.PSQLException e
                (let [sql-state (.getSQLState e)]
                  (if (= "23503" sql-state) ;; foreign_key_violation
                    (do
                      (log/warn "Cannot delete payer type - has related records" {:payer-type-id payer-type-id})
                      (h/json-response {:error "Cannot delete payer type: it has related payers. Update those payers first."} 409))
                    (do
                      (log/error e "Database error deleting payer type" {:payer-type-id payer-type-id :sql-state sql-state})
                      (h/json-response {:error "Failed to delete payer type"} 500)))))
              (catch Exception e
                (log/error e "Error deleting payer type" {:payer-type-id payer-type-id})
                (h/json-response {:error "Failed to delete payer type"} 500)))
            (h/json-response {:error "Invalid payer type ID"} 400))))
      (h/unauthorized-response))))


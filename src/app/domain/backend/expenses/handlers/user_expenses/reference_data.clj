(ns app.domain.backend.expenses.handlers.user-expenses.reference-data
  "Reference data (suppliers, payers) handlers for user expenses."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [taoensso.timbre :as log]))

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
              (let [get-supplier (requiring-resolve 'app.domain.backend.expenses.services.suppliers/get-supplier)
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
                payers-svc (requiring-resolve 'app.domain.backend.expenses.services.payers/list-payers)
                payers (payers-svc db {:limit limit :offset offset})]
            (h/json-response {:data payers}))
          (catch Exception e
            (log/error e "Error listing payers")
            (h/json-response {:error "Failed to list payers"} 500))))
      (h/unauthorized-response))))

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
                supplier-data (select-keys body [:display_name :address :tax_id])
                create-supplier! (requiring-resolve 'app.domain.backend.expenses.services.suppliers/create-supplier!)
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
                    updates (select-keys body [:display_name :address :tax_id])
                    update-supplier! (requiring-resolve 'app.domain.backend.expenses.services.suppliers/update-supplier!)
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
                payer-data (-> (select-keys body [:label :type :is_default])
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
                    updates (-> (select-keys body [:label :type :is_default])
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
              (let [delete-payer! (requiring-resolve 'app.domain.backend.expenses.services.payers/delete-payer!)
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

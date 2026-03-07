(ns app.domain.backend.expenses.handlers.user-expenses.reference-data
  "Reference data (suppliers, payers) handlers for user expenses."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]

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
  ([db] (default-payer-type-id db nil))
  ([db tenant-id]
   (some-> ((requiring-resolve 'app.domain.backend.expenses.services.payer-types/get-default-payer-type)
            db tenant-id)
     :id)))

(defn list-suppliers-handler
  "Handler factory for listing suppliers available to users.
   When a tenant-id is present, scopes to suppliers used in that tenant's expenses."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles "Role assignment required")]
        forbidden
        (try
          (let [tenant-id (h/get-tenant-id request)
                params (:query-params request)
                limit (h/parse-page-limit params 100)
                offset (h/parse-page-offset params)
                search (h/get-param params :search)
                order-by (h/parse-order-by params)
                order-dir (h/parse-order-dir params)
                extra-filters (when tenant-id
                                [[:or
                                  [:in :id {:select-distinct [:sa/supplier_id]
                                            :from [[:receipts :r]]
                                            :join [[:supplier_aliases :sa] [:= :sa/id :r/supplier_alias_id]]
                                            :where [:and [:= :r/tenant_id tenant-id]
                                                    [:is-not :r/supplier_alias_id nil]]}]
                                  [:in :id {:select-distinct [:supplier_id]
                                            :from [:expenses]
                                            :where [:and [:= :tenant_id tenant-id]
                                                    [:is-not :supplier_id nil]]}]]])
                opts (cond-> {:limit limit
                              :offset offset}
                       (some? search) (assoc :search search)
                       order-by (assoc :order-by order-by)
                       order-dir (assoc :order-dir order-dir)
                       extra-filters (assoc :extra-filters extra-filters))
                suppliers-svc (requiring-resolve 'app.domain.backend.expenses.services.suppliers/list-suppliers)
                count-suppliers (requiring-resolve 'app.domain.backend.expenses.services.suppliers/count-suppliers)
                suppliers (vec (suppliers-svc db opts))
                total (long (or (count-suppliers db (cond-> {:search search}
                                                      extra-filters (assoc :extra-filters extra-filters)))
                              0))]
            (h/json-response {:data suppliers
                              :total total
                              :limit limit
                              :offset offset}))
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
  "Handler factory for listing payers available to users (tenant-scoped)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles "Role assignment required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)]
          (try
            (let [params (:query-params request)
                  limit (h/parse-page-limit params 100)
                  offset (h/parse-page-offset params)
                  search (h/get-param params :search)
                  order-by (h/parse-order-by params)
                  order-dir (h/parse-order-dir params)
                  opts (cond-> {:limit limit
                                :offset offset}
                         (some? search) (assoc :search search)
                         order-by (assoc :order-by order-by)
                         order-dir (assoc :order-dir order-dir)
                         tenant-id (assoc :tenant-id tenant-id))
                  payers-svc (resolve-service-op-fn
                               'app.domain.backend.expenses.services.payers
                               :list
                               'list-payers)
                  count-payers (resolve-service-op-fn
                                 'app.domain.backend.expenses.services.payers
                                 :count)
                  payers (vec (payers-svc db opts))
                  total (long (or (count-payers db {:search search :tenant-id tenant-id}) 0))]
              (h/json-response {:data payers
                                :total total
                                :limit limit
                                :offset offset}))
            (catch Exception e
              (log/error e "Error listing payers")
              (h/json-response {:error "Failed to list payers"} 500)))))
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

(defn batch-delete-suppliers-handler
  "Handler factory for deleting multiple suppliers (shared catalog).

  Allowed roles: member/admin.

  Expects JSON body like:
  {:ids [<uuid> ...]}

  Returns:
  {:data {:deleted-count n :deleted-ids [...] :errors [...]}}"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-write-roles "Only members and admins can modify suppliers")]
        forbidden
        (try
          (let [body (h/read-body-params request)
                raw-ids (or (:ids body)
                          (:supplier_ids body)
                          (:supplier-ids body)
                          (:supplierIds body)
                          [])
                ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No supplier ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more supplier ids are invalid"} 400)

              :else
              (let [delete-supplier! (requiring-resolve 'app.domain.backend.expenses.services.suppliers/delete-supplier!)
                    deleted-ids (atom [])
                    errors (atom [])]
                (doseq [supplier-id ids]
                  (try
                    (if (boolean (delete-supplier! db supplier-id))
                      (swap! deleted-ids conj (str supplier-id))
                      (swap! errors conj {:id (str supplier-id)
                                          :error "not found"}))
                    (catch org.postgresql.util.PSQLException e
                      (let [sql-state (.getSQLState e)]
                        (swap! errors conj {:id (str supplier-id)
                                            :error (if (= "23503" sql-state)
                                                     "foreign key constraint"
                                                     "database error")
                                            :sql-state sql-state})))
                    (catch Exception e
                      (swap! errors conj {:id (str supplier-id)
                                          :error (.getMessage e)}))))
                (h/json-response {:data {:deleted-count (count @deleted-ids)
                                         :deleted-ids (vec @deleted-ids)
                                         :errors (vec @errors)}}))))
          (catch Exception e
            (log/error e "Error batch deleting suppliers")
            (h/json-response {:error "Failed to delete suppliers"} 500))))
      (h/unauthorized-response))))

(defn create-payer-handler
  "Handler factory for creating a payer (tenant-scoped).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-write-roles "Only members and admins can modify payers")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)]
          (try
            (let [body (h/read-json-body request)
                  ;; Require FK; fall back to default payer type when not provided
                  payer-type-id-raw (or (:payer_type_id body) (default-payer-type-id db tenant-id))
                  payer-type-id (cond
                                  (instance? java.util.UUID payer-type-id-raw) payer-type-id-raw
                                  :else (h/try-parse-uuid payer-type-id-raw))
                  _ (when (nil? payer-type-id)
                      (throw (ex-info "payer_type_id is required" {:status 400})))
                  payer-data (-> (select-keys body [:label :is_default])
                               (assoc :payer_type_id payer-type-id)
                               (cond-> tenant-id (assoc :tenant_id tenant-id))
                               (cond-> (contains? body :is_default)
                                 (update :is_default boolean)))
                  create-payer! (requiring-resolve 'app.domain.backend.expenses.services.payers/create-payer!)
                  payer (create-payer! db payer-data {:tenant-id tenant-id})]
              (h/json-response {:data payer} 201))
            (catch clojure.lang.ExceptionInfo e
              (log/warn "Validation error creating payer" {:error (ex-message e) :data (ex-data e)})
              (h/json-response {:error (ex-message e)} 400))
            (catch Exception e
              (log/error e "Error creating payer")
              (h/json-response {:error "Failed to create payer"} 500)))))
      (h/unauthorized-response))))

(defn update-payer-handler
  "Handler factory for updating a payer (tenant-scoped).

  Allowed roles: member/admin."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-write-roles "Only members and admins can modify payers")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              payer-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
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
                    payer (update-payer! db payer-id updates {:tenant-id tenant-id})]
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

(defn batch-delete-payers-handler
  "Handler factory for deleting multiple payers (tenant-scoped).

  Allowed roles: member/admin.

  Expects JSON body like:
  {:ids [<uuid> ...]}

  Returns:
  {:data {:deleted-count n :deleted-ids [...] :errors [...]}}"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-write-roles "Only members and admins can modify payers")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)]
          (try
            (let [body (h/read-body-params request)
                  raw-ids (or (:ids body)
                            (:payer_ids body)
                            (:payer-ids body)
                            (:payerIds body)
                            [])
                  ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
              (cond
                (empty? raw-ids)
                (h/json-response {:error "No payer ids provided"} 400)

                (empty? ids)
                (h/json-response {:error "One or more payer ids are invalid"} 400)

                :else
                (let [delete-payer! (resolve-service-op-fn
                                      'app.domain.backend.expenses.services.payers
                                      :delete!
                                      'delete-payer!)
                      deleted-ids (atom [])
                      errors (atom [])]
                  (doseq [payer-id ids]
                    (try
                      (if (boolean (delete-payer! db payer-id {:tenant-id tenant-id}))
                        (swap! deleted-ids conj (str payer-id))
                        (swap! errors conj {:id (str payer-id)
                                            :error "not found"}))
                      (catch org.postgresql.util.PSQLException e
                        (let [sql-state (.getSQLState e)]
                          (swap! errors conj {:id (str payer-id)
                                              :error (if (= "23503" sql-state)
                                                       "foreign key constraint"
                                                       "database error")
                                              :sql-state sql-state})))
                      (catch Exception e
                        (swap! errors conj {:id (str payer-id)
                                            :error (.getMessage e)}))))
                  (h/json-response {:data {:deleted-count (count @deleted-ids)
                                           :deleted-ids (vec @deleted-ids)
                                           :errors (vec @errors)}}))))
            (catch Exception e
              (log/error e "Error batch deleting payers")
              (h/json-response {:error "Failed to delete payers"} 500)))))
      (h/unauthorized-response))))

(def ^:private payer-type-manage-roles
  "User roles allowed to manage payer types."
  #{"admin" "owner"})

(defn list-payer-types-handler
  "Handler factory for listing payer types (tenant-scoped).

  Allowed roles: viewer/member/admin/owner."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles "Role assignment required")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)]
          (try
            (let [params (:query-params request)
                  limit (h/parse-page-limit params 100)
                  offset (h/parse-page-offset params)
                  search (h/get-param params :search)
                  order-by (h/parse-order-by params)
                  order-dir (h/parse-order-dir params)
                  opts (cond-> {:limit limit
                                :offset offset}
                         (some? search) (assoc :search search)
                         order-by (assoc :order-by order-by)
                         order-dir (assoc :order-dir order-dir)
                         tenant-id (assoc :tenant-id tenant-id))
                  list-payer-types (resolve-service-op-fn
                                     'app.domain.backend.expenses.services.payer-types
                                     :list)
                  count-payer-types (resolve-service-op-fn
                                      'app.domain.backend.expenses.services.payer-types
                                      :count)
                  payer-types (vec (list-payer-types db opts))
                  total (long (or (count-payer-types db {:search search :tenant-id tenant-id}) 0))]
              (h/json-response {:data payer-types
                                :total total
                                :limit limit
                                :offset offset}))
            (catch Exception e
              (log/error e "Error listing payer types")
              (h/json-response {:error "Failed to list payer types"} 500)))))
      (h/unauthorized-response))))

(defn create-payer-type-handler
  "Handler factory for creating a payer type (tenant-scoped).

  Allowed roles: admin/owner."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request payer-type-manage-roles "Only admins and owners can manage payer types")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)]
          (try
            (let [body (h/read-json-body request)
                  payer-type-data (-> (select-keys body [:label :is_default])
                                    (cond-> (contains? body :is_default)
                                      (update :is_default boolean))
                                    (cond-> tenant-id (assoc :tenant_id tenant-id)))
                  create-payer-type! (requiring-resolve 'app.domain.backend.expenses.services.payer-types/create-payer-type!)
                  payer-type (create-payer-type! db payer-type-data {:tenant-id tenant-id})]
              (h/json-response {:data payer-type} 201))
            (catch clojure.lang.ExceptionInfo e
              (log/warn "Validation error creating payer type" {:error (ex-message e) :data (ex-data e)})
              (h/json-response {:error (ex-message e)} 400))
            (catch Exception e
              (log/error e "Error creating payer type")
              (h/json-response {:error "Failed to create payer type"} 500)))))
      (h/unauthorized-response))))

(defn update-payer-type-handler
  "Handler factory for updating a payer type (tenant-scoped).

  Allowed roles: admin/owner."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request payer-type-manage-roles "Only admins and owners can manage payer types")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)
              payer-type-id (or (h/try-parse-uuid (get-in request [:path-params :id]))
                              (h/try-parse-uuid (get-in request [:parameters :path :id])))]
          (if payer-type-id
            (try
              (let [body (h/read-json-body request)
                    updates (-> (select-keys body [:label :is_default])
                              (cond-> (contains? body :is_default)
                                (update :is_default boolean)))
                    update-payer-type! (requiring-resolve 'app.domain.backend.expenses.services.payer-types/update-payer-type!)
                    payer-type (update-payer-type! db payer-type-id updates {:tenant-id tenant-id})]
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

(defn batch-delete-payer-types-handler
  "Handler factory for deleting multiple payer types (tenant-scoped).

  Allowed roles: admin/owner.

  Expects JSON body like:
  {:ids [<uuid> ...]}

  Returns:
  {:data {:deleted-count n :deleted-ids [...] :errors [...]}}"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request payer-type-manage-roles "Only admins and owners can manage payer types")]
        forbidden
        (let [tenant-id (h/get-tenant-id request)]
          (try
            (let [body (h/read-body-params request)
                  raw-ids (or (:ids body)
                            (:payer_type_ids body)
                            (:payer-type-ids body)
                            (:payerTypeIds body)
                            [])
                  ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
              (cond
                (empty? raw-ids)
                (h/json-response {:error "No payer type ids provided"} 400)

                (empty? ids)
                (h/json-response {:error "One or more payer type ids are invalid"} 400)

                :else
                (let [delete-payer-type! (resolve-service-op-fn
                                           'app.domain.backend.expenses.services.payer-types
                                           :delete!)
                      deleted-ids (atom [])
                      errors (atom [])]
                  (doseq [payer-type-id ids]
                    (try
                      (if (boolean (delete-payer-type! db payer-type-id {:tenant-id tenant-id}))
                        (swap! deleted-ids conj (str payer-type-id))
                        (swap! errors conj {:id (str payer-type-id)
                                            :error "not found"}))
                      (catch org.postgresql.util.PSQLException e
                        (let [sql-state (.getSQLState e)]
                          (swap! errors conj {:id (str payer-type-id)
                                              :error (if (= "23503" sql-state)
                                                       "foreign key constraint"
                                                       "database error")
                                              :sql-state sql-state})))
                      (catch Exception e
                        (swap! errors conj {:id (str payer-type-id)
                                            :error (.getMessage e)}))))
                  (h/json-response {:data {:deleted-count (count @deleted-ids)
                                           :deleted-ids (vec @deleted-ids)
                                           :errors (vec @errors)}}))))
            (catch Exception e
              (log/error e "Error batch deleting payer types")
              (h/json-response {:error "Failed to delete payer types"} 500)))))
      (h/unauthorized-response))))


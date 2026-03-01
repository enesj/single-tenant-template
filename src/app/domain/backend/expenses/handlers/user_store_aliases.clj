(ns app.domain.backend.expenses.handlers.user-store-aliases
  "User-facing store aliases endpoints.

  Mounted under /api/v1/expenses/store-aliases and intended for power users.

  IMPORTANT:
  - Role-gated to admin/owner.
  - Normalizes responses to {:data ...}."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.store-aliases :as store-aliases]
    [app.shared.adapters.database :as shared-db]
    [taoensso.timbre :as log]))

(def ^:private to-app shared-db/to-app)

(def ^:private allowed-roles
  #{"admin" "owner"})

(defn- ensure-admin-or-owner
  [request]
  (h/ensure-role request allowed-roles "Only admins and owners can access store aliases."))

(def ^:private max-page-limit
  500)

(defn- parse-page-limit
  [params default-limit]
  (-> (or (some-> (h/get-param params :limit) parse-long)
        default-limit)
    long
    (max 1)
    (min max-page-limit)))

(defn- parse-page-offset
  [params]
  (max 0 (long (or (some-> (h/get-param params :offset) parse-long) 0))))

(defn- body-has-any-key?
  [body ks]
  (boolean (some #(contains? body %) ks)))

(defn- body-get-first
  [body ks]
  (some #(get body %) ks))

(defn list-store-aliases-handler
  "List store aliases (admin/owner only)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles
                           "Role assignment required")]
        forbidden
        (try
          (let [qp (:query-params request)
                limit (parse-page-limit qp 200)
                offset (parse-page-offset qp)
                search (h/get-param qp :search)
                order-by (h/parse-order-by qp)
                order-dir (h/parse-order-dir qp)
                opts (cond-> {:limit limit
                              :offset offset
                              :search search}
                       order-by (assoc :order-by order-by)
                       order-dir (assoc :order-dir order-dir))
                rows (to-app ((:list store-aliases/service) db opts))
                rows (cond-> rows (sequential? rows) vec)
                total (long (or ((:count store-aliases/service) db {:search search}) 0))]
            (h/json-response {:data rows
                              :total total
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Failed to list store aliases" {:query-params (:query-params request)})
            (h/json-response {:error "Failed to list store aliases"} 500))))
      (h/unauthorized-response))))

(defn update-store-alias-handler
  "Update a store alias (admin/owner only)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (let [alias-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                           (get-in request [:parameters :path :id])))]
          (if-not alias-id
            (h/json-response {:error "Invalid store alias id"} 400)
            (try
              (let [body (h/read-body-params request)
                    raw-label-keys [:raw_label :raw-label :rawLabel]
                    raw-label-normalized-keys [:raw_label_normalized :raw-label-normalized :rawLabelNormalized]
                    store-id-keys [:store_id :store-id :storeId]
                    confidence-keys [:confidence]
                    raw-label-provided? (body-has-any-key? body raw-label-keys)
                    raw-label-normalized-provided? (body-has-any-key? body raw-label-normalized-keys)
                    store-id-provided? (body-has-any-key? body store-id-keys)
                    confidence-provided? (body-has-any-key? body confidence-keys)
                    raw-store-id (when store-id-provided?
                                   (body-get-first body store-id-keys))
                    store-id (when store-id-provided?
                               (h/try-parse-uuid raw-store-id))
                    updates (cond-> {}
                              raw-label-provided? (assoc :raw_label (body-get-first body raw-label-keys))
                              raw-label-normalized-provided? (assoc :raw_label_normalized (body-get-first body raw-label-normalized-keys))
                              store-id-provided? (assoc :store_id store-id)
                              confidence-provided? (assoc :confidence (body-get-first body confidence-keys)))]
                (cond
                  (empty? updates)
                  (h/json-response {:error "No store alias fields provided"} 400)

                  (and store-id-provided? (some? raw-store-id) (nil? store-id))
                  (h/json-response {:error "Invalid store_id"} 400)

                  :else
                  (if-let [updated (some-> ((:update! store-aliases/service) db alias-id updates)
                                     to-app)]
                    (h/json-response {:data updated})
                    (h/not-found-response "Store alias not found"))))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating store alias" {:error (ex-message e)
                                                                   :data (ex-data e)
                                                                   :alias-id (str alias-id)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Failed to update store alias" {:alias-id (str alias-id)})
                (h/json-response {:error "Failed to update store alias"} 500))))))
      (h/unauthorized-response))))

(defn batch-delete-store-aliases-handler
  "Batch delete store aliases (admin/owner only)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                raw-ids (or (:ids body)
                          (:store_alias_ids body)
                          (:store-alias-ids body)
                          (:storeAliasIds body)
                          [])
                ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No store alias ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more store alias ids are invalid"} 400)

              :else
              (let [delete! (:delete! store-aliases/service)
                    deleted-ids (atom [])
                    errors (atom [])]
                (doseq [id ids]
                  (try
                    (if (boolean (delete! db id))
                      (swap! deleted-ids conj (str id))
                      (swap! errors conj {:id (str id)
                                          :error "not found"}))
                    (catch org.postgresql.util.PSQLException e
                      (let [sql-state (.getSQLState e)]
                        (swap! errors conj {:id (str id)
                                            :error (if (= "23503" sql-state)
                                                     "foreign key constraint"
                                                     "database error")
                                            :sql-state sql-state})))
                    (catch Exception e
                      (swap! errors conj {:id (str id)
                                          :error (.getMessage e)}))))
                (h/json-response {:data {:deleted-count (count @deleted-ids)
                                         :deleted-ids (vec @deleted-ids)
                                         :errors (vec @errors)}}))))
          (catch Exception e
            (log/error e "Failed to batch delete store aliases" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete store aliases"} 500))))
      (h/unauthorized-response))))

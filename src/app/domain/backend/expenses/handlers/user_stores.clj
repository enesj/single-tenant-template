(ns app.domain.backend.expenses.handlers.user-stores
  "User-facing stores endpoints.

  These endpoints are mounted under /api/v1/expenses/stores and are intended
  for power users inside the main app.

  IMPORTANT:
  - These routes are role-gated to admin/owner.
  - Responses are normalized to {:data ...} to keep frontend handlers consistent."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.stores.service :as stores-service]
    [app.domain.backend.expenses.services.stores.upsert :as stores-upsert]
    [app.shared.adapters.database :as shared-db]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

;; -----------------------------------------------------------------------------
;; Helpers
;; -----------------------------------------------------------------------------

(def ^:private to-app shared-db/to-app)

(def ^:private allowed-roles
  #{"admin" "owner"})

(defn- ensure-admin-or-owner
  [request]
  (h/ensure-role request allowed-roles "Only admins and owners can access stores."))

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

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn list-stores-handler
  "List stores (admin/owner only).

  Query params:
  - limit (default 200)
  - offset (default 0)
  - search (optional)
  - order-by (optional)
  - order-dir (optional)"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (ensure-admin-or-owner request)]
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
                rows (to-app ((:list stores-service/entity-service) db opts))
                rows (cond-> rows (sequential? rows) vec)
                total (long (or ((:count stores-service/entity-service) db {:search search}) 0))]
            (h/json-response {:data rows
                              :total total
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Failed to list stores" {:query-params (:query-params request)})
            (h/json-response {:error "Failed to list stores"} 500))))
      (h/unauthorized-response))))

(defn create-store-handler
  "Create a store (admin/owner only).

  Expects JSON body like:
  {:supplier_id \"<uuid>\"
   :display_name \"Mega Market\"
   :address \"...\"     ;; optional
   :place_id \"...\"}   ;; optional"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                supplier-id-raw (h/get-param body :supplier_id)
                supplier-id (h/try-parse-uuid supplier-id-raw)
                display-name (some-> (h/get-param body :display_name) str str/trim)
                address (some-> (h/get-param body :address) str str/trim)
                place-id (some-> (h/get-param body :place_id) str str/trim)
                address* (when-not (str/blank? address) address)
                place-id* (when-not (str/blank? place-id) place-id)]
            (cond
              (nil? supplier-id-raw)
              (h/json-response {:error "supplier_id is required"} 400)

              (nil? supplier-id)
              (h/json-response {:error "Invalid supplier id"} 400)

              (str/blank? display-name)
              (h/json-response {:error "display_name is required"} 400)

              :else
              (let [store (-> (stores-upsert/find-or-create-store! db {:supplier_id supplier-id
                                                                       :display_name display-name
                                                                       :address address*
                                                                       :place_id place-id*})
                            to-app)]
                (h/json-response {:data store} 201))))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating store" {:error (ex-message e)
                                                         :data (ex-data e)})
            (h/json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Failed to create store" {:message (.getMessage e)})
            (h/json-response {:error "Failed to create store"} 500))))
      (h/unauthorized-response))))

(defn update-store-handler
  "Update a store (admin/owner only)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (let [store-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                           (get-in request [:parameters :path :id])))]
          (if-not store-id
            (h/json-response {:error "Invalid store id"} 400)
            (try
              (let [body (h/read-body-params request)
                    display-name-keys [:display_name :display-name :displayName]
                    address-keys [:address]
                    place-id-keys [:place_id :place-id :placeId]
                    display-name-provided? (body-has-any-key? body display-name-keys)
                    address-provided? (body-has-any-key? body address-keys)
                    place-id-provided? (body-has-any-key? body place-id-keys)
                    updates (cond-> {}
                              display-name-provided? (assoc :display_name (body-get-first body display-name-keys))
                              address-provided? (assoc :address (body-get-first body address-keys))
                              place-id-provided? (assoc :place_id (body-get-first body place-id-keys)))]
                (if (empty? updates)
                  (h/json-response {:error "No store fields provided"} 400)
                  (if-let [updated (some-> ((:update! stores-service/entity-service) db store-id updates)
                                     to-app)]
                    (h/json-response {:data updated})
                    (h/not-found-response "Store not found"))))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating store" {:error (ex-message e)
                                                             :data (ex-data e)
                                                             :store-id (str store-id)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Failed to update store" {:store-id (str store-id)})
                (h/json-response {:error "Failed to update store"} 500))))))
      (h/unauthorized-response))))

(defn batch-delete-stores-handler
  "Batch delete stores (admin/owner only).

  Expects JSON body like:
  {:ids [<uuid> ...]}

  Returns:
  {:data {:deleted-count n :deleted-ids [...] :errors [...]}}"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                raw-ids (or (:ids body)
                          (:store_ids body)
                          (:store-ids body)
                          (:storeIds body)
                          [])
                ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No store ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more store ids are invalid"} 400)

              :else
              (let [delete! (:delete! stores-service/entity-service)
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
            (log/error e "Failed to batch delete stores" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete stores"} 500))))
      (h/unauthorized-response))))

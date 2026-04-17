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
    [clojure.string :as str]
    [taoensso.timbre :as log]))

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn list-stores-handler
  "List stores (admin/owner only).
   When a tenant-id is present, scopes to stores used in that tenant's expenses.

   Query params:
   - limit (default 200)
   - offset (default 0)
   - search (optional)
   - order-by (optional)
   - order-dir (optional)"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles
                           "Role assignment required")]
        forbidden
        (try
          (let [tenant-id (h/get-tenant-id request)
                qp (:query-params request)
                limit (h/parse-page-limit qp 200)
                offset (h/parse-page-offset qp)
                search (or (h/get-param qp :search)
                         (h/get-param qp :display-name))
                sort-opts (h/parse-sort-params qp)
                supplier-id (some-> (h/get-param qp :supplier_id) h/try-parse-uuid)
                supplier-display-name (h/get-param qp :supplier-display-name)
                normalized-key (h/get-param qp :normalized-key)
                address (h/get-param qp :address)
                city-name (h/get-param qp :city-name)
                created-at-from (h/parse-instant-param (h/get-param qp :created-at-from))
                created-at-to (h/parse-instant-param (h/get-param qp :created-at-to))
                ;; When supplier_id is given (expand drill-down), skip tenant scoping —
                ;; the admin/owner is explicitly viewing all stores for that supplier.
                extra-filters (cond-> []
                                (and tenant-id (nil? supplier-id))
                                (conj [:or
                                       [:in :st/id {:select-distinct [:sta/store_id]
                                                    :from [[:receipts :r]]
                                                    :join [[:store_aliases :sta] [:= :sta/id :r/store_alias_id]]
                                                    :where [:and [:= :r/tenant_id tenant-id]
                                                            [:is-not :r/store_alias_id nil]]}]
                                       [:in :st/id {:select-distinct [:store_id]
                                                    :from [:expenses]
                                                    :where [:and [:= :tenant_id tenant-id]
                                                            [:is-not :store_id nil]]}]])
                                supplier-id
                                (conj [:= :st/supplier_id supplier-id])
                                created-at-from
                                (conj [:>= :st/created_at created-at-from])
                                created-at-to
                                (conj [:<= :st/created_at created-at-to]))
                extra-filters (when (seq extra-filters) extra-filters)
                opts (cond-> {:limit limit
                              :offset offset
                              :search search}
                    (seq sort-opts) (merge sort-opts)
                       extra-filters (assoc :extra-filters extra-filters)
                       (some? supplier-display-name) (assoc :supplier-display-name supplier-display-name)
                       (some? normalized-key) (assoc :normalized-key normalized-key)
                       (some? address) (assoc :address address)
                       (some? city-name) (assoc :city-name city-name))
                rows (h/to-app ((:list stores-service/entity-service) db opts))
                rows (cond-> rows (sequential? rows) vec)
                total (long (or ((:count stores-service/entity-service)
                                  db (select-keys opts [:search :extra-filters
                                                        :supplier-display-name :normalized-key
                                                        :address :city-name]))
                              0))]
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
      (if-let [forbidden (h/ensure-admin-or-owner request)]
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
                            h/to-app)]
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
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (let [store-id (h/parse-path-id request)]
          (if-not store-id
            (h/json-response {:error "Invalid store id"} 400)
            (try
              (let [body (h/read-body-params request)
                    display-name-keys [:display_name :display-name :displayName]
                    address-keys [:address]
                    place-id-keys [:place_id :place-id :placeId]
                    display-name-provided? (h/body-has-any-key? body display-name-keys)
                    address-provided? (h/body-has-any-key? body address-keys)
                    place-id-provided? (h/body-has-any-key? body place-id-keys)
                    updates (cond-> {}
                              display-name-provided? (assoc :display_name (h/body-get-first body display-name-keys))
                              address-provided? (assoc :address (h/body-get-first body address-keys))
                              place-id-provided? (assoc :place_id (h/body-get-first body place-id-keys)))]
                (if (empty? updates)
                  (h/json-response {:error "No store fields provided"} 400)
                  (if-let [updated (some-> ((:update! stores-service/entity-service) db store-id updates)
                                     h/to-app)]
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
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                [raw-ids ids] (h/parse-batch-ids body [:ids :store_ids :store-ids :storeIds])]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No store ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more store ids are invalid"} 400)

              :else
              (let [delete! (:delete! stores-service/entity-service)]
                (h/json-response {:data (h/batch-delete-entities #(delete! db %) ids)}))))
          (catch Exception e
            (log/error e "Failed to batch delete stores" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete stores"} 500))))
      (h/unauthorized-response))))

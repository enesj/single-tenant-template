(ns app.domain.backend.expenses.handlers.user-store-aliases
  "User-facing store aliases endpoints.

  Mounted under /api/v1/expenses/store-aliases and intended for power users.

  IMPORTANT:
  - Role-gated to admin/owner.
  - Normalizes responses to {:data ...}."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.store-aliases :as store-aliases]
    [taoensso.timbre :as log]))

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
                limit (h/parse-page-limit qp 200)
                offset (h/parse-page-offset qp)
                search (h/get-param qp :search)
                sort-opts (h/parse-sort-params qp)
                supplier-display-name (h/get-param qp :supplier-display-name)
                store-display-name (h/get-param qp :store-display-name)
                store-address (h/get-param qp :store-address)
                raw-label (h/get-param qp :raw-label)
                raw-label-normalized (h/get-param qp :raw-label-normalized)
                confidence-min (some-> (h/get-param qp :confidence-min) parse-double)
                confidence-max (some-> (h/get-param qp :confidence-max) parse-double)
                created-at-from (h/parse-instant-param (h/get-param qp :created-at-from))
                created-at-to (h/parse-instant-param (h/get-param qp :created-at-to))
                extra-filters (cond-> []
                                created-at-from (conj [:>= :sta/created_at created-at-from])
                                created-at-to (conj [:<= :sta/created_at created-at-to]))
                opts (cond-> {:limit limit
                              :offset offset
                              :search search}
                    (seq sort-opts) (merge sort-opts)
                       (some? supplier-display-name) (assoc :supplier-display-name supplier-display-name)
                       (some? store-display-name) (assoc :store-display-name store-display-name)
                       (some? store-address) (assoc :store-address store-address)
                       (some? raw-label) (assoc :raw-label raw-label)
                       (some? raw-label-normalized) (assoc :raw-label-normalized raw-label-normalized)
                       (some? confidence-min) (assoc :confidence-min confidence-min)
                       (some? confidence-max) (assoc :confidence-max confidence-max)
                       (seq extra-filters) (assoc :extra-filters extra-filters))
                rows (h/to-app ((:list store-aliases/service) db opts))
                rows (cond-> rows (sequential? rows) vec)
                count-opts (cond-> (select-keys opts [:search :supplier-display-name :store-display-name
                                                      :store-address :raw-label :raw-label-normalized
                                                      :confidence-min :confidence-max])
                             (seq extra-filters) (assoc :extra-filters extra-filters))
                total (long (or ((:count store-aliases/service) db count-opts) 0))]
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
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (let [alias-id (h/parse-path-id request)]
          (if-not alias-id
            (h/json-response {:error "Invalid store alias id"} 400)
            (try
              (let [body (h/read-body-params request)
                    raw-label-keys [:raw_label :raw-label :rawLabel]
                    raw-label-normalized-keys [:raw_label_normalized :raw-label-normalized :rawLabelNormalized]
                    store-id-keys [:store_id :store-id :storeId]
                    confidence-keys [:confidence]
                    raw-label-provided? (h/body-has-any-key? body raw-label-keys)
                    raw-label-normalized-provided? (h/body-has-any-key? body raw-label-normalized-keys)
                    store-id-provided? (h/body-has-any-key? body store-id-keys)
                    confidence-provided? (h/body-has-any-key? body confidence-keys)
                    raw-store-id (when store-id-provided?
                                   (h/body-get-first body store-id-keys))
                    store-id (when store-id-provided?
                               (h/try-parse-uuid raw-store-id))
                    updates (cond-> {}
                              raw-label-provided? (assoc :raw_label (h/body-get-first body raw-label-keys))
                              raw-label-normalized-provided? (assoc :raw_label_normalized (h/body-get-first body raw-label-normalized-keys))
                              store-id-provided? (assoc :store_id store-id)
                              confidence-provided? (assoc :confidence (h/body-get-first body confidence-keys)))]
                (cond
                  (empty? updates)
                  (h/json-response {:error "No store alias fields provided"} 400)

                  (and store-id-provided? (some? raw-store-id) (nil? store-id))
                  (h/json-response {:error "Invalid store_id"} 400)

                  :else
                  (if-let [updated (some-> ((:update! store-aliases/service) db alias-id updates)
                                     h/to-app)]
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
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                [raw-ids ids] (h/parse-batch-ids body [:ids :store_alias_ids :store-alias-ids :storeAliasIds])]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No store alias ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more store alias ids are invalid"} 400)

              :else
              (let [delete! (:delete! store-aliases/service)]
                (h/json-response {:data (h/batch-delete-entities #(delete! db %) ids)}))))
          (catch Exception e
            (log/error e "Failed to batch delete store aliases" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete store aliases"} 500))))
      (h/unauthorized-response))))

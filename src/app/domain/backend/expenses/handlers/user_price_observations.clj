(ns app.domain.backend.expenses.handlers.user-price-observations
  "User-facing (non-admin) price observations mutation endpoints.

  These endpoints are mounted under /api/v1/expenses/price-observations/:id.

  NOTE: Listing is handled by `user-expenses.supplier-detail` which is shared by
  supplier detail views and the power-user price observations page."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [taoensso.timbre :as log]))

(def ^:private allowed-roles
  #{"admin" "owner"})

(defn- ensure-admin-or-owner
  [request]
  (h/ensure-role request allowed-roles "Only admins and owners can modify price observations."))

(defn- resolve-service-op
  [op]
  (when-let [service-var (requiring-resolve 'app.domain.backend.expenses.services.price-observations/service)]
    (let [service-map (var-get service-var)]
      (when (map? service-map)
        (get service-map op)))))

(def ^:private allowed-update-keys
  #{:article_id
    :supplier_id
    :expense_item_id
    :observed_at
    :qty
    :unit_price
    :line_total
    :currency})

(defn update-price-observation-handler
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (let [obs-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                         (get-in request [:parameters :path :id])))]
          (if-not obs-id
            (h/json-response {:error "Invalid price observation id"} 400)
            (try
              (let [update! (resolve-service-op :update!)
                    body (h/read-body-params request)
                    updates (select-keys body allowed-update-keys)]
                (cond
                  (nil? update!)
                  (do
                    (log/error "Price observation service missing :update! op")
                    (h/json-response {:error "Update not available"} 500))

                  (empty? updates)
                  (h/json-response {:error "No updates provided"} 400)

                  :else
                  (let [updated (update! db obs-id updates)]
                    (if updated
                      (h/json-response {:data updated})
                      (h/not-found-response "Price observation not found")))))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating price observation" {:error (ex-message e)
                                                                         :data (ex-data e)
                                                                         :price-observation-id (str obs-id)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Failed to update price observation" {:message (.getMessage e)
                                                                   :price-observation-id (str obs-id)})
                (h/json-response {:error "Failed to update price observation"} 500))))))
      (h/unauthorized-response))))

(defn delete-price-observation-handler
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (let [obs-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                         (get-in request [:parameters :path :id])))]
          (if-not obs-id
            (h/json-response {:error "Invalid price observation id"} 400)
            (try
              (let [delete! (resolve-service-op :delete!)
                    deleted? (when delete! (boolean (delete! db obs-id)))]
                (cond
                  (nil? delete!)
                  (do
                    (log/error "Price observation service missing :delete! op")
                    (h/json-response {:error "Delete not available"} 500))

                  deleted?
                  (h/json-response {:data {:deleted true}})

                  :else
                  (h/not-found-response "Price observation not found")))
              (catch org.postgresql.util.PSQLException e
                (let [sql-state (.getSQLState e)]
                  (if (= "23503" sql-state)
                    (do
                      (log/warn "Cannot delete price observation - has related records" {:price-observation-id (str obs-id)})
                      (h/json-response {:error "Cannot delete price observation: it has related records."} 409))
                    (do
                      (log/error e "Database error deleting price observation" {:price-observation-id (str obs-id)
                                                                                :sql-state sql-state})
                      (h/json-response {:error "Failed to delete price observation"} 500)))))
              (catch Exception e
                (log/error e "Failed to delete price observation" {:message (.getMessage e)
                                                                   :price-observation-id (str obs-id)})
                (h/json-response {:error "Failed to delete price observation"} 500))))))
      (h/unauthorized-response))))

(defn batch-delete-price-observations-handler
  "Batch delete price observations (admin/owner only).

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
                          (:price_observation_ids body)
                          (:price-observation-ids body)
                          (:priceObservationIds body)
                          [])
                ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No price observation ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more price observation ids are invalid"} 400)

              :else
              (let [delete! (resolve-service-op :delete!)
                    deleted-ids (atom [])
                    errors (atom [])]
                (when (nil? delete!)
                  (log/error "Price observation service missing :delete! op")
                  (throw (ex-info "Delete not available" {:status 500})))
                (doseq [obs-id ids]
                  (try
                    (if (boolean (delete! db obs-id))
                      (swap! deleted-ids conj (str obs-id))
                      (swap! errors conj {:id (str obs-id)
                                          :error "not found"}))
                    (catch org.postgresql.util.PSQLException e
                      (let [sql-state (.getSQLState e)]
                        (swap! errors conj {:id (str obs-id)
                                            :error (if (= "23503" sql-state)
                                                     "foreign key constraint"
                                                     "database error")
                                            :sql-state sql-state})))
                    (catch Exception e
                      (swap! errors conj {:id (str obs-id)
                                          :error (.getMessage e)}))))
                (h/json-response {:data {:deleted-count (count @deleted-ids)
                                         :deleted-ids (vec @deleted-ids)
                                         :errors (vec @errors)}}))))
          (catch clojure.lang.ExceptionInfo e
            (let [{:keys [status]} (ex-data e)]
              (h/json-response {:error (ex-message e)} (or status 500))))
          (catch Exception e
            (log/error e "Failed to batch delete price observations" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete price observations"} 500))))
      (h/unauthorized-response))))

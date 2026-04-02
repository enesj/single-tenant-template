(ns app.domain.backend.expenses.handlers.user-manufacturers
  "User-facing manufacturers endpoints.

  These endpoints are mounted under /api/v1/expenses/manufacturers and are intended
  for power users inside the main app.

  IMPORTANT:
  - These routes are role-gated to admin/owner.
  - Responses are normalized to {:data ...} to keep frontend handlers consistent."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.manufacturers :as manufacturers]
    [taoensso.timbre :as log]))

;; -----------------------------------------------------------------------------
;; Helpers
;; -----------------------------------------------------------------------------

(defn- extract-display-name
  [body]
  (or (:display_name body)
    (:display-name body)
    (:displayName body)))

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn list-manufacturers-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles
                           "Role assignment required")]
        forbidden
        (try
          (let [qp (:query-params request)
                limit (h/parse-page-limit qp 200)
                offset (h/parse-page-offset qp)
                search (or (h/get-param qp :search)
                         (h/get-param qp :display-name))
                order-by (h/parse-order-by qp)
                order-dir (h/parse-order-dir qp)
                normalized-key (h/get-param qp :normalized-key)
                created-at-from (h/parse-instant-param (h/get-param qp :created-at-from))
                created-at-to (h/parse-instant-param (h/get-param qp :created-at-to))
                extra-filters (cond-> []
                                created-at-from (conj [:>= :created_at created-at-from])
                                created-at-to (conj [:<= :created_at created-at-to]))
                opts (cond-> {:limit limit
                              :offset offset
                              :search search}
                       order-by (assoc :order-by order-by)
                       order-dir (assoc :order-dir order-dir)
                       (some? normalized-key) (assoc :normalized-key normalized-key)
                       (seq extra-filters) (assoc :extra-filters extra-filters))
                rows (h/to-app ((:list manufacturers/service) db opts))
                rows (cond-> rows (sequential? rows) vec)
                count-opts (cond-> (select-keys opts [:search :normalized-key])
                             (seq extra-filters) (assoc :extra-filters extra-filters))
                total (long (or ((:count manufacturers/service) db count-opts) 0))]
            (h/json-response {:data rows
                              :total total
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Failed to list manufacturers" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list manufacturers"} 500)))))))

(defn create-manufacturer-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                display-name (extract-display-name body)
                manufacturer (h/to-app ((:create! manufacturers/service) db {:display_name display-name}))]
            (h/json-response {:data manufacturer} 201))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating manufacturer" {:error (ex-message e) :data (ex-data e)})
            (h/json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Failed to create manufacturer" {:message (.getMessage e)})
            (h/json-response {:error "Failed to create manufacturer"} 500)))))))

(defn update-manufacturer-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (let [manufacturer-id (h/parse-path-id request)]
          (if-not manufacturer-id
            (h/json-response {:error "Invalid manufacturer id"} 400)
            (try
              (let [body (h/read-body-params request)
                    display-name-provided? (or (contains? body :display_name)
                                             (contains? body :display-name)
                                             (contains? body :displayName))
                    display-name (extract-display-name body)
                    updates (cond-> {}
                              display-name-provided? (assoc :display_name display-name))
                    updated (some-> ((:update! manufacturers/service) db manufacturer-id updates)
                              h/to-app)]
                (if updated
                  (h/json-response {:data updated})
                  (h/not-found-response "Manufacturer not found")))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating manufacturer" {:error (ex-message e)
                                                                    :data (ex-data e)
                                                                    :manufacturer-id (str manufacturer-id)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Failed to update manufacturer" {:message (.getMessage e)
                                                              :manufacturer-id (str manufacturer-id)})
                (h/json-response {:error "Failed to update manufacturer"} 500)))))))))

(defn delete-manufacturer-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (let [manufacturer-id (h/parse-path-id request)]
          (if-not manufacturer-id
            (h/json-response {:error "Invalid manufacturer id"} 400)
            (try
              (let [deleted? (boolean ((:delete! manufacturers/service) db manufacturer-id))]
                (if deleted?
                  (h/json-response {:data {:deleted true}})
                  (h/not-found-response "Manufacturer not found")))
              (catch Exception e
                (log/error e "Failed to delete manufacturer" {:message (.getMessage e)
                                                              :manufacturer-id (str manufacturer-id)})
                (h/json-response {:error "Failed to delete manufacturer"} 500)))))))))

(defn batch-delete-manufacturers-handler
  "Batch delete manufacturers (admin/owner only).

  Expects JSON body like:
  {:ids [<uuid> ...]}

  Returns:
  {:data {:deleted-count n :deleted-ids [...] :errors [...]}}"
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                [raw-ids ids] (h/parse-batch-ids body [:ids :manufacturer_ids :manufacturer-ids :manufacturerIds])]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No manufacturer ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more manufacturer ids are invalid"} 400)

              :else
              (let [delete! (:delete! manufacturers/service)]
                (h/json-response {:data (h/batch-delete-entities #(delete! db %) ids)}))))
          (catch Exception e
            (log/error e "Failed to batch delete manufacturers" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete manufacturers"} 500)))))))

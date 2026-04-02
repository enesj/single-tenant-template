(ns app.domain.backend.expenses.handlers.user-cities
  "User-facing cities endpoints.

  These endpoints are mounted under /api/v1/expenses/cities and are intended
  for power users inside the main app.

  IMPORTANT:
  - These routes are role-gated to admin/owner.
  - Responses are normalized to {:data ...} to keep frontend handlers consistent."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.cities :as cities]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

;; -----------------------------------------------------------------------------
;; Helpers
;; -----------------------------------------------------------------------------

(defn- extract-name
  [body]
  (or (:name body)
    (:display_name body)
    (:display-name body)
    (:displayName body)))

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn list-cities-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (try
          (let [qp (:query-params request)
                limit (h/parse-page-limit qp 200)
                offset (h/parse-page-offset qp)
                search (or (h/get-param qp :search)
                         (h/get-param qp :name))
                order-by (h/parse-order-by qp)
                order-dir (h/parse-order-dir qp)
                normalized-key (h/get-param qp :normalized-key)
                zip (h/get-param qp :zip)
                country (h/get-param qp :country)
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
                       (some? zip) (assoc :zip zip)
                       (some? country) (assoc :country country)
                       (seq extra-filters) (assoc :extra-filters extra-filters))
                rows (h/to-app ((:list cities/service) db opts))
                rows (cond-> rows (sequential? rows) vec)
                count-opts (cond-> (select-keys opts [:search :normalized-key :zip :country])
                             (seq extra-filters) (assoc :extra-filters extra-filters))
                total (long (or ((:count cities/service) db count-opts) 0))]
            (h/json-response {:data rows
                              :total total
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Failed to list cities" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list cities"} 500)))))))

(defn create-city-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                name (some-> (extract-name body) str str/trim)]
            (if (str/blank? name)
              (h/json-response {:error "name is required"} 400)
              (let [city (h/to-app ((:create! cities/service) db {:name name}))]
                (h/json-response {:data city} 201))))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating city" {:error (ex-message e) :data (ex-data e)})
            (h/json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Failed to create city" {:message (.getMessage e)})
            (h/json-response {:error "Failed to create city"} 500)))))))

(defn update-city-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (let [city-id (h/parse-path-id request)]
          (if-not city-id
            (h/json-response {:error "Invalid city id"} 400)
            (try
              (let [body (h/read-body-params request)
                    name-keys [:name :display_name :display-name :displayName]
                    name-provided? (h/body-has-any-key? body name-keys)
                    name (when name-provided?
                           (some-> (h/body-get-first body name-keys) str str/trim))
                    updates (cond-> {}
                              name-provided? (assoc :name name))]
                (cond
                  (not name-provided?)
                  (h/json-response {:error "No city fields provided"} 400)

                  (str/blank? name)
                  (h/json-response {:error "name is required"} 400)

                  :else
                  (if-let [updated (some-> ((:update! cities/service) db city-id updates)
                                     h/to-app)]
                    (h/json-response {:data updated})
                    (h/not-found-response "City not found"))))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating city" {:error (ex-message e)
                                                            :data (ex-data e)
                                                            :city-id (str city-id)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Failed to update city" {:message (.getMessage e)
                                                      :city-id (str city-id)})
                (h/json-response {:error "Failed to update city"} 500)))))))))

(defn batch-delete-cities-handler
  "Batch delete cities (admin/owner only).

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
                [raw-ids ids] (h/parse-batch-ids body [:ids :city_ids :city-ids :cityIds])]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No city ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more city ids are invalid"} 400)

              :else
              (let [delete! (:delete! cities/service)]
                (h/json-response {:data (h/batch-delete-entities #(delete! db %) ids)}))))
          (catch Exception e
            (log/error e "Failed to batch delete cities" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete cities"} 500)))))))

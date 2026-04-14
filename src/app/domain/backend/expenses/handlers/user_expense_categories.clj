(ns app.domain.backend.expenses.handlers.user-expense-categories
  "User-facing expense category endpoints.

  These endpoints are mounted under /api/v1/expenses/expense-categories and are
  intended for power users inside the main app.

  IMPORTANT:
  - These routes are role-gated to admin/owner.
  - Responses are normalized to {:data ...} to keep frontend handlers consistent."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.expense-categories :as expense-categories]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn- parse-instant-param
  [raw]
  (when-let [value (some-> raw str str/trim not-empty)]
    (or (try
          (java.time.Instant/parse value)
          (catch Exception _ nil))
      (try
        (-> (java.time.LocalDate/parse value)
          (.atStartOfDay java.time.ZoneOffset/UTC)
          .toInstant)
        (catch Exception _ nil)))))

(defn- has-exclude-from-reports?
  [body]
  (or (contains? body :exclude-from-reports)
    (contains? body :exclude_from_reports)))

(defn- exclude-from-reports-value
  "Prefer the canonical app-style kebab-case key when both payload styles are present."
  [body]
  (cond
    (contains? body :exclude-from-reports)
    (boolean (:exclude-from-reports body))

    (contains? body :exclude_from_reports)
    (boolean (:exclude_from_reports body))

    :else false))

(defn list-expense-categories-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles
                           "Role assignment required")]
        forbidden
        (try
          (let [qp (:query-params request)
                tenant-id (h/get-tenant-id request)
                limit (h/parse-page-limit qp 200)
                offset (h/parse-page-offset qp)
                search (or (h/get-param qp :search)
                         (h/get-param qp :name))
                order-by (h/parse-order-by qp)
                order-dir (h/parse-order-dir qp)
                created-at-from (parse-instant-param (h/get-param qp :created-at-from))
                created-at-to (parse-instant-param (h/get-param qp :created-at-to))
                extra-filters (cond-> []
                                created-at-from (conj [:>= :created_at created-at-from])
                                created-at-to (conj [:<= :created_at created-at-to]))
                opts (cond-> {:limit limit
                              :offset offset
                              :search search}
                       tenant-id (assoc :tenant-id tenant-id)
                       order-by (assoc :order-by order-by)
                       order-dir (assoc :order-dir order-dir)
                       (seq extra-filters) (assoc :extra-filters extra-filters))
                rows (h/to-app ((:list expense-categories/service) db opts))
                rows (cond-> rows (sequential? rows) vec)
                count-opts (cond-> {:search search}
                             tenant-id (assoc :tenant-id tenant-id)
                             (seq extra-filters) (assoc :extra-filters extra-filters))
                total (long (or ((:count expense-categories/service) db count-opts) 0))]
            (h/json-response {:data rows
                              :total total
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Failed to list expense categories" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list expense categories"} 500)))))))

(defn create-expense-category-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                tenant-id (h/get-tenant-id request)
                exclude? (exclude-from-reports-value body)
                payload (cond-> {:name (:name body)
                                 :exclude_from_reports exclude?}
                          tenant-id (assoc :tenant_id tenant-id))
                expense-category (h/to-app ((:create! expense-categories/service) db payload))]
            (h/json-response {:data expense-category} 201))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating expense category" {:error (ex-message e) :data (ex-data e)})
            (h/json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Failed to create expense category" {:message (.getMessage e)})
            (h/json-response {:error "Failed to create expense category"} 500)))))))

(defn update-expense-category-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-admin-or-owner request)]
        forbidden
        (let [expense-category-id (h/parse-path-id request)]
          (if-not expense-category-id
            (h/json-response {:error "Invalid expense category id"} 400)
            (try
              (let [body (h/read-body-params request)
                    tenant-id (h/get-tenant-id request)
                    ;; Accept both snake_case (external JSON) and app-style
                    ;; kebab-case (internal callers / dynamic forms). A missing
                    ;; key must not clobber an existing value.
                    updates (cond-> (select-keys body [:name])
                              (has-exclude-from-reports? body)
                              (assoc :exclude_from_reports (exclude-from-reports-value body)))
                    updated (some-> ((:update! expense-categories/service) db expense-category-id updates
                                                                           (cond-> {}
                                                                             tenant-id (assoc :tenant-id tenant-id)))
                              h/to-app)]
                (if updated
                  (h/json-response {:data updated})
                  (h/not-found-response "Expense category not found")))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating expense category" {:error (ex-message e)
                                                                        :data (ex-data e)
                                                                        :expense-category-id (str expense-category-id)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Failed to update expense category" {:message (.getMessage e)
                                                                  :expense-category-id (str expense-category-id)})
                (h/json-response {:error "Failed to update expense category"} 500)))))))))

(defn batch-delete-expense-categories-handler
  "Batch delete expense categories (admin/owner only).

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
                tenant-id (h/get-tenant-id request)
                [raw-ids ids] (h/parse-batch-ids body [:ids :expense_category_ids :expense-category-ids :expenseCategoryIds])]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No expense category ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more expense category ids are invalid"} 400)

              :else
              (let [delete! (:delete! expense-categories/service)
                    delete-opts (cond-> {}
                                  tenant-id (assoc :tenant-id tenant-id))]
                (h/json-response {:data (h/batch-delete-entities #(delete! db % delete-opts) ids)}))))
          (catch Exception e
            (log/error e "Failed to batch delete expense categories" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete expense categories"} 500)))))))

(ns app.domain.backend.expenses.handlers.user-expenses.supplier-detail
  "User-facing supplier detail related lists.

  These endpoints are mounted under /api/v1/expenses and require an authenticated user.

  NOTE: This is single-tenant and intentionally mirrors the admin Supplier Detail view,
  but is exposed under the user app for household members."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [taoensso.timbre :as log]))

(defn list-article-aliases-handler
  "List article aliases, optionally filtered by supplier_id.

  Query params:
  - supplier_id (uuid, optional but strongly recommended)
  - limit (default 10)
  - offset (default 0)"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles "Role assignment required")]
        forbidden
        (try
          (let [params (:query-params request)
                 limit (or (some-> (h/get-param params :limit) parse-long) 10)
                 offset (or (some-> (h/get-param params :offset) parse-long) 0)
                 supplier-id (h/try-parse-uuid (h/get-param params :supplier_id))
                list-aliases (requiring-resolve 'app.domain.backend.expenses.services.article-aliases/list-article-aliases)
                rows (vec (list-aliases db (cond-> {:limit limit :offset offset}
                                             supplier-id (assoc :supplier_id supplier-id))))]
            (h/json-response {:data rows
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Error listing article aliases" {:query-params (:query-params request)})
            (h/json-response {:error "Failed to list article aliases"} 500))))
      (h/unauthorized-response))))

(defn list-price-observations-handler
  "List price observations, optionally filtered by supplier_id.

  Query params:
  - supplier_id (uuid, optional but strongly recommended)
  - limit (default 10)
  - offset (default 0)"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles "Role assignment required")]
        forbidden
        (try
          (let [params (:query-params request)
                 limit (or (some-> (h/get-param params :limit) parse-long) 10)
                 offset (or (some-> (h/get-param params :offset) parse-long) 0)
                 supplier-id (h/try-parse-uuid (h/get-param params :supplier_id))
                list-obs (requiring-resolve 'app.domain.backend.expenses.services.price-observations/list-price-observations)
                rows (vec (list-obs db (cond-> {:limit limit :offset offset}
                                         supplier-id (assoc :supplier_id supplier-id))))]
            (h/json-response {:data rows
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Error listing price observations" {:query-params (:query-params request)})
            (h/json-response {:error "Failed to list price observations"} 500))))
      (h/unauthorized-response))))

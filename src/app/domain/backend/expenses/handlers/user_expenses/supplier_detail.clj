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
  - search (string, optional)
  - limit (default 10)
  - offset (default 0)
  - order-by (snake/kebab/string/keyword, optional)
  - order-dir (asc/desc, optional)"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles "Role assignment required")]
        forbidden
        (try
          (let [params (:query-params request)
                limit (h/parse-page-limit params 10)
                offset (h/parse-page-offset params)
                supplier-id (h/try-parse-uuid (h/get-param params :supplier_id))
                search (h/get-param params :search)
                order-by (h/parse-order-by params)
                order-dir (h/parse-order-dir params)
                supplier-display-name (h/get-param params :supplier-display-name)
                article-canonical-name (h/get-param params :article-canonical-name)
                raw-label (h/get-param params :raw-label)
                raw-label-normalized (h/get-param params :raw-label-normalized)
                unit (h/get-param params :unit)
                created-at-from (h/parse-instant-param (h/get-param params :created-at-from))
                created-at-to (h/parse-instant-param (h/get-param params :created-at-to))
                extra-filters (cond-> []
                                created-at-from (conj [:>= :aa.created_at created-at-from])
                                created-at-to (conj [:<= :aa.created_at created-at-to]))
                opts (cond-> {:limit limit :offset offset}
                       supplier-id (assoc :supplier_id supplier-id)
                       (some? search) (assoc :search search)
                       order-by (assoc :order-by order-by)
                       order-dir (assoc :order-dir order-dir)
                       (some? supplier-display-name) (assoc :supplier-display-name supplier-display-name)
                       (some? article-canonical-name) (assoc :article-canonical-name article-canonical-name)
                       (some? raw-label) (assoc :raw-label raw-label)
                       (some? raw-label-normalized) (assoc :raw-label-normalized raw-label-normalized)
                       (some? unit) (assoc :unit unit)
                       (seq extra-filters) (assoc :extra-filters extra-filters))
                list-aliases (requiring-resolve 'app.domain.backend.expenses.services.article-aliases/list-article-aliases)
                count-aliases (requiring-resolve 'app.domain.backend.expenses.services.article-aliases/count-article-aliases)
                rows (vec (list-aliases db opts))
                total (long (or (count-aliases db opts) 0))]
            (h/json-response {:data rows
                              :total total
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Error listing article aliases" {:query-params (:query-params request)})
            (h/json-response {:error "Failed to list article aliases"} 500))))
      (h/unauthorized-response))))



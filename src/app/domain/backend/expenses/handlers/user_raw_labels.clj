(ns app.domain.backend.expenses.handlers.user-raw-labels
  "User-facing (non-admin) raw labels endpoints.

  Mounted under /api/v1/expenses/raw-labels.

  NOTE: role-gated to admin/owner." 
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.raw-labels :as raw-labels]
    [app.shared.adapters.database :as shared-db]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

(def ^:private to-app shared-db/to-app)

(def ^:private allowed-roles
  #{"admin" "owner"})

(defn- ensure-admin-or-owner
  [request]
  (h/ensure-role request allowed-roles "Only admins and owners can access this page."))

(defn list-raw-labels-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [qp (:query-params request)
                limit (or (some-> (h/get-param qp :limit) parse-long) 200)
                offset (or (some-> (h/get-param qp :offset) parse-long) 0)
                search (h/get-param qp :search)
                order-by (some-> (or (h/get-param qp :order_by)
                                   (h/get-param qp :order-by)
                                   (h/get-param qp :orderBy))
                           str
                           (str/replace "-" "_")
                           keyword)
                order-dir (some-> (or (h/get-param qp :order_dir)
                                    (h/get-param qp :order-dir)
                                    (h/get-param qp :orderDir))
                           str
                           str/lower-case
                           keyword)
                rows (to-app (raw-labels/list-raw-labels db {:limit limit
                                                             :offset offset
                                                             :search search
                                                             :order-by order-by
                                                             :order-dir order-dir}))]
            (h/json-response {:success true
                              :raw-labels rows}))
          (catch Exception e
            (log/error e "Failed to list raw labels" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list raw labels"} 500)))))))

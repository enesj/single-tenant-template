(ns app.domain.backend.expenses.routes.raw-labels
  "Admin API routes for raw labels.

  Mounted under /admin/api/expenses/raw-labels." 
  (:require
    [app.domain.backend.expenses.services.raw-labels :as raw-labels]
    [app.shared.adapters.database :as shared-db]
    [app.template.backend.middleware.admin :as admin-mw]
    [app.template.backend.routes.admin.utils :as utils]
    [clojure.string :as str]))

(def ^:private to-app shared-db/to-app)

(defn- ->kw
  [s]
  (when (seq s)
    (-> s
      str
      (str/replace "-" "_")
      str/lower-case
      keyword)))

(defn list-handler
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [qp (:query-params request)
            limit (utils/parse-int-param qp :limit 200)
            offset (utils/parse-int-param qp :offset 0)
            search (or (get qp :search) (get qp "search"))
            order-by (->kw (or (get qp :order_by)
                             (get qp "order_by")
                             (get qp :order-by)
                             (get qp "order-by")))
            order-dir (->kw (or (get qp :order_dir)
                              (get qp "order_dir")
                              (get qp :order-dir)
                              (get qp "order-dir")))
            rows (raw-labels/list-raw-labels db {:limit limit
                                                 :offset offset
                                                 :search search
                                                 :order-by order-by
                                                 :order-dir order-dir})]
        (utils/success-response {:raw-labels (to-app rows)})))
    "Failed to list raw labels"))

(defn routes
  [db]
  ["/raw-labels"
   {:middleware [(fn [handler]
                   (admin-mw/wrap-admin-role handler :admin))]}
   ["" {:get (list-handler db)}]])

(ns app.domain.backend.expenses.routes.suppliers
  "Admin API routes for expense suppliers."
  (:require
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.routes.route-configs :as configs]
    [app.template.backend.routes.admin.utils :as utils]))

(def ^:private purge-allowed-roles
  "Admin roles allowed to permanently purge suppliers."
  #{"admin" "owner"})

(defn- ensure-admin-or-owner!
  [request]
  (let [role (some-> request :admin :role str)]
    (when-not (contains? purge-allowed-roles role)
      (throw (ex-info "Insufficient permissions" {:status 403 :role role})))
    true))

(defn- purge-supplier-preview-handler
  [db]
  (utils/with-error-handling
    (fn [request]
      (ensure-admin-or-owner! request)
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (let [purge-preview (requiring-resolve 'app.domain.backend.expenses.services.suppliers/purge-supplier-preview)
              preview (purge-preview db id)]
          (utils/success-response {:preview preview}))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to load supplier purge preview"))

(defn- purge-supplier-handler
  [db]
  (utils/with-error-handling
    (fn [request]
      (ensure-admin-or-owner! request)
      (if-let [id (utils/parse-uuid-custom (get-in request [:path-params :id]))]
        (let [purge-supplier! (requiring-resolve 'app.domain.backend.expenses.services.suppliers/purge-supplier!)
              result (purge-supplier! db id)]
          (utils/success-response {:result result}))
        (utils/error-response "Invalid id" :status 400)))
    "Failed to purge supplier"))

(defn routes [db]
  (let [config (-> configs/supplier-config
                    (factory/register-entity-routes!))]
    (-> (factory/build-extended-routes db config)
      (conj ["/:id/purge-preview" {:get (purge-supplier-preview-handler db)}])
      (conj ["/:id/purge" {:post (purge-supplier-handler db)}]))))
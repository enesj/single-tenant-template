(ns app.domain.frontend.expenses.admin.adapters.admin-crud.register
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.domain.frontend.expenses.admin.adapters.admin-crud.common :as common]
    [app.domain.frontend.expenses.admin.adapters.admin-crud.entities :as entities]))

(defn register-all!
  []
  (doseq [{:keys [entity-key
                  base-uri
                  encode-id?
                  log-prefix
                  on-success-dispatch]}
          entities/admin-entities]
    (let [request-fn (fn [request-opts]
                       (common/build-admin-request
                         {:base-uri base-uri
                          :encode-id? encode-id?
                          :log-prefix log-prefix}
                         request-opts))]
      (adapters.core/register-admin-crud-bridge!
        {:entity-key entity-key
         :operations (common/build-crud-operations
                       {:request-fn request-fn
                        :on-success-dispatch on-success-dispatch})}))))



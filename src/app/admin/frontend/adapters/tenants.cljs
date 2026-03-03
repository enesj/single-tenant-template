(ns app.admin.frontend.adapters.tenants
  "Admin CRUD bridge wiring for tenant list actions."
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.events.tenants :as tenant-events]
    [app.admin.frontend.events.users.template.form-interceptors :as form-interceptors]
    [app.admin.frontend.utils.http :as admin-http]))

(form-interceptors/register-bridge-entity! :tenants)

(adapters.core/register-admin-crud-bridge!
  {:entity-key :tenants
   :operations
   {:batch-delete
    {:request (fn [{:keys [db]} entity-type ids default-effect]
                (if (adapters.core/admin-token db)
                  (let [ids* (->> (or ids [])
                               (remove nil?)
                               (map str)
                               distinct
                               vec)]
                    (assoc default-effect
                      :http-xhrio (admin-http/admin-delete
                                    {:uri "/admin/api/tenants/batch"
                                     :params {:ids ids*}
                                     :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids*]
                                     :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids*]})))
                  {:dispatch [:admin/redirect-to-login]}))
     :on-success (fn [_cofx _entity-type _ids _response default-effect]
                   (assoc default-effect :dispatch [::tenant-events/load-list]))}

    :update
    {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                (if (adapters.core/admin-token db)
                  (assoc default-effect
                    :http-xhrio (admin-http/admin-put
                                  {:uri (str "/admin/api/tenants/" id)
                                   :params form-data
                                   :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                   :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                  {:dispatch [:admin/redirect-to-login]}))
     :on-success (fn [_cofx _entity-type _id _response default-effect]
                   (assoc default-effect :dispatch [::tenant-events/load-list]))}}})
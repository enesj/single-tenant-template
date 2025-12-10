(ns app.template.backend.routes.crud
  "Generic CRUD routes for the template infrastructure.

   These routes provide HTTP endpoints for metadata-driven CRUD operations.
   They handle request/response conversion and delegate to the CRUD service
   for business logic."
  (:require
   [app.template.backend.crud.protocols :as crud-protocols]
   [app.template.backend.routes.utils :as route-utils]))

(defn get-items-handler
  "Create a handler for GET /api/{entity} - retrieve multiple items."
  [crud-service]
  (route-utils/create-crud-handler "get-items"
    (fn [{:keys [tenant-id entity-key query-params]}]
      (let [opts (route-utils/build-options
                   query-params
                   {})
            items (crud-protocols/get-items crud-service tenant-id entity-key opts)]
        (route-utils/success-response items)))))

(defn get-item-handler
  "Create a handler for GET /api/{entity}/{id} - retrieve single item."
  [crud-service]
  (route-utils/create-crud-handler "get-item"
    (fn [{:keys [tenant-id entity-key item-id query-params]}]
      (let [opts (route-utils/build-options
                   query-params
                   {})
            item (crud-protocols/get-item crud-service tenant-id entity-key item-id opts)]
        (if item
          (route-utils/success-response item)
          (route-utils/not-found-response "Item not found"))))))

(defn create-item-handler
  "Create a handler for POST /api/{entity} - create new item."
  [crud-service]
  (route-utils/create-crud-handler "create-item"
    (fn [{:keys [tenant-id user-id entity-key body-params query-params]}]
      (let [opts (route-utils/build-options
                   query-params
                   {:validate? true  ; Always validate on create
                    :user-id user-id})
            item (crud-protocols/create-item crud-service tenant-id entity-key body-params opts)]
        (route-utils/success-response item 201)))))

(defn update-item-handler
  "Create a handler for PUT /api/{entity}/{id} - update existing item."
  [crud-service]
  (route-utils/create-crud-handler "update-item"
    (fn [{:keys [tenant-id user-id entity-key item-id body-params query-params]}]
      (let [opts (route-utils/build-options
                   query-params
                   {:validate? true  ; Always validate on update
                    :user-id user-id})
            item (crud-protocols/update-item crud-service tenant-id entity-key item-id body-params opts)]
        (route-utils/success-response item)))))

(defn delete-item-handler
  "Create a handler for DELETE /api/{entity}/{id} - delete item."
  [crud-service]
  (route-utils/create-crud-handler "delete-item"
    (fn [{:keys [tenant-id entity-key item-id is-admin? admin]}]
      (let [opts (cond-> {}
                   is-admin? (assoc :is-admin? true :admin admin))
            result (crud-protocols/delete-item crud-service tenant-id entity-key item-id opts)]
        (route-utils/success-response result)))))

(defn batch-update-handler
  "Create a handler for PUT /api/{entity}/batch - batch update items."
  [crud-service]
  (route-utils/create-crud-handler "batch-update"
    (fn [{:keys [tenant-id user-id entity-key body-params query-params]}]
      (let [items (:items body-params)
            opts (route-utils/build-options
                   query-params
                   {:validate? true
                    :user-id user-id})
            result (crud-protocols/batch-update-items crud-service tenant-id entity-key items opts)]
        (route-utils/success-response result)))))

(defn batch-delete-handler
  "Create a handler for DELETE /api/{entity}/batch - batch delete items."
  [crud-service]
  (route-utils/create-crud-handler "batch-delete"
    (fn [{:keys [tenant-id entity-key body-params is-admin? admin]}]
      (let [item-ids (:ids body-params)
            opts (cond-> {}
                   is-admin? (assoc :is-admin? true :admin admin))
            result (crud-protocols/batch-delete-items crud-service tenant-id entity-key item-ids opts)]
        (route-utils/success-response result)))))

(defn validate-field-handler
  "Create a handler for POST /api/{entity}/validate - validate single field."
  [crud-service]
  (route-utils/create-crud-handler "validate-field"
    (fn [{:keys [entity-key body-params]}]
      (let [field-name (keyword (-> body-params keys first))
            value (get body-params field-name)
            ;; Get the validation service from the CRUD service
            validation-service (:validation-service crud-service)
            result (crud-protocols/validate-field validation-service entity-key field-name value)]
        (route-utils/success-response result)))))

(defn crud-routes
  "Return route configuration for CRUD operations.

   These routes provide metadata-driven CRUD endpoints for any entity
   defined in the entity metadata. Routes are mounted under /api/{entity}."
  [crud-service]
  [["/api/:entity"
    {:swagger {:tags ["CRUD Operations"]}}

    ;; List/Create operations
    [""
     {:get {:handler (get-items-handler crud-service)
            :summary "Get list of items"
            :parameters {:path [:map [:entity :string]]
                         :query [:map
                                 [:filters {:optional true} :map]
                                 [:order-by {:optional true} :string]
                                 [:limit {:optional true} :string]
                                 [:offset {:optional true} :string]
                                 [:include-joins {:optional true} :string]]}}

      :post {:handler (create-item-handler crud-service)
             :summary "Create new item"
             :parameters {:path [:map [:entity :string]]
                          :query [:map
                                  [:return-joins {:optional true} :string]]
                          :body :map}}}]

    ;; Batch operations (marked as conflicting with :id route)
    ["/batch"
     {:conflicting true
      :put {:handler (batch-update-handler crud-service)
            :summary "Batch update items"
            :parameters {:path [:map [:entity :string]]
                         :query [:map
                                 [:continue-on-error {:optional true} :string]]
                         :body [:map [:items [:vector :map]]]}}

      :delete {:handler (batch-delete-handler crud-service)
               :summary "Batch delete items"
               :parameters {:path [:map [:entity :string]]
                            :body [:map [:ids [:vector :any]]]}}}]

    ;; Field validation (marked as conflicting with :id route)
    ["/validate"
     {:conflicting true
      :post {:handler (validate-field-handler crud-service)
             :summary "Validate single field"
             :parameters {:path [:map [:entity :string]]
                          :body :map}}}]

    ;; Single item operations
    ["/:id"
     {:conflicting true
      :get {:handler (get-item-handler crud-service)
            :summary "Get single item"
            :parameters {:path [:map
                                [:entity :string]
                                [:id :any]]
                         :query [:map
                                 [:include-joins {:optional true} :string]]}}

      :put {:handler (update-item-handler crud-service)
            :summary "Update item"
            :parameters {:path [:map
                                [:entity :string]
                                [:id :any]]
                         :query [:map
                                 [:return-joins {:optional true} :string]]
                         :body :map}}

      :delete {:handler (delete-item-handler crud-service)
               :summary "Delete item"
               :parameters {:path [:map
                                   [:entity :string]
                                   [:id :any]]}}}]]])

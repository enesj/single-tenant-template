(ns app.domain.frontend.expenses.admin.adapters.admin-crud.common
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.utils.http :as admin-http]
    [taoensso.timbre :as log]))

(def lookup-params
  "Default query params used for FK lookup dropdowns in admin forms."
  {:limit 500
   :offset 0})

(defn redirect-to-login-effect
  []
  {:dispatch [:admin/redirect-to-login]})

(defn build-admin-request
  [{:keys [base-uri encode-id? log-prefix]} {:keys [method id ids params on-success on-failure]}]
  (let [id* (when id
              (let [id* (str id)]
                (if encode-id?
                  (js/encodeURIComponent id*)
                  id*)))
        uri (cond
              (seq ids) (str base-uri "/batch")
              id* (str base-uri "/" id*)
              :else base-uri)
        params* (if (seq ids) {:ids (mapv str ids)} params)]
    (when (seq log-prefix)
      (log/info log-prefix {:method method :uri uri :params params*}))
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params*
                               :on-success on-success
                               :on-failure on-failure})))

(defn build-crud-operations
  [{:keys [request-fn on-success-dispatch]}]
  {:fetch {:request (fn [{:keys [db]} entity-type default-effect]
                      (if (adapters.core/admin-token db)
                        (assoc default-effect
                          :http-xhrio (request-fn
                                        {:method :get
                                         :params lookup-params
                                         :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
                                         :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]}))
                        (redirect-to-login-effect)))}

   :batch-delete {:request (fn [{:keys [db]} entity-type ids default-effect]
                             (if (adapters.core/admin-token db)
                               (let [ids* (mapv str ids)]
                                 (assoc default-effect
                                   :http-xhrio (request-fn
                                                 {:method :delete
                                                  :ids ids*
                                                  :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids*]
                                                  :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids*]})))
                               (redirect-to-login-effect)))
                  :on-success (fn [& args]
                                (let [default-effect (last args)]
                                  (assoc default-effect :dispatch on-success-dispatch)))}
   :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (request-fn
                                         {:method :post
                                          :params form-data
                                          :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                          :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                         (redirect-to-login-effect)))
            :on-success (fn [& args]
                          (let [default-effect (last args)]
                            (assoc default-effect :dispatch on-success-dispatch)))}
   :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (request-fn
                                         {:method :put
                                          :id id
                                          :params form-data
                                          :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                          :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                         (redirect-to-login-effect)))
            :on-success (fn [& args]
                          (let [default-effect (last args)]
                            (assoc default-effect :dispatch on-success-dispatch)))}})

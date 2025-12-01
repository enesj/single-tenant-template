(ns app.shared.crud.factory
  (:require
    [app.shared.http.core :as http]
    [re-frame.core :as rf]))

(defn create-crud-handler
  "Returns a re-frame event handler fn for creating an entity."
  [entity-name context success-event error-event]
  (fn [{:keys [db]} [_ data]]
    (let [request (http/build-request db context :post (str "/" (name entity-name))
                    {:params data
                     :on-success success-event
                     :on-failure error-event})]
      {:http-xhrio request
       :db (assoc-in db [:forms (keyword entity-name) :submitting?] true)})))

(defn update-crud-handler
  "Returns a re-frame event handler fn for updating an entity."
  [entity-name context success-event error-event]
  (fn [{:keys [db]} [_ entity-id data]]
    (let [request (http/build-request db context :put (str "/" (name entity-name) "/" entity-id)
                    {:params data
                     :on-success success-event
                     :on-failure error-event})]
      {:http-xhrio request
       :db (assoc-in db [:forms (keyword entity-name) :submitting?] true)})))

(defn delete-crud-handler
  "Returns a re-frame event handler fn for deleting an entity."
  [entity-name context success-event error-event]
  (fn [{:keys [db]} [_ entity-id]]
    (let [request (http/build-request db context :delete (str "/" (name entity-name) "/" entity-id)
                    {:on-success success-event
                     :on-failure error-event})]
      {:http-xhrio request
       :db (assoc-in db [:forms (keyword entity-name) :submitting?] true)})))

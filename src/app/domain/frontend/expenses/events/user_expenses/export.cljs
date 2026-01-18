(ns app.domain.frontend.expenses.events.user-expenses.export
  "Export and bulk operations for user expenses."
  (:require
    [ajax.core :as ajax]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn- export-filename
  [export-format]
  (let [timestamp (.toISOString (js/Date.))
        ext (case export-format
              :pdf "pdf"
              :csv "csv"
              "csv")]
    (str "expenses-export-" timestamp "." ext)))

(defn- download-csv!
  [content filename]
  (let [blob (js/Blob. #js [content] #js {:type "text/csv;charset=utf-8"})
        url (.createObjectURL js/URL blob)
        link (.createElement js/document "a")]
    (set! (.-href link) url)
    (set! (.-download link) filename)
    (.appendChild (.-body js/document) link)
    (.click link)
    (.remove link)
    (js/setTimeout (fn [] (.revokeObjectURL js/URL url)) 1000)))

;; ---------------------------------------------------------------------------
;; Export expenses
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/export
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [params (or params {})
          raw-format (:format params)
          export-format (cond
                          (keyword? raw-format) raw-format
                          (string? raw-format) (keyword raw-format)
                          :else :csv)
          response-format (if (= export-format :csv)
                            (ajax/text-response-format)
                            (ajax/json-response-format {:keywords? true}))
          request-params (-> params
                           (dissoc :format)
                           (assoc :format (name export-format)))]
      {:db (-> db
             (assoc-in [:user-expenses :export :loading?] true)
             (assoc-in [:user-expenses :export :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri (str endpoints/list-endpoint "/export")
                      :admin-uri (str endpoints/admin-expenses-endpoint "/export")
                      :params request-params
                      :response-format response-format
                      :on-success [:user-expenses/export-success export-format]
                      :on-failure [:user-expenses/export-failure]})})))

(rf/reg-event-fx
  :user-expenses/export-success
  common-interceptors
  (fn [{:keys [db]} [export-format response]]
    (let [db' (-> db
                (assoc-in [:user-expenses :export :loading?] false)
                (assoc-in [:user-expenses :export :error] nil))]
      (if (= export-format :csv)
        (do
          (download-csv! (or response "") (export-filename export-format))
          {:db db'})
        (let [message (or (:message response) "Export completed.")]
          {:db db'
           :dispatch [:toast {:type :info :message message}]})))))

(rf/reg-event-db
  :user-expenses/export-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to export expenses" {:error error})
    (-> db
      (assoc-in [:user-expenses :export :loading?] false)
      (assoc-in [:user-expenses :export :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Delete all expenses (dangerous operation)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/delete-all
  common-interceptors
  (fn [{:keys [db]} [confirmation-token]]
    (if (= confirmation-token "DELETE_ALL_EXPENSES")
      {:db (-> db
             (assoc-in [:user-expenses :bulk :loading?] true)
             (assoc-in [:user-expenses :bulk :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/list-endpoint "/all")
                      :admin-uri (str endpoints/admin-expenses-endpoint "/all")
                      :params {:confirmation confirmation-token}
                      :on-success [:user-expenses/delete-all-success]
                      :on-failure [:user-expenses/delete-all-failure]})}
      {:db db
       :dispatch [:toast {:type :error :message "Invalid confirmation token"}]})))

(rf/reg-event-fx
  :user-expenses/delete-all-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (-> db
           (assoc-in [:user-expenses :bulk :loading?] false)
           (assoc-in [:user-expenses :bulk :error] nil)
           (assoc-in [:user-expenses :recent :data] [])
           (assoc-in [:user-expenses :summary :data] nil))
     :dispatch-n [[:toast {:type :success :message "All expenses deleted"}]
                  [:user-expenses/fetch-summary]]}))

(rf/reg-event-db
  :user-expenses/delete-all-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete all expenses" {:error error})
    (-> db
      (assoc-in [:user-expenses :bulk :loading?] false)
      (assoc-in [:user-expenses :bulk :error] (http/extract-error-message error)))))

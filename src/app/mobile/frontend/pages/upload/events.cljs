(ns app.mobile.frontend.pages.upload.events
  "Mobile upload re-frame events and subscriptions.
  Side-effect namespace: registrations happen on load."
  (:require
    [ajax.core :as ajax]
    [app.template.frontend.api.http :as http]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; Upload events
;; ========================================================================

(rf/reg-event-fx
  :mobile/upload-receipt
  (fn [{:keys [db]} [_ file callbacks]]
    (let [form-data (js/FormData.)
          callbacks (when (map? callbacks) callbacks)]
      (.append form-data "file" file)
      {:db (-> db
             (assoc-in [:mobile :upload :loading?] true)
             (assoc-in [:mobile :upload :error] nil))
       :http-xhrio {:method :post
                    :uri "/api/v1/expenses/upload"
                    :body form-data
                    :format {:write identity :content-type false}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:mobile/upload-receipt-success callbacks]
                    :on-failure [:mobile/upload-receipt-failure callbacks]}})))

(rf/reg-event-fx
  :mobile/upload-receipt-success
  (fn [{:keys [db]} [_ maybe-callbacks maybe-response]]
    (let [[callbacks response] (if (some? maybe-response)
                                 [maybe-callbacks maybe-response]
                                 [nil maybe-callbacks])
          receipt-id (get-in response [:data :id])
          duplicate? (:duplicate? response)
          on-success (:on-success callbacks)]
      (when (fn? on-success)
        (on-success response))
      {:db (-> db
             (assoc-in [:mobile :upload :loading?] false)
             (assoc-in [:mobile :upload :error] nil)
             (assoc-in [:mobile :upload :last-upload] response))
       :fx (cond-> [[:dispatch [:mobile/show-toast :mobile/toast-receipt-uploaded]]]
             (and receipt-id (not duplicate?))
             (conj [:http-xhrio
                    {:method :post
                     :uri (str "/api/v1/expenses/receipts/" receipt-id "/ocr")
                     :format (ajax/json-request-format)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success [:mobile/ocr-receipt-success receipt-id]
                     :on-failure [:mobile/ocr-receipt-failure receipt-id]}]))})))

(rf/reg-event-db
  :mobile/ocr-receipt-success
  (fn [db [_ _receipt-id _response]]
    db))

(rf/reg-event-db
  :mobile/ocr-receipt-failure
  (fn [db [_ receipt-id error]]
    (log/warn "Mobile OCR trigger failed" {:receipt-id receipt-id :error error})
    db))

(rf/reg-event-fx
  :mobile/upload-receipt-failure
  (fn [{:keys [db]} [_ maybe-callbacks maybe-error]]
    (let [[callbacks error] (if (some? maybe-error)
                              [maybe-callbacks maybe-error]
                              [nil maybe-callbacks])
          message (or (http/extract-error-message error) "Upload failed")
          on-error (:on-error callbacks)]
      (when (fn? on-error)
        (on-error message))
      {:db (-> db
             (assoc-in [:mobile :upload :loading?] false)
             (assoc-in [:mobile :upload :error] message))})))

(rf/reg-event-db
  :mobile/show-toast
  (fn [db [_ message]]
    (assoc-in db [:mobile :toast] message)))

(rf/reg-event-db
  :mobile/clear-toast
  (fn [db _]
    (assoc-in db [:mobile :toast] nil)))

;; ========================================================================
;; Upload subscriptions
;; ========================================================================

(rf/reg-sub
  :mobile/upload-loading?
  (fn [db _]
    (get-in db [:mobile :upload :loading?] false)))

(rf/reg-sub
  :mobile/upload-error
  (fn [db _]
    (get-in db [:mobile :upload :error])))

(rf/reg-sub
  :mobile/toast
  (fn [db _]
    (get-in db [:mobile :toast])))

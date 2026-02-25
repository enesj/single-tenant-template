(ns app.domain.frontend.expenses.events.user-expenses.receipts.ocr
  "UI-triggered OCR events for receipt processing."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private base-path [:user-expenses :receipts])

(defn- normalize-selected-receipt-ids
  [receipt-ids]
  (->> (or receipt-ids [])
    (map (fn [receipt-id]
           (some-> receipt-id str str/trim)))
    (remove str/blank?)
    distinct
    vec))

;; ---------------------------------------------------------------------------
;; OCR Events (UI-triggered)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/ocr-receipt
  common-interceptors
  (fn [{:keys [db]} [receipt-id]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri (str endpoints/receipts-endpoint "/" receipt-id "/ocr")
                    :on-success [:user-expenses/ocr-receipt-success receipt-id]
                    :on-failure [:user-expenses/ocr-receipt-failure receipt-id]})}))

(rf/reg-event-fx
  :user-expenses/ocr-receipt-success
  common-interceptors
  (fn [{:keys [db]} [_receipt-id _response]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) false)
           (assoc-in (conj base-path :error) nil))
     :dispatch [:user-expenses/refresh-receipts-list]}))

(rf/reg-event-db
  :user-expenses/ocr-receipt-failure
  common-interceptors
  (fn [db [_receipt-id error]]
    (log/warn "Failed to trigger OCR" {:error error})
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/ocr-selected
  common-interceptors
  (fn [{:keys [db]} [receipt-ids]]
    (let [ids (normalize-selected-receipt-ids receipt-ids)]
      (if (seq ids)
        {:db (-> db
               (assoc-in (conj base-path :action-loading?) true)
               (assoc-in (conj base-path :error) nil))
         :http-xhrio (x/xhrio db
                       {:method :post
                        :uri (str endpoints/receipts-endpoint "/ocr")
                        :params {:receipt_ids ids}
                        :on-success [:user-expenses/ocr-selected-success ids]
                        :on-failure [:user-expenses/ocr-selected-failure]})}
        {:db (-> db
               (assoc-in (conj base-path :action-loading?) false)
               (assoc-in (conj base-path :error) "Select at least one receipt to parse."))}))))

(rf/reg-event-fx
  :user-expenses/ocr-selected-success
  common-interceptors
  (fn [{:keys [db]} [_receipt-ids _response]]
    {:db (-> db
           (assoc-in (conj base-path :action-loading?) false)
           (assoc-in (conj base-path :error) nil))
     :dispatch-n [[:user-expenses/refresh-receipts-list]
                  [:app.template.frontend.events.list/clear-selection :receipts]]}))

(rf/reg-event-db
  :user-expenses/ocr-selected-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to trigger batch OCR" {:error error})
    (-> db
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (http/extract-error-message error)))))

(ns app.admin.frontend.events.receipts-approval
  "Admin receipt review + approve events for the `/admin/receipts` detail modal."
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.receipts :as receipts-events]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private detail-path [:admin :receipts :detail])
(def ^:private form-path [:admin :receipts :form])

(defn- normalize-receipt-id
  [receipt-id]
  (some-> receipt-id str str/trim not-empty))

(defn- begin-action
  [db]
  (-> db
    (assoc-in (conj form-path :loading?) true)
    (assoc-in (conj form-path :error) nil)
    (assoc-in (conj detail-path :action-loading?) true)
    (assoc-in (conj detail-path :error) nil)))

(defn- finish-action
  [db]
  (-> db
    (assoc-in (conj form-path :loading?) false)
    (assoc-in (conj detail-path :action-loading?) false)))

(defn- store-receipt
  [db receipt-id receipt]
  (cond-> db
    receipt
    (assoc-in (conj detail-path :by-id receipt-id) receipt)))

(defn- callback-fx
  [callback]
  (if callback
    [[:dispatch-later {:ms 100
                       :dispatch [:admin/call-receipt-modal-callback callback]}]]
    []))

(defn- invalid-receipt-id-db
  [db]
  (-> db
    (assoc-in (conj form-path :loading?) false)
    (assoc-in (conj form-path :error) "Receipt ID is required.")
    (assoc-in (conj detail-path :action-loading?) false)))

(rf/reg-event-db
  :admin/clear-receipt-form-error
  common-interceptors
  (fn [db _]
    (-> db
      (assoc-in (conj form-path :error) nil)
      (assoc-in (conj detail-path :error) nil))))

(rf/reg-event-fx
  :admin/approve-receipt
  common-interceptors
  (fn [{:keys [db]} [receipt-id form-data on-success]]
    (if-let [receipt-id* (normalize-receipt-id receipt-id)]
      {:db (begin-action db)
       :http-xhrio (admin-http/admin-post
                     {:uri (str "/admin/api/expenses/receipts/" receipt-id* "/approve")
                      :params form-data
                      :on-success [:admin/approve-receipt-success receipt-id* on-success]
                      :on-failure [:admin/approve-receipt-failure]})}
      {:db (invalid-receipt-id-db db)})))

(rf/reg-event-fx
  :admin/approve-receipt-success
  common-interceptors
  (fn [{:keys [db]} [receipt-id on-success response]]
    (let [expense (or (:expense response)
                    (get-in response [:data :expense]))
          receipt (or (:receipt response)
                    (get-in response [:data :receipt]))
          dispatches (cond-> [[::receipts-events/load-list {}]]
                       expense
                       (conj [::expenses-sync/upsert-expenses [expense]]))]
      {:db (-> db
             finish-action
             (assoc-in (conj form-path :error) nil)
             (assoc-in (conj detail-path :error) nil)
             (store-receipt receipt-id receipt))
       :dispatch-n dispatches
       :fx (callback-fx on-success)})))

(rf/reg-event-db
  :admin/approve-receipt-failure
  common-interceptors
  (fn [db [error]]
    (let [message (admin-http/extract-error-message error)]
      (log/warn "Failed to approve admin receipt"
        {:error error})
      (-> db
        finish-action
        (assoc-in (conj form-path :error) message)
        (assoc-in (conj detail-path :error) message)))))

(rf/reg-event-fx
  :admin/save-receipt-review
  common-interceptors
  (fn [{:keys [db]} [receipt-id form-data on-success]]
    (if-let [receipt-id* (normalize-receipt-id receipt-id)]
      {:db (begin-action db)
       :http-xhrio (admin-http/admin-post
                     {:uri (str "/admin/api/expenses/receipts/" receipt-id* "/review")
                      :params form-data
                      :on-success [:admin/save-receipt-review-success receipt-id* on-success]
                      :on-failure [:admin/save-receipt-review-failure]})}
      {:db (invalid-receipt-id-db db)})))

(rf/reg-event-fx
  :admin/save-receipt-review-success
  common-interceptors
  (fn [{:keys [db]} [receipt-id on-success response]]
    (let [receipt (or (:receipt response)
                    (get-in response [:data :receipt]))]
      {:db (-> db
             finish-action
             (assoc-in (conj form-path :error) nil)
             (assoc-in (conj detail-path :error) nil)
             (store-receipt receipt-id receipt))
       :dispatch-n [[::receipts-events/load-list {}]]
       :fx (callback-fx on-success)})))

(rf/reg-event-db
  :admin/save-receipt-review-failure
  common-interceptors
  (fn [db [error]]
    (let [message (admin-http/extract-error-message error)]
      (log/warn "Failed to save admin receipt review"
        {:error error})
      (-> db
        finish-action
        (assoc-in (conj form-path :error) message)
        (assoc-in (conj detail-path :error) message)))))

(rf/reg-event-fx
  :admin/update-posted-receipt
  common-interceptors
  (fn [{:keys [db]} [receipt-id form-data on-success]]
    (if-let [receipt-id* (normalize-receipt-id receipt-id)]
      {:db (begin-action db)
       :http-xhrio (admin-http/admin-post
                     {:uri (str "/admin/api/expenses/receipts/" receipt-id* "/update-posted")
                      :params form-data
                      :on-success [:admin/update-posted-receipt-success receipt-id* on-success]
                      :on-failure [:admin/update-posted-receipt-failure]})}
      {:db (invalid-receipt-id-db db)})))

(rf/reg-event-fx
  :admin/update-posted-receipt-success
  common-interceptors
  (fn [{:keys [db]} [receipt-id on-success response]]
    (let [expense (or (:expense response)
                    (get-in response [:data :expense]))
          receipt (or (:receipt response)
                    (get-in response [:data :receipt]))
          dispatches (cond-> [[::receipts-events/load-list {}]]
                       expense
                       (conj [::expenses-sync/upsert-expenses [expense]]))]
      {:db (-> db
             finish-action
             (assoc-in (conj form-path :error) nil)
             (assoc-in (conj detail-path :error) nil)
             (store-receipt receipt-id receipt))
       :dispatch-n dispatches
       :fx (callback-fx on-success)})))

(rf/reg-event-db
  :admin/update-posted-receipt-failure
  common-interceptors
  (fn [db [error]]
    (let [message (admin-http/extract-error-message error)]
      (log/warn "Failed to update admin posted receipt"
        {:error error})
      (-> db
        finish-action
        (assoc-in (conj form-path :error) message)
        (assoc-in (conj detail-path :error) message)))))

(rf/reg-event-fx
  :admin/call-receipt-modal-callback
  common-interceptors
  (fn [_ [callback]]
    (when callback
      (callback))
    {}))
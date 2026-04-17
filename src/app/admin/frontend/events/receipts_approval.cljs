(ns app.admin.frontend.events.receipts-approval
  "Admin receipt review + approve events for the `/admin/receipts` detail modal."
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.components.user-expense-form.normalization :as norm]
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

(defn- normalize-selected-receipt-ids
  [receipt-ids]
  (->> (or receipt-ids [])
    (map normalize-receipt-id)
    (remove nil?)
    distinct
    vec))

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

;; ---------------------------------------------------------------------------
;; Batch OCR (parse selected)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :admin/ocr-selected
  common-interceptors
  (fn [{:keys [db]} [receipt-ids]]
    (let [ids (normalize-selected-receipt-ids receipt-ids)]
      (if (seq ids)
        {:db (-> db
               begin-action
               (assoc-in (conj form-path :error) nil))
         :http-xhrio (admin-http/admin-post
                       {:uri "/admin/api/expenses/receipts/ocr"
                        :params {:receipt_ids ids}
                        :timeout 30000
                        :on-success [:admin/ocr-selected-success ids]
                        :on-failure [:admin/ocr-selected-failure]})}
        {:db (-> db
               finish-action
               (assoc-in (conj form-path :error) "Select at least one receipt to parse."))}))))

(rf/reg-event-fx
  :admin/ocr-selected-success
  common-interceptors
  (fn [{:keys [db]} [_receipt-ids _response]]
    {:db (-> db finish-action
           (assoc-in (conj form-path :error) nil)
           (assoc-in (conj detail-path :error) nil))
     :dispatch-n [[::receipts-events/load-list {}]
                  [:app.template.frontend.events.list/clear-selection :receipts]]}))

(rf/reg-event-db
  :admin/ocr-selected-failure
  common-interceptors
  (fn [db [error]]
    (let [message (admin-http/extract-error-message error)]
      (log/warn "Failed to trigger admin batch OCR" {:error error})
      (-> db
        finish-action
        (assoc-in (conj form-path :error) message)))))

;; ---------------------------------------------------------------------------
;; Batch post-selected (fetch each receipt, approve, repeat)
;; ---------------------------------------------------------------------------

(defn- batch-post-summary-message
  [succeeded failed]
  (let [success-count (count succeeded)
        failure-count (count failed)
        first-error (or (some-> failed first :error str str/trim not-empty)
                      "Unknown error")]
    (cond
      (zero? failure-count) nil
      (zero? success-count)
      (if (= 1 failure-count)
        (str "Failed to post selected receipt. " first-error)
        (str "Failed to post " failure-count " selected receipts. First error: " first-error))
      :else
      (str "Posted " success-count " receipt" (when (not= 1 success-count) "s")
        "; " failure-count " failed. First error: " first-error))))

(defn- post-selected-next-fx
  [db remaining succeeded failed]
  (if-let [receipt-id (first remaining)]
    {:db (-> db begin-action)
     :http-xhrio (admin-http/admin-get
                   {:uri (str "/admin/api/expenses/receipts/" receipt-id)
                    :on-success [:admin/post-selected-receipt-loaded receipt-id (vec (rest remaining)) succeeded failed]
                    :on-failure [:admin/post-selected-receipt-load-failure receipt-id (vec (rest remaining)) succeeded failed]})}
    {:db (-> db finish-action
           (assoc-in (conj form-path :error) (batch-post-summary-message succeeded failed))
           (assoc-in (conj detail-path :error) nil))
     :dispatch-n [[::receipts-events/load-list {}]
                  [:app.template.frontend.events.list/clear-selection :receipts]]}))

(rf/reg-event-fx
  :admin/post-selected
  common-interceptors
  (fn [{:keys [db]} [receipt-ids]]
    (let [ids (normalize-selected-receipt-ids receipt-ids)]
      (if (seq ids)
        (post-selected-next-fx db ids [] [])
        {:db (-> db
               finish-action
               (assoc-in (conj form-path :error) "Select at least one receipt to post."))}))))

(rf/reg-event-fx
  :admin/post-selected-receipt-loaded
  common-interceptors
  (fn [{:keys [db]} [receipt-id remaining succeeded failed response]]
    (let [receipt (or (:receipt response)
                    (get-in response [:data :receipt]))
          db* (cond-> (assoc-in db (conj detail-path :error) nil)
                receipt (store-receipt receipt-id receipt))
          values (when receipt (norm/normalize-receipt-data receipt))
          validation (when values (norm/validate-expense-values values))]
      (if (:ok? validation)
        {:db db*
         :http-xhrio (admin-http/admin-post
                       {:uri (str "/admin/api/expenses/receipts/" receipt-id "/approve")
                        :params (norm/prepare-expense-submit-values values)
                        :on-success [:admin/post-selected-approve-success receipt-id remaining succeeded failed]
                        :on-failure [:admin/post-selected-approve-failure receipt-id remaining succeeded failed]})}
        (let [message (or (:error validation) "Receipt is missing required fields.")]
          (post-selected-next-fx db* remaining succeeded
            (conj failed {:id (str receipt-id) :error message})))))))

(rf/reg-event-fx
  :admin/post-selected-receipt-load-failure
  common-interceptors
  (fn [{:keys [db]} [receipt-id remaining succeeded failed error]]
    (log/warn "Failed to load receipt during admin batch post"
      {:receipt-id receipt-id :error error})
    (post-selected-next-fx db remaining succeeded
      (conj failed {:id (str receipt-id)
                    :error (admin-http/extract-error-message error)}))))

(rf/reg-event-fx
  :admin/post-selected-approve-success
  common-interceptors
  (fn [{:keys [db]} [receipt-id remaining succeeded failed response]]
    (let [expense (or (:expense response) (get-in response [:data :expense]))
          receipt (or (:receipt response) (get-in response [:data :receipt]))
          db* (cond-> db receipt (store-receipt receipt-id receipt))]
      (cond-> (post-selected-next-fx db* remaining
                (conj succeeded (str receipt-id))
                failed)
        expense (assoc :dispatch [::expenses-sync/upsert-expenses [expense]])))))

(rf/reg-event-fx
  :admin/post-selected-approve-failure
  common-interceptors
  (fn [{:keys [db]} [receipt-id remaining succeeded failed error]]
    (log/warn "Failed to approve receipt during admin batch post"
      {:receipt-id receipt-id :error error})
    (post-selected-next-fx db remaining succeeded
      (conj failed {:id (str receipt-id)
                    :error (admin-http/extract-error-message error)}))))
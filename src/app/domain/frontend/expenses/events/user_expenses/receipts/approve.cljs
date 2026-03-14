(ns app.domain.frontend.expenses.events.user-expenses.receipts.approve
  "Approve, batch post-selected, and save-review events."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.components.user-expense-form.normalization :as norm]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private base-path [:user-expenses :receipts])

(defn- payer-default?
  [payer]
  (boolean
    (or (:is-default payer)
      (:isDefault payer))))

(defn- default-payer-id
  [db]
  (let [settings-id (some-> (get-in db [:user-expenses :settings :data :default-payer-id])
                      str
                      str/trim
                      not-empty)
        payers (get-in db [:user-expenses :payers :items])]
    (or settings-id
      (some->> (or payers [])
        (some (fn [p]
                (when (payer-default? p)
                  (:id p)))))
      (:id (first payers)))))

(defn- normalize-selected-receipt-ids
  [receipt-ids]
  (->> (or receipt-ids [])
    (map (fn [receipt-id]
           (some-> receipt-id str str/trim)))
    (remove str/blank?)
    distinct
    vec))

(defn- normalize-approve-values
  [db receipt]
  (let [default-payer (some-> (default-payer-id db) str str/trim not-empty)
        settings (get-in db [:user-expenses :settings :data])
        default-category (some-> (:default-expense-category-id settings) str str/trim not-empty)
        default-note (some-> (:default-note settings) str str/trim not-empty)
        normalized (norm/normalize-receipt-data receipt)
        payer-id (some-> (:payer_id normalized) str str/trim not-empty)
        category-id (some-> (:expense_category_id normalized) str str/trim not-empty)
        notes (some-> (:notes normalized) str str/trim not-empty)]
    (cond-> normalized
      (and (nil? payer-id) default-payer)
      (assoc :payer_id default-payer)
      (and (nil? category-id) default-category)
      (assoc :expense_category_id default-category)
      (and (nil? notes) default-note)
      (assoc :notes default-note))))

(defn- batch-post-summary-message
  [succeeded failed]
  (let [success-count (count succeeded)
        failure-count (count failed)
        first-error (or (some-> failed first :error str str/trim not-empty)
                      "Unknown error")]
    (cond
      (zero? failure-count)
      nil

      (zero? success-count)
      (if (= 1 failure-count)
        (str "Failed to post selected receipt. " first-error)
        (str "Failed to post " failure-count " selected receipts. First error: " first-error))

      :else
      (str "Posted " success-count " receipt" (when (not= 1 success-count) "s")
        "; " failure-count " failed. First error: " first-error))))

;; ---------------------------------------------------------------------------
;; Approve & post
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/approve-receipt
  common-interceptors
  (fn [{:keys [db]} [receipt-id form-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil)
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri (str endpoints/receipts-endpoint "/" receipt-id "/approve")
                    :params form-data
                    :on-success [:user-expenses/approve-receipt-success receipt-id on-success]
                    :on-failure [:user-expenses/approve-receipt-failure]})}))

(rf/reg-event-fx
  :user-expenses/approve-receipt-success
  common-interceptors
  (fn [{:keys [db]} [receipt-id on-success response]]
    (let [expense (get-in response [:data :expense])
          receipt (get-in response [:data :receipt])
          fx (cond-> []
               on-success (conj [:dispatch-later {:ms 100}
                                 :dispatch [:user-expenses/call-modal-callback on-success]]))]
      (cond-> {:db (-> db
                     (assoc-in [:user-expenses :form :loading?] false)
                     (assoc-in [:user-expenses :form :error] nil)
                     (assoc-in (conj base-path :action-loading?) false)
                     (assoc-in (conj base-path :error) nil)
                     (cond-> receipt
                       (assoc-in (conj base-path :by-id receipt-id) receipt)))
               :dispatch-n [[:user-expenses/fetch-recent {:limit 25 :offset 0}]
                            [:user-expenses/refresh-receipts-list]
                            [:user-expenses/fetch-receipt receipt-id]
                            [:user-expenses/close-receipt-detail-modal]]
               :fx fx}
        expense
        (assoc :dispatch [::expenses-sync/upsert-expenses [expense]])))))

(rf/reg-event-db
  :user-expenses/approve-receipt-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to approve receipt" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error))
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Batch post-selected
;; ---------------------------------------------------------------------------

(defn- post-selected-next-fx
  [db remaining succeeded failed]
  (if-let [receipt-id (first remaining)]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil)
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri (str endpoints/receipts-endpoint "/" receipt-id)
                    :on-success [:user-expenses/post-selected-receipt-loaded receipt-id (vec (rest remaining)) succeeded failed]
                    :on-failure [:user-expenses/post-selected-receipt-load-failure receipt-id (vec (rest remaining)) succeeded failed]})}
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] false)
           (assoc-in [:user-expenses :form :error] (batch-post-summary-message succeeded failed))
           (assoc-in (conj base-path :action-loading?) false)
           (assoc-in (conj base-path :error) nil))
     :dispatch-n [[:user-expenses/fetch-recent {:limit 25 :offset 0}]
                  [:user-expenses/refresh-receipts-list]
                  [:app.template.frontend.events.list/clear-selection :receipts]]}))

(rf/reg-event-fx
  :user-expenses/post-selected
  common-interceptors
  (fn [{:keys [db]} [receipt-ids]]
    (let [ids (normalize-selected-receipt-ids receipt-ids)]
      (if (seq ids)
        (post-selected-next-fx db ids [] [])
        {:db (-> db
               (assoc-in [:user-expenses :form :loading?] false)
               (assoc-in [:user-expenses :form :error] "Select at least one receipt to post.")
               (assoc-in (conj base-path :action-loading?) false)
               (assoc-in (conj base-path :error) nil))}))))

(rf/reg-event-fx
  :user-expenses/post-selected-receipt-loaded
  common-interceptors
  (fn [{:keys [db]} [receipt-id remaining succeeded failed response]]
    (let [receipt (:data response)
          db* (-> db
                (assoc-in (conj base-path :error) nil)
                (cond-> receipt
                  (assoc-in (conj base-path :by-id receipt-id) receipt)))
          values (normalize-approve-values db* receipt)
          validation (norm/validate-expense-values values)]
      (if (:ok? validation)
        {:db db*
         :http-xhrio (x/xhrio db*
                       {:method :post
                        :uri (str endpoints/receipts-endpoint "/" receipt-id "/approve")
                        :params (norm/prepare-expense-submit-values values)
                        :on-success [:user-expenses/post-selected-approve-success receipt-id remaining succeeded failed]
                        :on-failure [:user-expenses/post-selected-approve-failure receipt-id remaining succeeded failed]})}
        (let [message (or (:error validation) "Receipt is missing required fields.")]
          (post-selected-next-fx db* remaining succeeded (conj failed {:id (str receipt-id)
                                                                       :error message})))))))

(rf/reg-event-fx
  :user-expenses/post-selected-receipt-load-failure
  common-interceptors
  (fn [{:keys [db]} [receipt-id remaining succeeded failed error]]
    (log/warn "Failed to load receipt during batch post" {:receipt-id receipt-id
                                                          :error error})
    (let [message (http/extract-error-message error)]
      (post-selected-next-fx db
        remaining
        succeeded
        (conj failed {:id (str receipt-id)
                      :error message})))))

(rf/reg-event-fx
  :user-expenses/post-selected-approve-success
  common-interceptors
  (fn [{:keys [db]} [receipt-id remaining succeeded failed response]]
    (let [expense (get-in response [:data :expense])
          receipt (get-in response [:data :receipt])]
      (cond-> (post-selected-next-fx
                (cond-> db
                  receipt
                  (assoc-in (conj base-path :by-id receipt-id) receipt))
                remaining
                (conj succeeded (str receipt-id))
                failed)
        expense
        (assoc :dispatch [::expenses-sync/upsert-expenses [expense]])))))

(rf/reg-event-fx
  :user-expenses/post-selected-approve-failure
  common-interceptors
  (fn [{:keys [db]} [receipt-id remaining succeeded failed error]]
    (log/warn "Failed to approve receipt during batch post" {:receipt-id receipt-id
                                                             :error error})
    (let [message (http/extract-error-message error)]
      (post-selected-next-fx db
        remaining
        succeeded
        (conj failed {:id (str receipt-id)
                      :error message})))))

;; ---------------------------------------------------------------------------
;; Save receipt review (no approve/post)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/save-receipt-review
  common-interceptors
  (fn [{:keys [db]} [receipt-id form-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil)
           (assoc-in (conj base-path :action-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri (str endpoints/receipts-endpoint "/" receipt-id "/review")
                    :params form-data
                    :on-success [:user-expenses/save-receipt-review-success receipt-id on-success]
                    :on-failure [:user-expenses/save-receipt-review-failure]})}))

(rf/reg-event-fx
  :user-expenses/save-receipt-review-success
  common-interceptors
  (fn [{:keys [db]} [receipt-id on-success response]]
    (let [receipt (get-in response [:data :receipt])
          fx (cond-> []
               on-success (conj [:dispatch-later {:ms 100}
                                 :dispatch [:user-expenses/call-modal-callback on-success]]))]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (assoc-in (conj base-path :action-loading?) false)
             (assoc-in (conj base-path :error) nil)
             (cond-> receipt
               (assoc-in (conj base-path :by-id receipt-id) receipt)))
       :dispatch-n [[:user-expenses/refresh-receipts-list]
                    [:user-expenses/fetch-receipt receipt-id]]
       :fx fx})))

(rf/reg-event-db
  :user-expenses/save-receipt-review-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to save receipt review" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error))
      (assoc-in (conj base-path :action-loading?) false)
      (assoc-in (conj base-path :error) (http/extract-error-message error)))))

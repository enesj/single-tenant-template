(ns app.domain.frontend.expenses.events.user-expenses.receipts
  "User receipts inbox events (list/detail/approve)."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.components.user-expense-form.normalization :as norm]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.shared.bridges.crud :as crud-bridges]
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
        normalized (norm/normalize-receipt-data receipt)
        payer-id (some-> (:payer_id normalized) str str/trim not-empty)]
    (cond-> normalized
      (and (nil? payer-id) default-payer)
      (assoc :payer_id default-payer))))

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

(defn- parse-pos-int
  [value]
  (cond
    (number? value) (when (pos? value) (long value))
    (string? value) (let [n (js/parseInt value 10)]
                      (when (and (number? n) (not (js/isNaN n)) (pos? n))
                        (long n)))
    :else nil))

(defn- normalize-status-filter
  [status-filter]
  (letfn [(->status [value]
            (cond
              (map? value) (recur (or (:value value) (get value "value")))
              (keyword? value) (name value)
              (string? value) (some-> value str/trim not-empty)
              :else (some-> value str str/trim not-empty)))]
    (cond
      (vector? status-filter)
      (let [statuses (->> status-filter
                       (map ->status)
                       (remove nil?)
                       vec)]
        (when (seq statuses)
          statuses))

      :else
      (->status status-filter))))

(defn- current-receipts-page-params
  [db]
  (let [entity-key :receipts
        per-page (or (parse-pos-int (get-in db (paths/list-per-page entity-key)))
                   (parse-pos-int (get-in db (conj (paths/list-ui-state entity-key) :per-page)))
                   (parse-pos-int (get-in db (conj (paths/list-ui-state entity-key) :pagination :per-page)))
                   10)
        current-page (or (parse-pos-int (get-in db (paths/list-current-page entity-key)))
                       (parse-pos-int (get-in db (conj (paths/list-ui-state entity-key) :current-page)))
                       (parse-pos-int (get-in db (conj (paths/list-ui-state entity-key) :pagination :current-page)))
                       1)
        active-filters (or (get-in db (paths/list-filters entity-key)) {})
        status (normalize-status-filter (:status active-filters))
        sort-config (or (get-in db (paths/list-sort-config entity-key)) {})
        order-dir (let [direction (:direction sort-config)]
                    (when (contains? #{:asc :desc "asc" "desc"} direction)
                      (name (keyword direction))))]
    (cond-> {:limit per-page
             :offset (* (max 0 (dec current-page)) per-page)}
      (some? status) (assoc :status status)
      (some? order-dir) (assoc :order-dir order-dir))))

;; -----------------------------------------------------------------------------
;; Template CRUD bridge overrides
;;
;; The template list-view delete/batch-delete buttons dispatch template CRUD events
;; for an entity keyword (e.g. :receipts). For user pages (/receipts), we must
;; route deletions to the user receipts API:
;;   - DELETE /api/v1/expenses/receipts/:id
;;   - DELETE /api/v1/expenses/receipts/batch
;; and NOT to the generic template entity endpoints (/api/v1/entities/*), which
;; are deny-by-default for non-admin users.
;; -----------------------------------------------------------------------------

(crud-bridges/register-crud-bridge!
  {:entity-key :receipts
   :bridge-id :expenses-user-receipts
   :priority 90
   ;; IMPORTANT: User receipts pages live outside the admin UI. Do not route
   ;; deletes through the template entity endpoints (/api/v1/entities/*), which
   ;; are deny-by-default and will be blocked for regular users.
   :context-pred (fn [db]
                   (not (crud-bridges/in-admin-context? db)))
   :operations
   {:delete
    {:request (fn [{:keys [db]} entity-type id default-effect]
                (assoc default-effect
                  :db (assoc-in db (paths/entity-loading? entity-type) true)
                  :http-xhrio
                  (x/xhrio db
                    {:method :delete
                     :uri (str endpoints/receipts-endpoint "/" id)

                     :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                     :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]})))
     :on-success (fn [{:keys [db]} entity-type _id default-effect]
                   (assoc default-effect
                     :db (-> db
                           (assoc-in (paths/entity-loading? entity-type) false)
                           (assoc-in (paths/entity-error entity-type) nil))
                     :dispatch [:user-expenses/refresh-receipts-list]))}

    :batch-delete
    {:request (fn [{:keys [db]} entity-type ids default-effect]
                (assoc default-effect
                  :db (assoc-in db (paths/entity-loading? entity-type) true)
                  :http-xhrio
                  (x/xhrio db
                    {:method :delete
                     :uri (str endpoints/receipts-endpoint "/batch")
                     :params {:ids ids}

                     :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids]
                     :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids]})))
     :on-success (fn [{:keys [db]} entity-type _ids default-effect]
                   (assoc default-effect
                     :db (-> db
                           (assoc-in (paths/entity-loading? entity-type) false)
                           (assoc-in (paths/entity-error entity-type) nil))
                     :dispatch [:user-expenses/refresh-receipts-list]))}}})

;; ---------------------------------------------------------------------------
;; List receipts
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/refresh-receipts-list
  common-interceptors
  (fn [{:keys [db]} _]
    {:dispatch [:user-expenses/fetch-receipts (current-receipts-page-params db)]}))

(rf/reg-event-fx
  :user-expenses/fetch-receipts
  common-interceptors
  (fn [{:keys [db]} [payload]]
    (let [{:keys [limit offset status order-dir]} (or payload {})
          limit* (or limit 50)
          offset* (or offset 0)]
      {:db (-> db
             (assoc-in (paths/entity-loading? :receipts) true)
             (assoc-in (paths/entity-error :receipts) nil)
             (assoc-in (conj base-path :loading?) true)
             (assoc-in (conj base-path :error) nil)
             (assoc-in (conj base-path :limit) limit*)
             (assoc-in (conj base-path :offset) offset*))
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/receipts-endpoint

                      :params (cond-> {:limit limit* :offset offset*}
                                (some? status) (assoc :status status)
                                (some? order-dir) (assoc :order-dir order-dir))
                      :on-success [:user-expenses/fetch-receipts-success]
                      :on-failure [:user-expenses/fetch-receipts-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-receipts-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [rows (or (:data response) [])
          total (or (:total response) (count rows))
          limit (or (:limit response) (get-in db (conj base-path :limit)))
          offset (or (:offset response) (get-in db (conj base-path :offset)))]
      {:db (-> db
             (assoc-in (paths/entity-loading? :receipts) false)
             (assoc-in (paths/entity-error :receipts) nil)
             (assoc-in (conj base-path :loading?) false)
             (assoc-in (conj base-path :error) nil)
             (assoc-in (conj base-path :items) (vec rows))
             (assoc-in (conj base-path :total) total)
             (assoc-in (conj base-path :limit) limit)
             (assoc-in (conj base-path :offset) offset)
             (assoc-in (paths/list-total-items :receipts) total))
       :dispatch [::expenses-sync/sync-receipts rows]})))

(rf/reg-event-db
  :user-expenses/fetch-receipts-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch receipts" {:error error})
    (-> db
      (assoc-in (paths/entity-loading? :receipts) false)
      (assoc-in (paths/entity-error :receipts) (http/extract-error-message error))
      (assoc-in (conj base-path :loading?) false)
      (assoc-in (conj base-path :error) (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Receipt detail modal (UX parity with /admin/receipts)
;; ---------------------------------------------------------------------------

(rf/reg-event-db
  :user-expenses/open-receipt-detail-modal
  common-interceptors
  (fn [db [receipt-id]]
    (-> db
      (assoc-in (conj base-path :detail-modal :open?) true)
      (assoc-in (conj base-path :detail-modal :entity-id) (some-> receipt-id str)))))

(rf/reg-event-db
  :user-expenses/close-receipt-detail-modal
  common-interceptors
  (fn [db _]
    (-> db
      (assoc-in (conj base-path :detail-modal :open?) false)
      (assoc-in (conj base-path :detail-modal :entity-id) nil))))

;; ---------------------------------------------------------------------------
;; Receipt detail
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-receipt
  common-interceptors
  (fn [{:keys [db]} [receipt-id]]
    {:db (-> db
           (assoc-in (conj base-path :detail-loading?) true)
           (assoc-in (conj base-path :error) nil))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri (str endpoints/receipts-endpoint "/" receipt-id)

                    :on-success [:user-expenses/fetch-receipt-success receipt-id]
                    :on-failure [:user-expenses/fetch-receipt-failure]})}))

(rf/reg-event-db
  :user-expenses/fetch-receipt-success
  common-interceptors
  (fn [db [receipt-id response]]
    (let [receipt (:data response)]
      (-> db
        (assoc-in (conj base-path :detail-loading?) false)
        (assoc-in (conj base-path :error) nil)
        (assoc-in (conj base-path :by-id receipt-id) receipt)))))

(rf/reg-event-db
  :user-expenses/fetch-receipt-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch receipt" {:error error})
    (-> db
      (assoc-in (conj base-path :detail-loading?) false)
      (assoc-in (conj base-path :error) (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Approve & post
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/approve-receipt-from-list
  common-interceptors
  (fn [{:keys [db]} [receipt-id]]
    (let [payers (get-in db [:user-expenses :payers :items])
          settings (get-in db [:user-expenses :settings :data])]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil)
             (assoc-in (conj base-path :action-loading?) true)
             (assoc-in (conj base-path :error) nil)
             (assoc-in (conj base-path :detail-loading?) true))
       :dispatch-n (cond-> []
                     (empty? payers) (conj [:user-expenses/fetch-payers {:limit 100 :offset 0}])
                     (nil? settings) (conj [:user-expenses/fetch-settings]))
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri (str endpoints/receipts-endpoint "/" receipt-id)

                      :on-success [:user-expenses/approve-receipt-from-list-success receipt-id]
                      :on-failure [:user-expenses/approve-receipt-from-list-failure receipt-id]})})))

(rf/reg-event-fx
  :user-expenses/approve-receipt-from-list-success
  common-interceptors
  (fn [{:keys [db]} [receipt-id response]]
    (let [receipt (:data response)
          db* (-> db
                (assoc-in (conj base-path :detail-loading?) false)
                (assoc-in (conj base-path :error) nil)
                (assoc-in (conj base-path :by-id receipt-id) receipt))
          values (normalize-approve-values db* receipt)
          validation (norm/validate-expense-values values)]
      (if (:ok? validation)
        {:db db*
         :dispatch [:user-expenses/approve-receipt
                    receipt-id
                    (norm/prepare-expense-submit-values values)
                    nil]}
        (let [message (:error validation)]
          {:db (-> db*
                 (assoc-in [:user-expenses :form :loading?] false)
                 (assoc-in [:user-expenses :form :error] message)
                 (assoc-in (conj base-path :action-loading?) false)
                 (assoc-in (conj base-path :error) message))})))))

(rf/reg-event-db
  :user-expenses/approve-receipt-from-list-failure
  common-interceptors
  (fn [db [_receipt-id error]]
    (log/warn "Failed to load receipt for approval" {:error error})
    (let [message (http/extract-error-message error)]
      (-> db
        (assoc-in [:user-expenses :form :loading?] false)
        (assoc-in [:user-expenses :form :error] message)
        (assoc-in (conj base-path :detail-loading?) false)
        (assoc-in (conj base-path :action-loading?) false)
        (assoc-in (conj base-path :error) message)))))

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
           (assoc-in [:user-expenses :form :error] nil)
           (assoc-in (conj base-path :action-loading?) false)
           (assoc-in (conj base-path :error) (batch-post-summary-message succeeded failed)))
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
               (assoc-in [:user-expenses :form :error] nil)
               (assoc-in (conj base-path :action-loading?) false)
               (assoc-in (conj base-path :error) "Select at least one receipt to post."))}))))

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
     ;; Refresh the receipts list to show updated status
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
     ;; Refresh the receipts list and clear selection
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

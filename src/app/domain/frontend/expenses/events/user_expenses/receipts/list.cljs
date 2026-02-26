(ns app.domain.frontend.expenses.events.user-expenses.receipts.list
  "Receipt list events, pagination helpers, and user-receipts CRUD bridge."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
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

(def ^:private receipt-processing-statuses
  #{"uploaded" "parsing" "parsed" "extracting"})

(defn- receipt-processing?
  [status]
  (let [normalized-status (cond
                            (keyword? status) (name status)
                            (string? status) status
                            :else (some-> status str))]
    (contains? receipt-processing-statuses normalized-status)))

(defn- receipt-refine-pending?
  [receipt]
  (let [refine-pending (or (:refine-pending receipt)
                         (:refine_pending receipt)
                         (get-in receipt [:raw-extract-json :refine-pending])
                         (get-in receipt [:raw-extract-json :refine_pending])
                         (get-in receipt [:raw_extract_json :refine_pending]))]
    (true? refine-pending)))

(defn- response-has-processing?
  [rows]
  (boolean
    (some (fn [receipt]
            (or (receipt-processing? (or (:status receipt)
                                       (:receipts/status receipt)))
              (receipt-refine-pending? receipt)))
      rows)))

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
                      (name (keyword direction))))
        order-field (when-let [f (:field sort-config)] (name f))]
    (cond-> {:limit per-page
             :offset (* (max 0 (dec current-page)) per-page)}
      (some? status) (assoc :status status)
      (some? order-dir) (assoc :order-dir order-dir)
      (some? order-field) (assoc :order-by order-field))))

;; ---------------------------------------------------------------------------
;; Template CRUD bridge — routes user-receipts delete/batch-delete to the
;; user API (/api/v1/expenses/receipts/*) instead of the deny-by-default
;; generic entity endpoints.
;; ---------------------------------------------------------------------------

(crud-bridges/register-crud-bridge!
  {:entity-key :receipts
   :bridge-id :expenses-user-receipts
   :priority 90
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
  :user-expenses/check-receipts-processing-complete
  common-interceptors
  (fn [{:keys [db]} _]
    (let [check-path (conj base-path :processing-check)
          loading? (true? (get-in db (conj check-path :loading?)))
          refresh-pending? (true? (get-in db (conj check-path :refresh-pending?)))]
      (if (or loading? refresh-pending?)
        {}
        {:db (assoc-in db (conj check-path :loading?) true)
         :http-xhrio (x/xhrio db
                       {:method :get
                        :uri endpoints/receipts-endpoint
                        :params (current-receipts-page-params db)
                        :on-success [:user-expenses/check-receipts-processing-complete-success]
                        :on-failure [:user-expenses/check-receipts-processing-complete-failure]})}))))

(rf/reg-event-fx
  :user-expenses/check-receipts-processing-complete-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [rows (or (:data response) [])
          still-processing? (response-has-processing? rows)
          check-path (conj base-path :processing-check)
          db' (-> db
                (assoc-in (conj check-path :loading?) false)
                (assoc-in (conj check-path :refresh-pending?) (not still-processing?)))]
      (if still-processing?
        {:db db'}
        {:db db'
         :dispatch [:user-expenses/refresh-receipts-list]}))))

(rf/reg-event-db
  :user-expenses/check-receipts-processing-complete-failure
  common-interceptors
  (fn [db [error]]
    (log/debug "Failed to check receipts processing completion" {:error error})
    (assoc-in db (conj base-path :processing-check :loading?) false)))

(rf/reg-event-fx
  :user-expenses/fetch-receipts
  common-interceptors
  (fn [{:keys [db]} [payload]]
    (let [{:keys [limit offset status order-dir order-by]} (or payload {})
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
                                (some? order-dir) (assoc :order-dir order-dir)
                                (some? order-by) (assoc :order-by order-by))
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
             (assoc-in (conj base-path :processing-check :loading?) false)
             (assoc-in (conj base-path :processing-check :refresh-pending?) false)
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
      (assoc-in (conj base-path :processing-check :loading?) false)
      (assoc-in (conj base-path :processing-check :refresh-pending?) false)
      (assoc-in (conj base-path :error) (http/extract-error-message error)))))

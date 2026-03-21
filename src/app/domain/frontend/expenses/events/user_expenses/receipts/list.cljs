(ns app.domain.frontend.expenses.events.user-expenses.receipts.list
  "Receipt list events, pagination helpers, and user-receipts CRUD bridge."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.shared.pagination :as pagination]
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
  ([db]
   (current-receipts-page-params db {:include-status? true}))
  ([db {:keys [include-status?]
        :or {include-status? true}}]
   (let [entity-key :receipts
         per-page (paths/resolved-list-per-page db entity-key 10)
         current-page (paths/resolved-list-current-page db entity-key)
         active-filters (or (get-in db (paths/list-filters entity-key)) {})
         status (normalize-status-filter (:status active-filters))
         ;; Text filters: everything except :status, normalized to simple strings
         text-filters (reduce-kv
                        (fn [acc k v]
                          (if (= k :status)
                            acc
                            (let [normalized (cond
                                               (map? v) (or (:value v) (get v "value"))
                                               (keyword? v) (name v)
                                               (string? v) (some-> v str/trim not-empty)
                                               :else v)]
                              (if (some? normalized)
                                (assoc acc k normalized)
                                acc))))
                        {}
                        active-filters)
         sort-config (or (get-in db (paths/list-sort-config entity-key)) {})
         order-dir (let [direction (:direction sort-config)]
                     (when (contains? #{:asc :desc "asc" "desc"} direction)
                       (name (keyword direction))))
         order-field (when-let [f (:field sort-config)] (name f))]
     (cond-> (merge {:limit per-page
                     :offset (* (max 0 (dec current-page)) per-page)}
               text-filters)
       (and include-status? (some? status)) (assoc :status status)
       (some? order-dir) (assoc :order-dir order-dir)
       (some? order-field) (assoc :order-by order-field)))))

(defn- processing-check-page-params
  [db]
  (current-receipts-page-params db {:include-status? false}))

(defn- apply-refining-status
  [rows]
  (mapv (fn [receipt]
          (if (receipt-refine-pending? receipt)
            (assoc receipt :status "refining")
            receipt))
    (or rows [])))

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
                   (let [route-name (get-in db (paths/current-route-name))
                         admin-route? (and route-name (str/starts-with? (name route-name) "admin"))
                         pathname (when (exists? js/window)
                                    (some-> js/window .-location .-pathname))
                         in-admin-path? (and pathname (str/includes? pathname "/admin"))]
                     (not (or admin-route? in-admin-path?))))
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
                        :params (processing-check-page-params db)
                        :on-success [:user-expenses/check-receipts-processing-complete-success]
                        :on-failure [:user-expenses/check-receipts-processing-complete-failure]})}))))

(rf/reg-event-fx
  :user-expenses/check-receipts-processing-complete-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [rows (apply-refining-status (:data response))
          still-processing? (response-has-processing? rows)
          check-path (conj base-path :processing-check)
          total (or (:total response) (count rows))
          limit (or (:limit response) (get-in db (conj base-path :limit)))
          offset (or (:offset response) (get-in db (conj base-path :offset)))
          db' (-> db
                (assoc-in (conj check-path :loading?) false)
                (assoc-in (conj check-path :refresh-pending?) (not still-processing?))
                (assoc-in (conj base-path :items) rows)
                (assoc-in (conj base-path :total) total)
                (assoc-in (conj base-path :limit) limit)
                (assoc-in (conj base-path :offset) offset)
                (assoc-in (paths/list-total-items :receipts) total))]
      (if still-processing?
        {:db db'
         :dispatch [::expenses-sync/sync-receipts rows]}
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
    (let [request-params (merge {:limit pagination/default-page-size :offset 0} (when (map? payload) payload))
          limit* (:limit request-params)
          offset* (:offset request-params)]
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
                      :params request-params
                      :on-success [:user-expenses/fetch-receipts-success]
                      :on-failure [:user-expenses/fetch-receipts-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-receipts-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [rows (apply-refining-status (:data response))
          total (or (:total response) (count rows))
          limit (or (:limit response) (get-in db (conj base-path :limit)))
          offset (or (:offset response) (get-in db (conj base-path :offset)))]
      {:db (cond-> (-> db
                     (assoc-in (paths/entity-loading? :receipts) false)
                     (assoc-in (paths/entity-error :receipts) nil)
                     (assoc-in (conj base-path :loading?) false)
                     (assoc-in (conj base-path :error) nil)
                     (assoc-in (conj base-path :processing-check :loading?) false)
                     (assoc-in (conj base-path :processing-check :refresh-pending?) false)
                     (assoc-in (conj base-path :items) rows)
                     (assoc-in (conj base-path :total) total)
                     (assoc-in (conj base-path :limit) limit)
                     (assoc-in (conj base-path :offset) offset)
                     (assoc-in (paths/list-total-items :receipts) total))
             (map? (:date-highlights response))
             (assoc-in (conj (paths/list-ui-state :receipts) :date-highlights)
               (:date-highlights response)))
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

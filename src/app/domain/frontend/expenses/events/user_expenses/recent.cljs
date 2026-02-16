(ns app.domain.frontend.expenses.events.user-expenses.recent
  "User expense recent list events."
  (:require
    [app.domain.frontend.expenses.admin.adapters.normalize :as normalize]
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Recent expenses (list)
;; ---------------------------------------------------------------------------

(defn- ->positive-int
  [value fallback]
  (let [parsed (cond
                 (number? value) value
                 (string? value) (js/parseInt value 10)
                 :else fallback)]
    (if (and (number? parsed)
          (not (js/isNaN parsed))
          (pos? parsed))
      (int parsed)
      fallback)))

(defn- ->non-negative-int
  [value fallback]
  (let [parsed (cond
                 (number? value) value
                 (string? value) (js/parseInt value 10)
                 :else fallback)]
    (if (and (number? parsed)
          (not (js/isNaN parsed))
          (>= parsed 0))
      (int parsed)
      fallback)))

(defn- offset->page
  [offset limit]
  (inc (quot (max 0 offset) (max 1 limit))))

(defn- page->offset
  [page limit]
  (* (dec (max 1 page)) (max 1 limit)))

(defn- fetch-recent-fx
  [db {:keys [limit offset]}]
  (let [limit* (->positive-int limit 5)
        offset* (->non-negative-int offset 0)
        page* (offset->page offset* limit*)]
    {:db (-> db
           (assoc-in [:user-expenses :recent :loading?] true)
           (assoc-in [:user-expenses :recent :error] nil)
           (assoc-in [:user-expenses :recent :limit] limit*)
           (assoc-in [:user-expenses :recent :offset] offset*)
           (assoc-in [:user-expenses :recent :page] page*))
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri endpoints/list-endpoint
                    :params {:limit limit* :offset offset*}
                    :on-success [:user-expenses/fetch-recent-success]
                    :on-failure [:user-expenses/fetch-recent-failure]})}))

(rf/reg-event-fx
  :user-expenses/fetch-recent
  common-interceptors
  (fn [{:keys [db]} [{:keys [limit offset]}]]
    (fetch-recent-fx db {:limit limit :offset offset})))

(rf/reg-event-fx
  :user-expenses/recent-go-to-page
  common-interceptors
  (fn [{:keys [db]} [{:keys [page limit]}]]
    (let [limit* (->positive-int (or limit (get-in db [:user-expenses :recent :limit])) 25)
          page* (->positive-int page 1)
          offset* (page->offset page* limit*)]
      (fetch-recent-fx db {:limit limit* :offset offset*}))))

(rf/reg-event-fx
  :user-expenses/fetch-recent-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [data (or (:data response) [])
          total (->non-negative-int (or (:total response)
                                      (get-in response [:pagination :total]))
                  (count data))
          limit (->positive-int (or (:limit response)
                                  (get-in db [:user-expenses :recent :limit]))
                  5)
          offset (->non-negative-int (or (:offset response)
                                       (get-in db [:user-expenses :recent :offset]))
                   0)
          page (offset->page offset limit)]
      {:db (-> db
             (assoc-in [:user-expenses :recent :loading?] false)
             (assoc-in [:user-expenses :recent :error] nil)
             (assoc-in [:user-expenses :recent :items] (mapv normalize/expense->template-entity data))
             (assoc-in [:user-expenses :recent :total] total)
             (assoc-in [:user-expenses :recent :limit] limit)
             (assoc-in [:user-expenses :recent :offset] offset)
             (assoc-in [:user-expenses :recent :page] page))
       ;; Also sync into the shared template entity store so both admin and
       ;; user table views can depend on the same data source.
       :dispatch [::expenses-sync/sync-expenses data]})))

(rf/reg-event-db
  :user-expenses/fetch-recent-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to fetch recent user expenses" {:error error})
    (-> db
      (assoc-in [:user-expenses :recent :loading?] false)
      (assoc-in [:user-expenses :recent :error] (http/extract-error-message error)))))

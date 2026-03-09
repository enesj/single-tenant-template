(ns app.domain.frontend.expenses.events.user-expenses.search
  "Search events — cross-entity search with client-side debounce."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Debounce atom — survives hot-reload, one timer shared across the page
;; ---------------------------------------------------------------------------

(defonce ^:private debounce-timer (atom nil))

;; ---------------------------------------------------------------------------
;; Init
;; ---------------------------------------------------------------------------

(rf/reg-event-db
  :user-expenses/init-search
  common-interceptors
  (fn [db _]
    (when @debounce-timer
      (js/clearTimeout @debounce-timer)
      (reset! debounce-timer nil))
    (assoc-in db [:user-expenses :search]
              {:query nil
               :loading? false
               :results {}
               :selected nil
               :related nil
               :related-loading? false})))

;; ---------------------------------------------------------------------------
;; Query / debounce
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/set-search-query
  common-interceptors
  (fn [{:keys [db]} [query]]
    (when @debounce-timer
      (js/clearTimeout @debounce-timer))
    (if (and query (>= (count query) 2))
      (do
        (reset! debounce-timer
          (js/setTimeout
            #(rf/dispatch [:user-expenses/fetch-search-results query])
            300))
        {:db (-> db
               (assoc-in [:user-expenses :search :query] query)
               (assoc-in [:user-expenses :search :loading?] true))})
      {:db (-> db
             (assoc-in [:user-expenses :search :query] query)
             (assoc-in [:user-expenses :search :loading?] false)
             (assoc-in [:user-expenses :search :results] {})
             (assoc-in [:user-expenses :search :selected] nil)
             (assoc-in [:user-expenses :search :related] nil)
             (assoc-in [:user-expenses :search :related-loading?] false))})))

;; ---------------------------------------------------------------------------
;; HTTP fetch
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-search-results
  common-interceptors
  (fn [_ [query]]
    {:http-xhrio (http/api-request
                   {:method :get
                    :uri (str endpoints/list-endpoint "/search")
                    :params {"q" query}
                    :on-success [:user-expenses/search-results-success]
                    :on-failure [:user-expenses/search-results-failure]})}))

(rf/reg-event-db
  :user-expenses/search-results-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:user-expenses :search :loading?] false)
      (assoc-in [:user-expenses :search :results] (:results response))
      (assoc-in [:user-expenses :search :selected] nil)
      (assoc-in [:user-expenses :search :related] nil)
      (assoc-in [:user-expenses :search :related-loading?] false))))

(rf/reg-event-db
  :user-expenses/search-results-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Search failed" {:error error})
    (-> db
      (assoc-in [:user-expenses :search :loading?] false)
      (assoc-in [:user-expenses :search :results] {}))))

;; ---------------------------------------------------------------------------
;; Result selection
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/select-search-result
  common-interceptors
  (fn [{:keys [db]} [selected]]
    (let [types-with-related #{:articles :suppliers :stores :payers :expense-cats :manufacturers :categories :subcategories}
          has-related? (contains? types-with-related (keyword (:type selected)))]
      (cond-> {:db (-> db
                     (assoc-in [:user-expenses :search :selected] selected)
                     (assoc-in [:user-expenses :search :related] nil)
                     (assoc-in [:user-expenses :search :related-loading?] has-related?))}
        has-related?
        (assoc :dispatch [:user-expenses/fetch-search-related selected])))))

(rf/reg-event-db
  :user-expenses/clear-search-selection
  common-interceptors
  (fn [db _]
    (assoc-in db [:user-expenses :search :selected] nil)))

;; ---------------------------------------------------------------------------
;; Related records fetch
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-search-related
  common-interceptors
  (fn [_ [{:keys [type id]}]]
    {:http-xhrio (http/api-request
                   {:method :get
                    :uri (str endpoints/list-endpoint "/search/related")
                    :params {"type" (name type) "id" (str id)}
                    :on-success [:user-expenses/search-related-success]
                    :on-failure [:user-expenses/search-related-failure]})}))

(rf/reg-event-db
  :user-expenses/search-related-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:user-expenses :search :related-loading?] false)
      (assoc-in [:user-expenses :search :related] (:related response)))))

(rf/reg-event-db
  :user-expenses/search-related-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Related records fetch failed" {:error error})
    (-> db
      (assoc-in [:user-expenses :search :related-loading?] false)
      (assoc-in [:user-expenses :search :related] {}))))

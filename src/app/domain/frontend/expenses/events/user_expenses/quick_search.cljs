(ns app.domain.frontend.expenses.events.user-expenses.quick-search
  "Quick-search events — lightweight global catalog search for the smart expense form.
   Debounced HTTP calls to /expenses/quick-search, results stored flat and scored."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Debounce atom — one timer for quick-search, separate from main search
;; ---------------------------------------------------------------------------

(defonce ^:private debounce-timer (atom nil))

;; ---------------------------------------------------------------------------
;; Query / debounce
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/quick-search
  common-interceptors
  (fn [{:keys [db]} [query]]
    (when @debounce-timer
      (js/clearTimeout @debounce-timer))
    (if (and query (>= (count query) 2))
      (do
        (reset! debounce-timer
          (js/setTimeout
            #(rf/dispatch [:user-expenses/fetch-quick-search query])
            250))
        {:db (-> db
               (assoc-in [:user-expenses :quick-search :query] query)
               (assoc-in [:user-expenses :quick-search :loading?] true))})
      {:db (-> db
             (assoc-in [:user-expenses :quick-search :query] query)
             (assoc-in [:user-expenses :quick-search :loading?] false)
             (assoc-in [:user-expenses :quick-search :results] []))})))

;; ---------------------------------------------------------------------------
;; HTTP fetch
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-quick-search
  common-interceptors
  (fn [_ [query]]
    {:http-xhrio (http/api-request
                   {:method :get
                    :uri endpoints/quick-search-endpoint
                    :params {"q" query}
                    :on-success [:user-expenses/quick-search-success]
                    :on-failure [:user-expenses/quick-search-failure]})}))

(rf/reg-event-db
  :user-expenses/quick-search-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:user-expenses :quick-search :loading?] false)
      (assoc-in [:user-expenses :quick-search :results] (or (:results response) [])))))

(rf/reg-event-db
  :user-expenses/quick-search-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Quick search failed" {:error error})
    (-> db
      (assoc-in [:user-expenses :quick-search :loading?] false)
      (assoc-in [:user-expenses :quick-search :results] []))))

;; ---------------------------------------------------------------------------
;; Clear
;; ---------------------------------------------------------------------------

(rf/reg-event-db
  :user-expenses/clear-quick-search
  common-interceptors
  (fn [db _]
    (assoc-in db [:user-expenses :quick-search]
              {:query nil :loading? false :results []})))

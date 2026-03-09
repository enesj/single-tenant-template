(ns app.admin.frontend.events.search
  "Admin search events mirroring tenant search UX against global admin endpoints."
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private search-uri "/admin/api/expenses/search")
(def ^:private related-uri "/admin/api/expenses/search/related")
(def ^:private types-with-related
  #{:articles :suppliers :stores :payers :expense-cats :manufacturers :categories :subcategories})

(defonce ^:private debounce-timer (atom nil))

(rf/reg-event-db
  :admin/init-search
  common-interceptors
  (fn [db _]
    (when @debounce-timer
      (js/clearTimeout @debounce-timer)
      (reset! debounce-timer nil))
    (assoc-in db [:admin :search]
              {:query nil
               :loading? false
               :results {}
               :selected nil
               :related nil
               :related-loading? false})))

(rf/reg-event-fx
  :admin/set-search-query
  common-interceptors
  (fn [{:keys [db]} [query]]
    (when @debounce-timer
      (js/clearTimeout @debounce-timer))
    (if (and query (>= (count query) 2))
      (do
        (reset! debounce-timer
          (js/setTimeout
            #(rf/dispatch [:admin/fetch-search-results query])
            300))
        {:db (-> db
               (assoc-in [:admin :search :query] query)
               (assoc-in [:admin :search :loading?] true))})
      {:db (-> db
             (assoc-in [:admin :search :query] query)
             (assoc-in [:admin :search :loading?] false)
             (assoc-in [:admin :search :results] {})
             (assoc-in [:admin :search :selected] nil)
             (assoc-in [:admin :search :related] nil)
             (assoc-in [:admin :search :related-loading?] false))})))

(rf/reg-event-fx
  :admin/fetch-search-results
  common-interceptors
  (fn [_ [query]]
    {:http-xhrio (admin-http/admin-get
                   {:uri search-uri
                    :params {"q" query}
                    :on-success [:admin/search-results-success]
                    :on-failure [:admin/search-results-failure]})}))

(rf/reg-event-db
  :admin/search-results-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:admin :search :loading?] false)
      (assoc-in [:admin :search :results] (:results response))
      (assoc-in [:admin :search :selected] nil)
      (assoc-in [:admin :search :related] nil)
      (assoc-in [:admin :search :related-loading?] false))))

(rf/reg-event-db
  :admin/search-results-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Admin search failed" {:error error})
    (-> db
      (assoc-in [:admin :search :loading?] false)
      (assoc-in [:admin :search :results] {}))))

(rf/reg-event-fx
  :admin/select-search-result
  common-interceptors
  (fn [{:keys [db]} [selected]]
    (let [has-related? (contains? types-with-related (keyword (:type selected)))]
      (cond-> {:db (-> db
                     (assoc-in [:admin :search :selected] selected)
                     (assoc-in [:admin :search :related] nil)
                     (assoc-in [:admin :search :related-loading?] has-related?))}
        has-related?
        (assoc :dispatch [:admin/fetch-search-related selected])))))

(rf/reg-event-db
  :admin/clear-search-selection
  common-interceptors
  (fn [db _]
    (assoc-in db [:admin :search :selected] nil)))

(rf/reg-event-fx
  :admin/fetch-search-related
  common-interceptors
  (fn [_ [{:keys [type id]}]]
    {:http-xhrio (admin-http/admin-get
                   {:uri related-uri
                    :params {"type" (name type)
                             "id" (str id)}
                    :on-success [:admin/search-related-success]
                    :on-failure [:admin/search-related-failure]})}))

(rf/reg-event-db
  :admin/search-related-success
  common-interceptors
  (fn [db [response]]
    (-> db
      (assoc-in [:admin :search :related-loading?] false)
      (assoc-in [:admin :search :related] (:related response)))))

(rf/reg-event-db
  :admin/search-related-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Admin related records fetch failed" {:error error})
    (-> db
      (assoc-in [:admin :search :related-loading?] false)
      (assoc-in [:admin :search :related] {}))))
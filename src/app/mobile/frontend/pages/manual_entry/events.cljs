(ns app.mobile.frontend.pages.manual-entry.events
  "Re-frame events/subscriptions for the mobile manual-entry page."
  (:require
    [ajax.core :as ajax]
    [app.domain.frontend.expenses.shared.manual-entry.core :as manual-entry]
    [clojure.string :as str]
    [re-frame.core :as rf]))

(defn- entity-search-uri [entity-type]
  (case entity-type
    :supplier "/api/v1/expenses/suppliers"
    :store "/api/v1/expenses/stores"
    :article "/api/v1/expenses/articles"
    :category "/api/v1/expenses/expense-categories"
    :expense-category "/api/v1/expenses/expense-categories"
    :payer "/api/v1/expenses/payers"
    (str "/api/v1/expenses/" (name entity-type) "s")))

(defonce ^:private search-timer (atom nil))

(rf/reg-event-fx
  :mobile/quick-search
  (fn [{:keys [db]} [_ query]]
    (when-let [timer @search-timer]
      (js/clearTimeout timer))
    (let [q (some-> query str str/trim)]
      (if (>= (count (or q "")) 2)
        (do (reset! search-timer
              (js/setTimeout
                #(rf/dispatch [:mobile/quick-search-fetch q])
                250))
          {:db (-> db
                 (assoc-in [:mobile :quick-search :query] q)
                 (assoc-in [:mobile :quick-search :loading?] true))})
        {:db (assoc-in db [:mobile :quick-search]
                       {:query q :loading? false :results []})}))))

(rf/reg-event-fx
  :mobile/quick-search-fetch
  (fn [_ [_ query]]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/quick-add-search"
                  :params {:type "all" :q query}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/quick-search-success]
                  :on-failure [:mobile/quick-search-failure]}}))

(rf/reg-event-db
  :mobile/quick-search-success
  (fn [db [_ response]]
    (-> db
      (assoc-in [:mobile :quick-search :loading?] false)
      (assoc-in [:mobile :quick-search :results] (vec (or (:results response) []))))))

(rf/reg-event-db
  :mobile/quick-search-failure
  (fn [db _]
    (-> db
      (assoc-in [:mobile :quick-search :loading?] false)
      (assoc-in [:mobile :quick-search :results] []))))

(rf/reg-sub
  :mobile/quick-search-results
  (fn [db _]
    (get-in db [:mobile :quick-search :results] [])))

(rf/reg-sub
  :mobile/quick-search-loading?
  (fn [db _]
    (get-in db [:mobile :quick-search :loading?] false)))

(rf/reg-event-fx
  :mobile/fetch-payers
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/payers"
                  :params {:limit 100 :offset 0}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/payers-loaded]
                  :on-failure [:mobile/payers-failed]}}))

(rf/reg-event-db
  :mobile/payers-loaded
  (fn [db [_ response]]
    (assoc-in db [:mobile :payers] (vec (or (:data response) [])))))

(rf/reg-event-db
  :mobile/payers-failed
  (fn [db _]
    (assoc-in db [:mobile :payers] [])))

(rf/reg-sub
  :mobile/payers
  (fn [db _]
    (get-in db [:mobile :payers] [])))

(rf/reg-event-fx
  :mobile/fetch-expense-categories
  (fn [_ _]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/expense-categories"
                  :params {:limit 500 :offset 0}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/expense-categories-loaded]
                  :on-failure [:mobile/expense-categories-failed]}}))

(rf/reg-event-db
  :mobile/expense-categories-loaded
  (fn [db [_ response]]
    (assoc-in db [:mobile :expense-categories] (vec (or (:data response) [])))))

(rf/reg-event-db
  :mobile/expense-categories-failed
  (fn [db _]
    (assoc-in db [:mobile :expense-categories] [])))

(rf/reg-sub
  :mobile/expense-categories
  (fn [db _]
    (get-in db [:mobile :expense-categories] [])))

(rf/reg-event-fx
  :mobile/fetch-quick-add-history
  (fn [_ [_ supplier-id]]
    {:http-xhrio {:method :get
                  :uri "/api/v1/expenses/quick-add-history"
                  :params (cond-> {:limit 10}
                            supplier-id (assoc :supplier_id (str supplier-id)))
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:mobile/quick-add-history-loaded]
                  :on-failure [:mobile/quick-add-history-failed]}}))

(rf/reg-event-db
  :mobile/quick-add-history-loaded
  (fn [db [_ response]]
    (assoc-in db [:mobile :quick-add-history]
              {:loading? false
               :loaded? true
               :stores (vec (or (:stores response) []))
               :articles (vec (or (:articles response) []))})))

(rf/reg-event-db
  :mobile/quick-add-history-failed
  (fn [db _]
    (assoc-in db [:mobile :quick-add-history]
              {:loading? false
               :loaded? true
               :stores []
               :articles []})))

(rf/reg-sub
  :mobile/quick-add-history
  (fn [db _]
    (get-in db [:mobile :quick-add-history]
      {:loading? false
       :loaded? false
       :stores []
       :articles []})))

(rf/reg-event-fx
  :mobile/fetch-cooccurring-articles
  (fn [{:keys [db]} [_ article-ids supplier-id]]
    (let [ids (vec (keep identity article-ids))]
      (if (seq ids)
        {:http-xhrio {:method :get
                      :uri "/api/v1/expenses/quick-add-cooccurring"
                      :params (cond-> {:article_ids (str/join "," (map str ids))}
                                supplier-id (assoc :supplier_id (str supplier-id)))
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [:mobile/cooccurring-articles-loaded]
                      :on-failure [:mobile/cooccurring-articles-failed]}}
        {:db (assoc-in db [:mobile :cooccurring-articles]
                       {:loading? false
                        :results []})}))))

(rf/reg-event-db
  :mobile/cooccurring-articles-loaded
  (fn [db [_ response]]
    (assoc-in db [:mobile :cooccurring-articles]
              {:loading? false
               :results (vec (or (:results response) []))})))

(rf/reg-event-db
  :mobile/cooccurring-articles-failed
  (fn [db _]
    (assoc-in db [:mobile :cooccurring-articles]
              {:loading? false
               :results []})))

(rf/reg-sub
  :mobile/cooccurring-articles
  (fn [db _]
    (get-in db [:mobile :cooccurring-articles]
      {:loading? false
       :results []})))

(rf/reg-event-fx
  :mobile/fetch-context-suggestions
  (fn [_ [_ article-ids]]
    (let [ids (vec (keep identity article-ids))]
      (when (seq ids)
        {:http-xhrio {:method :get
                      :uri "/api/v1/expenses/quick-add-context-suggestions"
                      :params {:article_ids (str/join "," (map str ids))}
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [:mobile/context-suggestions-loaded]
                      :on-failure [:mobile/context-suggestions-failed]}}))))

(rf/reg-event-db
  :mobile/context-suggestions-loaded
  (fn [db [_ response]]
    (assoc-in db [:mobile :context-suggestions]
              {:suppliers (vec (or (:suppliers response) []))
               :stores (vec (or (:stores response) []))
               :categories (vec (or (:categories response) []))})))

(rf/reg-event-db
  :mobile/context-suggestions-failed
  (fn [db _]
    (assoc-in db [:mobile :context-suggestions]
              {:suppliers [] :stores [] :categories []})))

(rf/reg-sub
  :mobile/context-suggestions
  (fn [db _]
    (get-in db [:mobile :context-suggestions] {:suppliers [] :stores [] :categories []})))

(rf/reg-event-fx
  :mobile/search-entities
  (fn [{:keys [db]} [_ entity-type query filters]]
    (let [search-query (some-> query str str/trim)
          result-path [:mobile :search-results entity-type]
          supplier-id (some-> (:supplier_id filters) str str/trim not-empty)]
      (if (and search-query (>= (count search-query) 2))
        {:http-xhrio {:method :get
                      :uri (if (= entity-type :article)
                             "/api/v1/expenses/quick-add-search"
                             (entity-search-uri entity-type))
                      :params (if (= entity-type :article)
                                (cond-> {:type "article"
                                         :q search-query
                                         :limit 10}
                                  supplier-id (assoc :supplier_id supplier-id))
                                {:search search-query
                                 :per_page 10})
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [:mobile/search-results entity-type]
                      :on-failure [:mobile/search-results entity-type nil]}}
        {:db (assoc-in db result-path [])}))))

(rf/reg-event-db
  :mobile/search-results
  (fn [db [_ entity-type results]]
    (assoc-in db [:mobile :search-results entity-type]
              (vec (or (:data results) (:results results) results [])))))

(rf/reg-sub
  :mobile/search-results
  (fn [db [_ entity-type]]
    (get-in db [:mobile :search-results entity-type] [])))

(rf/reg-event-fx
  :mobile/create-expense
  (fn [{:keys [db]} [_ form-data]]
    (let [{:keys [items context currency purchased-at payer-id notes]} form-data
          body (manual-entry/prepare-submit-values
                 {:items items
                  :context context
                  :currency (or currency "BAM")
                  :purchased-at purchased-at
                  :payer-id payer-id
                  :notes notes})]
      {:db (-> db
             (assoc-in [:mobile :manual-entry :submitting?] true)
             (assoc-in [:mobile :manual-entry :error] nil))
       :http-xhrio {:method :post
                    :uri "/api/v1/expenses"
                    :params body
                    :format (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:mobile/create-expense-success]
                    :on-failure [:mobile/create-expense-failure]}})))

(rf/reg-event-fx
  :mobile/create-expense-success
  (fn [{:keys [db]} _]
    {:db (-> db
           (assoc-in [:mobile :manual-entry :submitting?] false)
           (assoc-in [:mobile :manual-entry :error] nil))
     :fx [[:dispatch [:mobile/show-toast :mobile/toast-expense-created]]
          [:dispatch [:mobile/navigate "/m/expenses"]]]}))

(rf/reg-event-db
  :mobile/create-expense-failure
  (fn [db [_ error]]
    (-> db
      (assoc-in [:mobile :manual-entry :submitting?] false)
      (assoc-in [:mobile :manual-entry :error]
        (get-in error [:response :error])))))

(rf/reg-sub
  :mobile/manual-entry-submitting?
  (fn [db _]
    (get-in db [:mobile :manual-entry :submitting?] false)))

(rf/reg-sub
  :mobile/manual-entry-error
  (fn [db _]
    (get-in db [:mobile :manual-entry :error])))

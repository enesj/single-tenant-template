(ns app.domain.frontend.expenses.events.user-expenses.quick-add-search
  "Dedicated backend-filtered search for Quick Add expense context inputs."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defonce ^:private debounce-timers (atom {}))

(defn- cancel-timer! [entity-type]
  (when-let [timer (get @debounce-timers entity-type)]
    (js/clearTimeout timer)
    (swap! debounce-timers dissoc entity-type)))

(defn- entity-path [entity-type]
  [:user-expenses :quick-add-search entity-type])

(defn- related-path []
  [:user-expenses :quick-add-related])

(defn- related-type-param [entity-type]
  (case entity-type
    :supplier "suppliers"
    :store "stores"
    nil))

(defn- search-params [entity-type query filters]
  (cond-> {:type (name entity-type)
           :q query}
    (and (contains? #{:store :all :article} entity-type)
      (some? (:supplier_id filters)))
    (assoc :supplier_id (:supplier_id filters))))

(rf/reg-event-fx
  :user-expenses/quick-add-search
  common-interceptors
  (fn [{:keys [db]} [entity-type query filters]]
    (cancel-timer! entity-type)
    (let [query* (some-> query str str/trim)]
      (if (>= (count (or query* "")) 2)
        (do
          (swap! debounce-timers assoc entity-type
            (js/setTimeout
              #(rf/dispatch [:user-expenses/fetch-quick-add-search entity-type query* filters])
              250))
          {:db (-> db
                 (assoc-in (conj (entity-path entity-type) :query) query*)
                 (assoc-in (conj (entity-path entity-type) :loading?) true))})
        {:db (assoc-in db (entity-path entity-type)
                       {:query query*
                        :loading? false
                        :results []})}))))

(rf/reg-event-fx
  :user-expenses/fetch-quick-add-search
  common-interceptors
  (fn [_ [entity-type query filters]]
    {:http-xhrio (http/api-request
                   {:method :get
                    :uri endpoints/quick-add-search-endpoint
                    :params (search-params entity-type query filters)
                    :on-success [:user-expenses/quick-add-search-success entity-type]
                    :on-failure [:user-expenses/quick-add-search-failure entity-type]})}))

(rf/reg-event-db
  :user-expenses/quick-add-search-success
  common-interceptors
  (fn [db [entity-type response]]
    (-> db
      (assoc-in (conj (entity-path entity-type) :loading?) false)
      (assoc-in (conj (entity-path entity-type) :results) (vec (or (:results response) []))))))

(rf/reg-event-db
  :user-expenses/quick-add-search-failure
  common-interceptors
  (fn [db [entity-type error]]
    (log/warn "Quick Add search failed" {:entity-type entity-type :error error})
    (-> db
      (assoc-in (conj (entity-path entity-type) :loading?) false)
      (assoc-in (conj (entity-path entity-type) :results) []))))

(rf/reg-event-db
  :user-expenses/clear-quick-add-search
  common-interceptors
  (fn [db [entity-type]]
    (cancel-timer! entity-type)
    (assoc-in db (entity-path entity-type)
              {:query nil
               :loading? false
               :results []})))

(rf/reg-event-fx
  :user-expenses/fetch-quick-add-related
  common-interceptors
  (fn [{:keys [db]} [entity-type entity-id limit]]
    (if (and entity-type entity-id (related-type-param entity-type))
      {:db (assoc-in db (related-path)
                     {:entity-type entity-type
                      :entity-id (str entity-id)
                      :loading? true
                      :related {}})
       :http-xhrio (http/api-request
                     {:method :get
                      :uri endpoints/related-search-endpoint
                      :params {"type" (related-type-param entity-type)
                               "id" (str entity-id)
                               "limit" (or limit 8)}
                      :on-success [:user-expenses/quick-add-related-success entity-type (str entity-id)]
                      :on-failure [:user-expenses/quick-add-related-failure entity-type (str entity-id)]})}
      {:db (assoc-in db (related-path)
                     {:entity-type nil
                      :entity-id nil
                      :loading? false
                      :related {}})})))

(rf/reg-event-db
  :user-expenses/quick-add-related-success
  common-interceptors
  (fn [db [entity-type entity-id response]]
    (assoc-in db (related-path)
              {:entity-type entity-type
               :entity-id entity-id
               :loading? false
               :related (or (:related response) {})})))

(rf/reg-event-db
  :user-expenses/quick-add-related-failure
  common-interceptors
  (fn [db [entity-type entity-id error]]
    (log/warn "Quick Add related fetch failed" {:entity-type entity-type :entity-id entity-id :error error})
    (assoc-in db (related-path)
              {:entity-type entity-type
               :entity-id entity-id
               :loading? false
               :related {}})))

(rf/reg-event-db
  :user-expenses/clear-quick-add-related
  common-interceptors
  (fn [db _]
    (assoc-in db (related-path)
              {:entity-type nil
               :entity-id nil
               :loading? false
               :related {}})))

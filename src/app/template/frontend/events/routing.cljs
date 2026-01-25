(ns app.template.frontend.events.routing
  "Routing events; update for navigation side effects."
  (:require
    [app.template.frontend.db.db :as db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.crud :as crud-events]
    [app.template.frontend.events.list.filters :as filter-events]
    [app.template.frontend.events.list.selection :as selection-events]
    [app.template.frontend.routing.router :as router-util]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [reitit.frontend.controllers :as rtfc]
    [reitit.frontend.easy :as rtfe]
    [taoensso.timbre :as log]))

;; Events
(rf/reg-event-fx
  :page/init-home
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :home)}))

(rf/reg-event-fx
  :page/cleanup
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (update-in db (butlast (paths/current-page)) dissoc (last (paths/current-page)))}))

(rf/reg-event-fx
  :page/init-about
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :about)}))

(rf/reg-event-fx
  :page/init-entities
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :entities)}))
     ;; Remove fetch-entities dispatch - the entities component handles this
     ;; based on route parameters, not UI state

(rf/reg-event-fx
  :page/init-entity-detail
  common-interceptors
  (fn [{:keys [db]} [_ entity-name]]
    (if entity-name
      {:db (-> db
             (assoc-in (paths/current-page) :entity-detail)
             (assoc-in [:ui :entity-name] (keyword entity-name)))
       :dispatch-n [[::crud-events/fetch-entities (keyword entity-name)]
                    [::filter-events/clear-filter-modal]]}
      {:db (assoc-in db (paths/current-page) :entity-detail)})))

(rf/reg-event-fx
  :page/init-entity-update
  common-interceptors
  (fn [{:keys [db]} [_ entity-name item-id]]
    {:db (-> db
           (assoc-in (paths/current-page) :entity-detail)
           (assoc-in [:ui :entity-name] (keyword entity-name))
           (assoc-in [:ui :editing-id] item-id))
     :dispatch-n [[::selection-events/fetch-item-by-id (keyword entity-name) item-id]
                  [::filter-events/clear-filter-modal]]}))

(rf/reg-event-fx
  :page/init-entity-add
  common-interceptors
  (fn [{:keys [db]} [_ entity-name]]
    (if entity-name
      {:db (-> db
             (assoc-in (paths/current-page) :entity-detail)
             (assoc-in [:ui :entity-name] (keyword entity-name))
             (assoc-in [:ui :show-add-form] true))
       :dispatch-n
       [[::crud-events/fetch-entities (keyword entity-name)]
        [::form-events/clear-form-errors (keyword entity-name)]
        [:app.template.frontend.events.config/set-show-add-form true]
        [::filter-events/clear-filter-modal]]}
      {:db (-> db
             (assoc-in (paths/current-page) :entity-detail)
             (assoc-in [:ui :show-add-form] true))})))

;; User expense tracking page events

(defn- role-str
  [db]
  (let [role (or (get-in db [:session :auth-session :user :role])
               (get-in db [:session :user :role]))]
    (cond
      (keyword? role) (name role)
      (string? role) role
      :else nil)))

(defn- unassigned?
  [db]
  (= (role-str db) "unassigned"))

(defn- redirect-to-waiting-room
  [db]
  {:db (assoc-in db (paths/current-page) :waiting-room)
   :dispatch [:navigate-to "/waiting-room"]})

(rf/reg-event-fx
  :page/init-waiting-room
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :waiting-room)}))

(rf/reg-event-fx
  :page/init-expenses-dashboard
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :expenses-dashboard)
       :dispatch [:user-expenses/init-dashboard]})))

(rf/reg-event-fx
  :page/init-expenses-list
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :expenses-list)
       ;; Ensure we don't accidentally show the generic CRUD add-form in the
       ;; user expenses list (it's driven by a global template flag).
       :dispatch-n [[:app.template.frontend.events.config/set-show-add-form false]
                    [:app.template.frontend.events.config/set-editing nil]]})))

(rf/reg-event-fx
  :page/init-expense-upload
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :expense-upload)})))

(rf/reg-event-fx
  :page/init-receipts-list
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :receipts-list)
       :dispatch [:user-expenses/fetch-receipts {:limit 50 :offset 0}]})))

(rf/reg-event-fx
  :page/init-receipt-detail
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :receipt-detail)})))

(rf/reg-event-fx
  :page/init-expense-new
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :expense-new)
       :dispatch-n [[:user-expenses/fetch-suppliers {:limit 100}]
                    [:user-expenses/fetch-payers {:limit 100}]]})))

(rf/reg-event-fx
  :page/init-expense-detail
  common-interceptors
  (fn [{:keys [db]} [_ expense-id]]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (-> db
             (assoc-in (paths/current-page) :expense-detail)
             (assoc-in [:ui :current-expense-id] expense-id))
       :dispatch (when expense-id [:user-expenses/fetch-expense expense-id])})))

(rf/reg-event-fx
  :page/init-expense-reports
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :expense-reports)
       :dispatch-n [[:user-expenses/fetch-summary]
                    [:user-expenses/fetch-by-month {:months-back 6}]
                    [:user-expenses/fetch-by-supplier {:limit 10}]]})))

(rf/reg-event-fx
  :page/init-expense-settings
  common-interceptors
  (fn [{:keys [db]} _]
    (let [role (role-str db)
          power-user? (contains? #{"admin" "owner"} role)]
      (cond
        (unassigned? db)
        (redirect-to-waiting-room db)

        (not power-user?)
        {:db (assoc-in db (paths/current-page) :expenses-dashboard)
         :dispatch [:navigate-to "/expenses"]}

        :else
        {:db (assoc-in db (paths/current-page) :expense-settings)
         :dispatch-n [[:user-expenses/fetch-settings]
                      [:user-expenses/fetch-payers {:limit 100}]]}))))

(rf/reg-event-fx
  :page/init-expense-suppliers
  common-interceptors
  (fn [{:keys [db]} _]
    (let [role (get-in db [:session :user :role])
          role-str (if (keyword? role) (name role) role)
          unassigned? (= role-str "unassigned")]
      (if unassigned?
        {:db (assoc-in db (paths/current-page) :waiting-room)
         :dispatch [:navigate-to "/waiting-room"]}
        {:db (assoc-in db (paths/current-page) :expense-suppliers)
         :dispatch-n [[:app.template.frontend.events.config/set-show-add-form false]
                      [:app.template.frontend.events.config/set-editing nil]
                      [:user-expenses/fetch-suppliers]]}))))

(rf/reg-event-fx
  :page/init-expense-payers
  common-interceptors
  (fn [{:keys [db]} _]
    (let [role (get-in db [:session :user :role])
          role-str (if (keyword? role) (name role) role)
          unassigned? (= role-str "unassigned")]
      (if unassigned?
        {:db (assoc-in db (paths/current-page) :waiting-room)
         :dispatch [:navigate-to "/waiting-room"]}
        {:db (assoc-in db (paths/current-page) :expense-payers)
         :dispatch-n [[:app.template.frontend.events.config/set-show-add-form false]
                      [:app.template.frontend.events.config/set-editing nil]
                      [:user-expenses/fetch-payers]]}))))

(rf/reg-event-fx
  :page/init-expense-items
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :expense-items)})))

(rf/reg-event-fx
  :page/init-expense-articles
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :expense-articles)})))

(rf/reg-event-fx
  :page/init-expense-manufacturers
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :expense-manufacturers)})))

(rf/reg-event-fx
  :page/init-expense-article-aliases
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :expense-article-aliases)})))

(rf/reg-event-fx
  :page/init-expense-manufacturer-aliases
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :expense-manufacturer-aliases)})))

(rf/reg-event-fx
  :page/init-expense-supplier-aliases
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :expense-supplier-aliases)})))

(rf/reg-event-fx
  :page/init-expense-price-observations
  common-interceptors
  (fn [{:keys [db]} _]
    (if (unassigned? db)
      (redirect-to-waiting-room db)
      {:db (assoc-in db (paths/current-page) :expense-price-observations)})))

(rf/reg-event-fx
  :page/init-unmapped-items
  common-interceptors
  (fn [{:keys [db]} _]
    ;; Keep routing init simple: page components handle auth/role gating.
    ;; This avoids relying on a specific db path for session/user.
    {:db (assoc-in db (paths/current-page) :unmapped-items)}))

(rf/reg-event-db
  :navigated
  common-interceptors
  (fn [db [new-match controllers]]
    (let [data (:data new-match)
          route-name (or (:name data) (:name new-match))]
      (log/info "routing :navigated" {:route-name route-name
                                      :data-keys (keys data)})
      (-> db
        (assoc :current-route new-match)
        (assoc :controllers controllers)))))

(rf/reg-fx
 :routing/push-state
  (fn [route-or-path]
    (try
      (cond
        ;; Preferred: explicit route-name navigation
        (vector? route-or-path) (apply rtfe/push-state route-or-path)
        (keyword? route-or-path) (rtfe/push-state route-or-path)

        ;; Backwards-compatible: allow UI code to pass a string path ("/foo/bar?x=1")
        (string? route-or-path)
        (let [location (.-location js/globalThis)
              origin (or (some-> location .-origin) "http://localhost")
              url (js/URL. route-or-path origin)
              path (.-pathname url)
              query-params (let [sp (js/URLSearchParams. (.-search url))
                                 entries (js/Array.from (.entries sp))]
                             (when (pos? (.-length entries))
                               (into {}
                                 (map (fn [[k v]] [(keyword k) v])
                                   (js->clj entries)))))
              match (router-util/match-by-path path)
              route-name (get-in match [:data :name])
              path-params (get-in match [:parameters :path])]
          (if route-name
            (rtfe/push-state route-name path-params query-params)
            (do
              (log/warn "No route match for path navigation" {:path route-or-path})
              (when location
                (set! (.-href location) route-or-path)))))

        :else
        (do
          (log/warn "Unsupported navigation target" {:to route-or-path})
          nil))
      (catch :default e
        (log/error e "Failed to navigate" {:to route-or-path})))))

(rf/reg-event-fx
  :navigate-to
  common-interceptors
  (fn [{:keys [db]} [new-match]]
    (cond
      (map? new-match)
      (let [old-match (:current-route db)
            controllers (rtfc/apply-controllers (:controllers old-match) new-match)]
        {:dispatch [:navigated new-match controllers]})

      ;; Programmatic navigation from UI code (pages, buttons)
      (or (string? new-match) (keyword? new-match) (vector? new-match))
      {:routing/push-state new-match}

      (nil? new-match)
      {}

      :else
      (do
        (log/warn "Unsupported :navigate-to target" {:target new-match})
        {}))))

;; Subscriptions
(rf/reg-sub
  :current-route
  (fn [db]
    (:current-route db)))

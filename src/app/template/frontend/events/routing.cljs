(ns app.template.frontend.events.routing
  (:require
    [app.template.frontend.db.db :as db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.crud :as crud-events]
    [app.template.frontend.events.list.filters :as filter-events]
    [app.template.frontend.events.list.selection :as selection-events]
    [re-frame.core :as rf]
    [reitit.frontend.controllers :as rtfc]
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
  :page/init-subscription
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :subscription)
     :dispatch [:subscription/initialize]}))

(rf/reg-event-fx
  :page/init-entities
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :entities)
     ;; Remove fetch-entities dispatch - the entities component handles this
     ;; based on route parameters, not UI state
     }))

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
(rf/reg-event-fx
  :page/init-waiting-room
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :waiting-room)}))

(rf/reg-event-fx
  :page/init-expenses-dashboard
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :expenses-dashboard)
     :dispatch [:user-expenses/init-dashboard]}))

(rf/reg-event-fx
  :page/init-expenses-list
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :expenses-list)}))

(rf/reg-event-fx
  :page/init-expense-upload
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :expense-upload)}))

(rf/reg-event-fx
  :page/init-expense-new
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :expense-new)
     :dispatch-n [[:user-expenses/fetch-suppliers {:limit 100}]
                  [:user-expenses/fetch-payers {:limit 100}]]}))

(rf/reg-event-fx
  :page/init-expense-detail
  common-interceptors
  (fn [{:keys [db]} [_ expense-id]]
    {:db (-> db
           (assoc-in (paths/current-page) :expense-detail)
           (assoc-in [:ui :current-expense-id] expense-id))
     :dispatch (when expense-id [:user-expenses/fetch-expense expense-id])}))

(rf/reg-event-fx
  :page/init-expense-reports
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :expense-reports)
     :dispatch-n [[:user-expenses/fetch-summary]
                  [:user-expenses/fetch-by-month {:months-back 6}]
                  [:user-expenses/fetch-by-supplier {:limit 10}]]}))

(rf/reg-event-fx
  :page/init-expense-settings
  common-interceptors
  (fn [{:keys [db]} _]
    {:db (assoc-in db (paths/current-page) :expense-settings)
     :dispatch-n [[:user-expenses/fetch-settings]
                  [:user-expenses/fetch-payers {:limit 100}]]}))

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

(rf/reg-event-fx
  :navigate-to
  common-interceptors
  (fn [{:keys [db]} [new-match]]
    (let [old-match (:current-route db)
          controllers (rtfc/apply-controllers (:controllers old-match) new-match)]
      {:dispatch [:navigated new-match controllers]})))

;; Subscriptions
(rf/reg-sub
  :current-route
  (fn [db]
    (:current-route db)))

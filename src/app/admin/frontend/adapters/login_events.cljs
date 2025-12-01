(ns app.admin.frontend.adapters.login-events
  "Adapter for login events to work with the template system"
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.db.paths :as paths]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; Direct load event used by admin events namespace

(rf/reg-event-fx
  ::load-login-events-direct
  (fn [{:keys [db]} [_ {:as params}]]
    (let [metadata-path (paths/entity-metadata :login-events)]
      (if (adapters.core/admin-token db)
        {:db (assoc-in db (conj metadata-path :loading?) true)
         :http-xhrio (admin-http/admin-get
                       {:uri "/admin/api/login-events"
                        :params params
                        :on-success [::login-events-loaded]
                        :on-failure [::login-events-load-failed]})}
        {:db (-> db
               (assoc-in (conj metadata-path :loading?) false)
               (assoc-in (conj metadata-path :error) "Authentication required"))}))))

;; Transform login event rows into template entities

(defn login-event->template-entity
  "Normalize login event data for the template entity store.
   Ensure IDs are strings and avoid namespaced duplicates."
  [event]
  (-> event
    (update :id #(when % (str %)))
    (update :principal-id #(when % (str %)))))

;; Register entity spec subscription and sync event for :login-events

(adapters.core/register-entity-spec-sub!
  {:entity-key :login-events})

(adapters.core/register-sync-event!
  {:event-id ::sync-login-events-to-template
   :entity-key :login-events
   :normalize-fn login-event->template-entity
   :log-prefix "[login-events] Syncing login events to template system:"})

(rf/reg-event-fx
  ::login-events-loaded
  (fn [{:keys [db]} [_ response]]
    (let [events (get response :events [])
          metadata-path (paths/entity-metadata :login-events)]
      {:db (-> db
             (assoc-in (conj metadata-path :loading?) false)
             (assoc-in (conj metadata-path :error) nil))
       :dispatch-n [[::sync-login-events-to-template events]
                    [:admin/login-events-loaded]]})))

(rf/reg-event-fx
  ::login-events-load-failed
  (fn [{:keys [db]} [_ error]]
    (let [error-msg "Failed to load login events"
          path (paths/entity-metadata :login-events)]
      (log/error "Login events load failed:" error)
      {:db (-> db
             (assoc-in (conj path :loading?) false)
             (assoc-in (conj path :error) error-msg))
       :dispatch [:admin/login-events-load-failed error]})))

(rf/reg-event-fx
  ::initialize-login-events-ui-state
  (fn [{:keys [db]} _]
    (let [metadata-path (paths/entity-metadata :login-events)
          ui-state-path (paths/list-ui-state :login-events)
          selected-ids-path (paths/entity-selected-ids :login-events)
          existing-per-page (or (get-in db (paths/list-per-page :login-events))
                              (get-in db (conj ui-state-path :per-page))
                              (get-in db (conj ui-state-path :pagination :per-page)))
          existing-page (or (get-in db (paths/list-current-page :login-events))
                          (get-in db (conj ui-state-path :current-page))
                          (get-in db (conj ui-state-path :pagination :current-page)))
          per-page (or existing-per-page 20)
          page (or existing-page 1)
          db* (adapters.core/assoc-paths db
                [[(conj metadata-path :sort) {:field :created-at :direction :desc}]
                 [(conj metadata-path :pagination) {:page page :per-page per-page}]
                 [(conj metadata-path :filters) {}]
                 [ui-state-path {:sort {:field :created-at :direction :desc}
                                 :current-page page
                                 :per-page per-page
                                 :pagination {:current-page page :per-page per-page}}]
                 [(paths/list-per-page :login-events) per-page]
                 [(paths/list-current-page :login-events) page]
                 [selected-ids-path #{}]])
          fetch-config (adapters.core/maybe-fetch-config db)]
      (cond-> {:db db*}
        fetch-config (assoc :dispatch-n [fetch-config])
        :else (assoc :dispatch-n [])))))

(defn init-login-events-adapter!
  "Initialize the login events adapter for template system integration"
  []
  (rf/dispatch [::initialize-login-events-ui-state]))

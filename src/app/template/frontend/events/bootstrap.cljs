(ns app.template.frontend.events.bootstrap
  "Infrastructure and bootstrap events for the template frontend.

   Handles application initialization, theme management, and core setup."
  (:require
    [ajax.core :as ajax]
    [app.template.frontend.events.auth :as auth-events]
    [app.template.frontend.events.config :as config-events]
    [app.template.frontend.interceptors.persistence :as persistence]
    [app.template.frontend.db.db :as db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; AJAX Setup Events
;; ========================================================================

(rf/reg-event-fx
  ::setup-ajax-common-interceptors
  common-interceptors
  (fn [_ [token]]
    (when token
      (let [token-interceptor (ajax/to-interceptor
                                {:name "CSRF Token"
                                 :request #(assoc-in % [:headers "X-CSRF-Token"] token)})]
        (swap! ajax/default-interceptors concat [token-interceptor])))
    {}))

(rf/reg-event-fx
  ::extract-csrf-token
  (fn [{:keys [db]} _]
    (if-let [token (-> js/document
                     (.querySelector "meta[name='csrf-token']")
                     (.getAttribute "content"))]
      {:db (assoc db :csrf-token token)
       :dispatch [::setup-ajax-common-interceptors token]}
      {:db db})))

;; ========================================================================
;; Database Initialization
;; ========================================================================

(rf/reg-event-fx
  ::initialize-db
  common-interceptors
  (fn [_ _]
    (log/debug "Initializing DB")
    (let [;; Define default UI config
          default-ui-config {:show-timestamps? true
                             :show-edit? true
                             :show-delete? true
                             :show-highlights? true
                             :show-select? true
                             :controls {:show-timestamps-control? true
                                        :show-edit-control? true
                                        :show-delete-control? true
                                        :show-highlights-control? true
                                        :show-select-control? true
                                        :show-invert-selection? true
                                        :show-delete-selected? true}}
          initial-db (-> db/default-db
                       (assoc-in [:ui :defaults] default-ui-config)
                       ;; Add empty entity-specific configs
                       (assoc-in [:ui :entity-configs] {})
                       ;; Initialize session data
                       (assoc-in [:session] {:loading? true})
                       ;; Keep existing UI settings for backward compatibility
                       (assoc-in [:ui :show-timestamps?] false)
                       (assoc-in [:ui :show-edit?] true)
                       (assoc-in [:ui :show-delete?] true)
                       (assoc-in [:ui :show-highlights?] true)
                       (assoc-in [:ui :show-select?] false)
                       (assoc-in [:ui :controls :show-timestamps-control?] true)
                       (assoc-in [:ui :controls :show-edit-control?] false)
                       (assoc-in [:ui :controls :show-delete-control?] true)
                       (assoc-in [:ui :controls :show-highlights-control?] true)
                       (assoc-in [:ui :controls :show-select-control?] false))]
      {:db initial-db
       :fx [[:dispatch [::persistence/load-stored-prefs]]
            [:dispatch [::auth-events/fetch-auth-status]]
            [:dispatch [::config-events/fetch-config]]]})))

;; ========================================================================
;; UI Helper Events
;; ========================================================================

(rf/reg-event-db
  ::set-entity-type
  common-interceptors
  (fn [db [page]]
    (assoc-in db [:ui :entity-name] page)))

;; ========================================================================
;; Theme Management
;; ========================================================================

(rf/reg-event-fx
  ::initialize-theme
  common-interceptors
  (fn [{:keys [db]} _]
    (let [stored-theme (-> js/localStorage
                         (.getItem "theme")
                         (or "light"))
          html-el (-> js/document .-documentElement)]
      ;; Apply the theme to the HTML element immediately
      (.setAttribute html-el "data-theme" stored-theme)
      (log/debug "Initialized theme to:" stored-theme)
      {:db (assoc-in db [:ui :theme] stored-theme)
       :fx [[:local-storage/set {:key "theme" :value stored-theme}]]})))

(rf/reg-event-fx
  ::set-theme
  common-interceptors
  (fn [{:keys [db]} [theme]]
    (log/debug "Setting theme to" theme)
    (let [theme (str theme)
          html-el (-> js/document .-documentElement)]
      (.setAttribute html-el "data-theme" theme)
      {:db (assoc-in db [:ui :theme] theme)
       :fx [[:local-storage/set {:key "theme" :value theme}]]})))

;; ========================================================================
;; Effects
;; ========================================================================

(rf/reg-fx
 :local-storage/set
  (fn [{:keys [key value]}]
    (js/localStorage.setItem (str key) (str value))))

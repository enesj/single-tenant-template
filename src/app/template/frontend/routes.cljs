(ns app.template.frontend.routes
  (:require
    [app.admin.frontend.core :as admin-core]
    [app.template.frontend.events.bootstrap :as bootstrap-events]
    [app.template.frontend.routing.router :as router-util]
    [app.template.frontend.routes.data :as routes-data]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [reitit.coercion.malli :as rcm]
    [reitit.frontend :as rtf]
    [reitit.frontend.easy :as rtfe]
    [taoensso.timbre :as log]))

(def routes routes-data/app-routes)

(defn- prefer-literal-route-conflicts
  "Resolve reitit path conflicts by preferring literal segments over parameterised ones.
  This lets routes like /expenses/new win over /expenses/:id without blowing up the router."
  [conflicts]
  (let [literal-routes (filter #(not (str/includes? (first %) ":")) conflicts)
        param-routes   (filter #(str/includes? (first %) ":") conflicts)]
    (concat literal-routes param-routes)))

;; Define the router instance early so it's available to usages below
(def router
  (rtf/router routes {:data {:coercion rcm/coercion}
                       :conflicts prefer-literal-route-conflicts}))

(defn on-navigate
  "Handle navigation to new routes with proper error handling"
  [new-match]
  ;; Debug the raw match to diagnose route recognition issues
  (when ^boolean goog.DEBUG
    (try
      ;; Logging code removed for brevity or if desired
      (catch :default _e
        (js/console.warn "on-navigate: logging failed"))))

  ;; Always update the route state first so UI can react. If nil, try manual match.
  (if new-match
    (rf/dispatch [:navigate-to new-match])
    (let [path (.-pathname js/window.location)
          manual (rtf/match-by-path router path)]
      (when ^boolean goog.DEBUG
        (log/info "on-navigate: new-match nil, manual match attempt"
          {:path path :manual-name (or (get-in manual [:data :name]) (:name manual))}))
      (when manual
        (rf/dispatch [:navigate-to manual]))))

  ;; Perform any route-specific side effects when we can detect the name
  (when (map? (or new-match (rtf/match-by-path router (.-pathname js/window.location))))
    (let [route-name (or (get-in new-match [:data :name]) (:name new-match))
          route-name-str (when route-name (name route-name))]

      (when route-name-str
        ;; Lazily initialize admin module when navigating to admin routes
        (when (str/starts-with? route-name-str "admin-")
          (admin-core/init-admin!))
        (case route-name
          ;; Handle home page
          :home (rf/dispatch [:page/init-home])
          :home-explicit (rf/dispatch [:page/init-home])

          ;; Handle generic entity routes
          :entity-detail
          (let [entity-name (get-in new-match [:parameters :path :entity-name])]
            (when entity-name
              (rf/dispatch [::bootstrap-events/set-entity-type (keyword entity-name)])))

          :entity-add
          (let [entity-name (get-in new-match [:parameters :path :entity-name])]
            (when entity-name
              (rf/dispatch [::bootstrap-events/set-entity-type (keyword entity-name)])
              (rf/dispatch [:app.template.frontend.events.config/set-show-add-form true])))

          :entity-update
          (let [entity-name (get-in new-match [:parameters :path :entity-name])
                item-id-str (get-in new-match [:parameters :path :item-id])
                ;; Always attempt to convert to number for consistency
                item-id (if (and (string? item-id-str) (re-matches #"^\d+$" item-id-str))
                          (js/parseInt item-id-str)
                          item-id-str)]
            (when (and entity-name item-id)
              (rf/dispatch [::bootstrap-events/set-entity-type (keyword entity-name)])
              ;; ONLY set the editing ID, don't dispatch init-entity-update here
              ;; This prevents duplicate fetches since the controller will handle the init
              (rf/dispatch [:app.template.frontend.events.config/set-editing item-id])))

          ;; Default - do nothing special for non-entity routes
          nil)))))

;; This function must be exported for core.cljs to use
(defn init-routes! []
  ;; Register the router with the utility namespace so routing events can match paths
  (router-util/set-router! router)

  (rtfe/start!
    router
    on-navigate
    {:use-fragment false}))

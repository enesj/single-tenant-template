(ns app.template.frontend.core
  (:require
    [app.admin.frontend.core :as admin-core]
    [app.admin.frontend.pages.audit :as admin-audit]
    [app.admin.frontend.pages.login :as admin-login]
    [app.admin.frontend.pages.users :as admin-users]
    [app.template.frontend.events.core] ; Load all event handlers
    [app.template.frontend.pages.about :refer [about-page]]
    [app.template.frontend.pages.entities :refer [entities-page]]
    [app.template.frontend.pages.home :refer [home-page]]
    [app.template.frontend.routes :as routes]
    [app.template.frontend.components.auth :refer [auth-component]]
    [app.template.frontend.components.confirm-dialog :refer [confirm-dialog]]
    [app.template.frontend.components.settings.global-settings :refer [settings-panel]]
    [app.template.frontend.components.change-password :refer [change-password-page]]
    [app.template.frontend.pages.login :refer [login-page]]
    [app.template.frontend.pages.register :refer [registration-page registration-success-page]]
    [app.template.frontend.pages.verify-email-success :refer [verify-email-success-page]]
    [app.template.frontend.pages.email-verification :refer [email-verified-page]]
    [app.template.frontend.pages.logout :refer [logout-page]]
    [app.template.frontend.pages.forgot-password :refer [forgot-password-page]]
    [app.template.frontend.pages.reset-password :refer [reset-password-page]]
    [app.template.frontend.pages.subscription :refer [subscription-page]]
    [app.template.frontend.utils.css-reload :as css-reload]
    [cljs.pprint :as pprint]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [re-frame.loggers :as rlog]
    [reitit.frontend :as rtf]
    [reitit.frontend.easy :as rtfe]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui] :as uix]
    [uix.dom :as uix.dom]
    [uix.re-frame :refer [use-subscribe] :as urf]))

(def log-colors
  {:info "\u001B[32m"                                       ; Green
   :warn "\u001B[33m"                                       ; Yellow
   :error "\u001B[31m"                                      ; Red
   :debug "\u001B[36m"                                      ; Cyan
   :reset "\u001B[0m"})                                     ; Reset color

(log/merge-config!
  {:output-fn (fn [{:keys [level ?file ?line vargs]}]
                (let [relative-path (if ?file
                                      (last (str/split ?file #"src/"))
                                      "unknown")
                      color (get log-colors level (:reset log-colors))
                      reset-color (:reset log-colors)
                      data-str (if (or (vector? vargs) (map? vargs))
                                 (str color "[" relative-path ":" ?line "] " ":\n"
                                   (with-out-str (pprint/pprint vargs)) reset-color)
                                 (str color "[" relative-path ":" ?line "] " ": " vargs reset-color))]
                  (.trimEnd data-str)))})

;; Configure logging/console noise level depending on environment
(defn setup-logging! []
  (if ^boolean goog.DEBUG
    ;; Dev: keep info/debug logs
    (log/merge-config! {:min-level :info})
    ;; Prod: quiet console and raise min level to warn
    (do
      ;; Silence common noisy console methods in production
      (let [noop (fn [& _] nil)]
        (doseq [m ["log" "debug" "info" "time" "timeEnd" "trace" "group" "groupCollapsed" "groupEnd"]]
          (aset js/console m noop)))
      ;; Ensure Timbre respects :warn minimum in production
      (log/merge-config! {:min-level :warn}))))

(defn suppress-re-frame-noise! []
  ;; Filter out known noisy re-frame warnings like handler overwrites and verbose :models-data
  (let [orig-warn (.-warn js/console)
        orig-log (.-log js/console)
        orig-err (.-error js/console)
        orig-group (or (.-group js/console) (.-log js/console))
        orig-groupEnd (or (.-groupEnd js/console) (fn [] nil))
        pass (fn [f & args] (.apply f js/console (to-array args)))
        warn* (fn [& args]
                (let [s (when (seq args) (str (first args)))
                      noisy? (and s (re-find #"re-frame: overwriting" s))]
                  (when-not noisy?
                    (apply pass orig-warn args))))
        log* (fn [& args]
               (let [process-arg (fn [arg]
                                   (if (and (map? arg) (contains? arg :models-data))
                                     (let [models-data (:models-data arg)
                                           top-level-keys (keys models-data)
                                           key-summary (str "{:models-data with " (count top-level-keys) " tables: " top-level-keys "}")]
                                       (assoc arg :models-data key-summary))
                                     arg))
                     processed-args (map process-arg args)]
                 (apply pass orig-log processed-args)))
        err* (fn [& args]
               (let [s (when (seq args) (str (first args)))
                     missing-handler? (and s (re-find #"re-frame: no" s))
                     event-id (nth args 3 nil)]
                 (when missing-handler?
                   (pass orig-warn (str "re-frame missing handler for event-id: " (pr-str event-id))))
                 (apply pass orig-err args)))
        group* (fn [& args] (apply pass orig-group args))
        groupEnd* (fn [& args] (apply pass orig-groupEnd args))]
    (rlog/set-loggers! {:warn warn*
                        :log log*
                        :error err*
                        :group group*
                        :groupEnd groupEnd*})))

(defonce last-non-nil-route (atom nil))

(defui current-page []
  (let [current-route (use-subscribe [:current-route])
        effective-route (cond
                          (map? current-route)
                          (do
                            (reset! last-non-nil-route current-route)
                            current-route)

                          @last-non-nil-route
                          @last-non-nil-route

                          :else
                          (let [path (.-pathname js/window.location)
                                manual (rtf/match-by-path routes/router path)]
                            (cond
                              manual
                              (do
                                (reset! last-non-nil-route manual)
                                manual)

                              ;; Fallback: if router lookup fails but URL is an admin
                              ;; users path, synthesize a minimal match so the admin
                              ;; page continues to render instead of going blank/home.
                              (= path "/admin/users")
                              {:data {:name :admin-users
                                      :view admin-users/admin-users-page}}

                              :else
                              nil)))
        entity-name (get-in effective-route [:parameters :path :entity-name])
        ;; Reitit `match` may expose `:name` and `:view` at top-level or under `:data`
        route-name (or (:name (:data effective-route)) (:name effective-route))
        route-view (or (:view (:data effective-route)) (:view effective-route))
        is-admin-route? (and route-name (str/starts-with? (name route-name) "admin"))]

    (when ^boolean goog.DEBUG
      (log/info "current-page route"
        {:route-name route-name
         :is-admin-route? is-admin-route?
         :raw-name (or (get-in effective-route [:data :name]) (:name effective-route))
         :path-params (get-in effective-route [:parameters :path])}))

    ;; Effect to initialize entity data when navigating directly to an entity page
    ;; NOTE: Entity initialization is now handled by route controllers in routes.cljs
    ;; This effect was causing duplicate dispatches and nil entity-name issues
    ;; Removed to prevent conflicts with the route controller system

    ($ :div
      ;; Only show the main navigation bar for non-admin routes
      (when-not is-admin-route?
        ($ :nav {:class "bg-gray-800 text-white p-4"}
          ($ :div {:class "container flex justify-between items-center"}
            ($ :div {:class "flex space-x-4"}
              ($ :a {:id "nav-link-home"
                     :href "/"
                     :on-click (fn [e]
                                 (.preventDefault e)
                                 (rtfe/push-state :home))
                     :class "cursor-pointer text-white hover:text-gray-300"} "Home")
              ($ :a {:id "nav-link-about"
                     :href "/about"
                     :on-click (fn [e]
                                 (.preventDefault e)
                                 (rtfe/push-state :about))
                     :class "cursor-pointer text-white hover:text-gray-300"} "About")
              ($ :a {:id "nav-link-entities"
                     :href "/entities"
                     :on-click (fn [e]
                                 (.preventDefault e)
                                 (rtfe/push-state :entities))
                     :class "cursor-pointer text-white hover:text-gray-300"} "Entities")
              ($ :a {:id "nav-link-subscription"
                     :href "/subscription"
                     :on-click (fn [e]
                                 (.preventDefault e)
                                 (rtfe/push-state :subscription))
                     :class "cursor-pointer text-white hover:text-gray-300"} "Subscription"))

            ;; Auth status with sign-in/sign-out buttons, followed by settings
            ($ :div {:class "flex items-center space-x-4"}
              ;; Auth component with multi-tenant support
              ($ auth-component)

              ;; Global settings control - positioned after auth
              ($ :div {:class "text-white ml-4 px-2 py-1 hover:bg-gray-700 rounded-md transition-colors duration-200"}
                ($ settings-panel {:entity-name entity-name
                                   :global-settings? true}))))))

      ;; Render the appropriate page based on route
      (if is-admin-route?
        ;; For admin routes, render the view when it's a function; otherwise fallback by route-name
        (if (fn? route-view)
          ($ route-view)
          (case route-name
            :admin-login ($ admin-login/admin-login-page)
            :admin-users ($ admin-users/admin-users-page)
            :admin-audit ($ admin-audit/admin-audit-page)
            nil))
        ;; For regular routes, use the existing case statement
        (case (:view (:data effective-route))
          :home ($ :div {:class "ds-container p-4"} ($ home-page))
          :about ($ :div {:class "ds-container p-4"} ($ about-page))
          :entities ($ :div {:class "ds-container p-4"} ($ entities-page))
          :entity-detail ($ :div {:class "ds-container p-4"} ($ entities-page))
          :login ($ login-page)
          :register ($ registration-page)
          :verify-email-success ($ verify-email-success-page)
          :email-verified ($ email-verified-page)
          :logout ($ logout-page)
          :forgot-password ($ forgot-password-page)
          :reset-password ($ reset-password-page)
          :change-password ($ change-password-page)
          :subscription ($ :div {:class "ds-container p-4"} ($ subscription-page))
          ;; If no matching route, default to home page instead of showing 'not found'
          ($ :div {:class "ds-container p-4"} ($ home-page))))

      ;; Add the confirm dialog component to the layout
      ($ confirm-dialog))))

(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

(defn mount-ui
  []
  (uix.dom/render-root ($ current-page) root))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn ^:dev/after-load clear-cache-and-render!
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code. We force a UI update by clearing
  ;; the Reframe subscription cache.
  (rf/clear-subscription-cache!)
  (mount-ui))

(defn init-app! []
  ;; Set up logging first so early logs are filtered appropriately
  (setup-logging!)
  (suppress-re-frame-noise!)
  (rf/dispatch-sync [:app.template.frontend.events.bootstrap/initialize-theme])
  (admin-core/init-admin!)  ;; Initialize admin module
  (routes/init-routes!)
  (mount-ui))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn init                                                  ;; Your app calls this when it starts. See shadow-cljs.edn :init-fn.
  []
  ;; Ensure logging is configured as early as possible
  (setup-logging!)

  (css-reload/reload-css)
  (rf/dispatch-sync [:app.template.frontend.events.bootstrap/initialize-db])
  (rf/dispatch-sync [:app.template.frontend.events.bootstrap/extract-csrf-token])
  (init-app!))

;; Function to run after HMR reloads
#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn after-load []
  (css-reload/reload-css))

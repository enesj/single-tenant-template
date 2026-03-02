(ns app.template.frontend.core
  "Template frontend bootstrap; wire init, routes, root view."
  (:require
    [app.admin.frontend.core :as admin-core]
    [app.admin.frontend.pages.audit :as admin-audit]
    [app.admin.frontend.pages.login :as admin-login]
    [app.admin.frontend.pages.users :as admin-users]
    [app.template.frontend.events.core] ; Load all event handlers
    [app.template.frontend.pages.about :refer [about-page]]
    [app.template.frontend.pages.entities :refer [entities-page]]
    [app.template.frontend.pages.home :refer [home-page]]
    [app.template.frontend.pages.invitation-accept :refer [invitation-accept-page]]
    [app.template.frontend.pages.impersonation-grants :refer [impersonation-grants-page]]
    [app.template.frontend.pages.tenant-members :refer [tenant-members-page]]
    [app.template.frontend.pages.tenant-select :refer [tenant-select-page]]
    [app.template.frontend.pages.waiting-room :refer [waiting-room-page]]
    ;; Domain pages via aggregator (not registry - to avoid circular deps)
    [app.domain.frontend.pages :as domain-pages]
    [app.template.frontend.routes :as routes]
    [app.template.frontend.components.confirm-dialog :refer [confirm-dialog]]
    [app.template.frontend.components.layout :refer [user-layout]]
    [app.template.frontend.components.change-password :refer [change-password-page]]
    [app.template.frontend.pages.login :refer [login-page]]
    [app.template.frontend.pages.register :refer [registration-page]]
    [app.template.frontend.pages.verify-email-success :refer [verify-email-success-page]]
    [app.template.frontend.pages.email-verification :refer [email-verified-page]]
    [app.template.frontend.pages.logout :refer [logout-page]]
    [app.template.frontend.pages.forgot-password :refer [forgot-password-page]]
    [app.template.frontend.pages.reset-password :refer [reset-password-page]]

    [app.template.frontend.utils.css-reload :as css-reload]
    [cljs.pprint :as pprint]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [re-frame.loggers :as rlog]
    [reitit.frontend :as rtf]
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

^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defui current-page []
  (let [current-route (use-subscribe [:current-route])
        ;; Domain pages map - loaded from aggregator
        all-domain-pages domain-pages/all-pages
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
        ;; Reitit `match` may expose `:name` and `:view` at top-level or under `:data`
        route-name (or (:name (:data effective-route)) (:name effective-route))
        route-view (or (:view (:data effective-route)) (:view effective-route))
        is-admin-route? (and route-name (str/starts-with? (name route-name) "admin"))
        ;; Get view keyword for domain page lookup
        view-key (get-in effective-route [:data :view])
        auth-view? (contains? #{:login
                                :register
                                :verify-email
                                :verify-email-success
                                :email-verified
                                :forgot-password
                                :reset-password
                                :waiting-room
                                :tenant-select
                                :invitation-accept}
                     view-key)
        page-el (if is-admin-route?
                  ;; For admin routes, render the view when it's a function; otherwise fallback by route-name
                  (if (fn? route-view)
                    ($ route-view)
                    (case route-name
                      :admin-login ($ admin-login/admin-login-page)
                      :admin-users ($ admin-users/admin-users-page)
                      :admin-audit ($ admin-audit/admin-audit-page)
                      nil))
                  ;; For regular routes, check domain pages first, then template pages
                  (if-let [domain-page-component (get all-domain-pages view-key)]
                    ;; Domain page found - render it
                    ($ domain-page-component)
                    ;; Template pages (core infrastructure)
                    (case view-key
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

                      :waiting-room ($ waiting-room-page)
                      :tenant-select ($ tenant-select-page)
                      :tenant-members ($ tenant-members-page)
                      :tenant-impersonation ($ impersonation-grants-page)
                      :invitation-accept ($ invitation-accept-page)
                      ;; If no matching route, default to home page instead of showing 'not found'
                      ($ :div {:class "ds-container p-4"} ($ home-page)))))]

    (when ^boolean goog.DEBUG
      (log/info "current-page route"
        {:route-name route-name
         :is-admin-route? is-admin-route?
         :raw-name (or (get-in effective-route [:data :name]) (:name effective-route))
         :path-params (get-in effective-route [:parameters :path])}))

    ($ :<>
      (if (and (not is-admin-route?) (not auth-view?))
        ($ user-layout page-el)
        page-el)
      ($ confirm-dialog))))

^{:clj-kondo/ignore [:inline-def :uninitialized-var]}
(defonce root
  (uix.dom/create-root (js/document.getElementById "app")))

^{:clj-kondo/ignore [:inline-def]}
(defn mount-ui
  []
  (uix.dom/render-root ($ current-page) root))

^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
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
  ;; Only initialize admin module when we're on an admin route; on-demand in router for later navigations
  (when (str/starts-with? (.-pathname js/window.location) "/admin")
    (admin-core/init-admin!))
  (routes/init-routes!) (mount-ui))

^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn init                                                  ;; Your app calls this when it starts. See shadow-cljs.edn :init-fn.
  []
  ;; Ensure logging is configured as early as possible
  (setup-logging!)

  (css-reload/reload-css)
  (rf/dispatch-sync [:app.template.frontend.events.bootstrap/initialize-db])
  (rf/dispatch-sync [:app.template.frontend.events.bootstrap/extract-csrf-token])
  (init-app!))

;; Function to run after HMR reloads
^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn after-load []
  (css-reload/reload-css))

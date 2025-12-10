(ns app.template.frontend.routes
  (:require
    [app.admin.frontend.routes :as admin-routes]
    [app.admin.frontend.core :as admin-core]
    [app.template.frontend.events.bootstrap :as bootstrap-events] ;; Import admin routes
    [clojure.string :as str]
    [re-frame.core :as rf]
    [reitit.coercion.spec :as rcs]
    [reitit.frontend :as rtf]
    [reitit.frontend.easy :as rtfe]
    [taoensso.timbre :as log]))

;; Helper functions for route controllers

(defn make-simple-controller
  "Creates a simple controller that dispatches an init event on start and cleanup on stop"
  [init-event]
  [{:start (fn [_] (rf/dispatch [init-event]))
    :stop (fn [_] (rf/dispatch [:page/cleanup]))}])

(defn- extract-entity-name
  "Extract entity name from match-or-identity"
  [match-or-identity]
  (if (map? match-or-identity)
    (get-in match-or-identity [:parameters :path :entity-name])
    (if (vector? match-or-identity)
      (first match-or-identity)                             ;; Handle vector case [entity-name item-id]
      match-or-identity)))                                  ;; Handle string case

(defn- extract-item-id
  "Extract item-id from match-or-identity and ensure it's a number if possible"
  [match-or-identity]
  (when (or (map? match-or-identity) (vector? match-or-identity))
    (let [raw-id (if (map? match-or-identity)
                   (get-in match-or-identity [:parameters :path :item-id])
                   (second match-or-identity))]             ;; Handle vector case [entity-name item-id]
      ;; Convert to number if possible
      (if (and (string? raw-id) (re-matches #"^\d+$" raw-id))
        (js/parseInt raw-id)
        raw-id))))

(defn make-entity-controller
  "Creates a controller for entity routes"
  [route-type]
  (let [identity-fn (case route-type
                      :update (fn [match]
                                [(get-in match [:parameters :path :entity-name])
                                 (get-in match [:parameters :path :item-id])])
                      (fn [match]
                        (get-in match [:parameters :path :entity-name])))
        event-name (case route-type
                     :add :page/init-entity-add
                     :update :page/init-entity-update
                     :detail :page/init-entity-detail)]
    [{:identity identity-fn
      :start (fn [match-or-identity]
               (let [entity-name (extract-entity-name match-or-identity)
                     ;; Extract item-id (already normalized to number in extract-item-id function)
                     item-id (when (= route-type :update)
                               (extract-item-id match-or-identity))]
                 (cond
                   ;; Update needs both entity-name and item-id
                   (and (= route-type :update) entity-name item-id)
                   (rf/dispatch [event-name entity-name item-id])

                   ;; Add and detail only need entity-name
                   (and (not= route-type :update) entity-name (not= entity-name "nil"))
                   (rf/dispatch [event-name entity-name])

                   :else
                   nil)))
      :stop (fn [_] (rf/dispatch [:page/cleanup]))}]))

(defn make-app-entity-controller
  "Creates a controller for app routes that map to entity functionality.
   Maps app route paths to specific entity names."
  [route-type app-path]
  (let [;; Map app paths to entity names
        entity-mapping {"/app/properties" "properties"
                        "/app/transactions" "transactions_v2"
                        "/app/financials" "transactions_v2"
                        "/app/reports" "cohost_balances"
                        "/app/cohosts" "property_cohosts"
                        "/app/users" "users"
                        "/app/invitations" "invitations"}

        entity-name (get entity-mapping app-path)

        identity-fn (case route-type
                      :update (fn [match]
                                [entity-name
                                 (or (get-in match [:parameters :path :property-id])
                                   (get-in match [:parameters :path :transaction-id])
                                   (get-in match [:parameters :path :cohost-id])
                                   (get-in match [:parameters :path :user-id])
                                   (get-in match [:parameters :path :item-id]))])
                      (fn [_match] entity-name))

        event-name (case route-type
                     :add :page/init-entity-add
                     :update :page/init-entity-update
                     :detail :page/init-entity-detail)]

    ;; Log the mapping for debugging
    (when ^boolean js/goog.DEBUG
      (log/info "🔧 App route controller created:"
        {:route-type route-type :app-path app-path :entity-name entity-name :event-name event-name}))

    [{:identity identity-fn
      :start (fn [match-or-identity]
               (when ^boolean js/goog.DEBUG
                 (log/info "🚀 App route controller starting:"
                   {:route-type route-type
                    :app-path app-path
                    :entity-name entity-name
                    :match-or-identity (if (map? match-or-identity)
                                         (select-keys match-or-identity [:parameters :data :path-params])
                                         match-or-identity)}))

               (let [item-id (when (= route-type :update)
                               (or (get-in match-or-identity [:parameters :path :property-id])
                                 (get-in match-or-identity [:parameters :path :transaction-id])
                                 (get-in match-or-identity [:parameters :path :cohost-id])
                                 (get-in match-or-identity [:parameters :path :user-id])
                                 (get-in match-or-identity [:parameters :path :item-id])))]
                 (cond
                   ;; Update needs both entity-name and item-id
                   (and (= route-type :update) entity-name item-id)
                   (do
                     (log/info "🎯 Dispatching update event:" {:entity-name entity-name :item-id item-id})
                     (rf/dispatch [event-name entity-name item-id]))

                   ;; Add and detail only need entity-name
                   (and (not= route-type :update) entity-name (not= entity-name "nil"))
                   (do
                     (log/info "🎯 Dispatching detail/add event:" {:entity-name entity-name :event-name event-name})
                     (rf/dispatch [event-name entity-name]))

                   :else
                   (do
                     (log/warn "❌ App route controller: missing entity mapping or item-id"
                       {:route-type route-type :app-path app-path :entity-name entity-name :item-id item-id})
                     ;; Try to dispatch anyway if we have entity-name
                     (when entity-name
                       (log/info "🔄 Attempting dispatch anyway with entity-name:" entity-name)
                       (rf/dispatch [event-name entity-name]))))))
      :stop (fn [_]
              (when ^boolean js/goog.DEBUG
                (log/info "🛑 App route controller stopping"))
              (rf/dispatch [:page/cleanup]))}]))

;; Define routes using the helper functions
(defn- generate-entity-routes
  "Generate generic entity routes that accept entity name as parameter"
  []
  [["/entities/:entity-name/add"
    {:name :entity-add
     :view :entity-detail
     :parameters {:path {:entity-name string?}}
     :controllers (make-entity-controller :add)}]

   ["/entities/:entity-name"
    {:name :entity-detail
     :view :entity-detail
     :parameters {:path {:entity-name string?}}
     :controllers (make-entity-controller :detail)}]

   ["/entities/:entity-name/update/:item-id"
    {:name :entity-update
     :view :entity-detail
     :parameters {:path {:entity-name string? :item-id string?}}
     :controllers (make-entity-controller :update)}]])

;; Simple redirect controller that navigates to entity URLs
(defn make-redirect-controller
  "Creates a controller that redirects to an entity URL using SPA navigation"
  [entity-name route-type]
  (let [target-path (case route-type
                      :detail (str "/entities/" entity-name)
                      :add (str "/entities/" entity-name "/add")
                      :update (fn [item-id] (str "/entities/" entity-name "/update/" item-id)))]
    [{:start (fn [match-or-identity]
               (when ^boolean js/goog.DEBUG
                 (log/info "🔄 Redirecting app route to entity:"
                   {:entity-name entity-name :route-type route-type :target-path target-path}))

               (if (= route-type :update)
                 ;; For update routes, extract the item-id from parameters
                 (let [item-id (or (get-in match-or-identity [:parameters :path :property-id])
                                 (get-in match-or-identity [:parameters :path :transaction-id])
                                 (get-in match-or-identity [:parameters :path :cohost-id])
                                 (get-in match-or-identity [:parameters :path :user-id])
                                 (get-in match-or-identity [:parameters :path :item-id]))]
                   (when item-id
                     ;; Use reitit navigation
                     (js/setTimeout #(rtfe/push-state (target-path item-id)) 10)))
                 ;; For detail and add routes, redirect directly using reitit navigation
                 (js/setTimeout #(rtfe/push-state target-path) 10)))
      :stop (fn [_] nil)}]))

(defn- generate-app-routes
  "Generate tenant application routes that redirect to entity functionality.
   These provide more intuitive URLs for property hosting workflows."
  []
  [;; App dashboard - redirect to home for now
   ["/app"
    {:name :app-home
     :view :home
     :controllers (make-simple-controller :page/init-home)}]

   ["/app/dashboard"
    {:name :app-dashboard
     :view :home
     :controllers (make-simple-controller :page/init-home)}]

   ;; Property management (redirects to entities/properties)
   ["/app/properties"
    {:name :app-properties
     :view :entity-detail
     :controllers (make-redirect-controller "properties" :detail)}]

   ["/app/properties/add"
    {:name :app-properties-add
     :view :entity-detail
     :controllers (make-redirect-controller "properties" :add)}]

   ["/app/properties/update/:property-id"
    {:name :app-properties-update
     :view :entity-detail
     :parameters {:path {:property-id string?}}
     :controllers (make-redirect-controller "properties" :update)}]

   ;; Financial management (redirects to entities/transactions_v2)
   ["/app/transactions"
    {:name :app-transactions
     :view :entity-detail
     :controllers (make-redirect-controller "transactions_v2" :detail)}]

   ["/app/transactions/add"
    {:name :app-transactions-add
     :view :entity-detail
     :controllers (make-redirect-controller "transactions_v2" :add)}]

   ["/app/transactions/update/:transaction-id"
    {:name :app-transactions-update
     :view :entity-detail
     :parameters {:path {:transaction-id string?}}
     :controllers (make-redirect-controller "transactions_v2" :update)}]

   ;; Financials alias for transactions
   ["/app/financials"
    {:name :app-financials
     :view :entity-detail
     :controllers (make-redirect-controller "transactions_v2" :detail)}]

   ;; Reports and analytics (redirects to entities/cohost_balances)
   ["/app/reports"
    {:name :app-reports
     :view :entity-detail
     :controllers (make-redirect-controller "cohost_balances" :detail)}]

   ;; Co-host management (redirects to entities/property_cohosts)
   ["/app/cohosts"
    {:name :app-cohosts
     :view :entity-detail
     :controllers (make-redirect-controller "property_cohosts" :detail)}]

   ["/app/cohosts/add"
    {:name :app-cohosts-add
     :view :entity-detail
     :controllers (make-redirect-controller "property_cohosts" :add)}]

   ["/app/cohosts/update/:cohost-id"
    {:name :app-cohosts-update
     :view :entity-detail
     :parameters {:path {:cohost-id string?}}
     :controllers (make-redirect-controller "property_cohosts" :update)}]

   ;; User management (redirects to entities/users)
   ["/app/users"
    {:name :app-users
     :view :entity-detail
     :controllers (make-redirect-controller "users" :detail)}]

   ["/app/users/add"
    {:name :app-users-add
     :view :entity-detail
     :controllers (make-redirect-controller "users" :add)}]

   ["/app/users/update/:user-id"
    {:name :app-users-update
     :view :entity-detail
     :parameters {:path {:user-id string?}}
     :controllers (make-redirect-controller "users" :update)}]

   ;; Invitations management (redirects to entities/invitations)
   ["/app/invitations"
    {:name :app-invitations
     :view :entity-detail
     :controllers (make-redirect-controller "invitations" :detail)}]

   ["/app/invitations/add"
    {:name :app-invitations-add
     :view :entity-detail
     :controllers (make-redirect-controller "invitations" :add)}]])

;; ========================================================================
;; User Expense Routes (Role-Gated)
;; ========================================================================

(defn- generate-user-expense-routes
  "Generate user-facing expense tracking routes.
   These routes are role-gated - users without proper role see waiting room."
  []
  [;; Waiting room for unassigned users
   ["/waiting-room"
    {:name :waiting-room
     :view :waiting-room
     :controllers (make-simple-controller :page/init-waiting-room)}]

   ;; User expense dashboard (main entry point)
   ["/expenses"
    {:name :expenses-dashboard
     :view :expenses-dashboard
     :controllers (make-simple-controller :page/init-expenses-dashboard)}]

   ;; Alias for dashboard
   ["/dashboard"
    {:name :user-dashboard
     :view :expenses-dashboard
     :controllers (make-simple-controller :page/init-expenses-dashboard)}]

   ;; Explicit nested dashboard path to avoid catching by /expenses/:expense-id
   ["/expenses/dashboard"
    {:name :expenses-dashboard-alias
     :view :expenses-dashboard
     :controllers (make-simple-controller :page/init-expenses-dashboard)}]

   ;; Expense list with history
   ["/expenses/list"
    {:name :expenses-list
     :view :expenses-list
     :controllers (make-simple-controller :page/init-expenses-list)}]

   ;; Upload receipt
   ["/expenses/upload"
    {:name :expenses-upload
     :view :expenses-upload
     :controllers (make-simple-controller :page/init-expenses-upload)}]

   ;; New expense (manual entry)
   ["/expenses/new"
    {:name :expenses-new
     :view :expenses-new
     :controllers (make-simple-controller :page/init-expenses-new)}]

   ;; Expense detail
   ["/expenses/:expense-id"
    {:name :expenses-detail
     :view :expenses-detail
     :parameters {:path {:expense-id string?}}
     :controllers (make-simple-controller :page/init-expenses-detail)}]

   ;; Reports
   ["/expenses/reports"
    {:name :expenses-reports
     :view :expenses-reports
     :controllers (make-simple-controller :page/init-expenses-reports)}]

   ;; User settings
   ["/expenses/settings"
    {:name :expenses-settings
     :view :expenses-settings
     :controllers (make-simple-controller :page/init-expenses-settings)}]])

;; Define routes using the helper functions
;; Define routes using the helper functions
(def routes
  "Vector of route definitions for the application"
  (into []
    (concat
      [[""
        {:name :home
         :view :home
         :controllers (make-simple-controller :page/init-home)}]

          ;; Add explicit /home route for better UX
       ["/home"
        {:name :home-explicit
         :view :home
         :controllers (make-simple-controller :page/init-home)}]

          ;; Authentication routes
       ["/login"
        {:name :login
         :view :login
         :controllers (make-simple-controller :page/init-login)}]

       ["/register"
        {:name :register
         :view :register
         :controllers (make-simple-controller :page/init-register)}]

       ["/verify-email"
        {:name :verify-email
         :view :verify-email
         :controllers (make-simple-controller :page/init-verify-email)}]

       ["/logout"
        {:name :logout
         :view :logout
         :controllers (make-simple-controller :page/init-logout)}]

       ;; Password reset routes
       ["/forgot-password"
        {:name :forgot-password
         :view :forgot-password
         :controllers (make-simple-controller :page/init-forgot-password)}]

       ["/reset-password"
        {:name :reset-password
         :view :reset-password
         :controllers (make-simple-controller :page/init-reset-password)}]

       ["/change-password"
        {:name :change-password
         :view :change-password
         :controllers (make-simple-controller :page/init-change-password)}]

       ["/onboarding"
        {:name :onboarding
         :view :onboarding
         :controllers (make-simple-controller :page/init-onboarding)}]

       ["/subscription"
        {:name :subscription
         :view :subscription
         :controllers (make-simple-controller :page/init-subscription)}]

          ;; Handle both with and without trailing slash
       ["/about"
        {:name :about
         :view :about
         :controllers (make-simple-controller :page/init-about)}]

       ["/about/"
        {:name :about-slash
         :view :about
         :controllers (make-simple-controller :page/init-about)}]

       ["/verify-email-success"
        {:name :verify-email-success
         :view :verify-email-success
         :controllers (make-simple-controller :page/init-verify-email-success)}]

       ["/email-verified"
        {:name :email-verified
         :view :email-verified}]

          ;; Handle both with and without trailing slash
       ["/entities"
        {:name :entities
         :view :entities
         :controllers (make-simple-controller :page/init-entities)}]

       ["/entities/"
        {:name :entities-slash
         :view :entities
         :controllers (make-simple-controller :page/init-entities)}]]
         ;; Add dynamically generated app routes (aliases to entities)
      (generate-app-routes)
         ;; Add user expense tracking routes
      (generate-user-expense-routes)
         ;; Add dynamically generated entity routes
      (generate-entity-routes)
      ;; Add admin routes
      admin-routes/admin-routes)))

(defn- prefer-literal-route-conflicts
  "Resolve reitit path conflicts by preferring literal segments over parameterised ones.
  This lets routes like /expenses/new win over /expenses/:id without blowing up the router."
  [conflicts]
  (let [literal-routes (filter #(not (str/includes? (first %) ":")) conflicts)
        param-routes   (filter #(str/includes? (first %) ":") conflicts)]
    (concat literal-routes param-routes)))

;; Define the router instance early so it's available to usages below
(def router
  (rtf/router routes {:data {:coercion rcs/coercion}
                      :conflicts prefer-literal-route-conflicts}))

(defn on-navigate
  "Handle navigation to new routes with proper error handling"
  [new-match]
  ;; Debug the raw match to diagnose route recognition issues
  (when ^boolean goog.DEBUG
    (try

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

          ;; Handle generic entity routes - REMOVED DUPLICATE DISPATCHES
          ;; The route controllers will handle these automatically
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

;; Router is defined above

;; This function must be exported for core.cljs to use
(defn init-routes! []
  ;; Start the router
  (when ^boolean goog.DEBUG

    (try
      (letfn [(flatten-routes [acc node prefix]
                (reduce
                  (fn [acc' item]
                    (cond
                      (and (vector? item)
                        (string? (first item)))
                        ;; Route node like ["/path" data & children]
                      (let [part (first item)
                            p (cond
                                (= part "") prefix
                                (str/starts-with? part "/") (str prefix part)
                                (= prefix "") (str "/" part)
                                :else (str prefix "/" part))
                            children (drop 2 item)]
                        (-> acc'
                          (conj p)
                          (flatten-routes children p)))

                      (sequential? item)
                      (flatten-routes acc' item prefix)

                      :else acc'))
                  acc
                  node))]
        (flatten-routes [] routes ""))
      (catch :default _e
        (js/console.warn "init-routes!: route snapshot logging failed"))))
  (rtfe/start!
    router
    on-navigate
    {:use-fragment false}))

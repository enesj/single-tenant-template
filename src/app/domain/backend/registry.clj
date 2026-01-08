(ns app.domain.backend.registry
  "Backend domain registry - provides domain manifests to template/admin.
   
   Each domain manifest contains:
   - :id - domain keyword identifier
   - :routes
     - :admin-api - fn (fn [db service-container] reitit-routes) for admin API
     - :user-api - fn (fn [db wrap-auth-mw app-config] reitit-routes) for user API
   - :ui-config
     - :user - map with paths to domain-owned UI config EDN files
   - :redirects
     - :post-login-path - default redirect after OAuth login
   - :spa-routes - vector of SPA paths to serve index.html
   
   Template/admin import this registry to dynamically compose routes."
  (:require
    [app.domain.backend.expenses.routes.core :as expenses-admin-routes]
    [app.domain.backend.expenses.routes.user-api :as expenses-user-routes]))

(def ^:private expenses-manifest
  {:id :expenses
   :routes
   {:admin-api (fn [db service-container]
                 ;; Extract app-config from service-container for OCR routes
                 (let [app-config (:config service-container)]
                   (expenses-admin-routes/routes db app-config)))
    :user-api (fn [db wrap-user-auth app-config]
                (expenses-user-routes/routes db wrap-user-auth app-config))}
   :ui-config
   {:user {:root-dir "src/app/domain/frontend/expenses/config"
           :paths {:entities "src/app/domain/frontend/expenses/config/entities.edn"
                   :view-options "src/app/domain/frontend/expenses/config/view-options.edn"
                   :form-fields "src/app/domain/frontend/expenses/config/form-fields.edn"
                   :table-columns "src/app/domain/frontend/expenses/config/table-columns.edn"}}}
   :redirects
   {:post-login-path "/expenses"}
   :spa-routes
   ["/waiting-room"
    "/dashboard"
    "/unmapped-items"
    "/expenses"
    "/expenses/list"
    "/expenses/upload"
    "/receipts"
    "/receipts/:receipt-id"
    "/expenses/new"
    "/expenses/reports"
    "/expenses/settings"
    "/expenses/:expense-id"
    "/suppliers"
    "/payers"]})

(def enabled-domains
  "Vector of enabled domain manifests.
   To add a new domain, add its manifest here."
  [expenses-manifest])

(defn get-domain
  "Get a domain manifest by id."
  [domain-id]
  (first (filter #(= domain-id (:id %)) enabled-domains)))

(defn all-admin-api-routes
  "Collect admin API routes from all enabled domains.
   Returns a vector of reitit route vectors."
  [db service-container]
  (mapv (fn [manifest]
          (when-let [route-fn (get-in manifest [:routes :admin-api])]
            (route-fn db service-container)))
    enabled-domains))

(defn- fn-supports-arity?
  "True if `f` has a declared invoke/doInvoke method for `arity`.

  This is used to support both legacy (2-arity) and newer (3-arity) domain route fns."
  [f arity]
  (some (fn [^java.lang.reflect.Method m]
          (and (#{"invoke" "doInvoke"} (.getName m))
            (= arity (count (.getParameterTypes m)))))
    (.getDeclaredMethods (class f))))

(defn- call-user-api-route-fn
  "Invoke a domain `:user-api` route function with the right arity.

  Supported signatures:
  - (fn [db wrap-user-auth] ...)
  - (fn [db wrap-user-auth app-config] ...)
  - (fn [db wrap-user-auth & [app-config]] ...)

  Prefer passing `app-config` when supported."
  [route-fn db wrap-user-auth app-config]
  (cond
    (fn-supports-arity? route-fn 3) (route-fn db wrap-user-auth app-config)
    (fn-supports-arity? route-fn 2) (route-fn db wrap-user-auth)
    ;; Fallback: preserve previous behavior.
    :else (route-fn db wrap-user-auth app-config)))

(defn all-user-api-routes
  "Collect user API routes from all enabled domains.
   Returns a vector of reitit route vectors.

   `app-config` is optional.

   Domain `:user-api` fns support either 2 or 3 args:
   - (fn [db wrap-user-auth] ...)
   - (fn [db wrap-user-auth app-config] ...)
   - (fn [db wrap-user-auth & [app-config]] ...)"
  [db wrap-user-auth & [app-config]]
  (mapv (fn [manifest]
          (when-let [route-fn (get-in manifest [:routes :user-api])]
            (call-user-api-route-fn route-fn db wrap-user-auth app-config)))
    enabled-domains))

(defn get-ui-config-paths
  "Get the UI config paths for all enabled domains.
   Returns a merged map of domain-id -> paths."
  []
  (into {}
    (map (fn [manifest]
           [(:id manifest) (get-in manifest [:ui-config :user :paths])]))
    enabled-domains))

(defn get-post-login-path
  "Get the post-login redirect path from the first domain with one defined.
   Returns \"/\" if no domain specifies a path."
  []
  (or (some #(get-in % [:redirects :post-login-path]) enabled-domains)
    "/"))

(defn all-spa-routes
  "Collect all SPA routes from enabled domains.
   These paths should serve index.html for client-side routing."
  []
  (mapcat :spa-routes enabled-domains))

(ns app.domain.backend.registry
  "Domain backend registry; register domain services/routes."
  (:require
    [app.domain.backend.expenses.routes.core :as expenses-admin-routes]
    [app.domain.backend.expenses.routes.user-api :as expenses-user-api-routes]
    [app.domain.shared.routes.expenses-user :as expenses-user-route-contract]))

(def ^:private expenses-manifest
  {:id :expenses
   :routes
   {:admin-api (fn [db service-container]
                 (expenses-admin-routes/routes db (:config service-container)))
    :user-api (fn [db wrap-user-auth app-config]
                (expenses-user-api-routes/routes db wrap-user-auth app-config))}

   :ui-config
   {:user {:root-dir "src/app/domain/frontend/expenses/config"
           :paths {:entities "src/app/domain/frontend/expenses/config/entities.edn"
                   :view-options "src/app/domain/frontend/expenses/config/view-options.edn"
                   :form-fields "src/app/domain/frontend/expenses/config/form-fields.edn"
               :table-columns "src/app/domain/frontend/expenses/config/table-columns.edn"
               :navigation "src/app/domain/frontend/expenses/config/navigation.edn"}}}

   :redirects
   {:post-login-path "/expenses"}

   :spa-routes expenses-user-route-contract/spa-fallback-paths})

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

(defn all-user-api-routes
  "Collect user API routes from all enabled domains.
   Returns a vector of reitit route vectors.

   `app-config` is optional.

   Domain `:user-api` fns must accept 3 args:
   - (fn [db wrap-user-auth app-config] ...)
   - (fn [db wrap-user-auth & [app-config]] ...)"
  [db wrap-user-auth & [app-config]]
  (mapv (fn [manifest]
          (when-let [route-fn (get-in manifest [:routes :user-api])]
            (route-fn db wrap-user-auth app-config)))
    enabled-domains))

(defn get-ui-config-paths
  "Get the UI config paths for all enabled domains.
   Returns a merged map of domain-id -> paths."
  []
  (into {}
    (map (fn [manifest]
           [(:id manifest) (get-in manifest [:ui-config :user :paths])]))
    enabled-domains))

(defn primary-user-ui-config-paths
  "Return user-facing UI config paths for the primary domain.

   This is a backwards-compat helper for call sites that still treat domain UI
   config as a single flat structure (single-domain setups).

   When multiple domains are enabled, this returns the first domain's paths
   (treating it as the primary domain).

   Returns nil when no domains are enabled."
  []
  (let [all-paths (get-ui-config-paths)]
    (when (seq all-paths)
      (val (first all-paths)))))

(defn get-admin-ui-config-paths
  "Get the ADMIN UI config path maps for all enabled domains.

   Used by the bootstrap path to assemble admin config defaults from
   domain-owned EDN files (view-options/table-columns/form-fields).

   Prefers explicit domain-owned admin config when present (`:ui-config :admin :paths`).
   Falls back to the domain's user UI config paths when no admin paths are provided.

     Returns a vector of maps like:
     [{:view-options <path> :form-fields <path> :table-columns <path>} ...]"
  []
  (into []
    (keep (fn [manifest]
            (or (get-in manifest [:ui-config :admin :paths])
              (get-in manifest [:ui-config :user :paths]))))
    enabled-domains))

(defn get-post-login-path
  "Get the post-login redirect path from the first domain with one defined.
   Returns \"/\" if no domain specifies a path."
  []
  (or (some #(get-in % [:redirects :post-login-path]) enabled-domains)
    "/"))

(defn all-spa-routes
  "Collect all SPA routes from enabled domains.
   These paths should serve index.html for client-side routing.

   Returns distinct paths in stable first-seen order.
   Nil paths and nil domain spa-route collections are ignored."
  []
  (->> enabled-domains
    (mapcat :spa-routes)
    (remove nil?)
    (distinct)
    (vec)))

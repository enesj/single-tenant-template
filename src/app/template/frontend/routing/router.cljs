(ns app.template.frontend.routing.router
  "A tiny indirection layer so non-routing code can match paths without
  directly depending on the router construction site.

  `app.template.frontend.routes/init-routes!` sets the router at startup.
  Other namespaces (e.g. routing events) can then call `match-by-path`.")

(defonce ^:private !router (atom nil))

(defn set-router!
  "Store the active reitit router instance. Safe to call multiple times."
  [router]
  (reset! !router router)
  router)

(defn get-router
  "Return the currently registered router (or nil if routing hasn't initialized)."
  []
  @!router)

(defn match-by-path
  "Match a URL path (e.g. \"/admin/users\") against the active router.

  Returns a reitit match map or nil if no router is registered or no match exists."
  [path]
  (when-let [router (get-router)]
    (reitit.frontend/match-by-path router path)))

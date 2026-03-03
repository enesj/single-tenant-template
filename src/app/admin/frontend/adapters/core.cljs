(ns app.admin.frontend.adapters.core
  "Admin-specific adapters for bridging admin entities to the template system.

  This namespace provides admin-specific context detection and convenience functions
  for registering admin CRUD bridges using the shared bridge infrastructure.

  Generic entity and database utilities live in the template layer:
  - app.template.frontend.shared.utils.entity
  - app.template.frontend.shared.utils.db"
  (:require
    [app.template.frontend.shared.bridges.crud :as shared-bridges]
    [app.template.frontend.db.paths :as paths]
    [clojure.string :as str]))

;; Ensure template CRUD bridge event handlers are registered during adapter load in tests
(defonce ^:private _ensure-bridges-registered
  (do
    (shared-bridges/register-template-crud-events!)
    true))

(defn admin-context?
  "Return true when the current runtime indicates the admin UI context."
  [db]
  (let [route-name (get-in db (paths/current-route-name))
        route-str (cond
                    (keyword? route-name)
                    (if-let [route-ns (namespace route-name)]
                      (str route-ns "/" (name route-name))
                      (name route-name))

                    (string? route-name)
                    route-name

                    :else nil)
        admin-route? (and route-str (str/starts-with? route-str "admin"))
        pathname (when (exists? js/window)
                   (some-> js/window .-location .-pathname))]
    (boolean (or admin-route?
               (and pathname (str/includes? pathname "/admin"))))))

(defn admin-token
  "Return the admin session token from db when available."
  [db]
  (:admin/token db))

;; ============================================================================
;; Admin Bridge Registration
;; ============================================================================

(defn register-admin-crud-bridge!
  "Register admin overrides for template CRUD events.

  This is a convenience function that registers a bridge with :admin bridge-id
  and admin-context? as the default context predicate.

  Expected options:
  - `:entity-key` (keyword, required)
  - `:operations` map keyed by `:delete`, `:create`, and/or `:update`. Each entry may
    provide `:request`, `:on-success`, and `:on-failure` functions that receive
    `(cofx entity-type & args default-effect)` and should return an effects map. When a
    handler returns nil the default template behavior is used.
  - `:context-pred` optional predicate `(fn [db])` controlling when overrides apply.
    Defaults to `admin-context?`.
  - `:priority` optional number for bridge ordering (default 200 - admin gets high priority).

  Returns the bridge configuration for verification."
  [opts]
  (shared-bridges/register-crud-bridge!
    (-> opts
      (assoc :bridge-id :admin)
      (assoc :context-pred (or (:context-pred opts) admin-context?))
      (assoc :priority (or (:priority opts) 200)))))

;; NOTE: Generic entity/db utilities are intentionally NOT re-exported here.
;; Callers should require the underlying template namespaces directly:
;; - app.template.frontend.shared.utils.entity
;; - app.template.frontend.shared.utils.db

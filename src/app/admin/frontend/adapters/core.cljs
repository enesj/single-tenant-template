(ns app.admin.frontend.adapters.core
  "Admin-specific adapters for bridging admin entities to the template system.

  This namespace provides admin-specific context detection and convenience functions
  for registering admin CRUD bridges using the shared bridge infrastructure.

  Generic entity and database utilities have been moved to:
  - app.shared.frontend.utils.entity
  - app.shared.frontend.utils.db"
  (:require
    [app.shared.frontend.bridges.crud :as shared-bridges]
    [app.shared.frontend.utils.db :as db-utils]
    [app.shared.frontend.utils.entity :as entity-utils]
    [clojure.string :as str]))

;; Ensure template CRUD bridge event handlers are registered during adapter load in tests
(defonce ^:private _ensure-bridges-registered
  (do
    (shared-bridges/register-template-crud-events!)
    true))

(defn admin-context?
  "Return true when the current runtime indicates the admin UI context."
  [db]
  (let [pathname (when (exists? js/window)
                   (some-> js/window .-location .-pathname))]
    (boolean (or (:admin/token db)
               (:admin/authenticated? db)
               (and pathname (str/includes? pathname "/admin"))))))

(defn admin-token
  "Return the admin session token from db or browser storage when available."
  [db]
  (or (:admin/token db)
    (when (exists? js/localStorage)
      (.getItem js/localStorage "admin-token"))))

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

;; Re-export shared utilities for backward compatibility
(def normalize-entity entity-utils/normalize-entity)
(def register-entity-spec-sub! entity-utils/register-entity-spec-sub!)
(def register-sync-event! entity-utils/register-sync-event!)
(def assoc-paths db-utils/assoc-paths)
(def maybe-fetch-config db-utils/maybe-fetch-config)

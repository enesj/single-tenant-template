(ns app.admin.frontend.handlers.generic
  "Generic admin entity handlers and hooks (single-tenant, no deletion constraints)."
  (:require
    [app.admin.frontend.security.wrapper :as security]))

(defn create-generic-additional-effects
  "Create entity-specific additional effects (e.g., security wrapper)."
  [entity-config]
  (let [{:keys [features]} entity-config]
    (fn []
      (when (true? (:security-wrapper? features))
        (security/init-security-wrapper!)))))

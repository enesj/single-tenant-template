(ns app.domain.frontend.expenses.admin.config.preload
  "Preload expenses domain entity configurations for admin UI.
   
   This mirrors the pattern in app.admin.frontend.config.preload but
   for domain-owned entity configurations."
  (:require
    [app.admin.frontend.system.entity-registry :as entity-registry]
    [cljs.reader :as reader]
    [shadow.resource :as resource]))

(defonce ^:private domain-entities
  (let [resource-content (resource/inline "app/domain/frontend/expenses/admin/config/entities.edn")]
    (when resource-content
      (let [parsed (reader/read-string resource-content)]
        ;; Domain entities get their :adapter-init-fn from the domain registry,
        ;; not from the EDN. The EDN provides only the UI metadata.
        (into {}
          (map (fn [[entity-key cfg]]
                 ;; Get the init-fn from entity-registry (which was merged from domain-registry)
                 (let [registry-entry (get entity-registry/entity-registry entity-key)
                       registry-init-fn (:init-fn registry-entry)
                       registry-actions (:actions registry-entry)
                       registry-custom-actions (:custom-actions registry-entry)
                       registry-modals (:modals registry-entry)]
                   [entity-key
                    (cond-> cfg
                      registry-init-fn (assoc :adapter-init-fn registry-init-fn)
                      (or registry-actions registry-custom-actions registry-modals)
                      (update :components
                        (fn [components]
                          (let [components (or components {})]
                            (cond-> components
                              registry-actions (assoc :actions registry-actions)
                              registry-custom-actions (assoc :custom-actions registry-custom-actions)
                              registry-modals (assoc :modals registry-modals))))))]))
            parsed))))))

;; Register domain entities during namespace load
(when domain-entities
  (entity-registry/register-entities! domain-entities))

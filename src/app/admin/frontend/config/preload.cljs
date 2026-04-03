(ns app.admin.frontend.config.preload
  "Preload critical admin *entity registry* so admin routes and adapters are available early.

   NOTE: We intentionally do NOT inline admin settings files (view-options.edn,
   form-fields.edn, table-columns.edn). Those are managed at runtime via the DB
   (seeded from these files at bootstrap); inlining them would make shadow-cljs
   treat them as build inputs.

   Domain admin entity configs can also be preloaded via domain config preloaders
   when the admin panel exposes domain pages.

   This repo currently does not preload any domain admin entity configs (Expenses
   admin pages removed)."
  (:require
    [app.admin.frontend.system.entity-registry :as entity-registry]
    [cljs.reader :as reader]
    [shadow.resource :as resource]))

(defonce ^:private preloaded-entities
  (let [resource-content (resource/inline "app/admin/frontend/config/entities.edn")]
    (when resource-content
      (let [parsed (reader/read-string resource-content)]
        (into {}
          (map (fn [[entity-key cfg]]
                 (let [registry-entry (get entity-registry/entity-registry entity-key)
                       registry-init-fn (:init-fn registry-entry)
                       registry-actions (:actions registry-entry)
                       registry-custom-actions (:custom-actions registry-entry)
                       registry-modals (:modals registry-entry)]
                   (let [registry-components (:components registry-entry)]
                     [entity-key
                      (cond-> cfg
                        registry-init-fn (assoc :adapter-init-fn registry-init-fn)
                        (or registry-actions registry-custom-actions registry-modals registry-components)
                        (update :components
                          (fn [components]
                            (let [components (or components {})]
                              (cond-> (merge components registry-components)
                                registry-actions (assoc :actions registry-actions)
                                registry-custom-actions (assoc :custom-actions registry-custom-actions)
                                registry-modals (assoc :modals registry-modals))))))])))
            parsed))))))

(when preloaded-entities
  (entity-registry/register-entities! preloaded-entities))

(ns app.domain.backend.expenses.services.service-configs
  "Facade for expenses service configs and normalizers."
  (:require
    [app.domain.backend.expenses.services.service-configs.normalization :as normalize]
    [app.domain.backend.expenses.services.service-configs.registry :as registry]))

;; Re-export normalization functions for backward compatibility.
(def unescape-html-entities normalize/unescape-html-entities)
(def normalize-supplier-key normalize/normalize-supplier-key)
(def normalize-store-key normalize/normalize-store-key)

;; Re-export registry API.
(def get-entity-config registry/get-entity-config)
(def register-all-entity-services! registry/register-all-entity-services!)

;; Preserve historical side-effect on require.
(register-all-entity-services!)

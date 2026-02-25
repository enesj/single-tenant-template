(ns app.domain.backend.expenses.services.service-configs
  "Facade for expenses service configs and normalizers."
  (:require
    [app.domain.backend.expenses.services.service-configs.config-maps :as config-maps]
    [app.domain.backend.expenses.services.service-configs.normalization :as normalize]
    [app.domain.backend.expenses.services.service-configs.registry :as registry]))

;; Re-export normalization functions for backward compatibility.
(def unescape-html-entities normalize/unescape-html-entities)
(def normalize-supplier-key normalize/normalize-supplier-key)
(def normalize-store-key normalize/normalize-store-key)
(def normalize-manufacturer-key normalize/normalize-manufacturer-key)
(def normalize-city-key normalize/normalize-city-key)

;; Re-export entity configs used directly by routes/services.
(def article-alias-config config-maps/article-alias-config)
(def supplier-alias-config config-maps/supplier-alias-config)
(def store-alias-config config-maps/store-alias-config)
(def price-observation-config config-maps/price-observation-config)
(def supplier-config config-maps/supplier-config)
(def store-config config-maps/store-config)
(def manufacturer-config config-maps/manufacturer-config)
(def category-config config-maps/category-config)
(def expense-category-config config-maps/expense-category-config)
(def city-config config-maps/city-config)
(def subcategory-config config-maps/subcategory-config)
(def payer-config config-maps/payer-config)
(def payer-type-config config-maps/payer-type-config)
(def article-config config-maps/article-config)
(def expense-config config-maps/expense-config)
(def expense-item-config config-maps/expense-item-config)
(def receipt-config config-maps/receipt-config)
(def price-history-config config-maps/price-history-config)
(def report-config config-maps/report-config)

;; Re-export registry API.
(def get-entity-config registry/get-entity-config)
(def register-all-entity-services! registry/register-all-entity-services!)

;; Preserve historical side-effect on require.
(register-all-entity-services!)

(ns app.domain.expenses.frontend.events.entity-configs
  "Configuration maps for expenses domain entities.
   
   Each configuration defines the specific details needed by the event factory
   to generate standard CRUD events for that entity type."
  (:require [app.domain.expenses.frontend.events.events-factory :as factory]))

;; =============================================================================
;; Entity Configurations
;; =============================================================================

(def suppliers-config
  {:entity-key :suppliers
   :base-path [:admin :expenses :suppliers]
   :api-endpoint "/admin/api/expenses/suppliers"
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def payers-config
  {:entity-key :payers
   :base-path [:admin :expenses :payers]
   :api-endpoint "/admin/api/expenses/payers"
   :has-forms? false
   :pagination-opts {:default-per-page 25
                     :param-keys {:limit-key :limit
                                  :offset-key :offset
                                  :page-key :page
                                  :per-page-key :per-page}}})

(def articles-config
  {:entity-key :articles
   :base-path [:admin :expenses :articles]
   :api-endpoint "/admin/api/expenses/articles"
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def receipts-config
  {:entity-key :receipts
   :base-path [:admin :expenses :receipts]
   :api-endpoint "/admin/api/expenses/receipts"
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def expenses-config
  {:entity-key :expenses
   :base-path [:admin :expenses :entries]
   :form-path [:admin :expenses :form]
   :api-endpoint "/admin/api/expenses/entries"
   :has-forms? true
   :pagination-opts {:default-per-page 25}})

(def price-observations-config
  {:entity-key :price-observations
   :base-path [:admin :expenses :price-observations]
   :api-endpoint "/admin/api/expenses/price-observations"
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def article-aliases-config
  {:entity-key :article-aliases
   :base-path [:admin :expenses :article-aliases]
   :api-endpoint "/admin/api/expenses/article-aliases"
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

;; =============================================================================
;; Configuration Registry
;; =============================================================================

(def all-entity-configs
  "Map of all entity configurations for easy lookup."
  {:suppliers suppliers-config
   :payers payers-config
   :articles articles-config
   :receipts receipts-config
   :expenses expenses-config
   :price-observations price-observations-config
   :article-aliases article-aliases-config})

;; =============================================================================
;; Registration Helper
;; =============================================================================

(defn register-all-entity-events!
  "Registers events for all configured entities."
  []
  (doseq [[_ config] all-entity-configs]
    (factory/register-entity-events! config))
  (println "Registered all expenses domain entity events"))
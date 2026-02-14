(ns app.domain.frontend.expenses.events.entity-configs
  "Configuration maps for expenses domain entities.
   
   Each configuration defines the specific details needed by the event factory
   to generate standard CRUD events for that entity type."
  (:require [app.domain.frontend.expenses.events.events-factory :as factory]))

;; =============================================================================
;; Entity Configurations
;; =============================================================================

(def suppliers-config
  {:entity-key :suppliers
   :base-path [:admin :expenses :suppliers]
   :api-endpoint "/admin/api/expenses/suppliers"
   :detail-response-key :supplier
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def manufacturers-config
  {:entity-key :manufacturers
   :base-path [:admin :expenses :manufacturers]
   :api-endpoint "/admin/api/expenses/manufacturers"
   :detail-response-key :manufacturer
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def payers-config
  {:entity-key :payers
   :base-path [:admin :expenses :payers]
   :api-endpoint "/admin/api/expenses/payers"
   :detail-response-key :payer
   :has-forms? false
   :pagination-opts {:default-per-page 25
                     :param-keys {:limit-key :limit
                                  :offset-key :offset
                                  :page-key :page
                                  :per-page-key :per-page}}})

(def payer-types-config
  {:entity-key :payer-types
   :base-path [:admin :expenses :payer-types]
   :api-endpoint "/admin/api/expenses/payer-types"
   :detail-response-key :payer-type
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def articles-config
  {:entity-key :articles
   :base-path [:admin :expenses :articles]
   :api-endpoint "/admin/api/expenses/articles"
   :detail-response-key :article
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def receipts-config
  {:entity-key :receipts
   :base-path [:admin :expenses :receipts]
   :api-endpoint "/admin/api/expenses/receipts"
   :detail-response-key :receipt
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def expenses-config
  {:entity-key :expenses
   :base-path [:admin :expenses :entries]
   :form-path [:admin :expenses :form]
   :api-endpoint "/admin/api/expenses/entries"
   :detail-response-key :expense  ;; API returns singular :expense, not :expenses
   :has-forms? true
   :pagination-opts {:default-per-page 25}})

(def expense-items-config
  {:entity-key :expense-items
   :base-path [:admin :expenses :expense-items]
   :api-endpoint "/admin/api/expenses/expense-items"
   :detail-response-key :expense-item
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def price-observations-config
  {:entity-key :price-observations
   :base-path [:admin :expenses :price-observations]
   :api-endpoint "/admin/api/expenses/price-observations"
   :detail-response-key :price-observation
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def article-aliases-config
  {:entity-key :article-aliases
   :base-path [:admin :expenses :article-aliases]
   :api-endpoint "/admin/api/expenses/article-aliases"
   :detail-response-key :article-alias
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def supplier-aliases-config
  {:entity-key :supplier-aliases
   :base-path [:admin :expenses :supplier-aliases]
   :api-endpoint "/admin/api/expenses/supplier-aliases"
   :detail-response-key :supplier-alias
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def stores-config
  {:entity-key :stores
   :base-path [:admin :expenses :stores]
   :api-endpoint "/admin/api/expenses/stores"
   :detail-response-key :store
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def store-aliases-config
  {:entity-key :store-aliases
   :base-path [:admin :expenses :store-aliases]
   :api-endpoint "/admin/api/expenses/store-aliases"
   :detail-response-key :store-alias
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

;; =============================================================================
;; Configuration Registry
;; =============================================================================

(def categories-config
  {:entity-key :categories
   :base-path [:admin :expenses :categories]
   :api-endpoint "/admin/api/expenses/categories"
   :detail-response-key :category
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def expense-categories-config
  {:entity-key :expense-categories
   :base-path [:admin :expenses :expense-categories]
   :api-endpoint "/admin/api/expenses/expense-categories"
   :detail-response-key :expense-category
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def cities-config
  {:entity-key :cities
   :base-path [:admin :expenses :cities]
   :api-endpoint "/admin/api/expenses/cities"
   :detail-response-key :city
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def countries-config
  {:entity-key :countries
   :base-path [:admin :expenses :countries]
   :api-endpoint "/admin/api/expenses/countries"
   :detail-response-key :country
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def subcategories-config
  {:entity-key :subcategories
   :base-path [:admin :expenses :subcategories]
   :api-endpoint "/admin/api/expenses/subcategories"
   :detail-response-key :subcategory
   :has-forms? false
   :pagination-opts {:default-per-page 25}})

(def all-entity-configs
  "Map of all entity configurations for easy lookup."
  {:suppliers suppliers-config
   :stores stores-config
   :manufacturers manufacturers-config
   :categories categories-config
   :expense-categories expense-categories-config
   :cities cities-config
   :countries countries-config
   :subcategories subcategories-config
   :payers payers-config
   :articles articles-config
   :receipts receipts-config
   :expenses expenses-config
   :expense-items expense-items-config
   :price-observations price-observations-config
   :article-aliases article-aliases-config
   :supplier-aliases supplier-aliases-config
   :store-aliases store-aliases-config
   :payer-types payer-types-config})

;; =============================================================================
;; Registration Helper
;; =============================================================================

(defn register-all-entity-events!
  "Registers events for all configured entities."
  []
  (doseq [[_ config] all-entity-configs]
    (factory/register-entity-events! config))
  (println "Registered all expenses domain entity events"))

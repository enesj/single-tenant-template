(ns app.domain.frontend.expenses.events.entity-configs
  "Configuration maps for expenses domain entities.

   Each configuration defines the specific details needed by the event factory
   to generate standard CRUD events for that entity type.")

;; =============================================================================
;; Entity Configurations
;; =============================================================================

(def suppliers-config
  {:entity-key :suppliers
   :base-path [:admin :expenses :suppliers]
   :api-endpoint "/admin/api/expenses/suppliers"
   :detail-response-key :supplier
   :has-forms? false
   :server-filter-keys {:display-name    :search
                        :normalized-key  :normalized-key}})

(def manufacturers-config
  {:entity-key :manufacturers
   :base-path [:admin :expenses :manufacturers]
   :api-endpoint "/admin/api/expenses/manufacturers"
   :detail-response-key :manufacturer
   :has-forms? false
   :server-filter-keys {:display-name    :search
                        :normalized-key  :normalized-key}})

(def payers-config
  {:entity-key :payers
   :base-path [:admin :expenses :payers]
   :api-endpoint "/admin/api/expenses/payers"
   :detail-response-key :payer
   :has-forms? false
   :pagination-opts {:param-keys {:limit-key :limit
                                  :offset-key :offset
                                  :page-key :page
                                  :per-page-key :per-page}}})

(def articles-config
  {:entity-key :articles
   :base-path [:admin :expenses :articles]
   :api-endpoint "/admin/api/expenses/articles"
   :detail-response-key :article
   :has-forms? false
   ;; Map frontend filter field-ids → backend query param names.
   ;; :canonical-name stays as :search for backward compat with the generic
   ;; text-search endpoint; other columns get per-column ILIKE params.
   :server-filter-keys {:canonical-name              :search
                        :category-name               :category-name
                        :subcategory-name            :subcategory-name
                        :manufacturer-display-name   :manufacturer-display-name
                        :unit                        :unit}})

(def receipts-config
  {:entity-key :receipts
   :base-path [:admin :expenses :receipts]
   :api-endpoint "/admin/api/expenses/receipts"
   :detail-response-key :receipt
   :has-forms? false
   :server-filter-keys {:original-filename :original-filename
                        :supplier-guess :supplier-guess
                        :status         :status}})

(def expenses-config
  {:entity-key :expenses
   :base-path [:admin :expenses :entries]
   :form-path [:admin :expenses :form]
   :api-endpoint "/admin/api/expenses/entries"
   :detail-response-key :expense  ;; API returns singular :expense, not :expenses
   :has-forms? true})

(def expense-items-config
  {:entity-key :expense-items
   :base-path [:admin :expenses :expense-items]
   :api-endpoint "/admin/api/expenses/expense-items"
   :detail-response-key :expense-item
   :has-forms? false
   :server-filter-keys {:raw-label :search
                        :unit :unit}})

(def article-aliases-config
  {:entity-key :article-aliases
   :base-path [:admin :expenses :article-aliases]
   :api-endpoint "/admin/api/expenses/article-aliases"
   :detail-response-key :article-alias
   :has-forms? false
   :server-filter-keys {:supplier-display-name :supplier-display-name
                        :article-canonical-name :article-canonical-name
                        :raw-label :raw-label
                        :raw-label-normalized :raw-label-normalized
                        :unit :unit
                        :supplier-id :supplier-id
                        :article-id :article-id}})

(def unmapped-aliases-config
  {:entity-key :unmapped-aliases
   :base-path [:admin :expenses :unmapped-aliases]
   :api-endpoint "/admin/api/expenses/articles/unmapped-aliases"
   :has-forms? false
   :server-filter-keys {:supplier-display-name :supplier-name
                        :raw-label :raw-label
                        :raw-label-normalized :raw-label-normalized
                        :unit :unit}})

(def supplier-aliases-config
  {:entity-key :supplier-aliases
   :base-path [:admin :expenses :supplier-aliases]
   :api-endpoint "/admin/api/expenses/supplier-aliases"
   :detail-response-key :supplier-alias
   :has-forms? false
   :server-filter-keys {:raw-label              :search
                        :supplier-id            :supplier-id
                        :unmapped-only          :unmapped-only
                        :supplier-display-name  :supplier-display-name
                        :raw-label-normalized   :raw-label-normalized}})

(def stores-config
  {:entity-key :stores
   :base-path [:admin :expenses :stores]
   :api-endpoint "/admin/api/expenses/stores"
   :detail-response-key :store
   :has-forms? false
   :server-filter-keys {:display-name           :search
                        :supplier-display-name  :supplier-display-name
                        :normalized-key         :normalized-key
                        :address                :address
                        :city-name              :city-name}})

(def store-aliases-config
  {:entity-key :store-aliases
   :base-path [:admin :expenses :store-aliases]
   :api-endpoint "/admin/api/expenses/store-aliases"
   :detail-response-key :store-alias
   :has-forms? false
   :server-filter-keys {:supplier-display-name :supplier-display-name
                        :store-display-name :store-display-name
                        :store-address :store-address
                        :raw-label :raw-label
                        :raw-label-normalized :raw-label-normalized}})

;; =============================================================================
;; Configuration Registry
;; =============================================================================

(def categories-config
  {:entity-key :categories
   :base-path [:admin :expenses :categories]
   :api-endpoint "/admin/api/expenses/categories"
   :detail-response-key :category
   :has-forms? false
   :server-filter-keys {:name        :search
                        :description :description}})

(def expense-categories-config
  {:entity-key :expense-categories
   :base-path [:admin :expenses :expense-categories]
   :api-endpoint "/admin/api/expenses/expense-categories"
   :detail-response-key :expense-category
   :has-forms? false
   :server-search-keys #{:name}})

(def cities-config
  {:entity-key :cities
   :base-path [:admin :expenses :cities]
   :api-endpoint "/admin/api/expenses/cities"
   :detail-response-key :city
   :has-forms? false
   :server-filter-keys {:name           :search
                        :normalized-key :normalized-key
                        :zip            :zip
                        :country        :country}})

(def countries-config
  {:entity-key :countries
   :base-path [:admin :expenses :countries]
   :api-endpoint "/admin/api/expenses/countries"
   :detail-response-key :country
   :has-forms? false})

(def subcategories-config
  {:entity-key :subcategories
   :base-path [:admin :expenses :subcategories]
   :api-endpoint "/admin/api/expenses/subcategories"
   :detail-response-key :subcategory
   :has-forms? false
   :server-filter-keys {:name          :search
                        :description   :description
                        :category-name :category-name}})

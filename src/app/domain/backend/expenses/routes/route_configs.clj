(ns app.domain.backend.expenses.routes.route-configs
  "Configuration maps for expenses domain route generation."
  (:require
    [app.domain.backend.expenses.services.service-configs :as svc-configs]
    [app.template.backend.routes.admin.utils :as utils]
    [app.template.backend.middleware.admin :as admin-mw]))

;; =============================================================================
;; Entity Route Configurations
;; =============================================================================

(comment
  ;; Query param maps are normalized upstream by the routes factory.
  ;; Use direct keyword access like (:search qp).
  :ok)

(def category-config
  {:entity-key :category
   :entity-plural :categories
   :route-segment "categories"
   :service 'app.domain.backend.expenses.services.categories
   :default-limit 100
   :default-order-by "name"
   :required-fields [:name]
   :has-count? true
   :has-search? true
   :custom-query-params (fn [qp]
                          {:search (:search qp)})
   :custom-count-params (fn [qp]
                          {:search (:search qp)})})

(def subcategory-config
  {:entity-key :subcategory
   :entity-plural :subcategories
   :route-segment "subcategories"
   :service 'app.domain.backend.expenses.services.subcategories
   :default-limit 100
   :default-order-by "name"
   :required-fields [:category-id :name]
   :has-count? true
   :has-search? true
   :custom-query-params (fn [qp]
                          {:search (:search qp)})
   :custom-count-params (fn [qp]
                          {:search (:search qp)})})

(def supplier-config
  {:entity-key :supplier
   :entity-plural :suppliers
   :route-segment "suppliers"
   :service 'app.domain.backend.expenses.services.suppliers
   :default-limit 100
   :default-order-by "display_name"
   :required-fields [:display-name]
   :has-count? true
   :has-search? true
   :custom-query-params (fn [qp]
                          {:search (:search qp)})
   :custom-count-params (fn [qp]
                          {:search (:search qp)})})

(def store-config
  {:entity-key :store
   :entity-plural :stores
   :route-segment "stores"
   :service 'app.domain.backend.expenses.services.stores
   :default-limit 100
   :default-order-by "display_name"
   :required-fields [:supplier-id :display-name]
   :has-count? true
   :has-search? false
   :custom-query-params (fn [qp]
                          {:search (:search qp)})
   :custom-count-params (fn [qp]
                          {:search (:search qp)})})

(def city-config
  {:entity-key :city
   :entity-plural :cities
   :route-segment "cities"
   :service 'app.domain.backend.expenses.services.cities
   :default-limit 100
   :default-order-by "name"
   :required-fields [:name]
   :has-count? true
   :has-search? true
   :custom-query-params (fn [qp]
                          {:search (:search qp)})
   :custom-count-params (fn [qp]
                          {:search (:search qp)})})

(def manufacturer-config
  {:entity-key :manufacturer
   :entity-plural :manufacturers
   :route-segment "manufacturers"
   :service 'app.domain.backend.expenses.services.manufacturers
   :default-limit 100
   :default-order-by "display_name"
   :required-fields [:display-name]
   :has-count? true
   :has-search? true
   :custom-query-params (fn [qp]
                          {:search (:search qp)})
   :custom-count-params (fn [qp]
                          {:search (:search qp)})})

(def payer-config
  {:entity-key :payer
   :entity-plural :payers
   :route-segment "payers"
   :service 'app.domain.backend.expenses.services.payers
   :default-limit 100
   :default-order-by "label"
   :required-fields [:payer-type-id :label]
   :has-count? true
   :has-search? false
   :custom-query-params (fn [_qp] {})})

(def payer-type-config
  {:entity-key :payer-type
   :entity-plural :payer-types
   :route-segment "payer-types"
   :service 'app.domain.backend.expenses.services.payer-types
   :default-limit 100
   :default-order-by "label"
   :required-fields [:label]
   :has-count? true
   :has-search? false
   ;; Require at least :admin role; owner also passes. Support is blocked.
   :route-middleware [(fn [handler] (admin-mw/wrap-admin-role handler :admin))]
   :custom-query-params (fn [_qp] {})})

(def article-config
  {:entity-key :article
   :entity-plural :articles
   :route-segment "articles"
   :service 'app.domain.backend.expenses.services.articles
   :default-limit 50
   :default-order-by "canonical_name"
   :required-fields [:canonical-name]
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:search (:search qp)})})

(def expense-config
  {:entity-key :expense
   :entity-plural :expenses
   :route-segment "entries"
   :service 'app.domain.backend.expenses.services.expenses
   :default-limit 50
   :default-order-by "created_at"
   :required-fields []
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:from (:from qp)
                           :to (:to qp)
                           :supplier-id (utils/parse-uuid-custom (:supplier-id qp))
                           :payer-id (utils/parse-uuid-custom (:payer-id qp))
                           :is-posted? (utils/parse-boolean-param qp :is-posted)
                           :order-dir (keyword (or (:order-dir qp) "desc"))})})

(def expense-item-config
  {:entity-key :expense-item
   :entity-plural :expense-items
   :route-segment "expense-items"
   :service 'app.domain.backend.expenses.services.expense-items
   :default-limit 100
   :default-order-by "created_at"
   ;; NOTE: raw_label is resolved server-side into expense_items.alias_id.
   ;; Keeping :required-fields minimal avoids rejecting requests that only send raw_label.
   :required-fields [:expense-id :line-total]
   :has-count? true
   :has-search? true
   :custom-query-params (fn [qp]
                          {:search (:search qp)})})

(def receipt-config
  {:entity-key :receipt
   :entity-plural :receipts
   :route-segment "receipts"
   :service 'app.domain.backend.expenses.services.receipts.queries
   :default-limit 50
   :default-order-by "receipt_date"
   :required-fields [:file-url]
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:status (:status qp)})
   :transform-request (fn [body]
                        (update body :status #(when % (name %))))})

(def price-observation-config
  {:entity-key :price-observation
   :entity-plural :price-observations
   :route-segment "price-observations"
   :service 'app.domain.backend.expenses.services.price-observations
   :default-limit 100
   :default-order-by "observed_at"
   :required-fields []
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:article-id (utils/parse-uuid-custom (:article-id qp))
                           :supplier-id (utils/parse-uuid-custom (:supplier-id qp))
                           :from (:from qp)
                           :to (:to qp)})})

(def article-alias-config
  {:entity-key :article-alias
   :entity-plural :article-aliases
   :route-segment "article-aliases"
   :service 'app.domain.backend.expenses.services.article-aliases
   :default-limit 50
   :default-order-by "raw_label"
   :required-fields [:supplier-id :raw-label :raw-label-normalized]
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:supplier-id (utils/parse-uuid-custom (:supplier-id qp))
                           :raw-label (:raw-label qp)
                           :article-id (utils/parse-uuid-custom (:article-id qp))})

   ;; Admin edit forms can send numeric fields as strings (HTML inputs always
   ;; emit strings). Coerce them at the boundary so Postgres doesn't reject the
   ;; update with a type error.
   :transform-request (fn [body]
                        (cond-> body
                          (contains? body :confidence)
                          (assoc :confidence (utils/parse-int-param body :confidence nil))))})

(def supplier-alias-config
  {:entity-key :supplier-alias
   :entity-plural :supplier-aliases
   :route-segment "supplier-aliases"
   :service 'app.domain.backend.expenses.services.supplier-aliases
   :default-limit 50
   :default-order-by "raw_label"
   :required-fields [:raw-label :raw-label-normalized]
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:supplier-id (utils/parse-uuid-custom (:supplier-id qp))
                           :unmapped-only (utils/parse-boolean-param qp :unmapped-only)
                           :search (:search qp)})

   ;; Allow clients to omit raw-label-normalized; compute it server-side.
   ;; Also coerce numeric fields (e.g. confidence) since form submissions are strings.
   :transform-request (fn [body]
                        (let [raw-label (:raw-label body)
                              normalized (or (:raw-label-normalized body)
                                           (when raw-label
                                             (svc-configs/normalize-supplier-key raw-label)))
                              confidence (when (contains? body :confidence)
                                           (utils/parse-int-param body :confidence nil))]
                          (cond-> body
                            normalized (assoc :raw-label-normalized normalized)
                            (contains? body :confidence) (assoc :confidence confidence))))})

(def store-alias-config
  {:entity-key :store-alias
   :entity-plural :store-aliases
   :route-segment "store-aliases"
   :service 'app.domain.backend.expenses.services.store-aliases
   :default-limit 50
   :default-order-by "raw_label"
   :required-fields [:raw-label :raw-label-normalized]
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:search (:search qp)})

   ;; Allow clients to omit raw-label-normalized; compute it server-side.
   ;; Also coerce numeric fields (e.g. confidence) since form submissions are strings.
   :transform-request (fn [body]
                        (let [raw-label (:raw-label body)
                              normalized (or (:raw-label-normalized body)
                                           (when raw-label
                                             (svc-configs/normalize-store-key raw-label)))
                              confidence (when (contains? body :confidence)
                                           (utils/parse-int-param body :confidence nil))]
                          (cond-> body
                            normalized (assoc :raw-label-normalized normalized)
                            (contains? body :confidence) (assoc :confidence confidence))))})

;; =============================================================================
;; Configuration Map
;; =============================================================================

(def entity-configs
  "Map of all entity configurations for easy lookup."
  {:categories category-config
   :subcategories subcategory-config
   :suppliers supplier-config
   :stores store-config
   :cities city-config
   :manufacturers manufacturer-config
   :payers payer-config
   :payer-types payer-type-config
   :articles article-config
   :expenses expense-config
   :expense-items expense-item-config
   :receipts receipt-config
   :price-observations price-observation-config
   :article-aliases article-alias-config
   :supplier-aliases supplier-alias-config
   :store-aliases store-alias-config})


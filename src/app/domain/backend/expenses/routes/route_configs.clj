(ns app.domain.backend.expenses.routes.route-configs
  "Configuration maps for expenses domain route generation."
  (:require
    [app.template.backend.routes.admin.utils :as utils]
    [app.template.backend.middleware.admin :as admin-mw]))

;; =============================================================================
;; Entity Route Configurations
;; =============================================================================

(defn- get-param
  "Get param from map, trying both keyword and string keys."
  [m k]
  (or (get m k) (get m (if (keyword? k) (name k) (keyword k)))))

(def supplier-config
  {:entity-key :supplier
   :entity-plural :suppliers
   :route-segment "suppliers"
   :service 'app.domain.backend.expenses.services.suppliers
   :default-limit 100
   :default-order-by "display_name"
   :required-fields [:display_name]
   :has-count? true
   :has-search? true
   :custom-query-params (fn [qp]
                          {:search (get-param qp :search)
                           :include_archived (utils/parse-boolean-param qp :include_archived)})
   :custom-count-params (fn [qp]
                         {:search (get-param qp :search)
                          :include_archived (utils/parse-boolean-param qp :include_archived)})})

(def payer-config
  {:entity-key :payer
   :entity-plural :payers
   :route-segment "payers"
   :service 'app.domain.backend.expenses.services.payers
   :default-limit 100
   :default-order-by "label"
   :required-fields [:payer_type_id :label]
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
   :required-fields [:canonical_name]
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:search (get-param qp :search)})})

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
                          {:from (get-param qp :from)
                           :to (get-param qp :to)
                           :supplier-id (or (utils/parse-uuid-custom (get-param qp :supplier_id))
                                          (utils/parse-uuid-custom (get-param qp :supplier-id)))
                           :payer-id (or (utils/parse-uuid-custom (get-param qp :payer_id))
                                       (utils/parse-uuid-custom (get-param qp :payer-id)))
                           :is-posted? (utils/parse-boolean-param qp :is_posted)
                           :order-dir (keyword (or (get-param qp :order-dir) "desc"))})})

(def expense-item-config
  {:entity-key :expense-item
   :entity-plural :expense-items
   :route-segment "expense-items"
   :service 'app.domain.backend.expenses.services.expense-items
   :default-limit 100
   :default-order-by "created_at"
  ;; NOTE: raw_label is now stored as expense_items.raw_label_id.
  ;; We still accept either :raw_label (text) or :raw_label_id (uuid) at the service layer.
  ;; Keeping :required-fields minimal avoids rejecting requests that send only :raw_label_id.
  :required-fields [:expense_id :line_total]
   :has-count? true
   :has-search? true
   :custom-query-params (fn [qp]
                          {:search (get-param qp :search)})})

(def receipt-config
  {:entity-key :receipt
   :entity-plural :receipts
   :route-segment "receipts"
  :service 'app.domain.backend.expenses.services.receipts.queries
   :default-limit 50
   :default-order-by "receipt_date"
   :required-fields [:file_url]
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:status (get-param qp :status)})
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
                          {:article-id (utils/parse-uuid-custom (get-param qp :article_id))
                           :supplier-id (utils/parse-uuid-custom (get-param qp :supplier_id))
                           :from (get-param qp :from)
                           :to (get-param qp :to)})})

(def article-alias-config
  {:entity-key :article-alias
   :entity-plural :article-aliases
   :route-segment "article-aliases"
   :service 'app.domain.backend.expenses.services.article-aliases
   :default-limit 50
   :default-order-by "raw_label"
   :required-fields [:supplier_id :raw_label :article_id]
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:supplier-id (utils/parse-uuid-custom (get-param qp :supplier_id))
                           :raw-label (get-param qp :raw-label)
                           :article-id (utils/parse-uuid-custom (get-param qp :article_id))})})

;; =============================================================================
;; Configuration Map
;; =============================================================================

(def entity-configs
  "Map of all entity configurations for easy lookup."
  {:suppliers supplier-config
  :payers payer-config
  :payer-types payer-type-config
   :articles article-config
   :expenses expense-config
   :expense-items expense-item-config
   :receipts receipt-config
   :price-observations price-observation-config
   :article-aliases article-alias-config})

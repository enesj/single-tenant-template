(ns app.domain.expenses.routes.route-configs
  "Configuration maps for expenses domain route generation."
  (:require
   [app.backend.routes.admin.utils :as utils]))

;; =============================================================================
;; Entity Route Configurations
;; =============================================================================

(def supplier-config
  {:entity-key :supplier
   :entity-plural :suppliers
   :route-segment "suppliers"
   :service 'app.domain.expenses.services.suppliers
   :default-limit 100
   :default-order-by "display_name"
   :required-fields [:display_name]
   :has-count? true
   :has-search? true
   :custom-query-params (fn [qp]
                          {:search (:search qp)})})

(def payer-config
  {:entity-key :payer
   :entity-plural :payers
   :route-segment "payers"
   :service 'app.domain.expenses.services.payers
   :default-limit 100
   :default-order-by "label"
   :required-fields [:type :label]
   :has-count? true
   :has-search? false
   :custom-query-params (fn [qp]
                          {:type (:type qp)})
   :transform-request (fn [body]
                        (update body :type #(when % (name %))))})

(def article-config
  {:entity-key :article
   :entity-plural :articles
   :route-segment "articles"
   :service 'app.domain.expenses.services.articles
   :default-limit 50
   :default-order-by "canonical_name"
   :required-fields [:canonical_name]
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:search (:search qp)})})

(def expense-config
  {:entity-key :expense
   :entity-plural :expenses
   :route-segment "entries"
   :service 'app.domain.expenses.services.expenses
   :default-limit 50
   :default-order-by "created_at"
   :required-fields []
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:from (:from qp)
                           :to (:to qp)
                           :supplier-id (or (utils/parse-uuid-custom (:supplier_id qp))
                                            (utils/parse-uuid-custom (:supplier-id qp)))
                           :payer-id (or (utils/parse-uuid-custom (:payer_id qp))
                                         (utils/parse-uuid-custom (:payer-id qp)))
                           :is-posted? (utils/parse-boolean-param qp :is_posted)
                           :order-dir (keyword (or (:order-dir qp) "desc"))})})

(def receipt-config
  {:entity-key :receipt
   :entity-plural :receipts
   :route-segment "receipts"
   :service 'app.domain.expenses.services.receipts
   :default-limit 50
   :default-order-by "receipt_date"
   :required-fields [:file_url]
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
   :service 'app.domain.expenses.services.price-observations
   :default-limit 100
   :default-order-by "observed_at"
   :required-fields []
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:article-id (utils/parse-uuid-custom (:article_id qp))
                           :supplier-id (utils/parse-uuid-custom (:supplier_id qp))
                           :from (:from qp)
                           :to (:to qp)})})

(def article-alias-config
  {:entity-key :article-alias
   :entity-plural :article-aliases
   :route-segment "article-aliases"
   :service 'app.domain.expenses.services.article-aliases
   :default-limit 50
   :default-order-by "raw_label"
   :required-fields [:supplier_id :raw_label :article_id]
   :has-count? false
   :has-search? false
   :custom-query-params (fn [qp]
                          {:supplier-id (utils/parse-uuid-custom (:supplier_id qp))
                           :raw-label (:raw-label qp)
                           :article-id (utils/parse-uuid-custom (:article_id qp))})})

;; =============================================================================
;; Configuration Map
;; =============================================================================

(def entity-configs
  "Map of all entity configurations for easy lookup."
  {:suppliers supplier-config
   :payers payer-config
   :articles article-config
   :expenses expense-config
   :receipts receipt-config
   :price-observations price-observation-config
   :article-aliases article-alias-config})
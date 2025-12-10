(ns app.domain.expenses.services.price-observations
  "CRUD helpers for price observations."
  (:require
    [app.domain.expenses.services.price-history :as price-history]
    [app.domain.expenses.services.service-configs :as configs]
    [app.domain.expenses.services.services-factory :as factory]))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config (configs/get-entity-config :price-observation))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service (factory/build-entity-service config))

;; Legacy function names for backward compatibility with routes
(def list-price-observations (:list service))
(def get-price-observation (:get service))
(def update-price-observation! (:update! service))
(def delete-price-observation! (:delete! service))
(def count-price-observations (:count service))

;; ============================================================================
;; Custom Operations
;; ============================================================================

(defn create-price-observation!
  "Create a price observation using price-history helper."
  [db {:keys [article_id supplier_id expense_item_id observed_at qty unit_price line_total currency] :as obs}]
  (price-history/record-observation!
    db (merge obs {:article_id article_id
                   :supplier_id supplier_id
                   :expense_item_id expense_item_id
                   :observed_at observed_at
                   :qty qty
                   :unit_price unit_price
                   :line_total line_total
                   :currency currency})))
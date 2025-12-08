(ns app.domain.expenses.services.payers
  "Payer CRUD services for expense tracking.
   Payers represent payment methods: cash, cards, bank accounts, or people."
  (:require
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

;; ============================================================================
;; CRUD Operations
;; ============================================================================

(defn create-payer!
  "Create a new payer (payment method).
   
   Args:
     db - Database connection
     data - Map with:
       :type - Required, one of: 'cash' 'card' 'account' 'person'
       :label - Required, display name
       :last4 - Optional, last 4 digits for cards
       :is_default - Optional, mark as default (default: false)
   
   Returns: Created payer record with generated :id"
  [db {:keys [type label last4 is_default] :as data}]
  (when-not type
    (throw (ex-info "type is required" {:data data})))
  (when-not label
    (throw (ex-info "label is required" {:data data})))
  (when-not (#{"cash" "card" "account" "person"} type)
    (throw (ex-info "Invalid payer type" {:type type :valid #{"cash" "card" "account" "person"}})))
  
  (let [payer-data {:id (UUID/randomUUID)
                    :type [:cast type :payer_type]
                    :label label
                    :last4 last4
                    :is_default (boolean is_default)}
        insert-sql (sql/format {:insert-into :payers
                                :values [payer-data]
                                :returning [:*]})]
    (jdbc/execute-one! db insert-sql {:builder-fn rs/as-unqualified-lower-maps})))

(defn get-payer
  "Get payer by ID.
   
   Args:
     db - Database connection
     id - Payer UUID
   
   Returns: Payer map or nil if not found"
  [db id]
  (let [query (sql/format {:select [:*]
                           :from [:payers]
                           :where [:= :id id]})]
    (jdbc/execute-one! db query {:builder-fn rs/as-unqualified-lower-maps})))

(defn list-payers
  "List all payers, optionally filtered by type.
   
   Args:
     db - Database connection
     opts - Map with optional keys:
       :type - Filter by payer type ('cash', 'card', 'account', 'person')
       :order-by - Column to order by (default :type then :label)
   
   Returns: Vector of payer maps"
  [db {:keys [type order-by] :or {order-by [[:type :asc] [:label :asc]]}}]
  (let [base-query {:select [:*]
                    :from [:payers]}
        query (cond-> base-query
                type
                (assoc :where [:= :type [:cast type :payer_type]])
                
                true
                (assoc :order-by order-by))
        sql-query (sql/format query)]
    (jdbc/execute! db sql-query {:builder-fn rs/as-unqualified-lower-maps})))

(defn update-payer!
  "Update payer fields.
   
   Args:
     db - Database connection
     id - Payer UUID
     updates - Map of fields to update
   
   Returns: Updated payer map or nil if not found"
  [db id updates]
  (let [updates-final (assoc updates :updated_at [:now])
        update-sql (sql/format {:update :payers
                                :set updates-final
                                :where [:= :id id]
                                :returning [:*]})]
    (jdbc/execute-one! db update-sql {:builder-fn rs/as-unqualified-lower-maps})))

(defn delete-payer!
  "Delete a payer by ID.
   WARNING: This will fail if payer is referenced by expenses.
   
   Args:
     db - Database connection
     id - Payer UUID
   
   Returns: Boolean indicating success"
  [db id]
  (let [delete-sql (sql/format {:delete-from :payers
                                :where [:= :id id]})]
    (pos? (::jdbc/update-count (jdbc/execute-one! db delete-sql)))))

;; ============================================================================
;; Default Payer Management
;; ============================================================================

(defn get-default-payer
  "Get the default payer for a specific type.
   
   Args:
     db - Database connection
     type - Payer type ('cash', 'card', 'account', 'person')
   
   Returns: Default payer map or nil"
  [db type]
  (let [query (sql/format {:select [:*]
                           :from [:payers]
                           :where [:and
                                   [:= :type [:cast type :payer_type]]
                                   [:= :is_default true]]
                           :limit 1})]
    (jdbc/execute-one! db query {:builder-fn rs/as-unqualified-lower-maps})))

(defn set-default-payer!
  "Set a payer as the default for its type.
   Automatically unsets other defaults of the same type.
   
   Args:
     db - Database connection
     id - Payer UUID to set as default
   
   Returns: Updated payer map"
  [db id]
  (jdbc/with-transaction [tx db]
    (let [payer (get-payer tx id)]
      (when-not payer
        (throw (ex-info "Payer not found" {:id id})))
      
      ;; Unset other defaults of the same type
      (let [unset-sql (sql/format {:update :payers
                                   :set {:is_default false}
                                   :where [:and
                                           [:= :type [:cast (:type payer) :payer_type]]
                                           [:<> :id id]]})]
        (jdbc/execute-one! tx unset-sql))
      
      ;; Set this one as default
      (update-payer! tx id {:is_default true}))))

;; ============================================================================
;; Smart Suggestions
;; ============================================================================

(defn suggest-payer
  "Suggest a payer based on payment hints from receipt extraction.
   
   Args:
     db - Database connection
     payment-hints - Map with optional keys:
       :method - 'cash', 'card', 'account'
       :card_last4 - Last 4 digits of card (if method is 'card')
   
   Returns: Suggested payer map or nil
   
   Logic:
     1. If card with matching last4 exists, return it
     2. If method specified, return default for that type
     3. Return first default payer found (any type)"
  [db payment-hints]
  (cond
    ;; Try to match card by last4
    (and (= "card" (:method payment-hints))
         (:card_last4 payment-hints))
    (let [query (sql/format {:select [:*]
                             :from [:payers]
                             :where [:and
                                     [:= :type [:cast "card" :payer_type]]
                                     [:= :last4 (:card_last4 payment-hints)]]
                             :limit 1})]
      (jdbc/execute-one! db query {:builder-fn rs/as-unqualified-lower-maps}))
    
    ;; Try default for specified method
    (:method payment-hints)
    (get-default-payer db (:method payment-hints))
    
    ;; Fallback to any default
    :else
    (let [query (sql/format {:select [:*]
                             :from [:payers]
                             :where [:= :is_default true]
                             :limit 1})]
      (jdbc/execute-one! db query {:builder-fn rs/as-unqualified-lower-maps}))))

(defn list-payers-by-type
  "Get payers grouped by type.
   Useful for UI dropdowns/selection.
   
   Args:
     db - Database connection
   
   Returns: Map of {type -> [payers]}"
  [db]
  (let [all-payers (list-payers db {})]
    (group-by :type all-payers)))

(defn count-payers
  "Count total payers, optionally filtered by type.
   
   Args:
     db - Database connection
     type - Optional payer type filter
   
   Returns: Integer count"
  [db & [type]]
  (let [base-query {:select [[[:count :*] :total]]
                    :from [:payers]}
        query (if type
                (assoc base-query :where [:= :type [:cast type :payer_type]])
                base-query)
        sql-query (sql/format query)
        result (jdbc/execute-one! db sql-query {:builder-fn rs/as-unqualified-lower-maps})]
    (:total result)))

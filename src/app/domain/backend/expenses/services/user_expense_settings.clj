(ns app.domain.backend.expenses.services.user-expense-settings
  "Persistence for per-user expenses settings.

   Stored in the domain table `user_expense_settings` keyed by user_id."
  (:require
    [app.shared.adapters.database :as db-adapter]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(def default-settings
  "Default values returned when a user has no persisted settings row yet."
  {:default-currency "BAM"
   :default-payer-id nil
  :notifications-enabled true
  ;; When true, allow the receipt OCR pipeline to run the optional AI refine step
  ;; (still subject to global Cerebras config / API key availability).
  :receipt-refine-enabled false})

(def allowed-currencies
  "Keep in sync with the domain :currency enum."
  #{"BAM" "EUR" "USD"})

(defn get-user-expense-settings
  "Return the persisted settings row for `user-id`, or nil if none exists.

  Returned keys are kebab-case keywords:
  {:default-currency \"BAM\"
   :default-payer-id <uuid|nil>
   :notifications-enabled <bool>
   :receipt-refine-enabled <bool>}"
  [db user-id]
  (when-not (instance? UUID user-id)
    (throw (ex-info "user-id must be a UUID" {:user-id user-id})))
  (-> (jdbc/execute-one!
        db
      (sql/format {:select [:default_currency :default_payer_id :notifications_enabled :receipt_refine_enabled]
                     :from [:user_expense_settings]
                     :where [:= :user_id user-id]})
        {:builder-fn rs/as-unqualified-lower-maps})
      db-adapter/to-app))

(defn upsert-user-expense-settings!
  "Insert/update settings for `user-id`.

  `settings` must already be validated and should include all effective keys:
  - :default-currency (string)
  - :default-payer-id (UUID or nil)
  - :notifications-enabled (boolean)
  - :receipt-refine-enabled (boolean)

  Returns the stored row (same shape as `get-user-expense-settings`)."
  [db user-id {:keys [default-currency default-payer-id notifications-enabled receipt-refine-enabled]}]
  (when-not (instance? UUID user-id)
    (throw (ex-info "user-id must be a UUID" {:user-id user-id})))
  (when-not (contains? allowed-currencies default-currency)
    (throw (ex-info "Unsupported currency" {:default-currency default-currency
                                            :allowed allowed-currencies})))
  (when-not (boolean? notifications-enabled)
    (throw (ex-info "notifications-enabled must be boolean" {:notifications-enabled notifications-enabled})))
  (when-not (boolean? receipt-refine-enabled)
    (throw (ex-info "receipt-refine-enabled must be boolean" {:receipt-refine-enabled receipt-refine-enabled})))
  (when-not (or (nil? default-payer-id) (instance? UUID default-payer-id))
    (throw (ex-info "default-payer-id must be UUID or nil" {:default-payer-id default-payer-id})))
  (-> (jdbc/execute-one!
        db
        [(str
           "INSERT INTO user_expense_settings "
           "(user_id, default_currency, default_payer_id, notifications_enabled, receipt_refine_enabled, created_at, updated_at) "
           "VALUES (?, ?::currency, ?, ?, ?, NOW(), NOW()) "
           "ON CONFLICT (user_id) DO UPDATE SET "
           "default_currency = EXCLUDED.default_currency, "
           "default_payer_id = EXCLUDED.default_payer_id, "
           "notifications_enabled = EXCLUDED.notifications_enabled, "
           "receipt_refine_enabled = EXCLUDED.receipt_refine_enabled, "
           "updated_at = NOW() "
           "RETURNING default_currency, default_payer_id, notifications_enabled, receipt_refine_enabled")
         user-id
         default-currency
         default-payer-id
         notifications-enabled
         receipt-refine-enabled]
        {:builder-fn rs/as-unqualified-lower-maps})
      db-adapter/to-app))

(defn effective-settings
  "Merge persisted settings over defaults."
  [persisted]
  (merge default-settings (or persisted {})))

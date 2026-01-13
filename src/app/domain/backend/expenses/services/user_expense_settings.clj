(ns app.domain.backend.expenses.services.user-expense-settings
  "Persistence for per-user expenses settings.

   Stored in the domain table `user_expense_settings` keyed by user_id."
  (:require
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(def default-settings
  "Default values returned when a user has no persisted settings row yet."
  {:default_currency "BAM"
   :default_payer_id nil
   :notifications_enabled true})

(def allowed-currencies
  "Keep in sync with the domain :currency enum."
  #{"BAM" "EUR" "USD"})

(defn get-user-expense-settings
  "Return the persisted settings row for `user-id`, or nil if none exists.

  Returned keys are snake_case keywords:
  {:default_currency \"BAM\" :default_payer_id <uuid|nil> :notifications_enabled <bool>}"
  [db user-id]
  (when-not (instance? UUID user-id)
    (throw (ex-info "user-id must be a UUID" {:user-id user-id})))
  (jdbc/execute-one!
    db
    (sql/format {:select [:default_currency :default_payer_id :notifications_enabled]
                 :from [:user_expense_settings]
                 :where [:= :user_id user-id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn upsert-user-expense-settings!
  "Insert/update settings for `user-id`.

  `settings` must already be validated and should include all effective keys:
  - :default_currency (string)
  - :default_payer_id (UUID or nil)
  - :notifications_enabled (boolean)

  Returns the stored row (same shape as `get-user-expense-settings`)."
  [db user-id {:keys [default_currency default_payer_id notifications_enabled]}]
  (when-not (instance? UUID user-id)
    (throw (ex-info "user-id must be a UUID" {:user-id user-id})))
  (when-not (contains? allowed-currencies default_currency)
    (throw (ex-info "Unsupported currency" {:default_currency default_currency
                                            :allowed allowed-currencies})))
  (when-not (boolean? notifications_enabled)
    (throw (ex-info "notifications_enabled must be boolean" {:notifications_enabled notifications_enabled})))
  (when-not (or (nil? default_payer_id) (instance? UUID default_payer_id))
    (throw (ex-info "default_payer_id must be UUID or nil" {:default_payer_id default_payer_id})))
  (jdbc/execute-one!
    db
    [(str
       "INSERT INTO user_expense_settings "
       "(user_id, default_currency, default_payer_id, notifications_enabled, created_at, updated_at) "
       "VALUES (?, ?::currency, ?, ?, NOW(), NOW()) "
       "ON CONFLICT (user_id) DO UPDATE SET "
       "default_currency = EXCLUDED.default_currency, "
       "default_payer_id = EXCLUDED.default_payer_id, "
       "notifications_enabled = EXCLUDED.notifications_enabled, "
       "updated_at = NOW() "
       "RETURNING default_currency, default_payer_id, notifications_enabled")
     user-id
     default_currency
     default_payer_id
     notifications_enabled]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn effective-settings
  "Merge persisted settings over defaults."
  [persisted]
  (merge default-settings (or persisted {})))

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
   :default-expense-category-id nil
   :default-note nil
   :notifications-enabled true
   ;; When true, auto-post extracted receipts after upload (if data is complete).
   :auto-post-after-upload-enabled false
   ;; When true, allow the receipt OCR pipeline to run the optional AI refine step
   ;; (still subject to global Cerebras config / API key availability).
   :receipt-refine-enabled false})

(def allowed-currencies
  "Keep in sync with the domain :currency enum."
  #{"BAM" "EUR" "USD"})

(defn get-user-expense-settings
  "Return the persisted settings row for `user-id` scoped to `tenant-id`, or nil
   if none exists.

   Returned keys are kebab-case keywords including :default-currency,
   :default-payer-id, :notifications-enabled, and :receipt-refine-enabled."
  [db tenant-id user-id]
  (when-not (instance? UUID tenant-id)
    (throw (ex-info "tenant-id must be a UUID" {:tenant-id tenant-id})))
  (when-not (instance? UUID user-id)
    (throw (ex-info "user-id must be a UUID" {:user-id user-id})))
  (-> (jdbc/execute-one!
        db
        (sql/format {:select [:default_currency :default_payer_id :default_expense_category_id :default_note
                              :notifications_enabled :auto_post_after_upload_enabled :receipt_refine_enabled]
                     :from [:user_expense_settings]
                     :where [:and
                             [:= :tenant_id tenant-id]
                             [:= :user_id user-id]]})
        {:builder-fn rs/as-unqualified-lower-maps})
    db-adapter/to-app))

(defn upsert-user-expense-settings!
  "Insert/update settings for `user-id` scoped to `tenant-id`.

  `settings` must already be validated and should include all effective keys:
  - :default-currency (string)
  - :default-payer-id (UUID or nil)
  - :notifications-enabled (boolean)
  - :receipt-refine-enabled (boolean)

  Returns the stored row (same shape as `get-user-expense-settings`)."
  [db tenant-id user-id {:keys [default-currency default-payer-id default-expense-category-id default-note
                                notifications-enabled auto-post-after-upload-enabled receipt-refine-enabled]}]
  (when-not (instance? UUID tenant-id)
    (throw (ex-info "tenant-id must be a UUID" {:tenant-id tenant-id})))
  (when-not (instance? UUID user-id)
    (throw (ex-info "user-id must be a UUID" {:user-id user-id})))
  (when-not (contains? allowed-currencies default-currency)
    (throw (ex-info "Unsupported currency" {:default-currency default-currency
                                            :allowed allowed-currencies})))
  (when-not (boolean? notifications-enabled)
    (throw (ex-info "notifications-enabled must be boolean" {:notifications-enabled notifications-enabled})))
  (when-not (boolean? auto-post-after-upload-enabled)
    (throw (ex-info "auto-post-after-upload-enabled must be boolean"
             {:auto-post-after-upload-enabled auto-post-after-upload-enabled})))
  (when-not (boolean? receipt-refine-enabled)
    (throw (ex-info "receipt-refine-enabled must be boolean" {:receipt-refine-enabled receipt-refine-enabled})))
  (when-not (or (nil? default-payer-id) (instance? UUID default-payer-id))
    (throw (ex-info "default-payer-id must be UUID or nil" {:default-payer-id default-payer-id})))
  (when-not (or (nil? default-expense-category-id) (instance? UUID default-expense-category-id))
    (throw (ex-info "default-expense-category-id must be UUID or nil" {:default-expense-category-id default-expense-category-id})))
  (-> (jdbc/execute-one!
        db
        [(str
           "INSERT INTO user_expense_settings "
           "(id, tenant_id, user_id, default_currency, default_payer_id, default_expense_category_id, default_note, "
           "notifications_enabled, auto_post_after_upload_enabled, receipt_refine_enabled) "
           "VALUES (?, ?, ?, ?::currency, ?, ?, ?, ?, ?, ?) "
           "ON CONFLICT (tenant_id, user_id) DO UPDATE SET "
           "default_currency = EXCLUDED.default_currency, "
           "default_payer_id = EXCLUDED.default_payer_id, "
           "default_expense_category_id = EXCLUDED.default_expense_category_id, "
           "default_note = EXCLUDED.default_note, "
           "notifications_enabled = EXCLUDED.notifications_enabled, "
           "auto_post_after_upload_enabled = EXCLUDED.auto_post_after_upload_enabled, "
           "receipt_refine_enabled = EXCLUDED.receipt_refine_enabled "
           "RETURNING default_currency, default_payer_id, default_expense_category_id, default_note, "
           "notifications_enabled, auto_post_after_upload_enabled, receipt_refine_enabled")
         (UUID/randomUUID)
         tenant-id
         user-id
         default-currency
         default-payer-id
         default-expense-category-id
         default-note
         notifications-enabled
         auto-post-after-upload-enabled
         receipt-refine-enabled]
        {:builder-fn rs/as-unqualified-lower-maps})
    db-adapter/to-app))

(defn effective-settings
  "Merge persisted settings over defaults."
  [persisted]
  (merge default-settings (or persisted {})))

(defn update-sticky-default-payer!
  "If the user's current default payer differs from `payer-id`, update it.
   Called after expense creation and receipt approval to keep the default
   in sync with the user's most recent choice.

   No-op when `payer-id` is nil."
  [db tenant-id user-id payer-id]
  (when (and payer-id tenant-id user-id)
    (let [t-id (if (string? tenant-id) (UUID/fromString tenant-id) tenant-id)
          u-id (if (string? user-id) (UUID/fromString user-id) user-id)
          p-id (if (string? payer-id) (UUID/fromString payer-id) payer-id)
          current (get-user-expense-settings db t-id u-id)
          current-payer-id (:default-payer-id current)]
      (when (not= current-payer-id p-id)
        (jdbc/execute-one! db
          [(str
             "INSERT INTO user_expense_settings (id, tenant_id, user_id, default_currency, default_payer_id, created_at, updated_at) "
             "VALUES (?, ?, ?, 'BAM'::currency, ?, now(), now()) "
             "ON CONFLICT (tenant_id, user_id) DO UPDATE SET "
             "default_payer_id = EXCLUDED.default_payer_id, "
             "updated_at = now()")
           (UUID/randomUUID) t-id u-id p-id])))))

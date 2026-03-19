(ns app.domain.backend.expenses.services.user-expense-settings
  "Persistence for per-user expenses settings.

   Stored in the domain table `user_expense_settings` keyed by user_id.

   Settings hierarchy (Phase 2):
   - Global settings: default_currency, default_note, auto_publish_after_upload,
     ai_receipt_enhancement — from `global_settings` table
   - Tenant settings: email_notifications — from `tenant_settings` table
   - Per-user settings: default_payer_id, default_expense_category_id — this table

   The old columns (default_currency, default_note, notifications_enabled,
   auto_post_after_upload_enabled, receipt_refine_enabled) still exist in the DB
   for backward compatibility with the legacy settings handler. They will be
   dropped in Phase 5 after the old settings page is removed."
  (:require
    [app.shared.adapters.database :as db-adapter]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(def default-settings
  "Default values returned when a user has no persisted settings row yet.
   NOTE: :default-currency, :default-note, :notifications-enabled,
   :auto-post-after-upload-enabled, and :receipt-refine-enabled are kept here
   for backward compat with the legacy settings handler. Use
   `effective-settings-with-global` for the new merged view."
  {:default-currency "BAM"
   :default-payer-id nil
   :default-expense-category-id nil
   :default-note nil
   :notifications-enabled true
   :auto-post-after-upload-enabled false
   :receipt-refine-enabled false})

(def ^:deprecated allowed-currencies
  "DEPRECATED: Use enabled_currencies table via global-settings service instead.
   Kept for backward compat with the legacy settings handler."
  #{"BAM" "EUR" "USD" "GBP" "CHF" "RSD" "TRY"})

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

;; ---------------------------------------------------------------------------
;; New settings hierarchy helpers (Phase 2)
;; ---------------------------------------------------------------------------

(def per-user-defaults
  "Default values for per-user settings only (the fields that stay in this table)."
  {:default-payer-id nil
   :default-expense-category-id nil})

(defn effective-settings-with-global
  "Merge per-user persisted settings with global settings to produce
   the complete effective settings map.

   Returns:
   - :default-currency, :default-note, :auto-publish-after-upload,
     :ai-receipt-enhancement — from global-settings
   - :default-payer-id, :default-expense-category-id — from per-user persisted
   - :receipt-ocr-provider — from per-user persisted (internal, not user-facing)"
  [global-settings persisted-user-settings]
  (let [global (or global-settings {})
        user (or persisted-user-settings {})]
    {:default-currency (or (:default-currency global) "BAM")
     :default-note (:default-note global)
     :auto-publish-after-upload (boolean (:auto-publish-after-upload global))
     :ai-receipt-enhancement (boolean (:ai-receipt-enhancement global))
     :default-payer-id (or (:default-payer-id user)
                         (:default-payer-id per-user-defaults))
     :default-expense-category-id (or (:default-expense-category-id user)
                                    (:default-expense-category-id per-user-defaults))
     :receipt-ocr-provider (or (:receipt-ocr-provider user) "mistral")}))

(defn update-user-default-category!
  "Update only the default expense category for a user.
   Used by the new profile page."
  [db tenant-id user-id category-id]
  (when-not (instance? UUID tenant-id)
    (throw (ex-info "tenant-id must be a UUID" {:tenant-id tenant-id})))
  (when-not (instance? UUID user-id)
    (throw (ex-info "user-id must be a UUID" {:user-id user-id})))
  (when-not (or (nil? category-id) (instance? UUID category-id))
    (throw (ex-info "category-id must be UUID or nil" {:category-id category-id})))
  (-> (jdbc/execute-one!
        db
        [(str
           "INSERT INTO user_expense_settings (id, tenant_id, user_id, default_currency, default_expense_category_id) "
           "VALUES (?, ?, ?, 'BAM'::currency, ?) "
           "ON CONFLICT (tenant_id, user_id) DO UPDATE SET "
           "default_expense_category_id = EXCLUDED.default_expense_category_id, "
           "updated_at = now() "
           "RETURNING default_payer_id, default_expense_category_id")
         (UUID/randomUUID)
         tenant-id
         user-id
         category-id]
        {:builder-fn rs/as-unqualified-lower-maps})
    db-adapter/to-app))

(ns app.domain.backend.expenses.services.user-expense-settings
  "Persistence for per-user expense defaults.

   After the settings hierarchy split, `user_expense_settings` stores only the
   per-user defaults that remain tenant-scoped:
   - default_payer_id
   - receipt_ocr_provider"
  (:require
    [app.domain.backend.expenses.services.expense-categories :as expense-categories]
    [app.shared.adapters.database :as db-adapter]
    [app.template.backend.security.privacy-subject :as privacy-subject]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(def per-user-defaults
  "Default values for the slimmed per-user settings row."
  {:default-payer-id nil
   :receipt-ocr-provider "mistral"})

(defn- settings-owner-clause
  "Match a settings row by subject ref."
  [user-id]
  (privacy-subject/user-match-clause :subject_ref user-id))

(defn- update-settings-row!
  [tx tenant-id user-id subject-ref updates]
  (when (seq updates)
    (jdbc/execute-one!
      tx
      (sql/format {:update :user_expense_settings
                   :set (merge updates
                          {:subject_ref subject-ref
                           :updated_at [:now]})
                   :where [:and
                           [:= :tenant_id tenant-id]
                           (settings-owner-clause user-id)]
                   :returning [:default_payer_id :receipt_ocr_provider]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- insert-settings-row!
  [tx tenant-id subject-ref updates]
  (jdbc/execute-one!
    tx
    (sql/format {:insert-into :user_expense_settings
                 :values [(merge {:id (UUID/randomUUID)
                                  :tenant_id tenant-id
                                  :subject_ref subject-ref}
                            updates)]
                 :returning [:default_payer_id :receipt_ocr_provider]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn get-user-expense-settings
  "Return the persisted subject-ref settings row for `user-id` scoped to `tenant-id`,
   or nil if none exists."
  [db tenant-id user-id]
  (when-not (instance? UUID tenant-id)
    (throw (ex-info "tenant-id must be a UUID" {:tenant-id tenant-id})))
  (when-not (instance? UUID user-id)
    (throw (ex-info "user-id must be a UUID" {:user-id user-id})))
  (-> (jdbc/execute-one!
        db
        (sql/format {:select [:default_payer_id :receipt_ocr_provider]
                     :from [:user_expense_settings]
                     :where [:and
                             [:= :tenant_id tenant-id]
                             (settings-owner-clause user-id)]
                     :limit 1})
        {:builder-fn rs/as-unqualified-lower-maps})
    db-adapter/to-app))

(defn get-subject-expense-settings
  "Return the persisted settings row for `subject-ref` scoped to `tenant-id`,
   or nil if none exists."
  [db tenant-id subject-ref]
  (when-not (instance? UUID tenant-id)
    (throw (ex-info "tenant-id must be a UUID" {:tenant-id tenant-id})))
  (when-let [subject-ref* (some-> subject-ref str str/trim not-empty)]
    (-> (jdbc/execute-one!
          db
          (sql/format {:select [:default_payer_id :receipt_ocr_provider]
                       :from [:user_expense_settings]
                       :where [:and
                               [:= :tenant_id tenant-id]
                               [:= :subject_ref subject-ref*]]
                       :limit 1})
          {:builder-fn rs/as-unqualified-lower-maps})
      db-adapter/to-app)))

(defn effective-settings
  "Merge persisted per-user settings over defaults."
  [persisted]
  (merge per-user-defaults (or persisted {})))

(defn effective-default-expense-category-id
  "Resolve the tenant default category id used when a flow omits category.

   `user-id` is accepted for call-site compatibility, but the tenant default is
   now the only source of truth for expense-category fallback resolution."
  [db tenant-id _user-id]
  (let [tenant-id* (cond
                     (instance? UUID tenant-id) tenant-id
                     (string? tenant-id) (UUID/fromString tenant-id)
                     :else tenant-id)]
    (some-> (expense-categories/get-default-expense-category db tenant-id*)
      :id)))

(defn update-sticky-default-payer!
  "If the user's current default payer differs from `payer-id`, update it.
   Called after expense creation and receipt approval to keep the default
   in sync with the user's most recent choice.

   No-op when `payer-id` is nil. New/updated rows are keyed by subject_ref."
  [db tenant-id user-id payer-id]
  (when (and payer-id tenant-id user-id)
    (let [t-id (if (string? tenant-id) (UUID/fromString tenant-id) tenant-id)
          u-id (if (string? user-id) (UUID/fromString user-id) user-id)
          p-id (if (string? payer-id) (UUID/fromString payer-id) payer-id)
          subject-ref (privacy-subject/user-subject-ref u-id)
          current (get-user-expense-settings db t-id u-id)
          current-payer-id (:default-payer-id current)]
      (when (not= current-payer-id p-id)
        (jdbc/with-transaction [tx db]
          (or (update-settings-row! tx t-id u-id subject-ref {:default_payer_id p-id})
            (insert-settings-row! tx t-id subject-ref {:default_payer_id p-id})))))))

(defn effective-settings-with-global
  "Merge global settings with per-user persisted settings to produce the
   complete effective settings map returned by the profile API."
  [global-settings persisted-user-settings]
  (let [global (or global-settings {})
        user (effective-settings persisted-user-settings)]
    {:default-currency (or (:default-currency global) "BAM")
     :default-note (:default-note global)
     :auto-publish-after-upload (boolean (:auto-publish-after-upload global))
     :ai-receipt-enhancement (boolean (:ai-receipt-enhancement global))
     :default-payer-id (:default-payer-id user)
     :receipt-ocr-provider (:receipt-ocr-provider user)}))

(defn update-user-defaults!
  "Update per-user defaults (payer only).
   Used by the profile page save-defaults action. New/updated rows are keyed by
   subject_ref."
  [db tenant-id user-id {:keys [default-payer-id]}]
  (when-not (instance? UUID tenant-id)
    (throw (ex-info "tenant-id must be a UUID" {:tenant-id tenant-id})))
  (when-not (instance? UUID user-id)
    (throw (ex-info "user-id must be a UUID" {:user-id user-id})))
  (when-not (or (nil? default-payer-id) (instance? UUID default-payer-id))
    (throw (ex-info "payer-id must be UUID or nil" {:payer-id default-payer-id})))
  (let [subject-ref (privacy-subject/user-subject-ref user-id)
        updates {:default_payer_id default-payer-id}]
    (-> (jdbc/with-transaction [tx db]
          (or (update-settings-row! tx tenant-id user-id subject-ref updates)
            (insert-settings-row! tx tenant-id subject-ref updates)))
      db-adapter/to-app)))

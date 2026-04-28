(ns app.domain.backend.expenses.privacy
  "Admin-facing privacy views for expenses domain data.

   These helpers keep global/admin operational access to receipts and expenses
   while removing direct user linkage from routine response payloads. They are
   intentionally API-boundary helpers: persistence rows still contain the IDs
   needed for user-facing authorization and ownership checks."
  (:require
    [app.shared.adapters.database :as shared-db]
    [clojure.walk :as walk]))

(def ^:private user-linkage-keys
  #{:user_id
    :user-id
    :users/id
    :created_by
    :created-by
    :created_by_user_id
    :created-by-user-id
    :created_by_full_name
    :created-by-full-name
    :created_by_name
    :created-by-name
    :created_by_email
    :created-by-email
    :created_by_email_ciphertext
    :created-by-email-ciphertext
    :user_full_name
    :user-full-name
    :user_display_name
    :user-display-name
    :user_ref
    :user-ref
    :user_email
    :user-email
    :user_email_ciphertext
    :user-email-ciphertext
    :email_masked
    :email-masked
    :email_ciphertext
    :email-ciphertext
    :email_lookup_hash
    :email-lookup-hash
    :email_key_version
    :email-key-version})

(defn scrub-user-linkage
  "Remove direct user identity/linkage fields from a response payload.

   Works on maps, vectors, lists, and nested payloads such as a receipt detail
   containing a `:linked-expense`. Business identifiers needed for editing the
   receipt/expense itself (`:id`, `:receipt-id`, `:expense-id`, payer/category
   IDs, etc.) are preserved."
  [payload]
  (walk/postwalk
    (fn [value]
      (if (map? value)
        (apply dissoc value user-linkage-keys)
        value))
    payload))

(defn admin-expense-view
  "Return an admin-facing expense payload without user linkage."
  [expense]
  (some-> expense shared-db/to-app scrub-user-linkage))

(defn admin-expenses-view
  "Return admin-facing expense payloads without user linkage."
  [expenses]
  (scrub-user-linkage (shared-db/to-app expenses)))

(defn admin-receipt-view
  "Return an admin-facing receipt payload without user linkage."
  [receipt]
  (some-> receipt shared-db/to-app scrub-user-linkage))

(defn admin-receipts-view
  "Return admin-facing receipt payloads without user linkage."
  [receipts]
  (scrub-user-linkage (shared-db/to-app receipts)))

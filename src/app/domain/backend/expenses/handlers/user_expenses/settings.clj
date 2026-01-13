(ns app.domain.backend.expenses.handlers.user-expenses.settings
  "User expense settings and export handlers.

   Settings are persisted per-user in `user_expense_settings`."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.user-expense-settings :as user-expense-settings]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Role definitions
;; ---------------------------------------------------------------------------

(def power-user-roles
  "Roles allowed to execute danger-zone actions (delete-all)."
  #{"admin" "owner"})

(def ^:private settings-read-roles
  "Roles allowed to view per-user settings."
  h/reference-data-read-roles)

(def ^:private settings-write-roles
  "Roles allowed to mutate per-user settings."
  h/reference-data-write-roles)

;; ---------------------------------------------------------------------------
;; Settings handlers
;; ---------------------------------------------------------------------------

(defn- parse-notifications-enabled
  [v]
  (cond
    (nil? v) nil
    (boolean? v) v
    (string? v) (case (str/lower-case (str/trim v))
                  "true" true
                  "false" false
                  nil)
    :else nil))

(defn- blank->nil
  [v]
  (cond
    (nil? v) nil
    (string? v) (let [s (str/trim v)]
                  (when-not (str/blank? s) s))
    :else v))

(defn- normalize-settings-update
  "Return {:settings <effective-settings>} or {:error <ring-response>}.

  Supports partial updates by applying only keys present in the request body.
  A blank :default_payer_id (\"\" / whitespace) clears the payer (sets nil)."
  [current body]
  ;; Normalize body keys to keywords (handles JSON string keys)
  (let [body (reduce-kv (fn [m k v]
                          (assoc m (keyword k) v))
               {}
               body)
        _ (log/info "After key normalization" {:body body})
        supported-keys #{:default_currency :default_payer_id :notifications_enabled}
        present-keys (->> supported-keys (filter #(contains? body %)) set)]
    (log/info "Settings validation" {:present-keys present-keys :supported-keys supported-keys})
    (cond
      (empty? present-keys)
      {:error (h/json-response {:error "No settings fields provided"} 400)}

      :else
      (let [currency (when (contains? body :default_currency)
                       (blank->nil (:default_currency body)))
            payer-id-raw (when (contains? body :default_payer_id)
                           (blank->nil (:default_payer_id body)))
            payer-id (when (contains? body :default_payer_id)
                       (if (nil? payer-id-raw)
                         nil
                         (h/try-parse-uuid payer-id-raw)))
            notifications (when (contains? body :notifications_enabled)
                            (parse-notifications-enabled (:notifications_enabled body)))]
        (cond
          (and (contains? body :default_currency)
            (nil? currency))
          {:error (h/json-response {:error "default_currency is required"} 400)}

          (and (contains? body :default_currency)
            (not (contains? user-expense-settings/allowed-currencies currency)))
          {:error (h/json-response {:error "Unsupported currency"
                                    :allowed (sort user-expense-settings/allowed-currencies)} 400)}

          (and (contains? body :default_payer_id)
            (some? payer-id-raw)
            (nil? payer-id))
          {:error (h/json-response {:error "default_payer_id must be a UUID (or blank to clear)"} 400)}

          (and (contains? body :notifications_enabled)
            (nil? notifications))
          {:error (h/json-response {:error "notifications_enabled must be a boolean"} 400)}

          :else
          {:settings
           (cond-> current
             (contains? body :default_currency) (assoc :default_currency currency)
             (contains? body :default_payer_id) (assoc :default_payer_id payer-id)
             (contains? body :notifications_enabled) (assoc :notifications_enabled notifications))})))))

(defn get-settings-handler
  "GET /api/v1/expenses/settings - fetch user settings.
   Returns defaults if the user has no persisted settings yet."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request settings-read-roles "Role assignment required")]
        forbidden
        (try
          (let [persisted (user-expense-settings/get-user-expense-settings db user-id)
                effective (user-expense-settings/effective-settings persisted)]
            (h/json-response effective))
          (catch Exception e
            (log/error e "Failed to load user expense settings" {:user-id user-id})
            (h/json-response {:error "Failed to load settings"} 500))))
      (h/unauthorized-response))))

(defn update-settings-handler
  "PUT /api/v1/expenses/settings - update user settings.
   Supports partial updates."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request settings-write-roles
                           "Only members, admins, and owners can update settings")]
        forbidden
        (try
          (let [body (h/read-body-params request)
                _ (log/info "Settings update request body" {:body body :body-type (type body)})
                persisted (user-expense-settings/get-user-expense-settings db user-id)
                current (user-expense-settings/effective-settings persisted)
                {:keys [settings error]} (normalize-settings-update current body)]
            (if error
              error
              (let [stored (user-expense-settings/upsert-user-expense-settings! db user-id settings)]
                (log/info "Updated user expense settings" {:user-id user-id
                                                           :keys (keys body)})
                (h/json-response stored))))
          (catch Exception e
            (log/error e "Failed to update user expense settings" {:user-id user-id})
            (h/json-response {:error "Failed to update settings"} 500))))
      (h/unauthorized-response))))

;; ---------------------------------------------------------------------------
;; Export handler
;; ---------------------------------------------------------------------------

(defn export-expenses-handler
  "GET /api/v1/expenses/export - export expenses as CSV/PDF.
   Returns a download URL or the data directly."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (let [params (:query-params request)
            format (or (h/get-param params :format) "csv")]
        (try
          (let [expenses (jdbc/execute! db
                           ["SELECT e.*, s.display_name as supplier_name, p.label as payer_label
                             FROM expenses e
                             LEFT JOIN suppliers s ON e.supplier_id = s.id
                             LEFT JOIN payers p ON e.payer_id = p.id
                             WHERE e.user_id = ? AND e.deleted_at IS NULL
                             ORDER BY e.purchased_at DESC
                             LIMIT 1000"
                            user-id])]
            (if (= format "csv")
              ;; Return CSV data directly
              (let [header "id,purchased_at,supplier,payer,total_amount,currency,notes\n"
                    rows (->> expenses
                           (map (fn [e]
                                  (str/join ","
                                    [(str (:expenses/id e))
                                     (str (:expenses/purchased_at e))
                                     (str "\"" (or (:supplier_name e) "") "\"")
                                     (str "\"" (or (:payer_label e) "") "\"")
                                     (str (:expenses/total_amount e))
                                     (str (:expenses/currency e))
                                     (str "\"" (str/replace (or (:expenses/notes e) "") "\"" "\"\"") "\"")])))
                           (str/join "\n"))
                    csv-content (str header rows)]
                {:status 200
                 :headers {"Content-Type" "text/csv"
                           "Content-Disposition" "attachment; filename=\"expenses.csv\""}
                 :body csv-content})
              ;; For PDF, return stub message
              (h/json-response {:message "PDF export not yet implemented"
                                :format format
                                :count (count expenses)} 200)))
          (catch Exception e
            (log/error e "Failed to export expenses")
            (h/json-response {:error "Export failed"} 500))))
      (h/unauthorized-response))))

;; ---------------------------------------------------------------------------
;; Delete-all handler (danger zone, admin/owner only)
;; ---------------------------------------------------------------------------

(defn delete-all-expenses-handler
  "DELETE /api/v1/expenses/all - permanently delete all user expenses.
   Requires admin/owner role and confirmation token."
  [db]
  (fn [request]
    (if-let [user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request power-user-roles
                           "Only admin/owner can delete all expenses")]
        forbidden
        (let [params (or (:body-params request)
                       (:query-params request)
                       {})
              confirmation (or (h/get-param params :confirmation)
                             (h/get-param params :token))]
          (if (= confirmation "DELETE_ALL_EXPENSES")
            (try
              ;; Soft-delete all expenses for user
              (let [result (jdbc/execute-one! db
                             ["UPDATE expenses
                               SET deleted_at = NOW()
                               WHERE user_id = ? AND deleted_at IS NULL"
                              user-id])]
                (log/warn "User deleted all expenses" {:user-id user-id
                                                       :affected (:next.jdbc/update-count result)})
                (h/json-response {:success true
                                  :deleted_count (or (:next.jdbc/update-count result) 0)}))
              (catch Exception e
                (log/error e "Failed to delete all expenses")
                (h/json-response {:error "Delete failed"} 500)))
            (h/json-response {:error "Invalid confirmation token"} 400))))
      (h/unauthorized-response))))

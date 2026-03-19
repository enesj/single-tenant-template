(ns app.domain.backend.expenses.services.global-settings
  "Service for platform-wide global settings (singleton row).

   Manages: default currency, default note, auto-publish toggle,
   AI receipt enhancement toggle, and the enabled currencies list."
  (:require
    [app.shared.adapters.database :as db-adapter]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

;; ---------------------------------------------------------------------------
;; Global Settings (singleton)
;; ---------------------------------------------------------------------------

(defn get-global-settings
  "Return the singleton global settings row.
   Returns nil if the row hasn't been seeded yet (shouldn't happen in practice)."
  [db]
  (-> (jdbc/execute-one!
        db
        (sql/format {:select [:*]
                     :from [:global_settings]
                     :limit 1})
        {:builder-fn rs/as-unqualified-lower-maps})
    db-adapter/to-app))

(defn update-global-settings!
  "Partial update of global settings. Only provided keys are changed.

   Accepted keys:
   - :default-currency (string, must be in enabled currencies)
   - :default-note (string or nil)
   - :auto-publish-after-upload (boolean)
   - :ai-receipt-enhancement (boolean)"
  [db updates]
  (let [allowed-keys #{:default-currency :default-note
                       :auto-publish-after-upload :ai-receipt-enhancement}
        filtered (select-keys updates allowed-keys)]
    (when (empty? filtered)
      (throw (ex-info "No valid update keys provided" {:updates updates})))
    (when (contains? filtered :default-currency)
      (let [currency (:default-currency filtered)
            enabled (jdbc/execute-one! db
                      (sql/format {:select [[:code :code]]
                                   :from [:enabled_currencies]
                                   :where [:= :code currency]})
                      {:builder-fn rs/as-unqualified-lower-maps})]
        (when-not enabled
          (throw (ex-info "Currency is not in the enabled list"
                   {:currency currency})))))
    (let [db-updates (cond-> {}
                       (contains? filtered :default-currency)
                       (assoc :default_currency (:default-currency filtered))

                       (contains? filtered :default-note)
                       (assoc :default_note (:default-note filtered))

                       (contains? filtered :auto-publish-after-upload)
                       (assoc :auto_publish_after_upload (:auto-publish-after-upload filtered))

                       (contains? filtered :ai-receipt-enhancement)
                       (assoc :ai_receipt_enhancement (:ai-receipt-enhancement filtered))

                       true
                       (assoc :updated_at [:now]))]
      (-> (jdbc/execute-one!
            db
            (sql/format {:update :global_settings
                         :set db-updates
                         :returning [:*]})
            {:builder-fn rs/as-unqualified-lower-maps})
        db-adapter/to-app))))

;; ---------------------------------------------------------------------------
;; Enabled Currencies
;; ---------------------------------------------------------------------------

(defn get-enabled-currencies
  "Return all enabled currencies, ordered with base currency first."
  [db]
  (->> (jdbc/execute!
         db
         (sql/format {:select [:id :code :name :is_base]
                      :from [:enabled_currencies]
                      :order-by [[:is_base :desc] [:code :asc]]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (mapv db-adapter/to-app)))

(defn add-enabled-currency!
  "Add a currency to the enabled list.
   Returns the new row. Throws if code already exists."
  [db {:keys [code name]}]
  (when (or (nil? code) (nil? name))
    (throw (ex-info "Currency code and name are required" {:code code :name name})))
  (-> (jdbc/execute-one!
        db
        (sql/format {:insert-into :enabled_currencies
                     :values [{:id (UUID/randomUUID)
                               :code code
                               :name name
                               :is_base false}]
                     :returning [:*]})
        {:builder-fn rs/as-unqualified-lower-maps})
    db-adapter/to-app))

(defn remove-enabled-currency!
  "Remove a currency from the enabled list.
   BAM (base currency) cannot be removed.
   Also validates that the default currency isn't being removed."
  [db code]
  (when (= code "BAM")
    (throw (ex-info "Cannot remove base currency BAM" {:code code})))
  (let [global (get-global-settings db)]
    (when (= code (:default-currency global))
      (throw (ex-info "Cannot remove the current default currency"
               {:code code :default-currency (:default-currency global)}))))
  (let [result (jdbc/execute-one!
                 db
                 (sql/format {:delete-from :enabled_currencies
                              :where [:= :code code]
                              :returning [:id]})
                 {:builder-fn rs/as-unqualified-lower-maps})]
    (when-not result
      (throw (ex-info "Currency not found in enabled list" {:code code})))
    true))

(comment
  ;; REPL exploration
  ;; (require '[app.domain.backend.expenses.services.global-settings :as gs] :reload)
  ;; (gs/get-global-settings db)
  ;; (gs/get-enabled-currencies db)
  ;; (gs/update-global-settings! db {:default-note "Test note"})
  :rcf)

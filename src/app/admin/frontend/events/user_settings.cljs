(ns app.admin.frontend.events.user-settings
  "Admin UI for editing domain-owned, user-facing UI defaults.

  This page powers /admin/user-settings, but the saved data lives in
  `src/app/domain/frontend/expenses/config/*` (via the backend API).

  It is intentionally NOT per-user localStorage prefs."
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private user-ui-config-uri
  "/admin/api/settings/user-ui-config")

(defn- safe-map [x]
  (if (map? x) x {}))

(defn- normalize-kw [x]
  (cond
    (nil? x) nil
    (keyword? x) x
    (string? x) (keyword x)
    :else (keyword (str x))))

(defn- normalize-cols [xs]
  (->> (or xs [])
    (keep normalize-kw)
    vec))

(defn- normalize-map-keys
  "Keywordize map keys using normalize-kw, skipping keys that normalize to nil.

  This is primarily to handle JSON-decoded responses where entity keys or
  setting keys arrive as strings." 
  [m]
  (reduce-kv
    (fn [acc k v]
      (if-let [k' (normalize-kw k)]
        (assoc acc k' v)
        acc))
    {}
    (safe-map m)))

(defn- normalize-entity-map
  "Normalize a top-level entity map of {entity-key -> config-map}.

  Entity keys are keywordized. The entity config map's keys are also
  keywordized (values are left as-is)." 
  [m]
  (reduce-kv
    (fn [acc entity-k entity-v]
      (if-let [entity-kw (normalize-kw entity-k)]
        (assoc acc entity-kw (if (map? entity-v) (normalize-map-keys entity-v) entity-v))
        acc))
    {}
    (safe-map m)))

(defn- normalize-user-view-options
  "Normalize user view-options config.

  - Entity keys are keywordized.
  - Entity config keys are keywordized.
  - Nested maps for defaults/locks (display + columns) have their keys
    keywordized (so :expenses lookups work and bulk controls compute correctly)." 
  [view-options]
  (reduce-kv
    (fn [acc entity-k entity-v]
      (if-let [entity-kw (normalize-kw entity-k)]
        (let [cfg (if (map? entity-v) (normalize-map-keys entity-v) {})
              cfg (cond-> cfg
                    (contains? cfg :display-defaults) (update :display-defaults normalize-map-keys)
                    (contains? cfg :display-locks) (update :display-locks normalize-map-keys)
                    (contains? cfg :column-defaults) (update :column-defaults normalize-map-keys)
                    (contains? cfg :column-locks) (update :column-locks normalize-map-keys))]
          (assoc acc entity-kw cfg))
        acc))
    {}
    (safe-map view-options)))

(defn- draft-config
  "Return the current draft config structure."
  [db]
  (safe-map (get-in db [:admin :user-settings :draft])))

(defn- saved-config
  "Return the last saved config structure."
  [db]
  (safe-map (get-in db [:admin :user-settings :saved])))

;; =============================================================================
;; Load
;; =============================================================================

(rf/reg-event-fx
  ::init
  (fn [{:keys [db]} _]
    (log/info "Init user-ui settings editor")
    {:db (-> db
           (assoc-in [:admin :user-settings :loading?] true)
           (assoc-in [:admin :user-settings :saving?] false)
           (assoc-in [:admin :user-settings :error] nil)
           (assoc-in [:admin :user-settings :tab] "view-options")
           (assoc-in [:admin :user-settings :last-saved] nil))
     :http-xhrio (admin-http/admin-get
                   {:uri user-ui-config-uri
                    :on-success [::load-success]
                    :on-failure [::load-failure]})}))

(rf/reg-event-db
  ::load-success
  (fn [db [_ response]]
    (let [entities (normalize-entity-map (:entities response))
          view-options (normalize-user-view-options (:view-options response))
          form-fields (normalize-entity-map (:form-fields response))
          table-columns (normalize-entity-map (:table-columns response))
          draft {:entities entities
                 :view-options view-options
                 :form-fields form-fields
                 :table-columns table-columns}]
      (log/info "Loaded user UI config" {:entities (count entities)
                                         :view-options (count view-options)
                                         :table-columns (count table-columns)})
      (-> db
        ;; Make this config available for user routes (non-admin).
        (assoc-in [:domain :config :entities] entities)
        (assoc-in [:domain :config :view-options] view-options)
        (assoc-in [:domain :config :form-fields] form-fields)
        (assoc-in [:domain :config :table-columns] table-columns)

        ;; Editor state
        (assoc-in [:admin :user-settings :draft] draft)
        (assoc-in [:admin :user-settings :saved] draft)
        (assoc-in [:admin :user-settings :loading?] false)
        (assoc-in [:admin :user-settings :saving?] false)
        (assoc-in [:admin :user-settings :error] nil)))))

(rf/reg-event-db
  ::load-failure
  (fn [db [_ error]]
    (let [msg (admin-http/extract-error-message error)]
      (log/error "Failed to load user UI config" error)
      (-> db
        (assoc-in [:admin :user-settings :loading?] false)
        (assoc-in [:admin :user-settings :saving?] false)
        (assoc-in [:admin :user-settings :error] msg)))))

;; =============================================================================
;; Tabs
;; =============================================================================

(rf/reg-event-db
  ::set-tab
  (fn [db [_ tab]]
    (assoc-in db [:admin :user-settings :tab] tab)))

;; =============================================================================
;; Draft editing: view-options (defaults + locks)
;; =============================================================================

;; new-state schema:
;; - {:kind :inherit}
;; - {:kind :default :value boolean}
;; - {:kind :lock :value boolean}
(rf/reg-event-db
  ::set-display-setting-draft
  (fn [db [_ entity setting-key new-state]]
    (let [entity-kw (normalize-kw entity)
          setting-kw (normalize-kw setting-key)
          defaults-path [:admin :user-settings :draft :view-options entity-kw :display-defaults]
          locks-path [:admin :user-settings :draft :view-options entity-kw :display-locks]
          kind (:kind new-state)
          value (:value new-state)]
      (cond
        (or (nil? entity-kw) (nil? setting-kw))
        db

        (= kind :inherit)
        (-> db
          (update-in defaults-path (fnil dissoc {}) setting-kw)
          (update-in locks-path (fnil dissoc {}) setting-kw))

        (and (= kind :default) (boolean? value))
        (-> db
          ;; A default only applies when not locked.
          (update-in locks-path (fnil dissoc {}) setting-kw)
          (assoc-in (conj defaults-path setting-kw) value))

        (and (= kind :lock) (boolean? value))
        (-> db
          ;; A lock supersedes any default.
          (update-in defaults-path (fnil dissoc {}) setting-kw)
          (assoc-in (conj locks-path setting-kw) value))

        :else
        db))))

;; =============================================================================
;; Draft editing: column visibility policy (defaults + locks)
;; =============================================================================

;; new-state schema:
;; - {:kind :inherit}
;; - {:kind :default :value boolean}
;; - {:kind :lock :value boolean}
(rf/reg-event-db
  ::set-column-visibility-setting-draft
  (fn [db [_ entity column-key new-state]]
    (let [entity-kw (normalize-kw entity)
          column-kw (normalize-kw column-key)
          defaults-path [:admin :user-settings :draft :view-options entity-kw :column-defaults]
          locks-path [:admin :user-settings :draft :view-options entity-kw :column-locks]
          kind (:kind new-state)
          value (:value new-state)]
      (cond
        (or (nil? entity-kw) (nil? column-kw))
        db

        (= kind :inherit)
        (-> db
          (update-in defaults-path (fnil dissoc {}) column-kw)
          (update-in locks-path (fnil dissoc {}) column-kw))

        (and (= kind :default) (boolean? value))
        (-> db
          ;; A default only applies when not locked.
          (update-in locks-path (fnil dissoc {}) column-kw)
          (assoc-in (conj defaults-path column-kw) value))

        (and (= kind :lock) (boolean? value))
        (-> db
          ;; A lock supersedes any default.
          (update-in defaults-path (fnil dissoc {}) column-kw)
          (assoc-in (conj locks-path column-kw) value))

        :else
        db))))

;; =============================================================================
;; Bulk helpers: column visibility defaults
;; =============================================================================

(rf/reg-event-db
  ::set-column-defaults-bulk
  (fn [db [_ entity column-keys value]]
    (let [entity-kw (normalize-kw entity)
          cols (->> (or column-keys [])
                 (keep normalize-kw)
                 vec)
          value (boolean value)
          defaults-path [:admin :user-settings :draft :view-options entity-kw :column-defaults]
          locks-path [:admin :user-settings :draft :view-options entity-kw :column-locks]]
      (cond
        (or (nil? entity-kw) (empty? cols))
        db

        :else
        (reduce (fn [db' col]
                  (assoc-in db' (conj defaults-path col) value))
          (update-in db locks-path (fnil (fn [m] (apply dissoc m cols)) {}))
          cols)))))

;; =============================================================================
;; Bulk helpers: apply tristate to many display settings / columns
;; =============================================================================

(rf/reg-event-db
  ::set-display-settings-bulk
  (fn [db [_ entity setting-keys new-state]]
    (let [entity-kw (normalize-kw entity)
          keys (->> (or setting-keys [])
                 (keep normalize-kw)
                 vec)
          kind (:kind new-state)
          value (:value new-state)
          defaults-path [:admin :user-settings :draft :view-options entity-kw :display-defaults]
          locks-path [:admin :user-settings :draft :view-options entity-kw :display-locks]]
      (cond
        (or (nil? entity-kw) (empty? keys))
        db

        (= kind :inherit)
        (-> db
          (update-in defaults-path (fnil (fn [m] (apply dissoc m keys)) {}))
          (update-in locks-path (fnil (fn [m] (apply dissoc m keys)) {})))

        (and (= kind :default) (boolean? value))
        (reduce (fn [db' k]
            (assoc-in db' (conj defaults-path k) value))
          (update-in db locks-path (fnil (fn [m] (apply dissoc m keys)) {}))
          keys)

        (and (= kind :lock) (boolean? value))
        (reduce (fn [db' k]
            (assoc-in db' (conj locks-path k) value))
          (update-in db defaults-path (fnil (fn [m] (apply dissoc m keys)) {}))
          keys)

        :else
        db))))

(rf/reg-event-db
  ::set-column-visibility-bulk
  (fn [db [_ entity column-keys new-state]]
    (let [entity-kw (normalize-kw entity)
          cols (->> (or column-keys [])
                 (keep normalize-kw)
                 vec)
          kind (:kind new-state)
          value (:value new-state)
          defaults-path [:admin :user-settings :draft :view-options entity-kw :column-defaults]
          locks-path [:admin :user-settings :draft :view-options entity-kw :column-locks]]
      (cond
        (or (nil? entity-kw) (empty? cols))
        db

        (= kind :inherit)
        (-> db
          (update-in defaults-path (fnil (fn [m] (apply dissoc m cols)) {}))
          (update-in locks-path (fnil (fn [m] (apply dissoc m cols)) {})))

        (and (= kind :default) (boolean? value))
        (reduce (fn [db' c]
            (assoc-in db' (conj defaults-path c) value))
          (update-in db locks-path (fnil (fn [m] (apply dissoc m cols)) {}))
          cols)

        (and (= kind :lock) (boolean? value))
        (reduce (fn [db' c]
            (assoc-in db' (conj locks-path c) value))
          (update-in db defaults-path (fnil (fn [m] (apply dissoc m cols)) {}))
          cols)

        :else
        db))))

(rf/reg-event-db
  ::reset-entity-display-draft
  (fn [db [_ entity]]
    (let [entity-kw (normalize-kw entity)
          saved-defaults (get-in db [:admin :user-settings :saved :view-options entity-kw :display-defaults])
          saved-locks (get-in db [:admin :user-settings :saved :view-options entity-kw :display-locks])
          saved-col-defaults (get-in db [:admin :user-settings :saved :view-options entity-kw :column-defaults])
          saved-col-locks (get-in db [:admin :user-settings :saved :view-options entity-kw :column-locks])]
      (cond
        (nil? entity-kw)
        db

        :else
        (-> db
          (cond->
            (map? saved-defaults)
            (assoc-in [:admin :user-settings :draft :view-options entity-kw :display-defaults] saved-defaults)

            (not (map? saved-defaults))
            (update-in [:admin :user-settings :draft :view-options entity-kw] dissoc :display-defaults))

          (cond->
            (map? saved-locks)
            (assoc-in [:admin :user-settings :draft :view-options entity-kw :display-locks] saved-locks)

            (not (map? saved-locks))
            (update-in [:admin :user-settings :draft :view-options entity-kw] dissoc :display-locks))

          (cond->
            (map? saved-col-defaults)
            (assoc-in [:admin :user-settings :draft :view-options entity-kw :column-defaults] saved-col-defaults)

            (not (map? saved-col-defaults))
            (update-in [:admin :user-settings :draft :view-options entity-kw] dissoc :column-defaults))

          (cond->
            (map? saved-col-locks)
            (assoc-in [:admin :user-settings :draft :view-options entity-kw :column-locks] saved-col-locks)

            (not (map? saved-col-locks))
            (update-in [:admin :user-settings :draft :view-options entity-kw] dissoc :column-locks)))))))

;; =============================================================================
;; Draft editing: table-columns defaults
;; =============================================================================

(rf/reg-event-db
  ::toggle-column-visibility-draft
  (fn [db [_ entity column-name]]
    (let [entity-kw (normalize-kw entity)
          column-kw (normalize-kw column-name)
          entity-config (safe-map (get-in db [:admin :user-settings :draft :table-columns entity-kw]))
          available (normalize-cols (:available-columns entity-config))
          always-visible-set (into #{} (normalize-cols (:always-visible entity-config)))
          has-default-visible? (contains? entity-config :default-visible-columns)
          default-visible (normalize-cols (if has-default-visible?
                                           (:default-visible-columns entity-config)
                                           available))
          visible-set (into #{} default-visible)
          visible-set (into visible-set always-visible-set)]
      (cond
        (or (nil? entity-kw) (nil? column-kw))
        db

        (not (some #{column-kw} available))
        db

        (contains? always-visible-set column-kw)
        db

        :else
        (let [currently-visible? (contains? visible-set column-kw)
              new-visible-set (if currently-visible?
                                (disj visible-set column-kw)
                                (conj visible-set column-kw))
              ;; Preserve ordering based on available-columns
              new-visible (->> available
                            (filter new-visible-set)
                            vec)]
          (assoc-in db
            [:admin :user-settings :draft :table-columns entity-kw :default-visible-columns]
            new-visible))))))

(rf/reg-event-db
  ::reset-columns-draft
  (fn [db [_ entity]]
    (let [entity-kw (normalize-kw entity)
          saved-entity-config (get-in db [:admin :user-settings :saved :table-columns entity-kw])]
      (if (nil? entity-kw)
        db
        (assoc-in db [:admin :user-settings :draft :table-columns entity-kw] (safe-map saved-entity-config))))))

;; =============================================================================
;; Save / Discard
;; =============================================================================

(rf/reg-event-db
  ::discard-draft
  (fn [db _]
    (assoc-in db [:admin :user-settings :draft] (saved-config db))))

(rf/reg-event-fx
  ::save
  (fn [{:keys [db]} _]
    (let [draft (draft-config db)
          payload (select-keys draft [:entities :view-options :form-fields :table-columns])]
      (log/info "Saving user UI config" {:entities (count (:entities payload))
                                         :view-options (count (:view-options payload))
                                         :table-columns (count (:table-columns payload))})
      {:db (-> db
             (assoc-in [:admin :user-settings :saving?] true)
             (assoc-in [:admin :user-settings :error] nil))
       :http-xhrio (admin-http/admin-put
                     {:uri user-ui-config-uri
                      :params payload
                      :on-success [::save-success]
                      :on-failure [::save-failure]})})))

(rf/reg-event-db
  ::save-success
  (fn [db [_ response]]
    (let [entities (normalize-entity-map (:entities response))
          view-options (normalize-user-view-options (:view-options response))
          form-fields (normalize-entity-map (:form-fields response))
          table-columns (normalize-entity-map (:table-columns response))
          saved {:entities entities
                 :view-options view-options
                 :form-fields form-fields
                 :table-columns table-columns}]
      (log/info "Saved user UI config")
      (-> db
        ;; Keep domain config in sync for user routes.
        (assoc-in [:domain :config :entities] entities)
        (assoc-in [:domain :config :view-options] view-options)
        (assoc-in [:domain :config :form-fields] form-fields)
        (assoc-in [:domain :config :table-columns] table-columns)

        ;; Editor state
        (assoc-in [:admin :user-settings :saved] saved)
        (assoc-in [:admin :user-settings :draft] saved)
        (assoc-in [:admin :user-settings :saving?] false)
        (assoc-in [:admin :user-settings :error] nil)
        (assoc-in [:admin :user-settings :last-saved] (js/Date.now))))))

(rf/reg-event-db
  ::save-failure
  (fn [db [_ error]]
    (let [msg (admin-http/extract-error-message error)]
      (log/error "Failed to save user UI config" error)
      (-> db
        (assoc-in [:admin :user-settings :saving?] false)
        (assoc-in [:admin :user-settings :error] msg)))))

;; =============================================================================
;; Subscriptions
;; =============================================================================

(rf/reg-sub
  ::draft
  (fn [db _]
    (draft-config db)))

#_(rf/reg-sub
  ::saved
  (fn [db _]
    (saved-config db)))

(rf/reg-sub
  ::dirty?
  (fn [db _]
    (not= (draft-config db) (saved-config db))))

(rf/reg-sub
  ::tab
  (fn [db _]
    (get-in db [:admin :user-settings :tab] "view-options")))

(rf/reg-sub
  ::loading?
  (fn [db _]
    (get-in db [:admin :user-settings :loading?] false)))

(rf/reg-sub
  ::saving?
  (fn [db _]
    (get-in db [:admin :user-settings :saving?] false)))

(rf/reg-sub
  ::error
  (fn [db _]
    (get-in db [:admin :user-settings :error])))

(rf/reg-sub
  ::last-saved
  (fn [db _]
    (get-in db [:admin :user-settings :last-saved])))

(rf/reg-sub
  ::table-columns-config
  (fn [db _]
    (safe-map (get-in db [:admin :user-settings :draft :table-columns]))))

;; =============================================================================
;; Draft editing: entities config
;; =============================================================================

(rf/reg-event-db
  ::set-entity-title-draft
  (fn [db [_ entity new-title]]
    (let [entity-kw (normalize-kw entity)]
      (if (nil? entity-kw)
        db
        (assoc-in db [:admin :user-settings :draft :entities entity-kw :title] new-title)))))

(rf/reg-event-db
  ::reset-entity-draft
  (fn [db [_ entity]]
    (let [entity-kw (normalize-kw entity)
          saved-entity (get-in db [:admin :user-settings :saved :entities entity-kw])]
      (if (nil? entity-kw)
        db
        (assoc-in db [:admin :user-settings :draft :entities entity-kw] (safe-map saved-entity))))))

;; =============================================================================
;; Draft editing: form-fields config
;; =============================================================================

#_(rf/reg-event-db
  ::set-form-field-list-draft
  (fn [db [_ entity field-type fields]]
    (let [entity-kw (normalize-kw entity)
          field-type-kw (normalize-kw field-type)]
      (if (or (nil? entity-kw) (nil? field-type-kw))
        db
        (assoc-in db [:admin :user-settings :draft :form-fields entity-kw field-type-kw] (vec fields))))))

(rf/reg-event-db
  ::toggle-form-field-draft
  (fn [db [_ entity field-type field-name]]
    (let [entity-kw (normalize-kw entity)
          field-type-kw (normalize-kw field-type)
          field-str (if (keyword? field-name) (name field-name) (str field-name))
          path [:admin :user-settings :draft :form-fields entity-kw field-type-kw]
          current-fields (vec (or (get-in db path) []))
          field-set (set current-fields)]
      (if (or (nil? entity-kw) (nil? field-type-kw))
        db
        (let [new-fields (if (contains? field-set field-str)
                           (vec (remove #{field-str} current-fields))
                           (conj current-fields field-str))]
          (assoc-in db path new-fields))))))

(rf/reg-event-db
  ::reset-form-fields-draft
  (fn [db [_ entity]]
    (let [entity-kw (normalize-kw entity)
          saved-config (get-in db [:admin :user-settings :saved :form-fields entity-kw])]
      (if (nil? entity-kw)
        db
        (assoc-in db [:admin :user-settings :draft :form-fields entity-kw] (safe-map saved-config))))))

;; =============================================================================
;; Draft editing: table-columns config (structural, not policy)
;; =============================================================================

(rf/reg-event-db
  ::set-table-column-list-draft
  (fn [db [_ entity list-type columns]]
    (let [entity-kw (normalize-kw entity)
          list-type-kw (normalize-kw list-type)]
      (if (or (nil? entity-kw) (nil? list-type-kw))
        db
        (assoc-in db [:admin :user-settings :draft :table-columns entity-kw list-type-kw] (vec columns))))))

(rf/reg-event-db
  ::toggle-table-column-in-list-draft
  (fn [db [_ entity list-type column-name]]
    (let [entity-kw (normalize-kw entity)
          list-type-kw (normalize-kw list-type)
          col-str (if (keyword? column-name) (name column-name) (str column-name))
          path [:admin :user-settings :draft :table-columns entity-kw list-type-kw]
          current-cols (vec (or (get-in db path) []))
          col-set (set current-cols)]
      (if (or (nil? entity-kw) (nil? list-type-kw))
        db
        (let [new-cols (if (contains? col-set col-str)
                         (vec (remove #{col-str} current-cols))
                         (conj current-cols col-str))]
          (assoc-in db path new-cols))))))

;; =============================================================================
;; Additional subscriptions for config editing
;; =============================================================================

#_(rf/reg-sub
  ::entities-config
  (fn [db _]
    (safe-map (get-in db [:admin :user-settings :draft :entities]))))

#_(rf/reg-sub
  ::form-fields-config
  (fn [db _]
    (safe-map (get-in db [:admin :user-settings :draft :form-fields]))))

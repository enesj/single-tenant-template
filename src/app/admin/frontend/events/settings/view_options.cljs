(ns app.admin.frontend.events.settings.view-options
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.events.settings.utils :as utils]
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Load View Options from Backend
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.events.settings/load-view-options
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:admin :settings :loading?] true)
     :http-xhrio (admin-http/admin-get
                   {:uri "/admin/api/settings"
                    :on-success [:app.admin.frontend.events.settings/load-view-options-success]
                    :on-failure [:app.admin.frontend.events.settings/load-view-options-failure]})}))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/load-view-options-success
  (fn [{:keys [db]} [_ response]]
    (let [view-options (:view-options response)]
      (log/info "Loaded view options from backend" {:count (count view-options)})
      {:db (-> db
             (assoc-in [:admin :settings :loading?] false)
             ;; Keep both the persisted (saved) copy and the editable draft.
             ;; Draft is what the UI edits; saved is used for diffing / discard.
             (assoc-in [:admin :settings :view-options-saved] view-options)
             (assoc-in [:admin :settings :view-options] view-options)
             ;; Also keep the global admin config in sync so list pages pick up locks
             ;; even if they aren't reading from :admin/:settings.
             (assoc-in [:admin :config :view-options] view-options)
             (assoc-in [:admin :settings :error] nil))})))

;; =============================================================================
;; View Options Draft Editing (staged changes)
;; =============================================================================

(rf/reg-event-db
  :app.admin.frontend.events.settings/set-view-option-draft
  (fn [db [_ entity-name setting-key new-value]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          setting-kw (if (keyword? setting-key) setting-key (keyword setting-key))
          display? (utils/display-setting-key? setting-kw)
          base-path [:admin :settings :view-options entity-kw]]
      (cond
        (and display? (nil? new-value))
        (update-in db (conj base-path :display-locks) (fnil dissoc {}) setting-kw)

        display?
        (assoc-in db (conj base-path :display-locks setting-kw) new-value)

        (nil? new-value)
        (update-in db base-path dissoc setting-kw)

        :else
        (assoc-in db (conj base-path setting-kw) new-value)))))

;; =============================================================================
;; Display toggles draft editing (explicit defaults vs locks)
;; =============================================================================

;; new-state schema:
;; - {:kind :inherit}
;; - {:kind :default :value boolean}
;; - {:kind :lock :value boolean}
(rf/reg-event-db
  :app.admin.frontend.events.settings/set-display-setting-draft
  (fn [db [_ entity-name setting-key new-state]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          setting-kw (if (keyword? setting-key) setting-key (keyword setting-key))
          kind (:kind new-state)
          value (:value new-state)
          defaults-path [:admin :settings :view-options entity-kw :display-defaults]
          locks-path [:admin :settings :view-options entity-kw :display-locks]]
      (cond
        (or (nil? entity-kw) (nil? setting-kw) (not (utils/display-setting-key? setting-kw)))
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
;; Column visibility draft editing (explicit defaults vs locks)
;; =============================================================================

;; new-state schema:
;; - {:kind :inherit}
;; - {:kind :default :value boolean}
;; - {:kind :lock :value boolean}
(rf/reg-event-db
  :app.admin.frontend.events.settings/set-column-visibility-setting-draft
  (fn [db [_ entity-name column-key new-state]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          column-kw (if (keyword? column-key) column-key (keyword column-key))
          kind (:kind new-state)
          value (:value new-state)
          defaults-path [:admin :settings :view-options entity-kw :column-defaults]
          locks-path [:admin :settings :view-options entity-kw :column-locks]]
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
  :app.admin.frontend.events.settings/set-column-defaults-bulk
  (fn [db [_ entity-name column-keys value]]
    (let [entity-kw (utils/normalize-kw entity-name)
          cols (utils/normalize-kws column-keys)
          value (boolean value)
          defaults-path [:admin :settings :view-options entity-kw :column-defaults]
          locks-path [:admin :settings :view-options entity-kw :column-locks]]
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
  :app.admin.frontend.events.settings/set-display-settings-bulk
  (fn [db [_ entity-name setting-keys new-state]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          keys (->> (or setting-keys [])
                 (map (fn [k] (if (keyword? k) k (keyword k))))
                 vec)
          kind (:kind new-state)
          value (:value new-state)
          defaults-path [:admin :settings :view-options entity-kw :display-defaults]
          locks-path [:admin :settings :view-options entity-kw :display-locks]]
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
  :app.admin.frontend.events.settings/set-column-visibility-bulk
  (fn [db [_ entity-name column-keys new-state]]
    (let [entity-kw (utils/normalize-kw entity-name)
          cols (utils/normalize-kws column-keys)
          kind (:kind new-state)
          value (:value new-state)
          defaults-path [:admin :settings :view-options entity-kw :column-defaults]
          locks-path [:admin :settings :view-options entity-kw :column-locks]]
      (log/info "Bulk column visibility change"
        {:entity entity-kw
         :columns-count (count cols)
         :state new-state})
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
  :app.admin.frontend.events.settings/reset-view-options-draft
  (fn [db _]
    (let [saved (utils/safe-map (get-in db [:admin :settings :view-options-saved]))]
      (assoc-in db [:admin :settings :view-options] saved))))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/save-view-options
  (fn [{:keys [db]} _]
    (let [draft (utils/safe-map (get-in db [:admin :settings :view-options]))]
      {:db (-> db
             (assoc-in [:admin :settings :saving?] true)
             (assoc-in [:admin :settings :error] nil))
       :http-xhrio (admin-http/admin-put
                     {:uri "/admin/api/settings"
                      :params {:view-options draft}
                      :on-success [:app.admin.frontend.events.settings/save-view-options-success]
                      :on-failure [:app.admin.frontend.events.settings/save-view-options-failure]})})))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/save-view-options-success
  (fn [{:keys [db]} [_ response]]
    (let [saved (utils/safe-map (:view-options response))]
      (log/info "View options saved successfully" {:count (count saved)})
      ;; Update the config-loader cache so admin/template list pages pick up the change.
      (config-loader/register-preloaded-config! :view-options saved)
      {:db (-> db
             (assoc-in [:admin :settings :saving?] false)
             (assoc-in [:admin :settings :last-saved] (js/Date.now))
             (assoc-in [:admin :settings :error] nil)
             (assoc-in [:admin :settings :view-options-saved] saved)
             (assoc-in [:admin :settings :view-options] saved)
             (assoc-in [:admin :config :view-options] saved))})))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/save-view-options-failure
  (fn [{:keys [db]} [_ error]]
    (log/error "Failed to save view options" error)
    (cond-> {:db (-> db
                   (assoc-in [:admin :settings :saving?] false)
                   (assoc-in [:admin :settings :error] "Failed to save view options"))}
      (utils/unauthorized? error) (assoc :dispatch [:admin/auth-invalid]))))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/load-view-options-failure
  (fn [{:keys [db]} [_ error]]
    (log/error "Failed to load view options" error)
    (cond-> {:db (-> db
                   (assoc-in [:admin :settings :loading?] false)
                   (assoc-in [:admin :settings :error] "Failed to load settings"))}
      (utils/unauthorized? error) (assoc :dispatch [:admin/auth-invalid]))))

;; =============================================================================
;; Update Single View Option Setting
;; =============================================================================

#_(rf/reg-event-fx
    :app.admin.frontend.events.settings/update-entity-setting
    (fn [{:keys [db]} [_ entity-name setting-key new-value]]
      (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
            setting-kw (if (keyword? setting-key) setting-key (keyword setting-key))
            display? (utils/display-setting-key? setting-kw)
            base-path [:admin :settings :view-options entity-kw]
            db' (cond
                  display?
                  (assoc-in db (conj base-path :display-locks setting-kw) new-value)

                  :else
                  (assoc-in db (conj base-path setting-kw) new-value))]
        {:db (assoc-in db' [:admin :settings :saving?] true)
         :http-xhrio (admin-http/admin-patch
                       {:uri "/admin/api/settings/entity"
                        :params {:entity-name (name entity-kw)
                                 :setting-key (name setting-kw)
                                 :setting-value new-value}
                        :on-success [:app.admin.frontend.events.settings/update-setting-success entity-kw setting-kw new-value]
                        :on-failure [:app.admin.frontend.events.settings/update-setting-failure entity-kw setting-kw]})})))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/update-setting-success
  (fn [{:keys [db]} [_ entity-kw setting-kw new-value _response]]
    (log/info "Setting updated successfully" {:entity entity-kw :setting setting-kw :value new-value})
    ;; Also update the config-loader cache so components pick up the change
    (let [current-options (config-loader/get-all-view-options)
          display? (utils/display-setting-key? setting-kw)
          path (if display?
                 [entity-kw :display-locks setting-kw]
                 [entity-kw setting-kw])
          updated-options (assoc-in current-options path new-value)]
      (config-loader/register-preloaded-config! :view-options updated-options))
    {:db (-> db
           (assoc-in [:admin :settings :saving?] false)
           (assoc-in [:admin :settings :last-saved] (js/Date.now))
           (assoc-in [:admin :settings :error] nil))}))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/update-setting-failure
  (fn [{:keys [db]} [_ entity-kw setting-kw error]]
    (log/error "Failed to update setting" {:entity entity-kw :setting setting-kw :error error})
    ;; Revert the optimistic update by reloading from backend
    (cond-> {:db (-> db
                   (assoc-in [:admin :settings :saving?] false)
                   (assoc-in [:admin :settings :error] "Failed to save setting"))
             :fx [[:dispatch [:app.admin.frontend.events.settings/load-view-options]]]}
      (utils/unauthorized? error) (assoc :dispatch [:admin/auth-invalid]))))

;; =============================================================================
;; Remove Setting (make user-configurable)
;; =============================================================================

#_(rf/reg-event-fx
    :app.admin.frontend.events.settings/remove-entity-setting
    (fn [{:keys [db]} [_ entity-name setting-key]]
      (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
            setting-kw (if (keyword? setting-key) setting-key (keyword setting-key))
            display? (utils/display-setting-key? setting-kw)
            base-path [:admin :settings :view-options entity-kw]
            db' (cond
                  display?
                  (update-in db (conj base-path :display-locks) (fnil dissoc {}) setting-kw)

                  :else
                  (update-in db base-path dissoc setting-kw))]
        {:db (assoc-in db' [:admin :settings :saving?] true)
         :http-xhrio (admin-http/admin-delete
                       {:uri "/admin/api/settings/entity"
                        :params {:entity-name (name entity-kw)
                                 :setting-key (name setting-kw)}
                        :on-success [:app.admin.frontend.events.settings/remove-setting-success entity-kw setting-kw]
                        :on-failure [:app.admin.frontend.events.settings/remove-setting-failure entity-kw setting-kw]})})))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/remove-setting-success
  (fn [{:keys [db]} [_ entity-kw setting-kw _response]]
    (log/info "Setting removed successfully" {:entity entity-kw :setting setting-kw})
    ;; Also update the config-loader cache
    (let [current-options (config-loader/get-all-view-options)
          display? (utils/display-setting-key? setting-kw)
          updated-options (if display?
                            (update-in current-options [entity-kw :display-locks] (fnil dissoc {}) setting-kw)
                            (update current-options entity-kw dissoc setting-kw))]
      (config-loader/register-preloaded-config! :view-options updated-options))
    {:db (-> db
           (assoc-in [:admin :settings :saving?] false)
           (assoc-in [:admin :settings :last-saved] (js/Date.now))
           (assoc-in [:admin :settings :error] nil))}))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/remove-setting-failure
  (fn [{:keys [db]} [_ entity-kw setting-kw error]]
    (log/error "Failed to remove setting" {:entity entity-kw :setting setting-kw :error error})
    (cond-> {:db (-> db
                   (assoc-in [:admin :settings :saving?] false)
                   (assoc-in [:admin :settings :error] "Failed to remove setting"))
             :fx [[:dispatch [:app.admin.frontend.events.settings/load-view-options]]]}
      (utils/unauthorized? error) (assoc :dispatch [:admin/auth-invalid]))))


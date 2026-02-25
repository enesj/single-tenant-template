(ns app.admin.frontend.events.settings.view-options
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.events.settings.utils :as utils]
    [app.admin.frontend.utils.http :as admin-http]
    [app.admin.frontend.utils.view-options-helpers :as vo-helpers]
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
;; - {:kind :default :value boolean|int}  (int for :per-page)
;; - {:kind :lock :value boolean|int}     (int for :per-page)
(rf/reg-event-db
  :app.admin.frontend.events.settings/set-display-setting-draft
  (fn [db [_ entity-name setting-key new-state]]
    (let [entity-kw (utils/normalize-kw entity-name)
          setting-kw (utils/normalize-kw setting-key)
          kind (:kind new-state)
          value (:value new-state)
          valid-value? (if (= setting-kw :per-page)
                         (and (integer? value) (pos? value))
                         (boolean? value))]
      (if (or (nil? entity-kw) (nil? setting-kw) (not (utils/display-setting-key? setting-kw)))
        db
        (vo-helpers/apply-display-setting
          db [:admin :settings :view-options entity-kw] setting-kw kind value valid-value?)))))

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
    (let [entity-kw (utils/normalize-kw entity-name)
          column-kw (utils/normalize-kw column-key)
          kind (:kind new-state)
          value (:value new-state)]
      (if (or (nil? entity-kw) (nil? column-kw))
        db
        (vo-helpers/apply-column-visibility-setting
          db [:admin :settings :view-options entity-kw] column-kw kind value)))))

;; =============================================================================
;; Bulk helpers: column visibility defaults
;; =============================================================================

(rf/reg-event-db
  :app.admin.frontend.events.settings/set-column-defaults-bulk
  (fn [db [_ entity-name column-keys value]]
    (let [entity-kw (utils/normalize-kw entity-name)
          cols (utils/normalize-kws column-keys)
          value (boolean value)]
      (if (or (nil? entity-kw) (empty? cols))
        db
        (vo-helpers/apply-column-defaults-bulk
          db [:admin :settings :view-options entity-kw] cols value)))))

;; =============================================================================
;; Bulk helpers: apply tristate to many display settings / columns
;; =============================================================================

(rf/reg-event-db
  :app.admin.frontend.events.settings/set-display-settings-bulk
  (fn [db [_ entity-name setting-keys new-state]]
    (let [entity-kw (utils/normalize-kw entity-name)
          ks (utils/normalize-kws setting-keys)
          kind (:kind new-state)
          value (:value new-state)]
      (if (or (nil? entity-kw) (empty? ks))
        db
        (vo-helpers/apply-display-settings-bulk
          db [:admin :settings :view-options entity-kw] ks kind value)))))

(rf/reg-event-db
  :app.admin.frontend.events.settings/set-column-visibility-bulk
  (fn [db [_ entity-name column-keys new-state]]
    (let [entity-kw (utils/normalize-kw entity-name)
          cols (utils/normalize-kws column-keys)
          kind (:kind new-state)
          value (:value new-state)]
      (log/info "Bulk column visibility change"
        {:entity entity-kw :columns-count (count cols) :state new-state})
      (if (or (nil? entity-kw) (empty? cols))
        db
        (vo-helpers/apply-column-visibility-bulk
          db [:admin :settings :view-options entity-kw] cols kind value)))))

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


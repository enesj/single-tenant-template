(ns app.admin.frontend.events.unified-settings
  "Unified events for settings pages with scope switching support.

   This namespace provides events and subscriptions that support both
   admin-settings and user-settings pages with a unified UI.

   Key concepts:
   - scope: :admin or :user - which config set to edit
   - mode: :view or :edit - whether viewing or editing
   - entity: keyword - which entity to edit in edit mode

   State structure:
   [:admin :unified-settings]
     :mode - :view | :edit
     :scope - :admin | :user (current editing scope)
     :selected-entity - keyword (current entity being edited)
     :admin-dirty? - boolean (admin draft differs from saved)
     :user-dirty? - boolean (user draft differs from saved)"
  (:require
    [app.admin.frontend.events.settings :as admin-settings-events]
    [app.admin.frontend.events.user-settings :as user-settings-events]
    [app.admin.frontend.settings.definitions :as defs]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Initialization
;; =============================================================================

(rf/reg-event-fx
  ::init
  (fn [{:keys [db]} [_ {:keys [initial-scope fixed-scope load-admin? load-user?] :as opts}]]
    (let [prev-state (get-in db [:admin :unified-settings])
          prev-fixed-scope (:fixed-scope prev-state)
          fixed-scope (when (some? fixed-scope) fixed-scope)
          ;; If we re-init for the *same* fixed-scope, keep mode/selection to avoid
          ;; surprising UI resets (e.g. jumping back to overview after an edit).
          reset-ui? (not= prev-fixed-scope fixed-scope)
          scope (or fixed-scope (:scope prev-state) initial-scope :admin)
          prev-mode (or (:mode prev-state) :view)
          mode (if reset-ui? :view prev-mode)
          prev-selected (:selected-entity prev-state)
          default-entity (->> (defs/entities-for-scope scope)
                           sort
                           first)
          selected-entity (cond
                            reset-ui? nil
                            (and (= mode :edit) (nil? prev-selected)) default-entity
                            :else prev-selected)
          load-admin? (if (some? load-admin?) load-admin? true)
          load-user? (if (some? load-user?) load-user? true)
          fx (cond-> []
               load-admin? (conj [:dispatch [::admin-settings-events/load-view-options]])
               ;; Keep these for parity/full-coverage work; harmless if the page doesn't render the tabs yet.
               load-admin? (conj [:dispatch [::admin-settings-events/load-form-fields]])
               load-admin? (conj [:dispatch [::admin-settings-events/load-table-columns]])
               load-user? (conj [:dispatch [::user-settings-events/init]]))]
      (log/info "Initializing unified settings"
        (merge
          {:scope scope
           :fixed-scope fixed-scope
           :reset-ui? reset-ui?
           :mode mode
           :selected-entity selected-entity}
          (dissoc opts :db)))
      {:db (-> db
             (assoc-in [:admin :unified-settings :mode] mode)
             (assoc-in [:admin :unified-settings :scope] scope)
             (assoc-in [:admin :unified-settings :fixed-scope] fixed-scope)
             (assoc-in [:admin :unified-settings :selected-entity] selected-entity))
       :fx fx})))

;; =============================================================================
;; Mode Toggle
;; =============================================================================

(rf/reg-event-fx
  ::set-mode
  (fn [{:keys [db]} [_ mode]]
    (let [prev-mode (get-in db [:admin :unified-settings :mode] :view)
          current-scope (get-in db [:admin :unified-settings :scope] :admin)
          selected-entity (get-in db [:admin :unified-settings :selected-entity])]
      (log/info "Setting unified settings mode"
        {:prev-mode prev-mode
         :new-mode mode
         :scope current-scope
         :selected-entity selected-entity}))
    (let [current-scope (get-in db [:admin :unified-settings :scope] :admin)
          ;; When entering edit mode, select first entity for current scope if none selected
          selected-entity (get-in db [:admin :unified-settings :selected-entity])
          default-entity (->> (defs/entities-for-scope current-scope)
                           sort
                           first)]
      {:db (-> db
               (assoc-in [:admin :unified-settings :mode] mode)
               (cond->
                 (and (= mode :edit) (nil? selected-entity))
                 (assoc-in [:admin :unified-settings :selected-entity] default-entity)))})))

(rf/reg-event-fx
  ::toggle-mode
  (fn [{:keys [db]} _]
    (let [current-mode (get-in db [:admin :unified-settings :mode] :view)
          new-mode (if (= current-mode :view) :edit :view)]
      {:dispatch [::set-mode new-mode]})))

;; =============================================================================
;; Scope Switching
;; =============================================================================

(rf/reg-event-fx
  ::set-scope
  (fn [{:keys [db]} [_ scope]]
    (let [fixed-scope (get-in db [:admin :unified-settings :fixed-scope])]
      (if fixed-scope
        (do
          (log/info "Ignoring scope change; page has fixed scope" {:requested scope :fixed fixed-scope})
          {:db db})
        (do
          (log/info "Setting unified settings scope" {:scope scope})
          (let [default-entity (->> (defs/entities-for-scope scope)
                                 sort
                                 first)]
            {:db (-> db
                   (assoc-in [:admin :unified-settings :scope] scope)
                   (assoc-in [:admin :unified-settings :selected-entity] default-entity))}))))))

;; =============================================================================
;; Entity Selection
;; =============================================================================

(rf/reg-event-fx
  ::set-selected-entity
  (fn [{:keys [db]} [_ entity-kw]]
    (log/info "Setting selected entity" {:entity entity-kw})
    {:db (assoc-in db [:admin :unified-settings :selected-entity] entity-kw)}))

;; =============================================================================
;; Save Current Scope
;; =============================================================================

(rf/reg-event-fx
  ::save-current-scope
  (fn [{:keys [db]} _]
    (let [scope (get-in db [:admin :unified-settings :scope] :admin)]
      (log/info "Saving current scope" {:scope scope})
      (case scope
        :admin {:dispatch [::admin-settings-events/save-view-options]}
        :user {:dispatch [::user-settings-events/save]}
        {}))))

;; =============================================================================
;; Discard Current Scope
;; =============================================================================

(rf/reg-event-fx
  ::discard-current-scope
  (fn [{:keys [db]} _]
    (let [scope (get-in db [:admin :unified-settings :scope] :admin)]
      (log/info "Discarding current scope" {:scope scope})
      (case scope
        :admin {:dispatch [::admin-settings-events/reset-view-options-draft]}
        :user {:dispatch [::user-settings-events/discard-draft]}
        {}))))

;; =============================================================================
;; Config Tab (view-options, form-fields, table-columns)
;; =============================================================================

(rf/reg-event-fx
  ::set-tab
  (fn [{:keys [db]} [_ tab]]
    {:db (assoc-in db [:admin :unified-settings :tab] tab)}))

;; =============================================================================
;; Subscriptions
;; =============================================================================

(rf/reg-sub
  ::mode
  (fn [db _]
    (get-in db [:admin :unified-settings :mode] :view)))

(rf/reg-sub
  ::scope
  (fn [db _]
    (get-in db [:admin :unified-settings :scope] :admin)))

(rf/reg-sub
  ::selected-entity
  (fn [db _]
    (get-in db [:admin :unified-settings :selected-entity])))

(rf/reg-sub
  ::tab
  (fn [db _]
    (get-in db [:admin :unified-settings :tab] "view-options")))

;; Derived: is current scope dirty?
(rf/reg-sub
  ::current-scope-dirty?
  :<- [::scope]
  :<- [::admin-settings-events/view-options-dirty?]
  :<- [::user-settings-events/dirty?]
  (fn [[scope admin-dirty? user-dirty?] _]
    (case scope
      :admin admin-dirty?
      :user user-dirty?
      false)))

;; Derived: is current scope saving?
(rf/reg-sub
  ::current-scope-saving?
  :<- [::scope]
  :<- [::admin-settings-events/saving?]
  :<- [::user-settings-events/saving?]
  (fn [[scope admin-saving? user-saving?] _]
    (case scope
      :admin admin-saving?
      :user user-saving?
      false)))

;; Derived: is current scope loading?
(rf/reg-sub
  ::current-scope-loading?
  :<- [::scope]
  :<- [::admin-settings-events/loading?]
  :<- [::user-settings-events/loading?]
  (fn [[scope admin-loading? user-loading?] _]
    (case scope
      :admin admin-loading?
      :user user-loading?
      false)))

;; Derived: current scope error
(rf/reg-sub
  ::current-scope-error
  :<- [::scope]
  :<- [::admin-settings-events/error]
  :<- [::user-settings-events/error]
  (fn [[scope admin-error user-error] _]
    (case scope
      :admin admin-error
      :user user-error
      nil)))

;; Admin view-options config for overview
(rf/reg-sub
  ::admin-view-options
  (fn [db _]
    (get-in db [:admin :settings :view-options] {})))

;; User view-options config for overview
(rf/reg-sub
  ::user-view-options
  (fn [db _]
    (get-in db [:admin :user-settings :draft :view-options] {})))

;; All configs for overview mode
(rf/reg-sub
  ::overview-configs
  :<- [::admin-view-options]
  :<- [::user-view-options]
  (fn [[admin-config user-config] _]
    {:admin admin-config
     :user user-config}))

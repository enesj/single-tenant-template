(ns app.template.frontend.components.action-components
  "Shared reusable action components for admin interface"
  (:require
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui]]))

;; =============================================================================
;; Common Icon Components
;; =============================================================================

(defui activate-icon
  "Icon for activating entities (users, tenants, etc.)"
  []
  ($ :svg {:width "16" :height "16" :viewBox "0 0 24 24" :fill "currentColor" :aria-hidden "true"}
    ($ :path {:d "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"})))

(defui suspend-icon
  "Icon for suspending entities (users, tenants, etc.)"
  []
  ($ :svg {:width "16" :height "16" :viewBox "0 0 24 24" :fill "currentColor" :aria-hidden "true"}
    ($ :path {:d "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zM4 12c0-4.41 3.59-8 8-8 1.85 0 3.55.63 4.9 1.69L5.69 16.9C4.63 15.55 4 13.85 4 12zm8 8c-1.85 0-3.55-.63-4.9-1.69L18.31 7.1C19.37 8.45 20 10.15 20 12c0 4.41-3.59 8-8 8z"})))

(defui view-details-icon
  "Icon for viewing entity details"
  []
  ($ :svg {:width "16" :height "16" :viewBox "0 0 24 24" :fill "currentColor" :aria-hidden "true"}
    ($ :path {:d "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"})))

(defui filter-icon
  "Icon for filter actions"
  []
  ($ :svg {:width "16" :height "16" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor"}
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"})))

(defui export-icon
  "Icon for export actions"
  []
  ($ :svg {:width "16" :height "16" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor"}
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"})))

(defui delete-icon
  "Icon for delete actions"
  []
  ($ :svg {:width "16" :height "16" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor"}
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"})))

;; =============================================================================
;; Loading Indicator Component
;; =============================================================================

(defui loading-spinner
  "Small loading spinner for individual actions"
  []
  ($ :span {:class "loading loading-spinner loading-xs"}))

;; =============================================================================
;; Action Handler Factory Functions
;; =============================================================================

(defn create-status-action-handlers
  "Factory function to create status action handlers (activate/suspend)"
  [entity-id entity-name entity-type dispatch-event]
  {:activate (fn []
               (log/info "Activating" entity-type entity-id entity-name)
               (rf/dispatch [dispatch-event entity-id :active]))

   :suspend (fn []
              (log/info "Suspending" entity-type entity-id entity-name)
              (rf/dispatch [dispatch-event entity-id :suspended]))})

(defn create-confirmation-handlers
  "Factory function to create confirmation handlers that wrap action handlers
   - Always wraps destructive/status actions with a confirm dialog
   - Also wraps view actions to normalize event handling in tests (stopPropagation)"
  [handlers entity-name entity-type & {:keys [custom-messages]}]
  (cond->
    {:activate (fn [e]
                 (.stopPropagation e)
                 (confirm-dialog/show-confirm
                   {:message (or (:activate custom-messages)
                               (str "Activate " entity-type " " entity-name "?"))
                    :title (str "Confirm " (clojure.string/capitalize entity-type) " Activation")
                    :on-confirm (:activate handlers)}))

     :suspend (fn [e]
                (.stopPropagation e)
                (confirm-dialog/show-confirm
                  {:message (or (:suspend custom-messages)
                              (str "Suspend " entity-type " " entity-name "? This will affect all related functionality."))
                   :title (str "Confirm " (clojure.string/capitalize entity-type) " Suspension")
                   :on-confirm (:suspend handlers)}))}

    ;; When present, wrap non-destructive view actions with simple event guards
    (:view-details handlers)
    (assoc :view-details (fn [e]
                           (when (.-stopPropagation e) (.stopPropagation e))
                           ((:view-details handlers) e)))

    (:view-billing handlers)
    (assoc :view-billing (fn [e]
                           (when (.-stopPropagation e) (.stopPropagation e))
                           ((:view-billing handlers) e)))))

(defn create-update-handlers
  "Factory function to create update handlers for properties like role, tier, etc."
  [entity-id entity-name entity-type dispatch-event property-name]
  (fn [new-value]
    (log/info "Updating" entity-type entity-id entity-name property-name "to" new-value)
    (rf/dispatch [dispatch-event entity-id new-value])))

;; =============================================================================
;; Common Action Groups
;; =============================================================================

(defn create-view-action-group
  "Create a standard 'View' action group with common view actions"
  [view-details-handler & [additional-actions]]
  (let [base-items [{:id "view-details"
                     :icon ($ view-details-icon)
                     :label "View Details"
                     :on-click view-details-handler}]]
    {:group-title "View"
     :items (if additional-actions
              (concat base-items additional-actions)
              base-items)}))

(defn create-status-action-group
  "Create a standard 'Status' action group with activate/suspend actions"
  [current-status confirmation-handlers loading-key & [{:keys [activate-label suspend-label]}]]
  (let [items (cond-> []
                (not= current-status "active")
                (conj {:id "activate-entity"
                       :icon ($ activate-icon)
                       :label (or activate-label "Activate")
                       :variant :success
                       :loading-key loading-key
                       :on-click (:activate confirmation-handlers)})

                (not= current-status "suspended")
                (conj {:id "suspend-entity"
                       :icon ($ suspend-icon)
                       :label (or suspend-label "Suspend")
                       :variant :error
                       :loading-key loading-key
                       :on-click (:suspend confirmation-handlers)}))]
    (when (seq items)
      {:group-title "Status"
       :items items})))

(defn create-property-action-group
  "Create an action group for property-based actions (role, tier, etc.)"
  [group-title current-value options update-handler loading-key]
  (let [items (map (fn [option]
                     {:id (str "set-" (name option))
                      :icon (case option
                              :free "🆓"
                              :pro "⭐"
                              :enterprise "🏢"
                              :owner "👑"
                              :admin "🔧"
                              :member "👤"
                              "📝")
                      :label (clojure.string/capitalize (name option))
                      :loading-key loading-key
                      :on-click #(update-handler option)})
                (remove #(= % current-value) options))]
    (when (seq items)
      {:group-title group-title
       :items items})))

(defn create-dangerous-action-group
  "Create a 'Dangerous Actions' group for destructive operations"
  [actions]
  (when (seq actions)
    {:group-title "Dangerous Actions"
     :items (map #(assoc % :variant :error) actions)}))

;; =============================================================================
;; Utility Functions
;; =============================================================================

(defn create-dropdown-ids
  "Generate consistent IDs for dropdown components"
  [entity-type entity-id]
  {:dropdown-id (str entity-type "-actions-dropdown-" entity-id)
   :trigger-id (str entity-type "-actions-trigger-" entity-id)})

(defn filter-empty-groups
  "Remove empty action groups from a collection"
  [groups]
  (remove #(empty? (:items %)) groups))

(defn get-entity-status
  "Extract entity status, handling both namespaced and plain keys"
  [entity entity-type]
  (let [namespaced-key (keyword (str (name entity-type) "/status"))
        plain-key :status]
    (or (get entity namespaced-key) (get entity plain-key))))

(defn get-entity-name
  "Extract entity name, handling both namespaced and plain keys"
  [entity entity-type]
  (let [namespaced-key (keyword (str (name entity-type) "/name"))
        plain-key :name]
    (or (get entity namespaced-key) (get entity plain-key) (get entity :email))))

;; =============================================================================
;; Loading State Utilities
;; =============================================================================

(defn create-loading-states-map
  "Create a loading states map from individual loading state subscriptions"
  [& {:keys [updating? loading-details? loading-other?]}]
  (cond-> {}
    updating? (assoc :updating? updating?)
    loading-details? (assoc :loading-details? loading-details?)
    loading-other? (assoc :loading-other? loading-other?)))

(defn is-action-loading?
  "Check if a specific action is currently loading"
  [loading-states loading-key]
  (get loading-states loading-key false))

;

(ns app.template.frontend.components.selection-counter
  "Universal selection counter component for list/table interfaces.

   This component provides consistent selection counting across all application modules:
   - Admin panel, tenant UI, customer portal, etc.
   - Supports different themes and styling variants
   - Configurable behavior and appearance"

  (:require
    [uix.core :refer [$ defui]]))

(defui selection-counter
  "Universal selection counter component.

   Props:
   - :selected-ids - collection of selected entity IDs
   - :entity-type - keyword representing the entity type (e.g., :users, :tenants, :audit-logs)
   - :item-label - custom label for items (defaults to entity-type name)
   - :variant - :admin (default), :tenant, :customer for different themes
   - :show-actions? - boolean to show action buttons (default: false)
   - :on-clear-selection - function to call when clear selection is clicked
   - :on-select-all - function to call when select all is clicked
   - :total-items - total number of items available (for select all functionality)
   - :class - optional additional CSS classes

   Theme Variants:
   - :admin - Uses 'ds-alert' classes (default)
   - :tenant - Uses tenant-themed styling
   - :customer - Uses customer-themed styling

   Examples:
   - Basic: {:selected-ids [1 2 3], :entity-type :users}
   - Custom: {:selected-ids [1 2], :item-label \"records\", :variant :tenant}"
  [{:keys [selected-ids entity-type item-label variant
           show-actions? on-clear-selection on-select-all
           total-items class]
    :or {variant :admin
         show-actions? false
         class ""}}]

  (when (seq selected-ids)
    (let [label (or item-label (name entity-type))
          selected-count (count selected-ids)
          total-count (or total-items 0)
          all-selected? (and (pos? total-count) (= selected-count total-count))]

      ($ :div {:class (str (case variant
                             :admin "ds-alert ds-alert-warning mb-4"
                             :tenant "tenant-alert tenant-alert-warning mb-4"
                             :customer "customer-alert customer-alert-warning mb-4"
                             "alert alert-warning mb-4")
                        " " class)}

        ;; Main selection count text
        ($ :span (str selected-count " " label " selected"))

        ;; Action buttons (optional)
        (when show-actions?
          ($ :div {:class "flex gap-2 ml-auto"}
            ;; Select all/none toggle
            (when (and on-select-all (pos? total-count))
              ($ :button
                {:class (case variant
                          :admin "ds-btn ds-btn-xs"
                          :tenant "tenant-btn tenant-btn-xs"
                          :customer "customer-btn customer-btn-xs"
                          "btn btn-xs")
                 :on-click #(on-select-all (if all-selected? [] :all))}
                (if all-selected? "Select None" "Select All")))

            ;; Clear selection button
            (when on-clear-selection
              ($ :button
                {:class (case variant
                          :admin "ds-btn ds-btn-xs"
                          :tenant "tenant-btn tenant-btn-xs"
                          :customer "customer-btn customer-btn-xs"
                          "btn btn-xs")
                 :on-click on-clear-selection}
                "Clear Selection"))))))))

;; Convenience functions for common use cases

(defn admin-selection-counter
  "Convenience function for admin-themed selection counter."
  [& args]
  ($ selection-counter (merge {:variant :admin} (first args))))

(defn tenant-selection-counter
  "Convenience function for tenant-themed selection counter."
  [& args]
  ($ selection-counter (merge {:variant :tenant} (first args))))

(defn customer-selection-counter
  "Convenience function for customer-themed selection counter."
  [& args]
  ($ selection-counter (merge {:variant :customer} (first args))))

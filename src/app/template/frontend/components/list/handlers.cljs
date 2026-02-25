(ns app.template.frontend.components.list.handlers
  (:require
    [app.template.frontend.events.config :as config-events]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.crud :as crud-events]
    [app.template.frontend.events.list.filters :as filter-events]
    [app.template.frontend.events.list.selection :as selection-events]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]))

(defn build-handlers
  [{:keys [entity-name
           entity-kw
           allow-add?
           use-modal-forms?
           on-add-success
           on-edit-success
           active-inline-filter
           active-filters
           visible-items
           set-active-inline-filter
           set-inline-filter-field-spec
           set-inline-filter-value
           set-add-modal-open!
           set-edit-modal-open!
           set-edit-modal-item!]}]
  {:handle-select-change (fn [item-id selected?]
                           (rf/dispatch [::selection-events/select-item entity-name item-id selected?]))

   :handle-select-all (fn [select-all?]
                        (rf/dispatch [::selection-events/select-all entity-name visible-items select-all?]))

   :handle-inline-filter-click (fn [field-id field-spec]
                                 (if (= active-inline-filter field-id)
                                   (do
                                     (set-active-inline-filter nil)
                                     (set-inline-filter-field-spec nil)
                                     (set-inline-filter-value ""))
                                   (let [field-key (if (keyword? field-id) field-id (keyword field-id))
                                         existing-filter-value (get active-filters field-key)]
                                     (set-active-inline-filter field-id)
                                     (set-inline-filter-field-spec field-spec)
                                     (set-inline-filter-value (or existing-filter-value "")))))

   :handle-filter-apply (fn [entity-type field-name filter-value keep-open?]
                          (rf/dispatch [::filter-events/apply-filter entity-type field-name filter-value keep-open?])
                          (when (not keep-open?)
                            (set-active-inline-filter nil)
                            (set-inline-filter-field-spec nil)
                            (set-inline-filter-value "")))

   :handle-filter-close (fn []
                          (set-active-inline-filter nil)
                          (set-inline-filter-field-spec nil)
                          (set-inline-filter-value ""))

   :handle-add-click (fn []
                       (when (not (false? allow-add?))
                         (rf/dispatch [::crud-events/clear-error entity-kw])
                         (rf/dispatch [::form-events/clear-form-errors entity-kw])
                         (rf/dispatch [::form-events/cancel-form entity-kw])
                         (if use-modal-forms?
                           (set-add-modal-open! true)
                           (do
                             (rf/dispatch [::config-events/set-show-add-form true])
                             (rf/dispatch [::config-events/set-editing nil])))))

   :handle-add-modal-close (fn []
                             (rf/dispatch [::crud-events/clear-error entity-kw])
                             (rf/dispatch [::form-events/clear-form-errors entity-kw])
                             (rf/dispatch [::form-events/cancel-form entity-kw])
                             (set-add-modal-open! false))

   :handle-add-modal-success (fn []
                               (set-add-modal-open! false)
                               (when on-add-success
                                 (on-add-success)))

   :handle-edit-click (fn [item]
                        (rf/dispatch [::crud-events/clear-error entity-kw])
                        (rf/dispatch [::form-events/clear-form-errors entity-kw])
                        (if use-modal-forms?
                          (do
                            (rf/dispatch [::form-events/cancel-form entity-kw])
                            (set-edit-modal-item! item)
                            (set-edit-modal-open! true))
                          (rf/dispatch [::config-events/set-editing (id-utils/extract-entity-id item)])))

   :handle-edit-modal-close (fn []
                              (rf/dispatch [::form-events/cancel-form entity-kw])
                              (set-edit-modal-open! false)
                              (set-edit-modal-item! nil))

   :handle-edit-modal-success (fn []
                                (rf/dispatch [::form-events/cancel-form entity-kw])
                                (set-edit-modal-open! false)
                                (set-edit-modal-item! nil)
                                (when on-edit-success
                                  (on-edit-success)))})
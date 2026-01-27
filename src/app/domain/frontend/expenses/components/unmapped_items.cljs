(ns app.domain.frontend.expenses.components.unmapped-items
  (:require
    [app.domain.frontend.expenses.events.unmapped-items :as unmapped-events]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-memo use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- supplier-option-label
  [s]
  (or (:display-name s) (:display_name s) (str (:id s))))

(defn- article-option-label
  [a]
  (or (:canonical-name a) (:canonical_name a) (str (:id a))))

(defui map-to-article-modal
  []
  (let [open? (use-subscribe [:expenses/unmapped-items-map-modal-open?])
        working? (use-subscribe [:expenses/unmapped-items-map-modal-working?])
        err (use-subscribe [:expenses/unmapped-items-map-modal-error])
        progress (use-subscribe [:expenses/unmapped-items-map-modal-progress])
        items (use-subscribe [:expenses/unmapped-items])
        selected-ids (use-subscribe [:expenses/unmapped-items-selected-ids])
        lookups-loading? (use-subscribe [:expenses/unmapped-items-lookups-loading?])
        lookups-error (use-subscribe [:expenses/unmapped-items-lookups-error])
        suppliers (use-subscribe [:expenses/unmapped-items-lookups-suppliers])
        articles (use-subscribe [:expenses/unmapped-items-lookups-articles])

        selected-items (use-memo
                         (fn []
                           (filterv (fn [it] (contains? selected-ids (:id it))) (or items [])))
                         [items selected-ids])
        supplier-id (use-memo
                      (fn []
                        (let [sids (->> selected-items (map :supplier-id) (remove nil?) set)]
                          (when (= 1 (count sids)) (first sids))))
                      [selected-items])
        supplier-name (use-memo
                        (fn []
                          (when supplier-id
                            (or (some (fn [s] (when (= supplier-id (:id s)) (supplier-option-label s))) suppliers)
                              (get-in (first selected-items) [:supplier-display-name]))))
                        [supplier-id suppliers selected-items])
        labels (use-memo
                 (fn []
                   (->> selected-items
                     (map :raw-label)
                     (remove str/blank?)
                     distinct
                     vec))
                 [selected-items])

        [mode set-mode!] (use-state :existing)
        [existing-article-id set-existing-article-id!] (use-state "")
        [new-article-name set-new-article-name!] (use-state "")
        ;; Tracks the last auto-prefilled name so we can safely update the suggestion
        ;; when the selected raw label changes (without overwriting user edits).
        [auto-prefill-name set-auto-prefill-name!] (use-state nil)]

    (use-effect
      (fn []
        ;; Reset local modal state after close so the next open starts clean.
        ;; (Resetting on open can race with very fast user clicks in tests/UI.)
        (when (not open?)
          (set-mode! :existing)
          (set-existing-article-id! "")
          (set-new-article-name! "")
          (set-auto-prefill-name! nil))
        js/undefined)
      [open?])

    (use-effect
      (fn []
        (when open?
          ;; Ensure lookups exist for select inputs.
          (rf/dispatch [::unmapped-events/load-lookups])
          ;; UX: in create-new mode, prefill with first label, and keep it in sync with
          ;; selection changes *only* while the user hasn't edited the input.
          (when (and (= mode :new) (seq labels))
            (let [desired (first labels)
                  can-autofill? (or (str/blank? new-article-name)
                                  (= new-article-name auto-prefill-name))]
              (when (and can-autofill? (not= new-article-name desired))
                (set-new-article-name! desired))
              (when (and can-autofill? (not= auto-prefill-name desired))
                (set-auto-prefill-name! desired)))))
        js/undefined)
      [open? mode new-article-name auto-prefill-name labels])

    ($ modal-wrapper
      {:visible? open?
       :title "Map unmapped aliases"
       :size :large
       :on-close [::unmapped-events/close-map-modal]
       :close-button-id "btn-close-map-unmapped-items"}

      ($ :div {:id "modal-map-unmapped-items"
               :class "space-y-4"}
        (when err
          ($ :div {:class "ds-alert ds-alert-error"}
            ($ :span (str err))))

        (when lookups-error
          ($ :div {:class "ds-alert ds-alert-warning"}
            ($ :span (str "Lookups warning: " lookups-error))))

        ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
          ($ :div {:class "ds-card-body space-y-2"}
            ($ :div {:class "text-sm text-base-content/70"}
              "Selected aliases: " (count selected-items))
            (when supplier-id
              ($ :div {:class "text-sm"}
                ($ :span {:class "font-semibold"} "Supplier: ")
                ($ :span (or supplier-name (str supplier-id)))))
            ($ :div {:class "max-h-40 overflow-auto text-sm"}
              ($ :ul {:class "list-disc pl-5"}
                (for [lbl labels]
                  ($ :li {:key lbl} lbl))))))

        ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
          ($ :div {:class "ds-card-body space-y-3"}
            ($ :div {:class "flex items-center gap-2"}
              ($ :button {:id "btn-use-existing-article"
                          :class (str "ds-btn ds-btn-sm " (when (= mode :existing) "ds-btn-primary"))
                          :type "button"
                          :on-click #(set-mode! :existing)}
                "Use existing article")
              ($ :button {:id "btn-create-article-from-labels"
                          :class (str "ds-btn ds-btn-sm " (when (= mode :new) "ds-btn-primary"))
                          :type "button"
                          :on-click #(set-mode! :new)}
                "Create new article"))

            (case mode
              :new
              ($ :div {:class "space-y-2"}
                ($ :label {:class "ds-form-control w-full"}
                  ($ :div {:class "label"}
                    ($ :span {:class "label-text"} "New article canonical name"))
                  ($ :input {:id "input-new-article-name"
                             :class "ds-input ds-input-bordered w-full"
                             :disabled (or working? lookups-loading?)
                             :value new-article-name
                             :on-change (fn [e]
                                          (let [v (.. e -target -value)]
                                            (set-new-article-name! v)
                                            ;; Any manual edit disables future auto-updates.
                                            (set-auto-prefill-name! nil)))})))

              ;; default: :existing
              ($ :div {:class "space-y-2"}
                ($ :label {:class "ds-form-control w-full"}
                  ($ :div {:class "label"}
                    ($ :span {:class "label-text"} "Select article"))
                  ($ :select {:id "select-map-article"
                              :class "ds-select ds-select-bordered w-full"
                              :disabled (or working? lookups-loading?)
                              :value existing-article-id
                              :on-change (fn [e] (set-existing-article-id! (.. e -target -value)))}
                    ($ :option {:value ""} "— choose —")
                    (for [a (or articles [])
                          :let [aid (:id a)
                                label (article-option-label a)]]
                      ($ :option {:key (str aid)
                                  :value (str aid)}
                        label))))))

            (when lookups-loading?
              ($ :div {:class "text-sm text-base-content/70"}
                "Loading articles/suppliers…"))

            (when progress
              ($ :div {:class "text-sm text-base-content/70"}
                (str "Mapped " (or (:done progress) 0) " / " (or (:total progress) 0)
                  (when (pos? (or (:failed progress) 0))
                    (str " (failed: " (:failed progress) ")")))))

            ($ :div {:class "ds-modal-action justify-end gap-2"}
              ($ :button {:id "btn-cancel-map-unmapped-items"
                          :type "button"
                          :class "ds-btn ds-btn-ghost"
                          :disabled working?
                          :on-click #(rf/dispatch [::unmapped-events/close-map-modal])}
                "Cancel")
              ($ :button {:id "btn-submit-map-unmapped-items"
                          :type "button"
                          :class "ds-btn ds-btn-primary"
                          :disabled (or working?
                                      (empty? selected-items)
                                      (and (= mode :existing) (str/blank? existing-article-id))
                                      (and (= mode :new) (str/blank? new-article-name)))
                          :on-click #(rf/dispatch
                                       [::unmapped-events/submit-map
                                        {:mode mode
                                         :existing-article-id existing-article-id
                                         :new-article-name new-article-name}])}
                (if working? "Working..." "Map")))))))))

(defui unmapped-items-panel
  "Shared content for both admin and user Unmapped Aliases pages.

  Props:
  - :breadcrumbs - vector of breadcrumb items: {:label string, :href string?}
  - :title - optional title string (default: Unmapped Aliases)"
  [{:keys [breadcrumbs title]
    :or {title "Unmapped Aliases"}}]
  (let [items (use-subscribe [:expenses/unmapped-items])
        loading? (use-subscribe [:expenses/unmapped-items-loading?])
        err (use-subscribe [:expenses/unmapped-items-error])
        suppliers (use-subscribe [:expenses/unmapped-items-lookups-suppliers])
        supplier-filter (use-subscribe [:expenses/unmapped-items-supplier-filter])
        selected-ids (use-subscribe [:expenses/unmapped-items-selected-ids])]

    (use-effect
      (fn []
        ;; Make sure lookup lists exist for the modal.
        (rf/dispatch [::unmapped-events/load-lookups])
        js/undefined)
      [])

    (use-effect
      (fn []
        ;; Load items when supplier filter changes.
        (rf/dispatch [::unmapped-events/load-unmapped-items
                      {:limit 50
                       :offset 0
                       :supplier-id supplier-filter}])
        js/undefined)
      [supplier-filter])

    ($ :div {:class "p-6 space-y-6"}
      ($ :div {:class "flex items-center justify-between"}
        ($ :div {:class "space-y-1"}
          (when (seq breadcrumbs)
            ($ :div {:class "text-sm breadcrumbs"}
              ($ :ul
                (for [{:keys [label href]} breadcrumbs]
                  (if href
                    ($ :li {:key (str label "-" href)} ($ :a {:href href} label))
                    ($ :li {:key label} label))))))
          ($ :h1 {:class "text-2xl font-bold"} title))

        ($ :div {:class "flex items-center gap-2"}
          ($ :button {:id "btn-refresh-unmapped-items"
                      :class "ds-btn ds-btn-outline ds-btn-sm"
                      :on-click #(rf/dispatch [::unmapped-events/load-unmapped-items
                                               {:limit 50
                                                :offset 0
                                                :supplier-id supplier-filter}])}
            "Refresh")
          ($ :button {:id "btn-map-unmapped-items"
                      :class "ds-btn ds-btn-primary ds-btn-sm"
                      :disabled (empty? selected-ids)
                      :on-click #(rf/dispatch [::unmapped-events/open-map-modal])}
            "Map to article…")))

      (when err
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str err))))

      ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
        ($ :div {:class "ds-card-body space-y-3"}
          ($ :div {:class "flex flex-wrap items-center gap-3"}
            ($ :div {:class "ds-form-control"}
              ($ :label {:class "label"}
                ($ :span {:class "label-text"} "Supplier"))
              ($ :select {:id "filter-supplier-unmapped-items"
                          :class "ds-select ds-select-bordered ds-select-sm"
                          :value (or (some-> supplier-filter str) "")
                          :on-change (fn [e]
                                       (let [v (.. e -target -value)
                                             sid (when (seq v) v)]
                                         (rf/dispatch [::unmapped-events/set-supplier-filter sid])))}
                ($ :option {:value ""} "All")
                (for [s (or suppliers [])
                      :let [sid (:id s)
                            label (supplier-option-label s)]]
                  ($ :option {:key (str sid) :value (str sid)} label))))

            ($ :div {:class "text-sm text-base-content/70"}
              (str "Showing " (count (or items [])) " unmapped alias(es).")))

          (cond
            loading?
            ($ :div {:class "flex justify-center p-8"}
              ($ :span {:class "ds-loading ds-loading-spinner text-primary"}))

            (empty? items)
            ($ :div {:class "text-sm text-base-content/70"}
              "No unmapped aliases found.")

            :else
            ($ :div {:class "overflow-x-auto"}
              ($ :table {:class "ds-table w-full"}
                ($ :thead
                  ($ :tr
                    ($ :th "")
                    ($ :th "Raw Label")
                    ($ :th "Supplier")
                    ($ :th "Occurrences")))
                ($ :tbody
                  (for [it items
                        :let [iid (:id it)
                              iid-str (str iid)
                              checked? (contains? selected-ids iid)]]
                    ($ :tr {:id (str "row-unmapped-alias-" iid-str)
                            :key iid-str}
                      ($ :td
                        ($ :input {:id (str "toggle-unmapped-alias-" iid-str)
                                   :type "checkbox"
                                   :class "ds-checkbox"
                                   :checked checked?
                                   :on-change #(rf/dispatch [::unmapped-events/toggle-select-item iid])}))
                      ($ :td {:class "max-w-xl"}
                        ($ :div {:class "font-medium break-words"}
                          (or (:raw-label it) "—"))
                        (when-let [n (:raw-label-normalized it)]
                          ($ :div {:class "text-xs text-base-content/60"}
                            n)))
                      ($ :td
                        (or (:supplier-display-name it)
                          (some-> (:supplier-id it) str)
                          "—"))
                      ($ :td
                        (or (:occurrence_count it)
                          (:occurrence-count it)
                          0))))))))))

      ($ map-to-article-modal))))

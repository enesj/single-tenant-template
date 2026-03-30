(ns app.admin.frontend.pages.domain.expenses.duplicates
  "Admin Dedup & Merge page.

  Detects near-duplicate Suppliers, Articles, Stores, and Manufacturers,
  then merges them by reassigning FK references."
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.events.duplicates :as dup-events]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

;; ============================================================================
;; Entity Tabs
;; ============================================================================

(def ^:private entity-types
  [{:key "suppliers"     :label "Suppliers"}
   {:key "articles"      :label "Articles"}
   {:key "stores"        :label "Stores"}
   {:key "manufacturers" :label "Manufacturers"}
   {:key "subcategories" :label "Subcategories"}])

(def ^:private modes
  [{:key "automatic" :label "Automatic"}
   {:key "manual" :label "Manual"}])

(defn- entity-label
  [entity-type]
  (or (some (fn [{:keys [key label]}]
              (when (= key entity-type)
                label))
        entity-types)
    entity-type))

(defn- article-entity?
  [entity-type]
  (= entity-type "articles"))

(defui mode-tabs []
  (let [current (use-subscribe [::dup-events/mode])]
    ($ :div {:class "ds-tabs ds-tabs-boxed mb-4" :id "dedup-mode-tabs"}
      (for [{:keys [key label]} modes]
        ($ :button {:key key
                    :id (str "dedup-mode-" key)
                    :class (str "ds-tab" (when (= current key) " ds-tab-active"))
                    :on-click #(rf/dispatch [::dup-events/set-mode key])}
          label)))))

(defui entity-tabs []
  (let [current (use-subscribe [::dup-events/entity-type])]
    ($ :div {:class "ds-tabs ds-tabs-boxed mb-4" :id "dedup-entity-tabs"}
      (for [{:keys [key label]} entity-types]
        ($ :button {:key      key
                    :id       (str "dedup-tab-" key)
                    :class    (str "ds-tab" (when (= current key) " ds-tab-active"))
                    :on-click #(rf/dispatch [::dup-events/set-entity-type key])}
          label)))))

;; ============================================================================
;; Strategy Selector — each button shows its cluster count and loading state
;; ============================================================================

(def ^:private strategies
  [{:key "exact"       :label "Exact Match"}
   {:key "prefix"      :label "Prefix Grouping"}
   {:key "trigram"     :label "Trigram"}
   {:key "levenshtein" :label "Levenshtein"}])

(defui strategy-button
  "A single strategy toggle button showing its cluster count and per-strategy spinner."
  [{:keys [strategy-key label current]}]
  (let [cnt      (use-subscribe [::dup-events/cluster-count-for-strategy strategy-key])
        loading? (use-subscribe [::dup-events/loading-for-strategy? strategy-key])
        active?  (= current strategy-key)]
    ($ :button
      {:id       (str "dedup-strategy-" strategy-key)
       :class    (str "ds-btn ds-join-item ds-btn-sm"
                   (if active? " ds-btn-primary" " ds-btn-outline"))
       :on-click #(rf/dispatch [::dup-events/set-strategy strategy-key])}
      label
      (if loading?
        ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs ml-1"})
        ($ :span {:class (str "ds-badge ds-badge-sm ml-1"
                           (if active? " ds-badge-ghost" " ds-badge-outline"))}
          (str cnt))))))

(defui strategy-selector []
  (let [current (use-subscribe [::dup-events/strategy])]
    ($ :div {:class "ds-join mb-4" :id "dedup-strategy-selector"}
      (for [{:keys [key label]} strategies]
        ($ strategy-button {:key          key
                            :strategy-key key
                            :label        label
                            :current      current})))))

;; ============================================================================
;; Cluster Card
;; ============================================================================

(defn- context-column-label
  [entity-type]
  (case entity-type
    "articles" "Prices"
    "stores" "Supplier"
    "subcategories" "Category"
    "Normalized Key"))

(defn- article-price-labels
  [member]
  (or (:price-labels member)
    (:price_labels member)
    []))

(defn- article-unit-label
  [member]
  (some-> (or (:unit member)
            (:unit_name member)
            (:unit-name member))
    str
    str/trim
    not-empty))

(defn- article-member-sort-key
  [member]
  [(or (:canonical-name member)
     (:canonical_name member)
     (:display-name member)
     (:display_name member)
     (:name member)
     "")
   (or (article-unit-label member) "")
   (or (:created-at member)
     (:created_at member)
     "")
   (or (:id member) "")])

(defn- store-supplier-label
  [member]
  (or (:supplier-display-name member)
    (:supplier_display_name member)
    "—"))

(defn- subcategory-category-label
  [member]
  (or (:category-name member)
    (:category_name member)
    "—"))

(defn- entity-member-id
  [member]
  (some-> (:id member) str))

(defn- entity-display-name
  [member]
  (or (:display-name member)
    (:canonical-name member)
    (:display_name member)
    (:canonical_name member)
    (:name member)
    "—"))

(defui candidate-context-content [{:keys [entity-type member normalized-key]}]
  (case entity-type
    "articles"
    (let [prices (seq (article-price-labels member))]
      (if prices
        ($ :div {:class "flex flex-wrap gap-1"}
          (for [label prices]
            ($ :span {:key label
                      :class "ds-badge ds-badge-sm ds-badge-outline font-mono"}
              label)))
        ($ :span {:class "text-base-content/40"} "—")))

    "stores"
    ($ :span {:class "text-sm text-base-content/70"}
      (store-supplier-label member))

    "subcategories"
    ($ :span {:class "text-sm text-base-content/70"}
      (subcategory-category-label member))

    ($ :span {:class "text-base-content/60 text-sm font-mono"}
      normalized-key)))

(defui candidate-context-cell [{:keys [entity-type member normalized-key]}]
  ($ :td {:class "p-2"}
    ($ candidate-context-content {:entity-type entity-type
                                  :member member
                                  :normalized-key normalized-key})))

(defui article-unit-cell [{:keys [member]}]
  ($ :td {:class "p-2"}
    (if-let [unit (article-unit-label member)]
      ($ :span {:class "ds-badge ds-badge-sm ds-badge-primary ds-badge-outline font-mono"}
        unit)
      ($ :span {:class "text-base-content/40"} "—"))))

(defui cluster-member [{:keys [member cluster-idx entity-type is-primary? is-secondary? on-select-primary on-toggle-secondary]}]
  (let [member-id (or (:id member) (str (:id member)))
        display-name (or (:display-name member)
                       (:canonical-name member)
                       (:display_name member)
                       (:canonical_name member)
                       (:name member)
                       "—")
        normalized-key (or (:normalized-key member)
                         (:normalized_key member)
                         "—")
        usage-count (or (:usage-count member)
                      (:usage_count member)
                      0)]
    ($ :tr {:class (when is-primary? "bg-primary/10")}
      ($ :td {:class "p-2"}
        ($ :input {:type "radio"
                   :id (str "dedup-primary-" cluster-idx "-" member-id)
                   :name (str "primary-" cluster-idx)
                   :class "ds-radio ds-radio-primary ds-radio-sm"
                   :checked is-primary?
                   :on-change (fn [_] (on-select-primary member-id))}))
      ($ :td {:class "p-2"}
        (when-not is-primary?
          ($ :input {:type "checkbox"
                     :id (str "dedup-secondary-" cluster-idx "-" member-id)
                     :class "ds-checkbox ds-checkbox-sm"
                     :checked (boolean is-secondary?)
                     :on-change (fn [_] (on-toggle-secondary member-id))})))
      ($ :td {:class "p-2 font-medium"} display-name)
      (when (article-entity? entity-type)
        ($ article-unit-cell {:member member}))
      ($ candidate-context-cell {:entity-type entity-type
                                 :member member
                                 :normalized-key normalized-key})
      ($ :td {:class "p-2 text-center"}
        ($ :span {:class "ds-badge ds-badge-sm ds-badge-ghost"} (str usage-count))))))

(defui cluster-card [{:keys [cluster idx]}]
  (let [selections (use-subscribe [::dup-events/selections])
        entity-type (use-subscribe [::dup-events/entity-type])
        merging? (use-subscribe [::dup-events/merging?])
        flagging? (use-subscribe [::dup-events/flagging?])
        members (cond->> (:members cluster)
                  (article-entity? entity-type) (sort-by article-member-sort-key))
        cluster-id (or (:cluster-id cluster) (:cluster_id cluster))
        member-ids (->> members
                     (map :id)
                     (remove nil?)
                     vec)
        cluster-sel (get selections idx {})
        primary-id (:primary-id cluster-sel)
        secondary-ids (set (:secondary-ids cluster-sel []))
        on-select-primary (use-callback
                            (fn [member-id]
                              (rf/dispatch [::dup-events/select-primary idx member-id]))
                            [idx])
        on-toggle-secondary (use-callback
                              (fn [member-id]
                                (rf/dispatch [::dup-events/toggle-secondary idx member-id]))
                              [idx])
        can-merge? (and primary-id (seq secondary-ids))
        can-hide? (and (seq cluster-id) (seq member-ids))]
    ($ :div {:class "ds-card bg-base-100 shadow-md mb-4"
             :id (str "dedup-cluster-" idx)}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex justify-between items-center mb-2"}
          ($ :h3 {:class "font-semibold text-base"}
            (str "Cluster " (inc idx) " (" (count members) " members)"))
          ($ :div {:class "flex items-center gap-2"}
            (when can-hide?
              ($ :button
                {:id (str "dedup-hide-btn-" idx)
                 :title "Hide this cluster as a false positive"
                 :class (str "ds-btn ds-btn-sm ds-btn-outline"
                          (when flagging? " ds-loading"))
                 :disabled (or flagging? merging?)
                 :on-click (fn [_]
                             (rf/dispatch [::dup-events/ignore-cluster
                                           {:entity-type (keyword entity-type)
                                            :cluster-id cluster-id
                                            :member-ids member-ids}]))}
                "Hide"))
            ($ :button
              {:id (str "dedup-merge-btn-" idx)
               :class (str "ds-btn ds-btn-sm ds-btn-warning"
                        (when-not can-merge? " ds-btn-disabled")
                        (when merging? " ds-loading"))
               :disabled (or (not can-merge?) merging? flagging?)
               :on-click (fn [_]
                           (when can-merge?
                             (rf/dispatch [::dup-events/merge-preview
                                           {:entity-type (keyword entity-type)
                                            :primary-id primary-id
                                            :secondary-ids (vec secondary-ids)
                                            :cluster-idx idx}])))}
              "Merge")))
        ($ :div {:class "overflow-x-auto"}
          ($ :table {:class "ds-table ds-table-sm w-full"}
            ($ :thead
              ($ :tr
                ($ :th {:class "p-2 w-16"} "Primary")
                ($ :th {:class "p-2 w-16"} "Merge")
                ($ :th {:class "p-2"} "Name")
                (when (article-entity? entity-type)
                  ($ :th {:class "p-2 w-24"} "Unit"))
                ($ :th {:class "p-2"} (context-column-label entity-type))
                ($ :th {:class "p-2 text-center w-24"} "Usage")))
            ($ :tbody
              (for [member members]
                (let [mid (or (:id member) (str (:id member)))]
                  ($ cluster-member
                    {:key mid
                     :member member
                     :cluster-idx idx
                     :entity-type entity-type
                     :is-primary? (= primary-id mid)
                     :is-secondary? (contains? secondary-ids mid)
                     :on-select-primary on-select-primary
                     :on-toggle-secondary on-toggle-secondary}))))))))))

;; ============================================================================
;; Merge Modal
;; ============================================================================

(defui manual-search-box []
  (let [query (use-subscribe [::dup-events/manual-query])
        loading? (use-subscribe [::dup-events/manual-loading?])
        entity-type (use-subscribe [::dup-events/entity-type])]
    ($ :div {:class "mb-4" :id "dedup-manual-search-panel"}
      ($ :label {:for "dedup-manual-search-input"
                 :class "block text-sm font-medium mb-2"}
        (str "Search " (entity-label entity-type)))
      ($ :div {:class "relative"}
        ($ :input {:id "dedup-manual-search-input"
                   :type "text"
                   :class "ds-input ds-input-bordered w-full pr-10"
                   :placeholder (str "Find " (entity-label entity-type) " records to merge")
                   :value (or query "")
                   :on-change (fn [e]
                                (rf/dispatch [::dup-events/set-manual-query (.. e -target -value)]))})
        (when loading?
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm absolute right-3 top-3"})))
      ($ :p {:class "mt-2 text-sm text-base-content/60"}
        "Search within the current entity tab and add records to a manual merge selection."))))

(defui manual-selected-member-row
  [{:keys [member entity-type is-primary? is-secondary? on-select-primary on-toggle-secondary on-remove]}]
  (let [member-id (entity-member-id member)
        normalized-key (or (:normalized-key member) (:normalized_key member) "—")]
    ($ :tr {:class (when is-primary? "bg-primary/10")}
      ($ :td {:class "p-2"}
        ($ :input {:type "radio"
                   :id (str "dedup-manual-primary-" member-id)
                   :name "dedup-manual-primary"
                   :class "ds-radio ds-radio-primary ds-radio-sm"
                   :checked is-primary?
                   :on-change (fn [_] (on-select-primary member))}))
      ($ :td {:class "p-2"}
        (when-not is-primary?
          ($ :input {:type "checkbox"
                     :id (str "dedup-manual-secondary-" member-id)
                     :class "ds-checkbox ds-checkbox-sm"
                     :checked (boolean is-secondary?)
                     :on-change (fn [_] (on-toggle-secondary member))})))
      ($ :td {:class "p-2 font-medium"} (entity-display-name member))
      (when (article-entity? entity-type)
        ($ article-unit-cell {:member member}))
      ($ :td {:class "p-2"}
        ($ candidate-context-content {:entity-type entity-type
                                      :member member
                                      :normalized-key normalized-key}))
      ($ :td {:class "p-2 text-right"}
        ($ :button {:id (str "dedup-manual-remove-" member-id)
                    :class "ds-btn ds-btn-ghost ds-btn-xs"
                    :on-click (fn [_] (on-remove member-id))}
          "Remove")))))

(defui manual-selection-panel []
  (let [selection (use-subscribe [::dup-events/manual-selection])
        members (use-subscribe [::dup-events/manual-selected-members])
        entity-type (use-subscribe [::dup-events/entity-type])
        merging? (use-subscribe [::dup-events/merging?])
        primary-id (:primary-id selection)
        secondary-ids (set (:secondary-ids selection))
        can-merge? (and primary-id (seq secondary-ids))
        on-select-primary (use-callback
                            (fn [member]
                              (rf/dispatch [::dup-events/manual-select-primary member]))
                            [])
        on-toggle-secondary (use-callback
                              (fn [member]
                                (rf/dispatch [::dup-events/manual-toggle-secondary member]))
                              [])
        on-remove (use-callback
                    (fn [member-id]
                      (rf/dispatch [::dup-events/manual-remove-result member-id]))
                    [])]
    ($ :div {:class "ds-card bg-base-100 shadow-md mb-4" :id "dedup-manual-selection"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex flex-col gap-3 md:flex-row md:items-center md:justify-between mb-3"}
          ($ :div
            ($ :h3 {:class "font-semibold text-base"} "Manual merge selection")
            ($ :p {:class "text-sm text-base-content/60"}
              "Pick one primary record and one or more merge candidates. Search again to add more records."))
          ($ :button {:id "dedup-manual-merge-btn"
                      :class (str "ds-btn ds-btn-warning"
                               (when-not can-merge? " ds-btn-disabled")
                               (when merging? " ds-loading"))
                      :disabled (or (not can-merge?) merging?)
                      :on-click (fn [_]
                                  (when can-merge?
                                    (rf/dispatch [::dup-events/merge-preview
                                                  {:entity-type (keyword entity-type)
                                                   :primary-id primary-id
                                                   :secondary-ids (vec secondary-ids)
                                                   :source :manual}])))}
            "Merge selected"))
        (if (seq members)
          ($ :div {:class "overflow-x-auto"}
            ($ :table {:class "ds-table ds-table-sm w-full"}
              ($ :thead
                ($ :tr
                  ($ :th {:class "p-2 w-16"} "Primary")
                  ($ :th {:class "p-2 w-16"} "Merge")
                  ($ :th {:class "p-2"} "Name")
                  (when (article-entity? entity-type)
                    ($ :th {:class "p-2 w-24"} "Unit"))
                  ($ :th {:class "p-2"} (context-column-label entity-type))
                  ($ :th {:class "p-2 w-24"} "Remove")))
              ($ :tbody
                (for [member members]
                  (let [member-id (entity-member-id member)]
                    ($ manual-selected-member-row
                      {:key member-id
                       :member member
                       :entity-type entity-type
                       :is-primary? (= primary-id member-id)
                       :is-secondary? (contains? secondary-ids member-id)
                       :on-select-primary on-select-primary
                       :on-toggle-secondary on-toggle-secondary
                       :on-remove on-remove}))))))
          ($ :div {:class "ds-alert ds-alert-info" :id "dedup-manual-empty-selection"}
            ($ :span "No records selected yet. Search for entities and add them here first.")))))))

(defui manual-search-result-row
  [{:keys [member entity-type selected? primary? on-set-primary on-toggle-selected]}]
  (let [member-id (entity-member-id member)
        normalized-key (or (:normalized-key member) (:normalized_key member) "—")]
    ($ :tr
      ($ :td {:class "p-2 font-medium"} (entity-display-name member))
      (when (article-entity? entity-type)
        ($ article-unit-cell {:member member}))
      ($ :td {:class "p-2"}
        ($ candidate-context-content {:entity-type entity-type
                                      :member member
                                      :normalized-key normalized-key}))
      ($ :td {:class "p-2"}
        (cond
          primary?
          ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"} "Primary")

          selected?
          ($ :span {:class "ds-badge ds-badge-outline ds-badge-sm"} "Selected")

          :else
          ($ :span {:class "text-base-content/40 text-sm"} "Not selected")))
      ($ :td {:class "p-2 text-right"}
        ($ :div {:class "flex justify-end gap-2"}
          ($ :button {:id (str "dedup-manual-primary-btn-" member-id)
                      :class (str "ds-btn ds-btn-xs"
                               (if primary? " ds-btn-primary" " ds-btn-outline"))
                      :on-click (fn [_] (on-set-primary member))}
            "Set primary")
          ($ :button {:id (str "dedup-manual-toggle-btn-" member-id)
                      :class (str "ds-btn ds-btn-xs"
                               (if selected? " ds-btn-ghost" " ds-btn-outline"))
                      :on-click (fn [_] (on-toggle-selected member))}
            (if selected? "Remove" "Add")))))))

(defui manual-search-results []
  (let [query (use-subscribe [::dup-events/manual-query])
        loading? (use-subscribe [::dup-events/manual-loading?])
        results (use-subscribe [::dup-events/manual-results])
        selection (use-subscribe [::dup-events/manual-selection])
        entity-type (use-subscribe [::dup-events/entity-type])
        selected-items (:selected-items selection)
        primary-id (:primary-id selection)
        on-set-primary (use-callback
                         (fn [member]
                           (rf/dispatch [::dup-events/manual-select-primary member]))
                         [entity-type])
        on-toggle-selected (use-callback
                             (fn [member]
                               (let [member-id (entity-member-id member)]
                                 (if (contains? selected-items member-id)
                                   (rf/dispatch [::dup-events/manual-remove-result member-id])
                                   (rf/dispatch [::dup-events/manual-add-result member]))))
                             [selected-items])]
    ($ :div {:class "ds-card bg-base-100 shadow-md" :id "dedup-manual-results"}
      ($ :div {:class "ds-card-body p-4"}
        ($ :h3 {:class "font-semibold text-base mb-3"} "Search results")
        (cond
          loading?
          ($ :div {:class "flex justify-center py-8"}
            ($ :span {:class "ds-loading ds-loading-spinner ds-loading-md text-primary"}))

          (< (count (or query "")) 2)
          ($ :div {:class "ds-alert ds-alert-info" :id "dedup-manual-search-hint"}
            ($ :span "Type at least 2 characters to search."))

          (empty? results)
          ($ :div {:class "ds-alert ds-alert-info" :id "dedup-manual-no-results"}
            ($ :span (str "No matching " (str/lower-case (entity-label entity-type)) " found.")))

          :else
          ($ :div {:class "overflow-x-auto"}
            ($ :table {:class "ds-table ds-table-sm w-full"}
              ($ :thead
                ($ :tr
                  ($ :th {:class "p-2"} "Name")
                  (when (article-entity? entity-type)
                    ($ :th {:class "p-2 w-24"} "Unit"))
                  ($ :th {:class "p-2"} (context-column-label entity-type))
                  ($ :th {:class "p-2 w-28"} "State")
                  ($ :th {:class "p-2 w-44"} "Actions")))
              ($ :tbody
                (for [member results]
                  (let [member-id (entity-member-id member)]
                    ($ manual-search-result-row
                      {:key member-id
                       :member member
                       :entity-type entity-type
                       :selected? (contains? selected-items member-id)
                       :primary? (= primary-id member-id)
                       :on-set-primary on-set-primary
                       :on-toggle-selected on-toggle-selected})))))))))))

(defui manual-mode-panel []
  ($ :div {:id "dedup-manual-panel"}
    ($ manual-search-box)
    ($ manual-selection-panel)
    ($ manual-search-results)))

(defui merge-modal []
  (let [show? (use-subscribe [::dup-events/show-merge-modal?])
        preview (use-subscribe [::dup-events/merge-preview])
        merging? (use-subscribe [::dup-events/merging?])
        pending (use-subscribe [::dup-events/pending-merge])
        entity-type (:entity-type pending)
        primary-id (:primary-id pending)
        secondary-ids (vec (:secondary-ids pending))]
    (when show?
      ($ :div {:class "ds-modal ds-modal-open" :id "dedup-merge-modal"}
        ($ :div {:class "ds-modal-box"}
          ($ :h3 {:class "font-bold text-lg mb-4"} "Confirm Merge")
          ($ :p {:class "mb-2 text-sm text-base-content/70"}
            "The following FK references will be reassigned to the primary record:")
          (when preview
            ($ :div {:class "overflow-x-auto mb-4"}
              ($ :table {:class "ds-table ds-table-sm w-full"}
                ($ :thead
                  ($ :tr
                    ($ :th "Table")
                    ($ :th {:class "text-right"} "Affected Rows")))
                ($ :tbody
                  (for [[table-name cnt] (sort-by first preview)]
                    ($ :tr {:key (str table-name)}
                      ($ :td (name table-name))
                      ($ :td {:class "text-right font-mono"} (str cnt))))))))
          ($ :div {:class "ds-modal-action"}
            ($ :button {:id "dedup-merge-cancel"
                        :class "ds-btn ds-btn-ghost"
                        :on-click #(rf/dispatch [::dup-events/close-merge-modal])}
              "Cancel")
            ($ :button {:id "dedup-merge-confirm"
                        :class (str "ds-btn ds-btn-warning"
                                 (when merging? " ds-loading"))
                        :disabled merging?
                        :on-click (fn [_]
                                    (rf/dispatch [::dup-events/execute-merge
                                                  {:entity-type entity-type
                                                   :primary-id primary-id
                                                   :secondary-ids secondary-ids}]))}
              "Confirm Merge")))))))

;; ============================================================================
;; Main Page
;; ============================================================================

(defui admin-duplicates-page []
  (let [error (use-subscribe [::dup-events/error])
        clusters (use-subscribe [::dup-events/clusters])
        strategy (use-subscribe [::dup-events/strategy])
        mode (use-subscribe [::dup-events/mode])
        loading? (use-subscribe [::dup-events/loading-for-strategy? strategy])]

    (use-effect
      (fn []
        (rf/dispatch [::dup-events/detect-all {}])
        js/undefined)
      [])

    ($ layout/admin-layout
      ($ :div {:class "p-6 min-h-screen"}
        ($ mode-tabs)
        ($ entity-tabs)

        (when (= mode "automatic")
          ($ strategy-selector))

        (when error
          ($ :div {:class "ds-alert ds-alert-error mb-4" :id "dedup-error"}
            ($ :span error)))

        (if (= mode "manual")
          ($ manual-mode-panel)
          (cond
            loading?
            ($ :div {:class "flex justify-center py-12"}
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"}))

            (and clusters (empty? clusters))
            ($ :div {:class "ds-alert ds-alert-info" :id "dedup-no-results"}
              ($ :span "No duplicates found for this strategy."))

            (seq clusters)
            ($ :div {:id "dedup-clusters-list"}
              ($ :p {:class "text-sm text-base-content/60 mb-3"}
                (str (count clusters) " cluster(s) found. Select a primary record and secondaries to merge."))
              (for [[idx cluster] (map-indexed vector clusters)]
                ($ cluster-card {:key idx :cluster cluster :idx idx})))))

        ($ merge-modal)))))

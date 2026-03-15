(ns app.admin.frontend.pages.domain.expenses.duplicates
  "Admin Dedup & Merge page.

  Detects near-duplicate Suppliers, Articles, Stores, and Manufacturers,
  then merges them by reassigning FK references."
  (:require
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.events.duplicates :as dup-events]
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
   {:key "manufacturers" :label "Manufacturers"}])

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
    "stores"   "Supplier"
    "Normalized Key"))

(defn- article-price-labels
  [member]
  (or (:price-labels member)
    (:price_labels member)
    []))

(defn- store-supplier-label
  [member]
  (or (:supplier-display-name member)
    (:supplier_display_name member)
    "—"))

(defui candidate-context-cell [{:keys [entity-type member normalized-key]}]
  (case entity-type
    "articles"
    (let [prices (seq (article-price-labels member))]
      ($ :td {:class "p-2"}
        (if prices
          ($ :div {:class "flex flex-wrap gap-1"}
            (for [label prices]
              ($ :span {:key   label
                        :class "ds-badge ds-badge-sm ds-badge-outline font-mono"}
                label)))
          ($ :span {:class "text-base-content/40"} "—"))))

    "stores"
    ($ :td {:class "p-2 text-sm text-base-content/70"}
      (store-supplier-label member))

    ($ :td {:class "p-2 text-base-content/60 text-sm font-mono"}
      normalized-key)))

(defui cluster-member [{:keys [member cluster-idx entity-type is-primary? is-secondary? on-select-primary on-toggle-secondary]}]
  (let [member-id     (or (:id member) (str (:id member)))
        display-name  (or (:display-name member)
                        (:canonical-name member)
                        (:display_name member)
                        (:canonical_name member)
                        "—")
        normalized-key (or (:normalized-key member)
                         (:normalized_key member)
                         "—")
        usage-count   (or (:usage-count member)
                        (:usage_count member)
                        0)]
    ($ :tr {:class (when is-primary? "bg-primary/10")}
      ($ :td {:class "p-2"}
        ($ :input {:type      "radio"
                   :id        (str "dedup-primary-" cluster-idx "-" member-id)
                   :name      (str "primary-" cluster-idx)
                   :class     "ds-radio ds-radio-primary ds-radio-sm"
                   :checked   is-primary?
                   :on-change (fn [_] (on-select-primary member-id))}))
      ($ :td {:class "p-2"}
        (when-not is-primary?
          ($ :input {:type      "checkbox"
                     :id        (str "dedup-secondary-" cluster-idx "-" member-id)
                     :class     "ds-checkbox ds-checkbox-sm"
                     :checked   (boolean is-secondary?)
                     :on-change (fn [_] (on-toggle-secondary member-id))})))
      ($ :td {:class "p-2 font-medium"} display-name)
      ($ candidate-context-cell {:entity-type   entity-type
                                 :member        member
                                 :normalized-key normalized-key})
      ($ :td {:class "p-2 text-center"}
        ($ :span {:class "ds-badge ds-badge-sm ds-badge-ghost"} (str usage-count))))))

(defui cluster-card [{:keys [cluster idx]}]
  (let [selections  (use-subscribe [::dup-events/selections])
        entity-type (use-subscribe [::dup-events/entity-type])
        merging?    (use-subscribe [::dup-events/merging?])
        flagging?   (use-subscribe [::dup-events/flagging?])
        members     (:members cluster)
        cluster-id  (or (:cluster-id cluster) (:cluster_id cluster))
        member-ids  (->> members
                      (map :id)
                      (remove nil?)
                      vec)
        cluster-sel     (get selections idx {})
        primary-id      (:primary-id cluster-sel)
        secondary-ids   (set (:secondary-ids cluster-sel []))
        on-select-primary   (use-callback
                              (fn [member-id]
                                (rf/dispatch [::dup-events/select-primary idx member-id]))
                              [idx])
        on-toggle-secondary (use-callback
                              (fn [member-id]
                                (rf/dispatch [::dup-events/toggle-secondary idx member-id]))
                              [idx])
        can-merge? (and primary-id (seq secondary-ids))
        can-hide?  (and (seq cluster-id) (seq member-ids))]
    ($ :div {:class "ds-card bg-base-100 shadow-md mb-4"
             :id    (str "dedup-cluster-" idx)}
      ($ :div {:class "ds-card-body p-4"}
        ($ :div {:class "flex justify-between items-center mb-2"}
          ($ :h3 {:class "font-semibold text-base"}
            (str "Cluster " (inc idx) " (" (count members) " members)"))
          ($ :div {:class "flex items-center gap-2"}
            (when can-hide?
              ($ :button
                {:id       (str "dedup-hide-btn-" idx)
                 :title    "Hide this cluster as a false positive"
                 :class    (str "ds-btn ds-btn-sm ds-btn-outline"
                             (when flagging? " ds-loading"))
                 :disabled (or flagging? merging?)
                 :on-click (fn [_]
                             (rf/dispatch [::dup-events/ignore-cluster
                                           {:entity-type (keyword entity-type)
                                            :cluster-id  cluster-id
                                            :member-ids  member-ids}]))}
                "Hide"))
            ($ :button
              {:id       (str "dedup-merge-btn-" idx)
               :class    (str "ds-btn ds-btn-sm ds-btn-warning"
                           (when-not can-merge? " ds-btn-disabled")
                           (when merging? " ds-loading"))
               :disabled (or (not can-merge?) merging? flagging?)
               :on-click (fn [_]
                           (when can-merge?
                             (rf/dispatch [::dup-events/merge-preview
                                           {:entity-type  (keyword entity-type)
                                            :primary-id   primary-id
                                            :secondary-ids (vec secondary-ids)
                                            :cluster-idx  idx}])))}
              "Merge")))
        ($ :div {:class "overflow-x-auto"}
          ($ :table {:class "ds-table ds-table-sm w-full"}
            ($ :thead
              ($ :tr
                ($ :th {:class "p-2 w-16"} "Primary")
                ($ :th {:class "p-2 w-16"} "Merge")
                ($ :th {:class "p-2"} "Name")
                ($ :th {:class "p-2"} (context-column-label entity-type))
                ($ :th {:class "p-2 text-center w-24"} "Usage")))
            ($ :tbody
              (for [[i member] (map-indexed vector members)]
                (let [mid (or (:id member) (str (:id member)))]
                  ($ cluster-member
                    {:key                mid
                     :member             member
                     :cluster-idx        idx
                     :entity-type        entity-type
                     :is-primary?        (= primary-id mid)
                     :is-secondary?      (contains? secondary-ids mid)
                     :on-select-primary  on-select-primary
                     :on-toggle-secondary on-toggle-secondary}))))))))))

;; ============================================================================
;; Merge Modal
;; ============================================================================

(defui merge-modal []
  (let [show?       (use-subscribe [::dup-events/show-merge-modal?])
        preview     (use-subscribe [::dup-events/merge-preview])
        merging?    (use-subscribe [::dup-events/merging?])
        pending     (use-subscribe [::dup-events/pending-merge])
        selections  (use-subscribe [::dup-events/selections])
        entity-type (use-subscribe [::dup-events/entity-type])
        cluster-idx  (:cluster-idx pending)
        cluster-sel  (when cluster-idx (get selections cluster-idx {}))
        primary-id   (when cluster-sel (:primary-id cluster-sel))
        secondary-ids (when cluster-sel (vec (:secondary-ids cluster-sel [])))]
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
            ($ :button {:id       "dedup-merge-cancel"
                        :class    "ds-btn ds-btn-ghost"
                        :on-click #(rf/dispatch [::dup-events/close-merge-modal])}
              "Cancel")
            ($ :button {:id       "dedup-merge-confirm"
                        :class    (str "ds-btn ds-btn-warning"
                                    (when merging? " ds-loading"))
                        :disabled merging?
                        :on-click (fn [_]
                                    (rf/dispatch [::dup-events/execute-merge
                                                  {:entity-type  (keyword entity-type)
                                                   :primary-id   primary-id
                                                   :secondary-ids secondary-ids}]))}
              "Confirm Merge")))))))

;; ============================================================================
;; Main Page
;; ============================================================================

(defui admin-duplicates-page []
  (let [error       (use-subscribe [::dup-events/error])
        clusters    (use-subscribe [::dup-events/clusters])
        entity-type (use-subscribe [::dup-events/entity-type])
        strategy    (use-subscribe [::dup-events/strategy])
        loading?    (use-subscribe [::dup-events/loading-for-strategy? strategy])]

    ;; Auto-detect all strategies on mount
    (use-effect
      (fn []
        (rf/dispatch [::dup-events/detect-all {}])
        js/undefined)
      [])

    ($ layout/admin-layout
      ($ :div {:class "p-6 min-h-screen"}

        ;; Entity type tabs
        ($ entity-tabs)

        ;; Strategy selector (with live counts)
        ($ strategy-selector)

        ;; Error display
        (when error
          ($ :div {:class "ds-alert ds-alert-error mb-4" :id "dedup-error"}
            ($ :span error)))

        ;; Results for the selected strategy
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
              ($ cluster-card {:key idx :cluster cluster :idx idx}))))

        ;; Merge confirmation modal
        ($ merge-modal)))))

(ns app.domain.frontend.expenses.components.related-records-wizard
  "Generic related records wizard modal usable by any entity.

  This is the multi-entity equivalent of `article-related-records-wizard`.
  Instead of hard-coding article-specific subs and events, it derives
  all keywords from the entity-singular and entity-key props."
  (:require
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.components.shared-utils :as shared]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

;; ============================================================================
;; Record summary - shared across all entity types
;; ============================================================================

(defn- details-label
  [key-value]
  (-> key-value
    name
    (str/replace #"-" " ")
    (str/split #"\s+")
    (->> (map str/capitalize)
      (str/join " "))))

(defn- money-label
  [amount currency]
  (if (some? amount)
    (str amount (when (seq currency) (str " " currency)))
    "\u2014"))

(defn record-summary
  "Generate a one-line summary for any related record type."
  [related-type record]
  (case related-type
    "expenses"
    (str (or (shared/format-date (:purchased-at record)) "Unknown date")
      " \u2022 "
      (or (:supplier-display-name record) "Unknown provider")
      " \u2022 "
      (money-label (:total-amount record) (:currency record)))

    "receipts"
    (str (or (:original-filename record) "Unnamed receipt")
      " \u2022 "
      (or (:status record) "unknown"))

    "providers"
    (str (or (:display-name record) "Unnamed provider")
      (when-let [nk (:normalized-key record)]
        (str " \u2022 " nk)))

    "stores"
    (str (or (:display-name record) "Unnamed store")
      (when-let [supplier (:supplier-display-name record)]
        (str " \u2022 " supplier)))

    "manufacturers"
    (str (or (:display-name record) "Unnamed manufacturer")
      (when-let [nk (:normalized-key record)]
        (str " \u2022 " nk)))

    "subcategories"
    (str (or (:name record) "Unnamed subcategory")
      (when-let [cat (:category-name record)]
        (str " \u2022 " cat)))

    "articles"
    (str (or (:canonical-name record) "Unnamed article")
      (when-let [mfr (:manufacturer-display-name record)]
        (str " \u2022 " mfr)))

    (or (str (:id record)) "Record")))

;; ============================================================================
;; Type label helper
;; ============================================================================

(defn- related-type-label
  [related-type]
  (case related-type
    "expenses" "Expenses"
    "receipts" "Receipts"
    "providers" "Providers"
    "stores" "Stores"
    "manufacturers" "Manufacturers"
    "subcategories" "Subcategories"
    "articles" "Articles"
    "Records"))

;; ============================================================================
;; Generic wizard component
;; ============================================================================

(defui related-records-wizard
  "Generic related records wizard component.

  Props:
  - :entity-singular   - e.g. \"supplier\"
  - :entity-key        - e.g. :suppliers
  - :type-options      - [{:id \"expenses\" :label \"Expenses\" :description \"...\"}]
  - :entity-name-fn    - (fn [entity] -> string)
  - :record-summary-fn - (fn [related-type record] -> string), defaults to `record-summary`"
  [{:keys [entity-singular entity-key type-options entity-name-fn record-summary-fn]}]
  (let [s entity-singular
        events-ns (str "app.domain.frontend.expenses.events." (name entity-key))
        summary-fn (or record-summary-fn record-summary)
        open? (use-subscribe [(keyword "expenses" (str s "-related-records-modal-open?"))])
        entity (use-subscribe [(keyword "expenses" (str s "-related-records-modal-entity"))])
        step (use-subscribe [(keyword "expenses" (str s "-related-records-step"))])
        related-type (use-subscribe [(keyword "expenses" (str s "-related-records-type"))])
        records (use-subscribe [(keyword "expenses" (str s "-related-records"))])
        selected-record (use-subscribe [(keyword "expenses" (str s "-related-record"))])
        loading? (use-subscribe [(keyword "expenses" (str s "-related-records-loading?"))])
        error (use-subscribe [(keyword "expenses" (str s "-related-records-error"))])
        entity-id* (or (:id entity) "unknown")
        entity-display (if entity-name-fn
                         (entity-name-fn entity)
                         (or (:display-name entity)
                           (:canonical-name entity)
                           (:name entity)
                           (:raw-label entity)
                           (str s)))]
    ($ modal-wrapper
      {:id (str "modal-related-records-" s "-" entity-id*)
       :visible? open?
       :title "Show related records"
       :size :large
       :close-button-id (str "btn-close-related-records-" s "-" entity-id*)
       :on-close [(keyword events-ns "close-related-records-modal")]}
      ($ :div {:class "space-y-5"
               :id (str "wizard-related-records-" s "-" entity-id*)}
        ;; Context header
        ($ :div {:class "rounded-lg border border-base-300 bg-base-200/40 p-4 space-y-2"
                 :id (str "context-related-records-" s "-" entity-id*)}
          ($ :div {:class "flex items-center justify-between gap-3"}
            ($ :div {:class "ds-badge ds-badge-outline ds-badge-sm"
                     :id (str "text-related-records-step-" s "-" entity-id*)}
              (str "Step " step " of 3"))
            ($ :div {:class "text-xs uppercase tracking-wide text-base-content/60"}
              (str "Selected " s)))
          ($ :div {:class "text-sm font-semibold text-base-content break-words"
                   :id (str "text-related-records-name-" s "-" entity-id*)}
            entity-display))

        (case step
          ;; Step 1: Select type
          1
          ($ :div {:class "space-y-4"
                   :id (str "step-1-related-records-" s "-" entity-id*)}
            ($ :div {:class "text-sm text-base-content/80"}
              "Choose related record type")
            ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-2"}
              (for [{:keys [id label description]} type-options
                    :let [selected? (= related-type id)]]
                ($ :button {:key id
                            :id (str "btn-related-type-" id "-" s "-" entity-id*)
                            :class (str "w-full rounded-lg border px-4 py-3 text-left transition "
                                     "focus:outline-none focus-visible:ring-2 focus-visible:ring-primary "
                                     (if selected?
                                       "border-primary bg-primary/10 shadow-sm"
                                       "border-base-300 bg-base-100 hover:border-base-content/30 hover:bg-base-200/60"))
                            :on-click (fn []
                                        (rf/dispatch [(keyword events-ns "select-related-type") id]))}
                  ($ :div {:class "flex items-start justify-between gap-2"}
                    ($ :span {:class "text-sm font-medium text-base-content"} label)
                    (when selected?
                      ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"} "Selected")))
                  ($ :div {:class "text-xs text-base-content/70 mt-1"} description))))
            ($ :div {:class "flex items-center justify-between gap-3"}
              ($ :div {:id (str "text-related-step-1-selection-" s "-" entity-id*)
                       :class "text-xs text-base-content/60"}
                (if related-type
                  (str "Selected: " (related-type-label related-type))
                  "Select one type to continue."))
              ($ :button {:id (str "btn-next-related-step-1-" s "-" entity-id*)
                          :class "ds-btn ds-btn-primary"
                          :disabled (nil? related-type)
                          :on-click (fn []
                                      (rf/dispatch [(keyword events-ns "next-related-records-step")]))}
                "Next")))

          ;; Step 2: Select record
          2
          ($ :div {:class "space-y-3"
                   :id (str "step-2-related-records-" s "-" entity-id*)}
            ($ :div {:class "flex items-center justify-between gap-3"}
              ($ :div {:class "text-sm text-base-content/80"}
                (str "Choose one " (str/lower-case (related-type-label related-type)) " record"))
              ($ :div {:class "flex items-center gap-2"}
                ($ :button {:id (str "btn-back-related-step-1-" s "-" entity-id*)
                            :class "ds-btn ds-btn-ghost ds-btn-sm"
                            :on-click (fn []
                                        (rf/dispatch [(keyword events-ns "back-related-records-step")]))}
                  "Back")
                ($ :button {:id (str "btn-next-related-step-2-" s "-" entity-id*)
                            :class "ds-btn ds-btn-primary ds-btn-sm"
                            :disabled (or loading? (nil? selected-record))
                            :on-click (fn []
                                        (rf/dispatch [(keyword events-ns "next-related-records-step")]))}
                  "Next")))

            (cond
              loading?
              ($ :div {:id (str "loading-related-records-" s "-" entity-id*)
                       :class "rounded-lg border border-base-300 bg-base-200/40 p-3 flex items-center gap-2"}
                ($ :span {:class "ds-loading ds-loading-spinner ds-loading-sm"})
                ($ :span {:class "text-sm"} "Loading related records..."))

              error
              ($ :div {:id (str "error-related-records-" s "-" entity-id*)
                       :class "ds-alert ds-alert-error"}
                ($ :span (str error)))

              (seq records)
              ($ :div {:class "space-y-2 max-h-[420px] overflow-y-auto pr-1"
                       :id (str "list-related-records-" s "-" entity-id*)}
                (for [record records
                      :let [record-id (or (:id record) (hash record))
                            selected? (= selected-record record)]]
                  ($ :button {:key (str record-id)
                              :id (str "btn-select-related-record-" related-type "-" record-id "-" s "-" entity-id*)
                              :class (str "w-full rounded-lg border p-3 text-left transition "
                                       "focus:outline-none focus-visible:ring-2 focus-visible:ring-primary "
                                       (if selected?
                                         "border-primary bg-primary/10 shadow-sm"
                                         "border-base-300 bg-base-100 hover:border-base-content/30 hover:bg-base-200/60"))
                              :on-click (fn []
                                          (rf/dispatch [(keyword events-ns "select-related-record") record]))}
                    ($ :div {:class "flex items-start justify-between gap-3"}
                      ($ :span {:class "text-sm text-base-content"}
                        (summary-fn related-type record))
                      (when selected?
                        ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"} "Selected"))))))

              :else
              ($ :div {:id (str "empty-related-records-" s "-" entity-id*)
                       :class "rounded-lg border border-dashed border-base-300 bg-base-100 p-3 text-sm text-base-content/70"}
                "No related records found.")))

          ;; Step 3: Record details
          3
          ($ :div {:class "space-y-3"
                   :id (str "step-3-related-records-" s "-" entity-id*)}
            ($ :div {:class "flex items-center justify-between"}
              ($ :div {:class "text-sm text-base-content/80"}
                (str "Details: " (related-type-label related-type)))
              ($ :button {:id (str "btn-back-related-step-2-" s "-" entity-id*)
                          :class "ds-btn ds-btn-ghost ds-btn-sm"
                          :on-click (fn []
                                      (rf/dispatch [(keyword events-ns "back-related-records-step")]))}
                "Back"))

            (if (map? selected-record)
              ($ :div {:class "grid grid-cols-1 md:grid-cols-2 gap-2"
                       :id (str "details-related-record-" s "-" entity-id*)}
                (for [[kv value] (sort-by (comp name key) (seq selected-record))]
                  ($ :div {:key (str (name kv))
                           :id (str "detail-related-record-" (name kv) "-" s "-" entity-id*)
                           :class "p-3 rounded-lg border border-base-300 bg-base-100 space-y-1"}
                    ($ :div {:class "text-xs uppercase tracking-wide text-base-content/60"}
                      (details-label kv))
                    ($ :div {:class "text-sm"}
                      (shared/format-value value "\u2014" false)))))
              ($ :div {:class "rounded-lg border border-dashed border-base-300 bg-base-100 p-3 text-sm text-base-content/70"}
                "No record selected.")))

          nil)))))

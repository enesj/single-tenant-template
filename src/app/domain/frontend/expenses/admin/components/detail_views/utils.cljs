(ns app.domain.frontend.expenses.admin.components.detail-views.utils
  "Shared helpers for detail view components."
  (:require
    [app.template.frontend.components.shared-utils :as shared]
    [app.template.frontend.utils.id :as id-utils]
    [uix.core :refer [$ defui]]))

(defn label-value
  [label value]
  ($ :div {:class "flex flex-col gap-1 p-3 bg-base-200 rounded-lg"}
    ($ :span {:class "text-xs uppercase tracking-wide text-base-content/70"} label)
    ($ :span {:class "text-sm font-medium"}
      (shared/format-value value "—" false))))

(defn format-money
  [amount currency]
  (when (some? amount)
    (str amount (when currency (str " " currency)))))

(defn- column-value
  [row {:keys [key value-fn]}]
  (cond
    (fn? value-fn) (value-fn row)
    key (get row key)
    :else nil))

(defui related-table
  [{:keys [title rows columns empty-label view-all-href view-all-id header-actions]}]
  (let [rows* (take 5 (or rows []))]
    ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
      ($ :div {:class "ds-card-body space-y-3"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :h2 {:class "text-lg font-semibold"} title)
          ($ :div {:class "flex items-center gap-2"}
            header-actions
            (when view-all-href
              ($ :a {:id view-all-id
                     :href view-all-href
                     :class "ds-btn ds-btn-ghost ds-btn-xs"}
                "View all"))))
        (if (seq rows*)
          ($ :div {:class "overflow-x-auto"}
            ($ :table {:class "ds-table w-full"}
              ($ :thead
                ($ :tr
                  (for [{:keys [label]} columns]
                    ($ :th {:key label} label))))
              ($ :tbody
                (for [row rows*
                      :let [row-id (or (id-utils/extract-entity-id row) (hash row))]]
                  ($ :tr {:key (str row-id)}
                    (for [col columns
                          :let [cell-value (column-value row col)]]
                      ($ :td {:key (str row-id "-" (:label col))}
                        (shared/format-value cell-value "—" false))))))))
          ($ :div {:class "text-sm text-base-content/70"}
            (or empty-label "No related records found.")))))))



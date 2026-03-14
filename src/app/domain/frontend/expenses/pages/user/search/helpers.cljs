(ns app.domain.frontend.expenses.pages.user.search.helpers
  "Formatting helpers and detail panel UI primitives shared across
  search detail body components."
  (:require
    [app.template.frontend.utils.timestamp :as timestamp]
    [uix.core :refer [$]]))

;; ---------------------------------------------------------------------------
;; Formatting helpers
;; ---------------------------------------------------------------------------

(defn format-amount [amount currency]
  (when amount
    (try
      (.toLocaleString (js/Number amount) "en-US"
        #js {:style "currency"
             :currency (or currency "USD")
             :minimumFractionDigits 2
             :maximumFractionDigits 2})
      (catch :default _
        (str (or currency "") " " amount)))))

(defn format-date [s]
  (when s (timestamp/format-timestamp-string s)))

;; ---------------------------------------------------------------------------
;; Detail panel primitives
;; ---------------------------------------------------------------------------

(defn detail-row [label value]
  (when value
    ($ :div {:class "flex justify-between gap-4 py-2 border-b border-base-200 last:border-0"}
      ($ :span {:class "text-sm text-base-content/50 flex-shrink-0"} label)
      ($ :span {:class "text-sm font-medium text-right truncate"} (str value)))))

;; Sortable column header
(defn sort-header [{:keys [label col sort-col sort-dir on-sort class]}]
  (let [active? (= col sort-col)
        arrow (if active? (if (= sort-dir :asc) " \u25B2" " \u25BC") "")]
    ($ :th {:class (str class " cursor-pointer select-none hover:text-base-content")
            :on-click #(on-sort col)}
      (str label arrow))))

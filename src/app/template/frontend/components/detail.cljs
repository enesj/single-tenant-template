(ns app.template.frontend.components.detail
  "Reusable detail-view components (headers, cards, and fields).

  These components were extracted from admin-only UI helpers so they can be reused
  by domain user pages without depending on `app.admin.*` namespaces."
  (:require
    [app.template.frontend.utils.display :as display]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

(defui detail-modal-header
  "Rich header layout for detail modals.

  Props:
  - :title (required)
  - :subtitle
  - :eyebrow
  - :icon (React element, hiccup vector, or string)
  - :icon-bg
  - :meta (vector of {:label :value :value-class})
  - :right (single element or seq of elements)
  - :class"
  [props]
  (let [{:keys [title subtitle eyebrow icon icon-bg meta right class]} props
        present? (fn [v]
                   (cond
                     (nil? v) false
                     (and (string? v) (str/blank? v)) false
                     :else true))
        icon-node (cond
                    (nil? icon) nil
                    (display/react-element? icon) icon
                    (vector? icon) icon
                    :else ($ :span {:class "text-xl font-semibold text-primary"} icon))
        normalized-right (when right
                           (if (sequential? right)
                             (into [] (remove nil?) right)
                             [right]))
        meta-items (->> meta
                     (keep (fn [{:keys [label value value-class] :as item}]
                             (when (and item (present? value))
                               {:label label
                                :value value
                                :value-class value-class}))))]
    ($ :div {:class (str "relative w-full overflow-hidden rounded-xl border border-base-200 bg-base-100 shadow-sm "
                      (or class ""))}
      ($ :div {:class "absolute inset-0 bg-gradient-to-r from-primary/10 via-accent/5 to-transparent opacity-90"})
      ($ :div {:class "relative flex flex-col gap-4 px-6 py-5 select-none"}
        ($ :div {:class "flex items-start justify-between gap-4"}
          ($ :div {:class "flex items-start gap-4"}
            (when icon-node
              ($ :div {:class (str "flex h-12 w-12 items-center justify-center rounded-2xl bg-white shadow-md ring-1 ring-primary/20 "
                                (or icon-bg ""))}
                icon-node))
            ($ :div {:class "space-y-1"}
              (when eyebrow
                ($ :span {:class "text-xs font-semibold uppercase tracking-wide text-primary/80"}
                  eyebrow))
              ($ :h2 {:class "text-xl font-semibold text-base-content"}
                title)
              (when subtitle
                (cond
                  (display/react-element? subtitle) subtitle
                  (vector? subtitle) subtitle
                  :else ($ :p {:class "text-sm text-base-content/70"}
                          subtitle)))))
          (when (seq normalized-right)
            ($ :div {:class "flex flex-wrap items-center justify-end gap-2"}
              (map-indexed
                (fn [idx item]
                  (when item
                    ($ :div {:key idx :class "flex items-center"}
                      (cond
                        (display/react-element? item) item
                        (vector? item) item
                        :else ($ :span {:class "text-sm text-base-content/80"} item)))))
                normalized-right))))
        (when (seq meta-items)
          ($ :div {:class "flex flex-wrap gap-3"}
            (map-indexed
              (fn [idx {:keys [label value value-class]}]
                ($ :div {:key idx
                         :class "flex items-center gap-2 rounded-full border border-base-200 bg-base-100/80 px-3 py-1 text-xs text-base-content/70 shadow-sm"}
                  (when label
                    ($ :span {:class "font-semibold text-base-content/80"}
                      label))
                  (cond
                    (display/react-element? value) value
                    (vector? value) value
                    :else ($ :span {:class (str "text-base-content/70 " (or value-class ""))}
                            value))))
              meta-items)))))))

(defui detail-field
  "Display a detail field with label and formatted value."
  [props]
  (let [{:keys [label value hint]} props
        formatted (display/format-value value "—" true)]
    ($ :div {:class "flex justify-between items-center py-3 border-b border-base-200 last:border-b-0"}
      ($ :span {:class "text-sm font-medium text-base-content/70 min-w-[120px]"}
        label)
      (cond
        (display/react-element? formatted)
        ($ :div {:class "text-base ml-4 flex-1"} formatted)

        (vector? formatted)
        ($ :div {:class "text-base ml-4 flex-1"} formatted)

        :else
        ($ :span {:class "text-base font-medium text-base-content ml-4 flex-1 text-right"}
          formatted))
      (when hint
        ($ :div {:class "col-span-2 text-xs text-base-content/50 italic mt-1"} hint)))))

(defui detail-card
  "Create a card containing multiple detail fields."
  [props]
  (let [{:keys [title fields options]} props
        {:keys [class title-class]} (or options {})]
    ($ :div {:class (str "ds-card ds-card-bordered bg-base-100 shadow-lg p-4 " (or class ""))}
      ($ :div {:class "flex items-center gap-2 mb-3"}
        ($ :div {:class "w-1 h-4 rounded-full bg-primary"})
        ($ :h3 {:class (str "text-base font-semibold text-base-content " (or title-class ""))}
          title))
      ($ :div {:class "space-y-3"}
        (map-indexed
          (fn [idx field]
            ($ detail-field (assoc field :key idx)))
          fields)))))

(defn ip-address-badge
  "Create a formatted IP address badge."
  [ip]
  (if (and ip (not= ip "Unknown") (not= ip "N/A"))
    ($ :span {:class "ds-badge ds-badge-outline ds-badge-sm font-mono"} ip)
    ($ :span {:class "text-base-content/50 italic"} "Unknown")))

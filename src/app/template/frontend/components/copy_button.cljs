(ns app.template.frontend.components.copy-button
  "Reusable copy to clipboard button component"
  (:require
    [uix.core :refer [$ defui use-state]]))

(defui copy-to-clipboard-button
  "Reusable copy to clipboard button with visual feedback.

   Args:
     props: Map with keys:
       :text - The text to copy to clipboard (required)
       :class - Additional CSS classes (default: ds-btn ds-btn-xs ds-btn-ghost ds-btn-circle absolute top-2 right-2 opacity-60 hover:opacity-100 transition-opacity)
       :title - Tooltip text (default: \"Copy to clipboard\")
       :size - Icon size (default: \"w-3 h-3\")
       :success-duration - Duration in ms to show success state (default: 2000)
       :on-success - Optional callback function called when copy succeeds
       :on-click - Optional additional click handler"
  [{:keys [text class title size success-duration on-success on-click]
    :or {class "ds-btn ds-btn-xs ds-btn-ghost ds-btn-circle absolute top-2 right-2 opacity-60 hover:opacity-100 transition-opacity"
         title "Copy to clipboard"
         size "w-3 h-3"
         success-duration 2000}}]
  (let [[copied? set-copied!] (use-state false)]
    ($ :button
      {:class class
       :title title
       :on-click (fn [e]
                   (.stopPropagation e)
                   (.writeText js/navigator.clipboard text)
                   (set-copied! true)
                   (when on-success (on-success text))
                   (when on-click (on-click e))
                   (js/setTimeout #(set-copied! false) success-duration))}
      (if copied?
        ($ :svg {:class (str size " text-green-600") :fill "none" :stroke "currentColor" :view-box "0 0 24 24"}
          ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M5 13l4 4L19 7"}))
        ($ :svg {:class size :fill "none" :stroke "currentColor" :view-box "0 0 24 24"}
          ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                    :d "M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"}))))))

(defui copy-to-clipboard-inline-button
  "Inline copy button with text label, suitable for use in forms and tables.

   Args:
     props: Map with keys:
       :text - The text to copy to clipboard (required)
       :label - Button label (default: \"Copy\")
       :class - Additional CSS classes
       :size - Button size (default: ds-btn-xs)
       :success-text - Text to show when copied (default: \"Copied!\")"
  [{:keys [text label class size success-text]
    :or {label "Copy"
         class ""
         size "ds-btn-xs"
         success-text "Copied!"}}]
  (let [[copied? set-copied!] (use-state false)]
    ($ :button
      {:class (str "ds-btn " size " " class)
       :on-click (fn [e]
                   (.stopPropagation e)
                   (.writeText js/navigator.clipboard text)
                   (set-copied! true)
                   (js/setTimeout #(set-copied! false) 2000))}
      (if copied?
        success-text
        label))))

(comment
  ;; Usage examples:

  ;; Basic floating copy button (like in audit modal)
  ($ copy-to-clipboard-button {:text "Some text to copy"})

  ;; Custom styling
  ($ copy-to-clipboard-button
    {:text "Custom text"
     :class "ds-btn ds-btn-sm ds-btn-primary"
     :size "w-4 h-4"
     :title "Copy this content"})

  ;; Inline copy button with label
  ($ copy-to-clipboard-inline-button
    {:text "Some text"
     :label "Copy ID"
     :class "ds-btn-outline"}))

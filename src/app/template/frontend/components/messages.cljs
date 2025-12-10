(ns app.template.frontend.components.messages
  (:require
   [app.template.frontend.components.button :refer [button]]
   [app.template.frontend.components.icons :refer [delete-icon]]
   [app.template.frontend.events.form :as form-events]
   [app.template.frontend.events.list.crud :as crud-events]
   [re-frame.core :as rf]
   [uix.core :refer [$ defui]]))

(defui format-error
  {:prop-types {:error {:type [:string :map]}}}
  [{:keys [error]}]
  ($ :div
    (cond
      (string? error) error
      (map? error) (let [{:keys [message details]} error]
                     ($ :div
                       ($ :div
                         ($ :span {:class "font-bold"} message))
                       (when details
                         ($ :div {:class "mt-2"}
                           ($ :div {:class "mt-1"}
                             ($ :div
                               ($ :span {:class "font-bold"} "Message: ")
                               ($ :span (:message details)))
                             (when (:type details) ($ :div
                                                     ($ :span {:class "font-bold"} "Type: ")
                                                     ($ :span (name (:type details)))))
                             (when (:cause details) ($ :div
                                                      ($ :span {:class "font-bold"} "Cause: ")
                                                      ($ :span (str (:cause details)))))
                             (when (:details details) ($ :div
                                                        ($ :span {:class "font-bold"} "Details: ")
                                                        ($ :span (pr-str (:details details))))))))))

      :else (str error))))

(defui error-alert
  {:prop-types {:error {:type [:string :map :nil]}
                :entity-name {:type :string}}}
  [{:keys [error entity-name]}]
  (when error
    ($ :div {:class "ds-alert ds-alert-error ds-alert-soft mb-4"
             :id "error-alert-component"}
      ($ :div {:class "flex items-start"}
        ($ :div {:class "flex-shrink-0"}
          ($ :svg {:class "h-5 w-5 text-red-400"
                   :xmlns "http://www.w3.org/2000/svg"
                   :viewBox "0 0 20 20"
                   :fill "currentColor"}
            ($ :path {:fill-rule "evenodd"
                      :d "M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                      :clip-rule "evenodd"})))
        ($ :div {:class "ml-3 flex-1"}
          ($ :div {:class "text-sm font-bold text-red-800"}
            "Error")
          ($ :div {:class "mt-1 text-sm text-red-700"}
            ($ format-error {:error error})))
        ($ :div {:class "ml-4 flex-shrink-0 flex"}
          ($ button
            {:btn-type :ghost
             :class "!p-1 !min-h-0 !h-auto"
             :on-click #(do
                          (rf/dispatch [::crud-events/clear-error (keyword entity-name)])
                          (rf/dispatch [::form-events/clear-form-errors (keyword entity-name)]))}
            ($ :span {:class "sr-only"} "Close")
            ($ delete-icon)))))))

(defui success-alert
  {:prop-types {:message {:type :string}
                :entity-name {:type :string}}}
  [{:keys [message]}]
  (when message
    ($ :div {:class "ds-alert ds-alert-success  ds-alert-soft mb-4"
             :id "success-alert-component"}
      ($ :div {:class "flex items-start"}
        ($ :div {:class "flex-shrink-0"}
          ($ :svg {:class "h-5 w-5 text-green-400"
                   :xmlns "http://www.w3.org/2000/svg"
                   :viewBox "0 0 20 20"
                   :fill "currentColor"}
            ($ :path {:fill-rule "evenodd"
                      :d "M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                      :clip-rule "evenodd"})))
        ($ :div {:class "ml-3 flex-1"}
          ($ :div {:class "text-sm font-bold text-green-800"}
            "Success")
          ($ :div {:class "mt-1 text-sm text-green-700"}
            message))))))

(ns app.template.frontend.components.confirm-dialog
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.modal :refer [modal]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

;; Define event handlers for the confirm dialog
(rf/reg-event-db
  ::open-confirm-dialog
  (fn [db [_ props]]
    (assoc-in db [:ui :confirm-dialog] (merge {:open? true} props))))

(rf/reg-event-db
  ::close-confirm-dialog
  (fn [db _]
    (assoc-in db [:ui :confirm-dialog :open?] false)))

;; Define subscription for confirm dialog state
(rf/reg-sub
  ::confirm-dialog-state
  (fn [db]
    (get-in db [:ui :confirm-dialog] {:open? false})))

;; Helper function to show confirm dialog
(defn show-confirm
  "Show a confirmation dialog

  Options:
  - title: Title for the dialog (optional)
  - message: Message to display (required)
  - on-confirm: Function to call when confirmed (required)
  - on-cancel: Function to call when cancelled (optional)
  - confirm-text: Text for confirm button (default: 'Confirm')
  - cancel-text: Text for cancel button (default: 'Cancel')"
  [{:keys [title message on-confirm on-cancel confirm-text cancel-text]
    :or {title "Confirm Action"
         confirm-text "Confirm"
         cancel-text "Cancel"}}]
  (rf/dispatch [::open-confirm-dialog
                {:title title
                 :message message
                 :confirm-text confirm-text
                 :cancel-text cancel-text
                 :on-confirm on-confirm
                 :on-cancel on-cancel}]))

(defui confirm-dialog
  "Confirmation dialog component that renders when opened via the show-confirm function"
  []
  (let [dialog-state (use-subscribe [::confirm-dialog-state])
        ;; Handle both cases: if dialog-state is already a map or if it's a reference
        state-value (if (satisfies? cljs.core/IDeref dialog-state)
                      @dialog-state
                      dialog-state)
        {:keys [open? message title confirm-text cancel-text on-confirm on-cancel]} state-value

        handle-confirm (fn []
                         (rf/dispatch [::close-confirm-dialog])
                         (when on-confirm (on-confirm)))

        handle-cancel (fn []
                        (rf/dispatch [::close-confirm-dialog])
                        (when on-cancel (on-cancel)))]

    (when open?
      ($ modal
        {:id "confirm-dialog"
         :on-close handle-cancel
         :draggable? false
         :width "450px"
         :class "shadow-2xl"
         :header ($ :div {:class "w-full text-lg font-semibold text-center py-2 border-b border-gray-200"}
                   (or title "Confirm Action"))}

        ;; Content
        ($ :div {:class "p-6"}
          ;; Message
          ($ :div {:class "mb-8 text-gray-700 text-center text-lg"}
            message)

          ;; Buttons
          ($ :div {:class "flex justify-center gap-4"}
            ($ button
              {:id "confirm-dialog-cancel"
               :btn-type :cancel
               :class "px-6 py-2 rounded-lg hover:bg-gray-200 transition-colors duration-200"
               :on-click handle-cancel}
              (or cancel-text "Cancel"))

            ($ button
              {:id "confirm-dialog-confirm"
               :btn-type :primary
               :class "px-6 py-2 rounded-lg hover:opacity-90 transition-colors duration-200 font-medium"
               :on-click handle-confirm}
              (or confirm-text "Confirm"))))))))

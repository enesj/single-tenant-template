(ns app.domain.frontend.expenses.pages.user.receipt-detail
  "Receipt detail route: renders the receipts list + opens the detail modal.

  This keeps /receipts and /receipts/:receipt-id UX aligned with /admin/receipts."
  (:require
    [app.domain.frontend.expenses.pages.user.receipts-list :refer [receipts-list-page]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui receipt-detail-page []
  (let [current-route (use-subscribe [:current-route])
        receipt-id (or (get-in current-route [:path-params :receipt-id])
                     (get-in current-route [:parameters :path :receipt-id]))
        modal-open? (boolean (use-subscribe [:user-expenses/receipt-detail-modal-open?]))
        [opened-once? set-opened-once!] (use-state false)]

    (use-effect
      (fn []
        (when receipt-id
          ;; Ensure list page data is present for deep links.
          (rf/dispatch [:user-expenses/fetch-receipts {:limit 50 :offset 0}])
          (rf/dispatch [:user-expenses/open-receipt-detail-modal receipt-id])
          (set-opened-once! true))
        js/undefined)
      [receipt-id])

    (use-effect
      (fn []
        (when (and opened-once?
                receipt-id
                (not modal-open?))
          (rf/dispatch [:navigate-to "/receipts"]))
        js/undefined)
      [opened-once? receipt-id modal-open?])

    ($ receipts-list-page)))


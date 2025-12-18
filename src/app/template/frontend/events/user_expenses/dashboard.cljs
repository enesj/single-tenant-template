(ns app.template.frontend.events.user-expenses.dashboard
  (:require
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]))

;; ---------------------------------------------------------------------------
;; Dashboard bootstrap
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/init-dashboard
  common-interceptors
  (fn [{:keys [_db]} _]
    {:dispatch-n [[:user-expenses/fetch-summary]
                  [:user-expenses/fetch-by-month {:months-back 6}]
                  [:user-expenses/fetch-recent {:limit 5}]]}))


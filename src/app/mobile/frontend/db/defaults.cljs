(ns app.mobile.frontend.db.defaults
  "Initial re-frame app-db state for the mobile app.")

(def default-db
  {:locale :bs
   :current-route nil
   :session {:loading? true
             :authenticated? false}
   :mobile {:active-tab :dashboard
            :offline-queue []
            :sync-status :idle
            :pending-review-count 0}})

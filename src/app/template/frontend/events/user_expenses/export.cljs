(ns app.template.frontend.events.user-expenses.export
  (:require
    [app.template.frontend.db.db :refer [common-interceptors]]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Export (placeholder)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/export
  common-interceptors
  (fn [_cofx [opts]]
    (log/info "Export requested" opts)
    ;; TODO: Implement actual export
    {}))

;; ---------------------------------------------------------------------------
;; Delete all (placeholder)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/delete-all
  common-interceptors
  (fn [_cofx _]
    (log/info "Delete all requested")
    ;; TODO: Implement actual delete all
    {}))


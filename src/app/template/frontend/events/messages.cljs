(ns app.template.frontend.events.messages
  "Simple message/notification events used by template frontend.

   These handlers exist primarily to avoid missing-handler warnings and to
   provide a single place to record the last user-facing message in app-db.

   UI components can subscribe to [:ui :last-message] if they want to render
   toasts or banners."
  (:require
    [re-frame.core :as rf]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [taoensso.timbre :as log]))

(rf/reg-event-db
  :app.template.frontend.events.messages/show-success
  common-interceptors
  ;; NOTE: common-interceptors includes trim-v, so the event vector here is [title message]
  (fn [db [title message]]
    (log/info "UI success message:" {:title title :message message})
    (assoc-in db [:ui :last-message] {:type :success
                                      :title title
                                      :message message})))

(rf/reg-event-db
  :app.template.frontend.events.messages/show-error
  common-interceptors
  ;; NOTE: common-interceptors includes trim-v, so the event vector here is [title message]
  (fn [db [title message]]
    (log/warn "UI error message:" {:title title :message message})
    (assoc-in db [:ui :last-message] {:type :error
                                      :title title
                                      :message message})))

;; ---------------------------------------------------------------------------
;; Generic toast events
;; ---------------------------------------------------------------------------

(rf/reg-event-db
  :toast
  common-interceptors
  (fn [db [toast]]
    ;; Domain code often dispatches: [:toast {:type :success :message "..."}]
    ;; This handler exists primarily to avoid missing-handler console errors.
    ;; UI layers may render :ui/:toasts using template notification components.
        (let [{:keys [type title message] :as t} (or toast {})
          toast' (merge {:id (random-uuid)
                         :type (or type :info)
                         :message (or message "")}
                   (select-keys t [:title :duration :position]))]
      (-> db
        (update-in [:ui :toasts] (fnil conj []) toast')
        (assoc-in [:ui :last-message] {:type (:type toast')
                                       :title title
                                       :message (:message toast')})))))

(rf/reg-event-db
  :toast/dismiss
  common-interceptors
  (fn [db [toast-id]]
    (update-in db [:ui :toasts]
      (fn [xs]
        (->> (or xs [])
          (remove (fn [t] (= (:id t) toast-id)))
          vec)))))


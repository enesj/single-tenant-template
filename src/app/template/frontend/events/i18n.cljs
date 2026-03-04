(ns app.template.frontend.events.i18n
  (:require
    [re-frame.core :as rf]))

(rf/reg-event-db
  ::set-locale
  (fn [db [_ locale]]
    (assoc db :locale locale)))

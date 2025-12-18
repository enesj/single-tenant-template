(ns app.template.frontend.db.interceptors
  (:require
    [app.template.frontend.db.flags :as flags]
    [app.template.frontend.db.validation :as validation]
    [re-frame.core :as re-frame]))

(def check-spec-interceptor
  (re-frame/->interceptor
    :id :check-spec
    :after (fn [context]
             (let [db (get-in context [:effects :db])]
               (when (and (flags/validation-enabled?) db)
                 (let [event (get-in context [:coeffects :event])
                       event-id (when (vector? event) (first event))
                       models-data (:models-data db)]
                   (when (validation/should-validate-event? models-data event-id)
                     (try
                       (validation/validate-db db models-data)
                       (catch :default exception
                         (let [strict? (flags/strict-validation-enabled?)
                               initialization-event? (validation/initialization-event? event-id)]
                           (validation/log-validation-error! strict? event exception)
                           (when (and strict? (not initialization-event?))
                             (throw exception))))))))
               context))))

(def common-interceptors
  (cond-> [re-frame/trim-v]
    ^boolean goog.DEBUG (conj re-frame/debug)
    (flags/validation-enabled?) (conj check-spec-interceptor)))

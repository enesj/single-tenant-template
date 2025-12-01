(ns app.shared.events.core
  (:require [re-frame.core :as rf]))

(defn success-handler [_db event-vec response]
  (rf/dispatch (conj event-vec response)))

(defn error-handler [_db event-vec error]
  (rf/dispatch (conj event-vec error)))

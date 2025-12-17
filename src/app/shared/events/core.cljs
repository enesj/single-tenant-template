(ns app.shared.events.core
  "Generic re-frame event handlers.
   
   NOTE: Reader-discarded - handlers not used. Project uses http interceptors
   for success/error handling instead of these generic dispatchers."
  #_(:require [re-frame.core :as rf]))

#_(defn success-handler [_db event-vec response]
    (rf/dispatch (conj event-vec response)))

#_(defn error-handler [_db event-vec error]
    (rf/dispatch (conj event-vec error)))

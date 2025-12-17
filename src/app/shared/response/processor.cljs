(ns app.shared.response.processor
  "API response processing utilities.
   
   NOTE: Reader-discarded - function not in use. If needed, consider using
   camel-snake-kebab.extras/transform-keys with csk/->kebab-case-keyword."
  #_(:require [camel-snake-kebab.core :as csk]
              [camel-snake-kebab.extras :as cske]))

#_(defn process-api-response
    "Transform response keys to kebab-case.
     NOTE: Not in use - requires camel-snake-kebab.extras for map key transformation."
    [response]
    (cske/transform-keys csk/->kebab-case-keyword response))

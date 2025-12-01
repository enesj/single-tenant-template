(ns app.shared.response.processor
  (:require [camel-snake-kebab.core :as csk]))

(defn process-api-response [response]
  (-> response
    (csk/->kebab-case-keys)))

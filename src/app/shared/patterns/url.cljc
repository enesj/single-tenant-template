(ns app.shared.patterns.url
  (:require [app.shared.patterns.common :as common]))

(def url-pattern
  "URL validation for http, https, and ftp protocols"
  #"^(https?|ftp)://[^\s/$.?#].[^\s]*$")

(def http-url-pattern
  "HTTP/HTTPS URL validation"
  #"^https?://[^\s/$.?#].[^\s]*$")

(defn valid-url?
  "Validate URL format"
  [url]
  (common/matches-pattern? url-pattern url))

(defn valid-http-url?
  "Validate HTTP/HTTPS URL format"
  [url]
  (common/matches-pattern? http-url-pattern url))

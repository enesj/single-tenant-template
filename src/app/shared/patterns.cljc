(ns app.shared.patterns
  "Shared regex/patterns; extend for validation needs."
  (:require
    [app.shared.patterns.date-time :as date-time]
    [app.shared.patterns.email :as email]
    [app.shared.patterns.phone :as phone]
    [app.shared.patterns.slug :as slug]
    [app.shared.patterns.url :as url]))

;; Re-export Email Patterns
(def valid-email? email/valid-email?)

;; Re-export Date and Time Patterns
(def valid-iso-date? date-time/valid-iso-date?)
(def valid-us-date? date-time/valid-us-date?)

;; Re-export URL Patterns
(def valid-url? url/valid-url?)
(def valid-http-url? url/valid-http-url?)

;; Re-export Slug Patterns
(def valid-slug? slug/valid-slug?)

;; Re-export Phone Patterns
(def valid-phone-e164? phone/valid-phone-e164?)
(def valid-phone? phone/valid-phone?)

(ns app.shared.patterns
  "Cross-platform regex patterns for the hosting application.
   Provides a consolidated entry point for domain-specific patterns."
  (:require
    [app.shared.patterns.auth :as auth]
    [app.shared.patterns.common :as common]
    [app.shared.patterns.date-time :as date-time]
    [app.shared.patterns.email :as email]
    [app.shared.patterns.phone :as phone]
    [app.shared.patterns.slug :as slug]
    [app.shared.patterns.url :as url]))

;; Re-export core validation utility
(def matches-pattern? common/matches-pattern?)

;; Re-export Email Patterns
(def email-pattern email/email-pattern)
(def email-simple-pattern email/email-simple-pattern)
(def valid-email? email/valid-email?)
(def valid-email-simple? email/valid-email-simple?)

;; Re-export Date and Time Patterns
(def iso-date-pattern date-time/iso-date-pattern)
(def iso-datetime-pattern date-time/iso-datetime-pattern)
(def us-date-pattern date-time/us-date-pattern)
(def valid-iso-date? date-time/valid-iso-date?)
(def valid-us-date? date-time/valid-us-date?)

;; Re-export URL Patterns
(def url-pattern url/url-pattern)
(def http-url-pattern url/http-url-pattern)
(def valid-url? url/valid-url?)
(def valid-http-url? url/valid-http-url?)

;; Re-export Slug Patterns
(def slug-pattern slug/slug-pattern)
(def valid-slug? slug/valid-slug?)

;; Re-export Phone Patterns
(def phone-e164-pattern phone/phone-e164-pattern)
(def phone-loose-pattern phone/phone-loose-pattern)
(def valid-phone-e164? phone/valid-phone-e164?)
(def valid-phone? phone/valid-phone?)

;; Re-export Auth Patterns
(def username-pattern auth/username-pattern)
(def password-strong-pattern auth/password-strong-pattern)
(def valid-username? auth/valid-username?)
(def valid-password-strong? auth/valid-password-strong?)

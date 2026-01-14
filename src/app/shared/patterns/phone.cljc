(ns app.shared.patterns.phone
  "Phone number patterns.

  Notes:
  - This is intentionally *lightweight* and permissive; it is meant to prevent
    obviously-invalid input, not to be a complete international phone library.
  - Prefer storing phone numbers normalized to E.164 where possible."
  (:require
    [app.shared.patterns.common :as common]))

(def phone-e164-pattern
  "E.164-ish phone number pattern.

  Accepts optional leading + and 7-15 digits (first digit non-zero)."
  #"^\+?[1-9]\d{6,14}$")

(def phone-loose-pattern
  "Loose phone number pattern for UI forms.

  Allows digits and common separators. Requires at least 7 characters overall.
  This avoids rejecting formats like (555) 123-4567 while still filtering out
  obviously-invalid strings." 
  #"^[0-9+\-\s().]{7,}$")

(defn valid-phone-e164?
  "True when `phone` matches `phone-e164-pattern`." 
  [phone]
  (common/matches-pattern? phone-e164-pattern phone))

(defn valid-phone?
  "True when `phone` matches a permissive phone pattern.

  This uses `phone-loose-pattern` (not strict E.164) to reduce false negatives
  in UI forms." 
  [phone]
  (common/matches-pattern? phone-loose-pattern phone))

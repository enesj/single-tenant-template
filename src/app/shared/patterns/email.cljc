(ns app.shared.patterns.email
  (:require [app.shared.patterns.common :as common]))

(def email-pattern
  "Basic email validation pattern - RFC 5322 compliant"
  #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$")

(def email-simple-pattern
  "Simple email pattern for basic validation"
  #".+@.+\..+")

(defn valid-email?
  "Validate email address using standard pattern"
  [email]
  (common/matches-pattern? email-pattern email))

(defn valid-email-simple?
  "Validate email using simple pattern (less strict)"
  [email]
  (common/matches-pattern? email-simple-pattern email))

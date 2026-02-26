(ns app.shared.patterns.auth
  (:require [app.shared.patterns.common :as common]))

(def username-pattern
  "Username pattern (letters, numbers, underscores, hyphens)"
  #"^[a-zA-Z0-9_-]+$")

(def password-strong-pattern
  "Strong password pattern (8+ chars, uppercase, lowercase, number, special char)"
  #"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$")

^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn valid-username?
  "Validate username format"
  [username]
  (common/matches-pattern? username-pattern username))

^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn valid-password-strong?
  "Validate strong password pattern"
  [password]
  (common/matches-pattern? password-strong-pattern password))

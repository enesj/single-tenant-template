(ns app.shared.patterns.date-time
  (:require [app.shared.patterns.common :as common]))

(def iso-date-pattern
  "ISO 8601 date format (YYYY-MM-DD)"
  #"^\d{4}-\d{2}-\d{2}$")

^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(def iso-datetime-pattern
  "ISO 8601 datetime format with optional timezone"
  #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:\d{2})$")

(def us-date-pattern
  "US date format (MM/DD/YYYY)"
  #"^\d{1,2}/\d{1,2}/\d{4}$")

(defn valid-iso-date?
  "Validate ISO date format (YYYY-MM-DD)"
  [date-str]
  (common/matches-pattern? iso-date-pattern date-str))

(defn valid-us-date?
  "Validate US date format (MM/DD/YYYY)"
  [date-str]
  (common/matches-pattern? us-date-pattern date-str))

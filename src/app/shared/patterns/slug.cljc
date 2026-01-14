(ns app.shared.patterns.slug
  "Slug/kebab-case patterns used by both backend validation and admin UI forms."
  (:require
    [app.shared.patterns.common :as common]))

(def slug-pattern
  "Kebab-case slug pattern (lowercase alnum segments separated by single dashes)."
  #"^[a-z0-9]+(?:-[a-z0-9]+)*$")

(defn valid-slug?
  "True when `slug` matches `slug-pattern`."
  [slug]
  (common/matches-pattern? slug-pattern slug))

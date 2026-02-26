(ns app.domain.frontend.expenses.subs.article-aliases
  (:require
    [app.domain.frontend.expenses.subs.related-records-factory :as rr-subs]))

(def ^:private base-path [:admin :expenses :article-aliases])

;; Related records modal subs
(rr-subs/register-related-records-subs!
  {:entity-singular "article-alias"
   :base-path base-path})

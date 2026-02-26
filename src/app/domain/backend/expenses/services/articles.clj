(ns app.domain.backend.expenses.services.articles
  "Compatibility facade — delegates to articles sub-modules.

  New code should require app.domain.backend.expenses.services.articles.service."
  (:require
    [app.domain.backend.expenses.services.articles.service :as service]))

(def normalize-article-key service/normalize-article-key)
(def normalize-alias-label service/normalize-alias-label)

(def create-article! service/create-article!)

(def find-or-create-article-by-canonical-name! service/find-or-create-article-by-canonical-name!)

(def list-articles service/list-articles)
(def update-article! service/update-article!)
(def delete-article! service/delete-article!)
(def count-articles service/count-articles)

(def list-related-records service/list-related-records)

(def find-article-by-alias service/find-article-by-alias)
(def create-alias! service/create-alias!)
(def batch-create-aliases! service/batch-create-aliases!)

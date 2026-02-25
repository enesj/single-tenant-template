(ns app.domain.backend.expenses.services.articles.service
  "Aggregated article service API.

  Preferred require point for new code; articles.clj delegates here for
  backward compatibility."
  (:require
    [app.domain.backend.expenses.services.articles.aliases :as aliases]
    [app.domain.backend.expenses.services.articles.crud :as crud]
    [app.domain.backend.expenses.services.articles.normalization :as normalization]
    [app.domain.backend.expenses.services.articles.related-records :as related-records]))

;; Normalization
(def normalize-article-key normalization/normalize-article-key)
(def normalize-alias-label normalization/normalize-alias-label)

;; CRUD
(def create-article! crud/create-article!)
(def get-article-by-normalized-key crud/get-article-by-normalized-key)
(def find-or-create-article-by-canonical-name! crud/find-or-create-article-by-canonical-name!)
(def get-article crud/get-article)
(def list-articles crud/list-articles)
(def update-article! crud/update-article!)
(def delete-article! crud/delete-article!)
(def count-articles crud/count-articles)
(def search-articles crud/search-articles)

;; Related records
(def list-related-records related-records/list-related-records)

;; Aliases
(def find-article-by-alias aliases/find-article-by-alias)
(def create-alias! aliases/create-alias!)
(def batch-create-aliases! aliases/batch-create-aliases!)

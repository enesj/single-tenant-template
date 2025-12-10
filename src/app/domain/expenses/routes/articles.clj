(ns app.domain.expenses.routes.articles
  "Admin API routes for articles, aliases, and price history."
  (:require
    [app.domain.expenses.routes.routes-factory :as factory]
    [app.domain.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/article-config
                    (factory/register-entity-routes!))]
    (factory/build-standard-routes db config)))
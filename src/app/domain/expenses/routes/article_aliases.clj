(ns app.domain.expenses.routes.article-aliases
  "Admin API routes for article aliases."
  (:require
    [app.domain.expenses.routes.routes-factory :as factory]
    [app.domain.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/article-alias-config
                    (factory/register-entity-routes!))]
    (factory/build-standard-routes db config)))
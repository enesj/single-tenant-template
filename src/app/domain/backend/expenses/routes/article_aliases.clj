(ns app.domain.backend.expenses.routes.article-aliases
  "Admin API routes for article aliases."
  (:require
    [app.domain.backend.expenses.routes.routes-factory :as factory]
    [app.domain.backend.expenses.routes.route-configs :as configs]))

(defn routes [db]
  (let [config (-> configs/article-alias-config
                    (factory/register-entity-routes!))]
    (factory/build-standard-routes db config)))
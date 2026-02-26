(ns app.domain.frontend.expenses.events.article-alias-bulk
  "Bulk alias creation for a single article (admin UX)."
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.utils.http :as admin-http]
    [app.domain.frontend.expenses.events.article-aliases :as aliases-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :articles :add-aliases-modal])

(defn- set-error [db msg]
  (assoc-in db (conj base-path :error) msg))

(defn- set-working [db working?]
  (assoc-in db (conj base-path :working?) (boolean working?)))



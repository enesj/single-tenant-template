(ns app.domain.frontend.expenses.admin.components.detail-views.article
  "Article detail view component and add-aliases modal."
  (:require
    [app.domain.frontend.expenses.admin.components.detail-views.utils :as utils]
    [app.domain.frontend.expenses.events.article-alias-bulk :as alias-bulk-events]
    [app.domain.frontend.expenses.subs.article-alias-bulk :as alias-bulk-subs]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))





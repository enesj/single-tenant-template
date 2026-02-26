(ns app.domain.frontend.expenses.components.form-fields.selects
  "Select input components for expense form (supplier, article, expense)"
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers :as h]
    [app.domain.frontend.expenses.events.articles :as articles-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [app.domain.frontend.expenses.ui.select-options :as select-options]

    [app.template.frontend.components.form.fields.select :refer [select-input]]

    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

^{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}

(defui supplier-select-input
  [{:keys [id label error required inline class on-change value form-id formId]}]
  (let [suppliers (use-subscribe [:expenses/suppliers])
        form-id* (or form-id formId)
        field-id (or id (when form-id* (str form-id* "-select")))
        options (h/options-from-items suppliers select-options/supplier-label)]
    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load-list {:limit 200 :offset 0}])
        js/undefined)
      [])
    ($ select-input
      {:id field-id
       :formId form-id*
       :label label
       :error error
       :required required
       :inline inline
       :class class
       :value value
       :options options
       :on-change on-change})))

(defui article-select-input
  [{:keys [id label error required inline class on-change value form-id formId]}]
  (let [articles (use-subscribe [:expenses/articles])
        form-id* (or form-id formId)
        field-id (or id (when form-id* (str form-id* "-select")))
        options (h/options-from-items articles select-options/article-label)]
    (use-effect
      (fn []
        (rf/dispatch [::articles-events/load-list {:limit 200 :offset 0}])
        js/undefined)
      [])
    ($ select-input
      {:id field-id
       :formId form-id*
       :label label
       :error error
       :required required
       :inline inline
       :class class
       :value value
       :options options
       :on-change on-change})))

(defui expense-select-input
  [{:keys [id label error required inline class on-change value form-id formId]}]
  (let [expenses (use-subscribe [:expenses/expenses])
        form-id* (or form-id formId)
        field-id (or id (when form-id* (str form-id* "-select")))
        options (h/options-from-items expenses select-options/expense-label)]
    (use-effect
      (fn []
        (rf/dispatch [:app.domain.frontend.expenses.events.expenses/load-list {:limit 200 :offset 0}])
        js/undefined)
      [])
    ($ select-input
      {:id field-id
       :formId form-id*
       :label label
       :error error
       :required required
       :inline inline
       :class class
       :value value
       :options options
       :on-change on-change})))
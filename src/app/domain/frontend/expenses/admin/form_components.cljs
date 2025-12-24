(ns app.domain.frontend.expenses.admin.form-components
  "Registers domain-specific admin form field components for expenses entities."
  (:require
    [app.admin.frontend.specs.form-components :as form-components]
    [app.domain.frontend.expenses.components.form-fields :as form-fields]))

(defn- register-expenses-form-components!
  []
  (form-components/register-form-field-component!
    {:entity-key :article-aliases
     :field-key :supplier-id
     :component form-fields/supplier-select-input})
  (form-components/register-form-field-component!
    {:entity-key :article-aliases
     :field-key :article-id
     :component form-fields/article-select-input})
  (form-components/register-form-field-component!
    {:entity-key :price-observations
     :field-key :supplier-id
     :component form-fields/supplier-select-input})
  (form-components/register-form-field-component!
    {:entity-key :price-observations
     :field-key :article-id
     :component form-fields/article-select-input}))

(defonce ^:private _register
  (register-expenses-form-components!))

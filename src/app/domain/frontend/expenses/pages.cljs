(ns app.domain.frontend.expenses.pages
  "Domain pages aggregator - provides page component mapping for routes.

  This namespace centralizes the mapping of route :view keywords to
  their corresponding page components, allowing the template to
  dynamically render domain pages without hardcoded imports."
  (:require
    [app.domain.frontend.expenses.pages.user.expenses-dashboard :refer [expenses-dashboard-page]]
    [app.domain.frontend.expenses.pages.user.expenses-list :refer [expenses-list-page]]
    [app.domain.frontend.expenses.pages.user.expense-new :refer [expense-new-page]]
    [app.domain.frontend.expenses.pages.user.expense-detail :refer [expense-detail-page]]
    [app.domain.frontend.expenses.pages.user.expense-upload :refer [expense-upload-page]]
    [app.domain.frontend.expenses.pages.user.expense-reports :refer [expense-reports-page]]
    [app.domain.frontend.expenses.pages.user.expense-settings :refer [expense-settings-page]]
    [app.domain.frontend.expenses.pages.user.suppliers :refer [suppliers-page]]
    [app.domain.frontend.expenses.pages.user.payers :refer [payers-page]]
    [app.domain.frontend.expenses.pages.user.expense-items :refer [expense-items-page]]
    [app.domain.frontend.expenses.pages.user.receipts-list :refer [receipts-list-page]]
    [app.domain.frontend.expenses.pages.user.receipt-detail :refer [receipt-detail-page]]
    [app.domain.frontend.expenses.pages.user.articles :refer [articles-page]]
    [app.domain.frontend.expenses.pages.user.article-aliases :refer [article-aliases-page]]
    [app.domain.frontend.expenses.pages.user.manufacturers :refer [manufacturers-page]]
    [app.domain.frontend.expenses.pages.user.manufacturer-aliases :refer [manufacturer-aliases-page]]
    [app.domain.frontend.expenses.pages.user.supplier-aliases :refer [supplier-aliases-page]]
    [app.domain.frontend.expenses.pages.user.payer-types :refer [payer-types-page]]
    [app.domain.frontend.expenses.pages.user.price-observations :refer [price-observations-page]]
    [app.domain.frontend.expenses.pages.user.unmapped-items :refer [unmapped-items-page]]))

(def pages
  "Map of route :view keywords to page components.

  The keys must match the :view values in the domain routes (user.cljs).
  The template's current-page component uses this map to render domain pages."
  {:expenses-dashboard expenses-dashboard-page
   :expenses-list expenses-list-page
   :expense-upload expense-upload-page
   :receipts-list receipts-list-page
   :expense-new expense-new-page
   :expense-detail expense-detail-page
   :receipt-detail receipt-detail-page
   :expense-reports expense-reports-page
   :expense-settings expense-settings-page
   :expense-suppliers suppliers-page
   :expense-payers payers-page
  :expense-items expense-items-page
   :expense-articles articles-page
  :expense-manufacturers manufacturers-page
   :expense-article-aliases article-aliases-page
  :expense-manufacturer-aliases manufacturer-aliases-page
  :expense-supplier-aliases supplier-aliases-page
   :expense-payer-types payer-types-page
   :expense-price-observations price-observations-page
   :unmapped-items unmapped-items-page})

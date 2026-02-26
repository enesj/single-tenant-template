(ns app.domain.frontend.expenses.admin.adapters.admin-crud.entities)

(def admin-entities
  [{:entity-key :suppliers
    :base-uri "/admin/api/expenses/suppliers"
    :log-prefix "🔧 suppliers-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.suppliers/load-list {}]}
   {:entity-key :expenses
    :base-uri "/admin/api/expenses/entries"
    :log-prefix "💸 expenses-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.expenses/load-list {}]}
   {:entity-key :expense-items
    :base-uri "/admin/api/expenses/expense-items"
    :log-prefix "🧾 expense-items-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.expense-items/load-list {}]}
   {:entity-key :receipts
    :base-uri "/admin/api/expenses/receipts"
    :log-prefix "🧾 receipts-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.receipts/load-list {}]}
   {:entity-key :payers
    :base-uri "/admin/api/expenses/payers"
    :log-prefix "🔧 payers-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.payers/load-list {}]}
   {:entity-key :articles
    :base-uri "/admin/api/expenses/articles"
    :log-prefix "📦 articles-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.articles/load-list {}]}
   {:entity-key :article-aliases
    :base-uri "/admin/api/expenses/article-aliases"
    :log-prefix "🔗 article-aliases-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.article-aliases/load-list {}]}
   {:entity-key :supplier-aliases
    :base-uri "/admin/api/expenses/supplier-aliases"
    :log-prefix "🔁 supplier-aliases-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.supplier-aliases/load-list {}]}
   {:entity-key :stores
    :base-uri "/admin/api/expenses/stores"
    :log-prefix "🏬 stores-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.stores/load-list {}]}
   {:entity-key :store-aliases
    :base-uri "/admin/api/expenses/store-aliases"
    :log-prefix "🏷️ store-aliases-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.store-aliases/load-list {}]}
   {:entity-key :manufacturers
    :base-uri "/admin/api/expenses/manufacturers"
    :log-prefix "🏭 manufacturers-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.manufacturers/load-list {}]}
   {:entity-key :cities
    :base-uri "/admin/api/expenses/cities"
    :log-prefix "🏙️ cities-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.cities/load-list {}]}
   {:entity-key :countries
    :base-uri "/admin/api/expenses/countries"
    :encode-id? true
    :log-prefix "🌍 countries-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.countries/load-list {}]}
   {:entity-key :categories
    :base-uri "/admin/api/expenses/categories"
    :log-prefix "📂 categories-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.categories/load-list {}]}
   {:entity-key :subcategories
    :base-uri "/admin/api/expenses/subcategories"
    :log-prefix "📁 subcategories-request:"
    :on-success-dispatch [:app.domain.frontend.expenses.events.subcategories/load-list {}]}])

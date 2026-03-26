(ns app.template.frontend.i18n.entities)

(def dicts
  {:bs
   {;; Suppliers page
    :suppliers/title                      "Dobavljači"
    :suppliers/subtitle                   "Zajednički katalog dobavljača za vaše domaćinstvo"
    :suppliers/btn-dashboard              "Nadzorna ploča"
    :suppliers/read-only-notice           "Pristup samo za čitanje. Zamolite člana domaćinstva da ažurira dobavljače."
    :suppliers/detail-title               "Detalji dobavljača"
    :suppliers/not-found                  "Dobavljač nije pronađen."
    :suppliers/col-name                   "Ime"
    :suppliers/col-normalized-key         "Normalizovani ključ"
    :suppliers/col-address                "Adresa"
    :suppliers/col-created                "Kreirano"
    :suppliers/col-id                     "ID"
    :suppliers/recent-expenses-title      "Nedavni troškovi"
    :suppliers/recent-expenses-empty      "Još nema troškova za ovog dobavljača."
    :suppliers/aliases-title              "Pravila artikala"
    :suppliers/aliases-empty              "Nema pravila artikala za ovog dobavljača."
    :suppliers/col-purchased              "Kupljeno"
    :suppliers/col-total                  "Ukupno"
    :suppliers/col-payer                  "Platitelj"
    :suppliers/col-status                 "Status"
    :suppliers/col-raw-label              "Originalna oznaka"
    :suppliers/col-article                "Artikal"
    :suppliers/col-normalized             "Normalizovano"
    :suppliers/delete-title               "Obriši dobavljača"
    :suppliers/delete-msg                 "Želite li obrisati ovog dobavljača?"

    ;; Payers page
    :payers/title                         "Platitelji"
    :payers/subtitle                      "Zajednički načini plaćanja za vaše domaćinstvo"
    :payers/btn-dashboard                 "Nadzorna ploča"
    :payers/read-only-notice              "Pristup samo za čitanje. Zamolite člana domaćinstva da ažurira platitelje."
    :payers/delete-title                  "Obriši platitelja"
    :payers/delete-msg                    "Želite li obrisati ovog platitelja?"

    ;; Articles page
    :articles/title                       "Artikli"
    :articles/subtitle                    "Katalog artikala za napredne korisnike (koristi se za mapiranje i pravila)"
    :articles/btn-dashboard               "Nadzorna ploča"
    :articles/btn-unmapped                "Neizmapirana pravila"
    :articles/delete-title                "Obriši artikal"
    :articles/delete-msg                  "Želite li obrisati ovaj artikal?"

    ;; Categories page
    :categories/title                     "Kategorije"
    :categories/subtitle                  "Katalog kategorija za napredne korisnike (koristi se za potkategorije)."
    :categories/btn-dashboard             "Nadzorna ploča"
    :categories/delete-title              "Obriši kategoriju"
    :categories/delete-msg                "Želite li obrisati ovu kategoriju?"

    ;; Stores page
    :stores/title                         "Trgovine"
    :stores/subtitle                      "Katalog trgovina za napredne korisnike (normalizacija i pravila)."
    :stores/btn-dashboard                 "Nadzorna ploča"
    :stores/btn-store-aliases             "Pravila trgovina"
    :stores/delete-title                  "Obriši trgovinu"
    :stores/delete-msg                    "Želite li obrisati ovu trgovinu?"

    ;; Article aliases page
    :article-aliases/title                "Pravila artikala"
    :article-aliases/subtitle             "Katalog pravila za napredne korisnike (mapiranje putem neizmapirani)"
    :article-aliases/btn-dashboard        "Nadzorna ploča"
    :article-aliases/btn-unmapped         "Neizmapirana pravila"
    :article-aliases/delete-title         "Obriši pravilo artikla"
    :article-aliases/delete-msg           "Želite li obrisati ovo pravilo artikla?"

    ;; Subcategories page
    :subcategories/title                  "Potkategorije"
    :subcategories/subtitle               "Katalog potkategorija za napredne korisnike (koristi se za Artikle)."
    :subcategories/btn-dashboard          "Nadzorna ploča"
    :subcategories/delete-title           "Obriši potkategoriju"
    :subcategories/delete-msg             "Želite li obrisati ovu potkategoriju?"

    ;; Manufacturers page
    :manufacturers/title                  "Proizvođači"
    :manufacturers/subtitle               "Katalog proizvođača za napredne korisnike (koristi se za Artikle)."
    :manufacturers/btn-dashboard          "Nadzorna ploča"
    :manufacturers/delete-title           "Obriši proizvođača"
    :manufacturers/delete-msg             "Želite li obrisati ovog proizvođača?"

    ;; Expense categories page
    :expense-categories/title             "Kategorije troškova"
    :expense-categories/subtitle          "Katalog vrsta troškova za napredne korisnike."
    :expense-categories/btn-dashboard     "Nadzorna ploča"
    :expense-categories/delete-title      "Obriši kategoriju troška"
    :expense-categories/delete-msg        "Želite li obrisati ovu kategoriju troška?"

    ;; Cities page
    :cities/title                         "Gradovi"
    :cities/subtitle                      "Katalog gradova (globalni; admin zaključan)."
    :cities/btn-dashboard                 "Nadzorna ploča"
    :cities/delete-title                  "Obriši grad"
    :cities/delete-msg                    "Želite li obrisati ovaj grad?"

    ;; Payer types page
    :payer-types/title                    "Vrste platitelja"
    :payer-types/subtitle                 "Katalog vrsta platitelja za napredne korisnike."
    :payer-types/btn-dashboard            "Nadzorna ploča"
    :payer-types/delete-title             "Obriši vrstu platitelja"
    :payer-types/delete-msg               "Želite li obrisati ovu vrstu platitelja?"

    ;; Supplier aliases page
    :supplier-aliases/title               "Pravila dobavljača"
    :supplier-aliases/subtitle            "Katalog pravila dobavljača za napredne korisnike."
    :supplier-aliases/btn-dashboard       "Nadzorna ploča"
    :supplier-aliases/btn-suppliers       "Dobavljači"
    :supplier-aliases/delete-title        "Obriši pravilo dobavljača"
    :supplier-aliases/delete-msg          "Želite li obrisati ovo pravilo dobavljača?"

    ;; Store aliases page
    :store-aliases/title                  "Pravila trgovina"
    :store-aliases/subtitle               "Katalog pravila trgovina za napredne korisnike."
    :store-aliases/btn-dashboard          "Nadzorna ploča"
    :store-aliases/btn-stores             "Trgovine"
    :store-aliases/delete-title           "Obriši pravilo trgovine"
    :store-aliases/delete-msg             "Želite li obrisati ovo pravilo trgovine?"}

   :en
   {;; Suppliers page
    :suppliers/title                      "Suppliers"
    :suppliers/subtitle                   "Shared supplier catalog for your household"
    :suppliers/btn-dashboard              "Dashboard"
    :suppliers/read-only-notice           "Read-only access. Ask a household member to update suppliers."
    :suppliers/detail-title               "Supplier Details"
    :suppliers/not-found                  "Supplier not found."
    :suppliers/col-name                   "Name"
    :suppliers/col-normalized-key         "Normalized Key"
    :suppliers/col-address                "Address"
    :suppliers/col-created                "Created At"
    :suppliers/col-id                     "ID"
    :suppliers/recent-expenses-title      "Recent Expenses"
    :suppliers/recent-expenses-empty      "No expenses for this supplier yet."
    :suppliers/aliases-title              "Article Aliases"
    :suppliers/aliases-empty              "No article aliases for this supplier."
    :suppliers/col-purchased              "Purchased"
    :suppliers/col-total                  "Total"
    :suppliers/col-payer                  "Payer"
    :suppliers/col-status                 "Status"
    :suppliers/col-raw-label              "Raw Label"
    :suppliers/col-article                "Article"
    :suppliers/col-normalized             "Normalized"
    :suppliers/delete-title               "Delete supplier"
    :suppliers/delete-msg                 "Do you want to delete this supplier?"

    ;; Payers page
    :payers/title                         "Payers"
    :payers/subtitle                      "Shared payment methods for your household"
    :payers/btn-dashboard                 "Dashboard"
    :payers/read-only-notice              "Read-only access. Ask a household member to update payers."
    :payers/delete-title                  "Delete payer"
    :payers/delete-msg                    "Do you want to delete this payer?"

    ;; Articles page
    :articles/title                       "Articles"
    :articles/subtitle                    "Power-user article catalog (used for mapping and aliases)"
    :articles/btn-dashboard               "Dashboard"
    :articles/btn-unmapped                "Unmapped Aliases"
    :articles/delete-title                "Delete article"
    :articles/delete-msg                  "Do you want to delete this article?"

    ;; Categories page
    :categories/title                     "Categories"
    :categories/subtitle                  "Power-user category catalog (used for Subcategories)."
    :categories/btn-dashboard             "Dashboard"
    :categories/delete-title              "Delete category"
    :categories/delete-msg                "Do you want to delete this category?"

    ;; Stores page
    :stores/title                         "Stores"
    :stores/subtitle                      "Power-user store catalog (used for normalization and store aliases)."
    :stores/btn-dashboard                 "Dashboard"
    :stores/btn-store-aliases             "Store Aliases"
    :stores/delete-title                  "Delete store"
    :stores/delete-msg                    "Do you want to delete this store?"

    ;; Article aliases page
    :article-aliases/title                "Article Aliases"
    :article-aliases/subtitle             "Power-user alias catalog (mapping is managed via Unmapped Aliases)"
    :article-aliases/btn-dashboard        "Dashboard"
    :article-aliases/btn-unmapped         "Unmapped Aliases"
    :article-aliases/delete-title         "Delete article alias"
    :article-aliases/delete-msg           "Do you want to delete this article alias?"

    ;; Subcategories page
    :subcategories/title                  "Subcategories"
    :subcategories/subtitle               "Power-user subcategory catalog (used for Articles)."
    :subcategories/btn-dashboard          "Dashboard"
    :subcategories/delete-title           "Delete subcategory"
    :subcategories/delete-msg             "Do you want to delete this subcategory?"

    ;; Manufacturers page
    :manufacturers/title                  "Manufacturers"
    :manufacturers/subtitle               "Power-user manufacturer catalog (used for Articles)."
    :manufacturers/btn-dashboard          "Dashboard"
    :manufacturers/delete-title           "Delete manufacturer"
    :manufacturers/delete-msg             "Do you want to delete this manufacturer?"

    ;; Expense categories page
    :expense-categories/title             "Expense Categories"
    :expense-categories/subtitle          "Power-user expense type catalog."
    :expense-categories/btn-dashboard     "Dashboard"
    :expense-categories/delete-title      "Delete expense category"
    :expense-categories/delete-msg        "Do you want to delete this expense category?"

    ;; Cities page
    :cities/title                         "Cities"
    :cities/subtitle                      "Cities catalog (global; admin-locked)."
    :cities/btn-dashboard                 "Dashboard"
    :cities/delete-title                  "Delete city"
    :cities/delete-msg                    "Do you want to delete this city?"

    ;; Payer types page
    :payer-types/title                    "Payer Types"
    :payer-types/subtitle                 "Power-user payer type catalog."
    :payer-types/btn-dashboard            "Dashboard"
    :payer-types/delete-title             "Delete payer type"
    :payer-types/delete-msg               "Do you want to delete this payer type?"

    ;; Supplier aliases page
    :supplier-aliases/title               "Supplier Aliases"
    :supplier-aliases/subtitle            "Power-user supplier alias catalog."
    :supplier-aliases/btn-dashboard       "Dashboard"
    :supplier-aliases/btn-suppliers       "Suppliers"
    :supplier-aliases/delete-title        "Delete supplier alias"
    :supplier-aliases/delete-msg          "Do you want to delete this supplier alias?"

    ;; Store aliases page
    :store-aliases/title                  "Store Aliases"
    :store-aliases/subtitle               "Power-user store alias catalog."
    :store-aliases/btn-dashboard          "Dashboard"
    :store-aliases/btn-stores             "Stores"
    :store-aliases/delete-title           "Delete store alias"
    :store-aliases/delete-msg             "Do you want to delete this store alias?"}})

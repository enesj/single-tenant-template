(ns app.domain.frontend.expenses.routes.user
  "User-facing expense tracking routes.
   These routes are role-gated - users without proper role see waiting room."
  (:require
    [app.domain.shared.routes.expenses-user :as expenses-user-routes]
    [app.template.frontend.routes.controllers :as controllers]))

(def ^:private route-options-by-id
  {:waiting-room
   {:name :waiting-room
    :view :waiting-room
    :controllers (controllers/make-simple-controller :page/init-waiting-room)}

   :expenses-dashboard
   {:name :expenses-dashboard
    :view :expenses-dashboard
    :controllers (controllers/make-simple-controller :page/init-expenses-dashboard)}

   :user-dashboard
   {:name :user-dashboard
    :view :expenses-dashboard
    :controllers (controllers/make-simple-controller :page/init-expenses-dashboard)}

   :unmapped-items
   {:name :unmapped-items
    :view :unmapped-items
    :controllers (controllers/make-simple-controller :page/init-unmapped-items)}

   :expenses-dashboard-alias
   {:name :expenses-dashboard-alias
    :view :expenses-dashboard
    :controllers (controllers/make-simple-controller :page/init-expenses-dashboard)}

   :expenses-list
   {:name :expenses-list
    :view :expenses-list
    :controllers (controllers/make-simple-controller :page/init-expenses-list)}

   :expense-upload
   {:name :expense-upload
    :view :expense-upload
    :controllers (controllers/make-simple-controller :page/init-expense-upload)}

   :receipts
   {:name :receipts
    :view :receipts-list
    :controllers (controllers/make-simple-controller :page/init-receipts-list)}

   :receipt-detail
   {:name :receipt-detail
    :view :receipt-detail
    :parameters {:path {:receipt-id string?}}
    :controllers (controllers/make-simple-controller :page/init-receipt-detail)}

   :expense-new
   {:name :expense-new
    :view :expense-new
    :controllers (controllers/make-simple-controller :page/init-expense-new)}

   :expense-reports
   {:name :expense-reports
    :view :expense-reports
    :controllers (controllers/make-simple-controller :page/init-expense-reports)}

   :expense-settings
   {:name :expense-settings
    :view :expense-settings
    :controllers (controllers/make-simple-controller :page/init-expense-settings)}

   :expense-suppliers
   {:name :expense-suppliers
    :view :expense-suppliers
    :controllers (controllers/make-simple-controller :page/init-expense-suppliers)}

   :expense-payers
   {:name :expense-payers
    :view :expense-payers
    :controllers (controllers/make-simple-controller :page/init-expense-payers)}

   :expense-stores
   {:name :expense-stores
    :view :expense-stores
    :controllers (controllers/make-simple-controller :page/init-expense-stores)}

   :expense-store-aliases
   {:name :expense-store-aliases
    :view :expense-store-aliases
    :controllers (controllers/make-simple-controller :page/init-expense-store-aliases)}

   :expense-items
   {:name :expense-items
    :view :expense-items
    :controllers (controllers/make-simple-controller :page/init-expense-items)}

   :expense-articles
   {:name :expense-articles
    :view :expense-articles
    :controllers (controllers/make-simple-controller :page/init-expense-articles)}

   :expense-manufacturers
   {:name :expense-manufacturers
    :view :expense-manufacturers
    :controllers (controllers/make-simple-controller :page/init-expense-manufacturers)}

   :expense-categories
   {:name :expense-categories
    :view :expense-categories
    :controllers (controllers/make-simple-controller :page/init-expense-categories)}

   :expense-categories-catalog
   {:name :expense-categories-catalog
    :view :expense-categories-catalog
    :controllers (controllers/make-simple-controller :page/init-expense-categories-catalog)}

   :expense-cities
   {:name :expense-cities
    :view :expense-cities
    :controllers (controllers/make-simple-controller :page/init-expense-cities)}

   :expense-subcategories
   {:name :expense-subcategories
    :view :expense-subcategories
    :controllers (controllers/make-simple-controller :page/init-expense-subcategories)}

   :expense-payer-types
   {:name :expense-payer-types
    :view :expense-payer-types}

   :expense-article-aliases
   {:name :expense-article-aliases
    :view :expense-article-aliases
    :controllers (controllers/make-simple-controller :page/init-expense-article-aliases)}

   :expense-supplier-aliases
   {:name :expense-supplier-aliases
    :view :expense-supplier-aliases
    :controllers (controllers/make-simple-controller :page/init-expense-supplier-aliases)}

   :expense-price-observations
   {:name :expense-price-observations
    :view :expense-price-observations
    :controllers (controllers/make-simple-controller :page/init-expense-price-observations)}

   :expense-detail
   {:name :expense-detail
    :view :expense-detail
    :parameters {:path {:expense-id string?}}
    :controllers (controllers/make-simple-controller :page/init-expense-detail)}})

(defn- descriptor->route
  [{:keys [id path]}]
  (let [route-options (get route-options-by-id id)]
    (when-not route-options
      (throw (ex-info "Missing frontend route options for canonical descriptor"
               {:route-id id
                :path path})))
    [path route-options]))

(defn routes
  "Generate user-facing expense tracking routes from canonical descriptors."
  []
  (mapv descriptor->route expenses-user-routes/route-descriptors))

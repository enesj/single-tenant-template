(ns app.domain.frontend.registry
  "Domain frontend registry; register domain routes/components."
  (:require
    [app.domain.frontend.expenses.routes.user :as expenses-user-routes]
    ;; Domain-local init: loads events/subs via side effects
    app.domain.frontend.expenses.init))

(def ^:private expenses-manifest
  {:id :expenses
   :routes {:user expenses-user-routes/routes}
   :init! (fn [] nil)
   :admin-entities {}
   :admin-domain-groups
   {:expenses
    {:title "Expenses Management"
     :description "Manage articles, suppliers, and manufacturers"
     :icon "💰"
     :entities #{:articles
                 :article-aliases
                 :suppliers
                 :supplier-aliases
                 :manufacturers
                 :categories
                 :subcategories
                 :stores
                 :store-aliases

                 :unmapped-aliases}
     :color "accent"
     :scope :admin}}
   :user-domain-groups
   {:expenses-user
    {:title "User Expenses"
     :description "User-facing expense tracking and management"
     :icon "💰"
     :entities #{:expenses
                 :receipts
                 :suppliers
                 :stores
                 :store-aliases
                 :payers
                 :expense-items
                 :articles
                 :categories
                 :subcategories
                 :article-aliases}
     :color "accent"
     :scope :user}}})

(def enabled-domains
  "Vector of enabled domain manifests.
   To add a new domain, add its manifest here."
  [expenses-manifest])

(defn get-domain
  "Get a domain manifest by id."
  [domain-id]
  (first (filter #(= domain-id (:id %)) enabled-domains)))

(defn init-all-domains!
  "Initialize all enabled domains (loads events/subs)."
  []
  (doseq [manifest enabled-domains]
    (when-let [init-fn (:init! manifest)]
      (init-fn))))

(defn all-user-routes
  "Collect user routes from all enabled domains.
   Routes are stored as functions and called lazily to avoid circular dependencies.
   Returns a vector of reitit route vectors."
  []
  (mapcat (fn [manifest]
            (let [routes-fn (get-in manifest [:routes :user])]
              (if (fn? routes-fn)
                (routes-fn)
                routes-fn)))
    enabled-domains))

(defn all-admin-entities
  "Collect admin entity registry entries from all enabled domains.
   Returns a merged map of entity-keyword -> registry-entry."
  []
  (apply merge (map :admin-entities enabled-domains)))

(defn all-admin-domain-groups
  "Collect admin domain groups from all enabled domains.
   Returns a merged map of group-keyword -> group-config."
  []
  (apply merge (map :admin-domain-groups enabled-domains)))

(defn all-user-domain-groups
  "Collect user domain groups from all enabled domains.
   Returns a merged map of group-keyword -> group-config."
  []
  (apply merge (map :user-domain-groups enabled-domains)))

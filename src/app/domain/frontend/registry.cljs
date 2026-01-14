(ns app.domain.frontend.registry
  "Frontend domain registry - provides domain manifests to template/admin.

   Each domain manifest contains:
   - :id - domain keyword identifier
   - :routes
     - :user - fn returning reitit frontend route vectors for user-facing pages
   - :init! - fn that ensures domain events/subs are loaded (side effects)
   - :admin-entities - map of entity keywords to admin entity registry entries
   - :admin-domain-groups - map of group keywords to admin domain group configs
   - :user-domain-groups - map of group keywords to user domain group configs
   
   Template/admin import this registry to dynamically compose routes and UI.
   
   NOTE: Pages are NOT included in the manifest to avoid circular dependencies.
   Use app.domain.frontend.pages instead."
  (:require
    [app.domain.frontend.expenses.routes.user :as expenses-user-routes]
    ;; Domain-local init: loads events/subs via side effects
    app.domain.frontend.expenses.init
    ;; Domain adapters - admin (loads all adapter modules)
    [app.domain.frontend.expenses.adapters :as expenses-adapter]
    [app.domain.frontend.expenses.admin.components.detail-modals :as detail-modals]
    [app.domain.frontend.expenses.admin.components.entity-actions :as entity-actions]
    ))

(def ^:private expenses-manifest
  {:id :expenses
   :routes
   {:user expenses-user-routes/routes}  ;; User routes are safe to include
   ;; NOTE: Pages are NOT included here to avoid circular dependencies.
   ;; Use app.domain.frontend.pages for page components.
   :init! (fn []
            ;; All events/subs are loaded via require above
            ;; No additional initialization needed
            nil)
   :admin-entities
   {:expenses {:init-fn expenses-adapter/init-expenses-adapter!}
    :receipts {:init-fn expenses-adapter/init-receipts-adapter!
               :custom-actions entity-actions/admin-receipts-actions
               :modals [detail-modals/admin-receipt-detail-modal]}
    :suppliers {:init-fn expenses-adapter/init-suppliers-adapter!
                :custom-actions entity-actions/admin-suppliers-actions
                :modals [detail-modals/admin-supplier-detail-modal]}
    :payers {:init-fn expenses-adapter/init-payers-adapter!
             :custom-actions entity-actions/admin-payers-actions
             :modals [detail-modals/admin-payer-detail-modal]}
    :articles {:init-fn expenses-adapter/init-articles-adapter!
               :custom-actions entity-actions/admin-articles-actions
               :modals [detail-modals/admin-article-detail-modal]}
    :expense-items {:init-fn expenses-adapter/init-expense-items-adapter!
                    :custom-actions entity-actions/admin-expense-items-actions
                    :modals [detail-modals/admin-expense-item-detail-modal]}
    :article-aliases {:init-fn expenses-adapter/init-article-aliases-adapter!
                      :custom-actions entity-actions/admin-article-aliases-actions
                      :modals [detail-modals/admin-article-alias-detail-modal]}
    :price-observations {:init-fn expenses-adapter/init-price-observations-adapter!
                         :custom-actions entity-actions/admin-price-observations-actions
                         :modals [detail-modals/admin-price-observation-detail-modal]}}
   :admin-domain-groups {}
   :user-domain-groups
   {:expenses-user
    {:title "Expenses"
     :description "User-facing expense tracking and management"
     :icon "💰"
     :entities #{:expenses
                 :receipts
                 :suppliers
                 :payers
                 :expense-items
                 :articles
                 :article-aliases
                 :price-observations}
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

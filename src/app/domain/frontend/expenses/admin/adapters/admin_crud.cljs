(ns app.domain.frontend.expenses.admin.adapters.admin-crud
  "CRUD bridge registrations for expenses domain entities.

   These bridges customize how template CRUD operations work for
   expenses-related entities (suppliers, expenses, receipts, payers,
   articles, article-aliases, supplier-aliases, manufacturers,
   price-observations)."
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.utils.http :as admin-http]
    [taoensso.timbre :as log]))

(def ^:private lookup-params
  "Default query params used for FK lookup dropdowns in admin forms."
  {:limit 500
   :offset 0})

(defn- suppliers-request
  "Create HTTP request config for suppliers admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/suppliers"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "🔧 suppliers-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :suppliers

   :operations
   {:fetch {:request (fn [{:keys [db]} entity-type default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (suppliers-request
                                         {:method :get
                                          :params lookup-params
                                          :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
                                          :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]}))
                         {:dispatch [:admin/redirect-to-login]}))}

    :delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (suppliers-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id default-effect]
                           ;; Reuse default DB updates but refresh via expenses domain loader
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.suppliers/load-list {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (suppliers-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.suppliers/load-list {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (suppliers-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.suppliers/load-list {}]))}}})

(defn- expenses-request
  "Create HTTP request config for expenses admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/entries"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "💸 expenses-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :expenses

   :operations
   {:fetch {:request (fn [{:keys [db]} entity-type default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (expenses-request
                                         {:method :get
                                          :params lookup-params
                                          :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
                                          :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]}))
                         {:dispatch [:admin/redirect-to-login]}))}

    :delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (expenses-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.expenses/load-list {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (expenses-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.expenses/load-list {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (expenses-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.expenses/load-list {}]))}}})

(defn- expense-items-request
  "Create HTTP request config for expense items admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/expense-items"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "🧾 expense-items-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :expense-items

   :operations
   {:fetch {:request (fn [{:keys [db]} entity-type default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (expense-items-request
                                         {:method :get
                                          :params lookup-params
                                          :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
                                          :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]}))
                         {:dispatch [:admin/redirect-to-login]}))}

    :delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (expense-items-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.expense-items/load-list {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (expense-items-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.expense-items/load-list {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (expense-items-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.expense-items/load-list {}]))}}})

(defn- receipts-request
  "Create HTTP request config for receipts admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/receipts"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "🧾 receipts-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :receipts

   :operations
   {:fetch {:request (fn [{:keys [db]} entity-type default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (receipts-request
                                         {:method :get
                                          :params lookup-params
                                          :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
                                          :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]}))
                         {:dispatch [:admin/redirect-to-login]}))}

    :delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (receipts-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.receipts/load-list {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (receipts-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.receipts/load-list {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (receipts-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.receipts/load-list {}]))}}})

(defn- payers-request
  "Create HTTP request config for payers admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/payers"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "🔧 payers-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :payers

   :operations
   {:fetch {:request (fn [{:keys [db]} entity-type default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (payers-request
                                         {:method :get
                                          :params lookup-params
                                          :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
                                          :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]}))
                         {:dispatch [:admin/redirect-to-login]}))}

    :delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (payers-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.payers/load-list {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (payers-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.payers/load-list {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (payers-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.payers/load-list {}]))}}})

(defn- articles-request
  "Create HTTP request config for articles admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/articles"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "📦 articles-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :articles

   :operations
   {:fetch {:request (fn [{:keys [db]} entity-type default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (articles-request
                                         {:method :get
                                          :params lookup-params
                                          :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
                                          :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]}))
                         {:dispatch [:admin/redirect-to-login]}))}

    :delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (articles-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.articles/load-list {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (articles-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.articles/load-list {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (articles-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.articles/load-list {}]))}}})

(defn- article-aliases-request
  "Create HTTP request config for article aliases admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/article-aliases"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "🔗 article-aliases-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :article-aliases

   :operations
   {:fetch {:request (fn [{:keys [db]} entity-type default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (article-aliases-request
                                         {:method :get
                                          :params lookup-params
                                          :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
                                          :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]}))
                         {:dispatch [:admin/redirect-to-login]}))}

    :delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (article-aliases-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.article-aliases/load-list {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (article-aliases-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.article-aliases/load-list {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (article-aliases-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.article-aliases/load-list {}]))}}})

(defn- price-observations-request
  "Create HTTP request config for price observations admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/price-observations"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "📈 price-observations-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :price-observations

   :operations
   {:fetch {:request (fn [{:keys [db]} entity-type default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (price-observations-request
                                         {:method :get
                                          :params lookup-params
                                          :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
                                          :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]}))
                         {:dispatch [:admin/redirect-to-login]}))}

    :delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (price-observations-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.price-observations/load-list {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (price-observations-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.price-observations/load-list {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (price-observations-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.price-observations/load-list {}]))}}})

(defn- supplier-aliases-request
  "Create HTTP request config for supplier aliases admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/supplier-aliases"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "🔁 supplier-aliases-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :supplier-aliases

   :operations
   {:fetch {:request (fn [{:keys [db]} entity-type default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (supplier-aliases-request
                                         {:method :get
                                          :params lookup-params
                                          :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
                                          :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]}))
                         {:dispatch [:admin/redirect-to-login]}))}

    :delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (supplier-aliases-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.supplier-aliases/load-list {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (supplier-aliases-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.supplier-aliases/load-list {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (supplier-aliases-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.supplier-aliases/load-list {}]))}}})

(defn- manufacturers-request
  "Create HTTP request config for manufacturers admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/manufacturers"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "🏭 manufacturers-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :manufacturers

   :operations
   {:fetch {:request (fn [{:keys [db]} entity-type default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (manufacturers-request
                                         {:method :get
                                          :params lookup-params
                                          :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
                                          :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]}))
                         {:dispatch [:admin/redirect-to-login]}))}

    :delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (manufacturers-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.manufacturers/load-list {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (manufacturers-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.manufacturers/load-list {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (manufacturers-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.frontend.expenses.events.manufacturers/load-list {}]))}}})

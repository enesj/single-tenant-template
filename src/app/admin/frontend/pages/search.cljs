(ns app.admin.frontend.pages.search
  "Admin global cross-entity search page."
  (:require
    [app.admin.frontend.components.layout :as layout]
    app.admin.frontend.events.search
    app.admin.frontend.subs.search
    [app.domain.frontend.expenses.pages.user.search :as tenant-search]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-search-page []
  (let [query (use-subscribe [:admin/search-query])
        loading? (use-subscribe [:admin/search-loading?])
        results (use-subscribe [:admin/search-results])
        selected (use-subscribe [:admin/search-selected])
        related (use-subscribe [:admin/search-related])
        related-loading? (use-subscribe [:admin/search-related-loading?])]
    ($ layout/admin-layout
      ($ :div {:class "h-full"}
        ($ tenant-search/search-page-content
          {:query query
           :loading? loading?
           :results results
           :selected selected
           :related related
           :related-loading? related-loading?
           :set-query! #(rf/dispatch [:admin/set-search-query %])
           :select-result! #(rf/dispatch [:admin/select-search-result %])
           :clear-selection! #(rf/dispatch [:admin/clear-search-selection])})))))
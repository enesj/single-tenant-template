(ns app.domain.frontend.pages
  "Domain pages aggregator - provides page component mapping for routes.
   
   This namespace is separate from the domain registry to avoid circular
   dependencies. The registry is used by admin/entity-registry which can
   be pulled in via template components, creating a cycle with pages.
   
   Template/frontend/core.cljs should require this namespace directly
   to get domain page components."
  (:require
    [app.domain.frontend.expenses.pages :as expenses-pages]))

(def all-pages
  "Map of route :view keywords to page components.
   
   The keys must match the :view values in the domain routes (user.cljs).
   The template's current-page component uses this map to render domain pages."
  (merge expenses-pages/pages))

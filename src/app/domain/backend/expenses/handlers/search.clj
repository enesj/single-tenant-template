(ns app.domain.backend.expenses.handlers.search
  "Compatibility facade — requires sub-namespaces for all search handlers.
   Callers using (:as search) continue to work via the def aliases below."
  (:require
    [app.domain.backend.expenses.handlers.search.cross-entity :as ce]
    [app.domain.backend.expenses.handlers.search.quick-search :as qs]
    [app.domain.backend.expenses.handlers.search.quick-add :as qa]
    [app.domain.backend.expenses.handlers.search.context-suggestions :as cs]
    [app.domain.backend.expenses.handlers.search.related-records :as rr]))

;; ── Cross-entity search ───────────────────────────────────────────────────────
(def user-search-handler ce/user-search-handler)
(def admin-search-handler ce/admin-search-handler)

;; ── Quick search (smart expense form dropdown) ────────────────────────────────
(def quick-search-handler qs/quick-search-handler)

;; ── Quick add search (expense creation flow) ──────────────────────────────────
(def quick-add-search-handler qa/quick-add-search-handler)
(def cooccurring-articles-handler qa/cooccurring-articles-handler)

(def quick-add-history-handler qa/quick-add-history-handler)

;; ── Context suggestions ───────────────────────────────────────────────────────
(def context-suggestions-handler cs/context-suggestions-handler)

;; ── Related records ───────────────────────────────────────────────────────────
(def user-related-handler rr/user-related-handler)
(def admin-related-handler rr/admin-related-handler)

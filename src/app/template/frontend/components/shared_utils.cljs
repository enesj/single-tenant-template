(ns app.template.frontend.components.shared-utils
  "Template-level shared utilities.

  This namespace exists to avoid domain/user code depending on admin-only
  aggregators (`app.admin.frontend.components.shared-utils`). Prefer requiring
  this from domain code."
  (:require
    [app.template.frontend.components.detail :as detail]
    [app.template.frontend.utils.display :as display]))

;; Formatting

(def format-value display/format-value)
(def format-date display/format-date)

;; Detail UI components
(def detail-modal-header detail/detail-modal-header)



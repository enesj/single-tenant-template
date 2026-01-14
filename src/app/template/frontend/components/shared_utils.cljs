(ns app.template.frontend.components.shared-utils
  "Template-level shared utilities.

  This namespace exists to avoid domain/user code depending on admin-only
  aggregators (`app.admin.frontend.components.shared-utils`). Prefer requiring
  this from domain code."
  (:require
    [app.template.frontend.components.detail :as detail]
    [app.template.frontend.utils.display :as display]))

;; Formatting
(def react-element? display/react-element?)
(def format-value display/format-value)
(def format-date display/format-date)
(def format-relative-time display/format-relative-time)
(def user-initials display/user-initials)
(def tenant-label display/tenant-label)

;; Detail UI components
(def detail-modal-header detail/detail-modal-header)
(def detail-field detail/detail-field)
(def detail-card detail/detail-card)
(def ip-address-badge detail/ip-address-badge)

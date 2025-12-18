(ns app.admin.frontend.components.shared-utils
  "Shared utility functions for admin components.
   Aggregates formatting, UI components, and validation utilities."
  (:require
    [app.admin.frontend.components.format :as fmt]
    [app.admin.frontend.components.ui :as ui]
    [app.admin.frontend.components.validation :as val]))

;; ============================================================================
;; Re-exports for Backward Compatibility
;; ============================================================================

;; Formatting
(def react-element? fmt/react-element?)
(def format-value fmt/format-value)
(def format-date fmt/format-date)
(def format-relative-time fmt/format-relative-time)
(def user-initials fmt/user-initials)
(def tenant-label fmt/tenant-label)
(def create-csv-export-data fmt/create-csv-export-data)
(def download-as-json fmt/download-as-json)

;; UI Components
(def status-badge ui/status-badge)
(def role-badge ui/role-badge)
(def verification-badge ui/verification-badge)
(def metric-badge ui/metric-badge)
(def detail-modal-header ui/detail-modal-header)
(def detail-field ui/detail-field)
(def detail-card ui/detail-card)
(def ip-address-badge ui/ip-address-badge)

;; Validation
(def validate-email-format val/validate-email-format)
(def validate-required-fields val/validate-required-fields)

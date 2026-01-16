(ns app.admin.frontend.components.format
  "Formatting and export utilities for admin frontend components."
  (:require
    [app.template.frontend.utils.display :as display]))

;; Delegate shared formatting behavior to the template layer so admin/domain/template
;; UIs stay consistent without duplicating logic.
(def format-value display/format-value)

(def user-initials display/user-initials)

(def tenant-label display/tenant-label)

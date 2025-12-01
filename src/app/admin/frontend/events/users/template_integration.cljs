(ns app.admin.frontend.events.users.template-integration
  "Main entry point for template system integration - imports all modular handlers"
  (:require
    [app.admin.frontend.events.users.template.delete-handlers]
    [app.admin.frontend.events.users.template.form-interceptors]
    [app.admin.frontend.events.users.template.messages]
    [app.admin.frontend.events.users.template.success-handlers]))

;; This namespace serves as the main entry point for admin template system integration.
;; All template-related event handlers are now organized into focused modules:
;;
;; 1. messages.cljs - Success/error message management and UI feedback
;; 2. delete-handlers.cljs - Template delete operation overrides for admin context
;; 3. form-interceptors.cljs - Form submission routing and admin API integration
;; 4. success-handlers.cljs - Admin form success/failure handlers and workflows
;;
;; Simply require this namespace to get access to all template integration events.
;; No event handlers are defined here - they're all in the specific modules.
;;
;; REFACTORING COMPLETED:
;; - Original file: 291 lines of mixed template integration concerns
;; - New structure: 4 focused modules + this 25-line entry point
;; - Benefits: Clear separation of concerns, easier testing, better maintainability

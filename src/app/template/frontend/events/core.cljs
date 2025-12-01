(ns app.template.frontend.events.core
  "Main events orchestration layer for template frontend.

   Requires all domain-specific event namespaces for side effects
   (Re-frame event registration) and provides a central coordination point.

   This namespace acts as the entry point for the events system,
   ensuring all event handlers are properly registered through
   namespace requires."
  (:require ;; Template event namespaces (loaded for side effects)
 ;; These requires are essential for Re-frame event registration
    [app.template.frontend.events.auth]
    [app.template.frontend.events.bootstrap]
    [app.template.frontend.events.config]
    [app.template.frontend.events.messages]
    [app.template.frontend.events.onboarding] ;; Routing remains separate as already modular
    [app.template.frontend.events.routing]))

;; ========================================================================
;; Orchestration Layer
;; ========================================================================

;; This namespace serves as a coordination point where all domain-specific
;; event namespaces are required. The actual event handlers are defined
;; in their respective domain namespaces:
;;
;; - bootstrap: Application initialization, theme management, AJAX setup
;; - auth: Authentication flow, session management, logout functionality
;; - config: Configuration fetching, UI state management
;; - onboarding: Complete onboarding workflow with step management
;; - routing: URL routing and navigation (already modular)
;;
;; By requiring all these namespaces, we ensure that their Re-frame
;; event registrations are executed when this namespace is loaded.

;; ========================================================================
;; Cross-Domain Event Coordination
;; ========================================================================

;; Future enhancement: If cross-domain events are needed, they would be
;; defined here. Examples might include:
;; - Authentication success triggering config fetch
;; - Onboarding completion triggering dashboard initialization
;; - Theme changes affecting multiple domain UIs
;;
;; For now, cross-domain coordination is handled via :fx dispatches
;; in the individual domain event handlers.

;; ========================================================================
;; Migration Notes
;; ========================================================================

;; This refactoring maintains backward compatibility:
;; - All existing event keywords remain the same
;; - Event dispatch calls from components work unchanged
;; - Subscriptions continue to function normally
;; - Only the organization of the code has changed
;;
;; The domain boundaries are:
;; - Bootstrap: ::bootstrap/* and infrastructure events
;; - Auth: ::auth/* and authentication-related page events
;; - Config: ::config/* and UI state management events
;; - Onboarding: :onboarding/* and related workflow events
;; - Routing: Navigation and URL handling (unchanged)

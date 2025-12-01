(ns app.backend.routes.auth
  "Legacy auth routes namespace.

   In the single-tenant template this namespace is kept only for
   backward compatibility. All real auth handling lives in
   `app.template.backend.routes.auth` and is wired via DI.  Here we
   simply alias the public handlers so that any old references compile."
  (:require
    [app.template.backend.routes.auth :as template-auth]))

;; Thin shims delegating to the template auth routes

(def logout-handler
  "Alias to template logout handler."
  template-auth/logout-handler)

(def auth-status-handler
  "Alias to template auth status handler."
  template-auth/auth-status-handler)

(def register-handler
  "Alias to template user registration handler."
  template-auth/register-handler)

(def login-handler
  "Alias to template email/password login handler."
  template-auth/login-handler)

(def test-auth-handler
  "Alias to template test-auth handler (dev only)."
  template-auth/test-auth-handler)

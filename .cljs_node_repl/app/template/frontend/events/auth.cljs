(ns app.template.frontend.events.auth
  "Authentication and session management events for the template/frontend layer.

   Handles user authentication flow, session management, and logout functionality."
  (:require
    [app.template.frontend.events.auth.change-password]
    [app.template.frontend.events.auth.fx]
    [app.template.frontend.events.auth.guard]
    [app.template.frontend.events.auth.login]
    [app.template.frontend.events.auth.logout]
    [app.template.frontend.events.auth.pages]
    [app.template.frontend.events.auth.password-reset]
    [app.template.frontend.events.auth.register]
    [app.template.frontend.events.auth.status]
    [app.template.frontend.events.auth.utils]
    [app.template.frontend.events.auth.verify-email]))


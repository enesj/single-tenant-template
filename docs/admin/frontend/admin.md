<!-- ai: {:tags [:frontend] :kind :guide} -->

# Admin Panel (Single-Tenant) Feature Guide

## Overview

The admin panel in this single-tenant app covers user administration, audit logging, login event monitoring, and system health. There is no tenant switching or billing.

## Architecture Map (Frontend)

```
app.admin.frontend.*
├── core.cljs           ; bootstraps admin UI, loads configs
├── routes.cljs         ; admin routing (dashboard, users, audit, login-events)
├── pages/
│   ├── dashboard.cljs  ; overview
│   ├── users.cljs      ; user management + per-user activity modal
│   ├── audit.cljs      ; audit log table
│   └── login_events.cljs ; global login events table
├── components/         ; layout, tables, stats, monitoring widgets
│   └── sidebar.cljs    ; main navigation with DaisyUI menu
└── adapters/
    ├── users.cljs
    ├── audit.cljs
    └── login_events.cljs
```

## Key Features

| Feature | Purpose | Access | Route |
|---------|---------|--------|-------|
| User Administration | Manage users | Admin | `/admin/users` |
| Audit Logging | View admin actions | Admin | `/admin/audit` |
| Login Events | View admin/user login attempts | Admin | `/admin/login-events` |
| System Monitoring | Basic health/status | Admin | `/admin/dashboard` |
| Admin Settings | Configure admin UI + entity settings | Admin | `/admin/admin-settings` |
| User UI Config | Configure user-facing defaults/locks | Admin | `/admin/user-settings` |

## Activity & Login Monitoring

- **Audit Logs** (`/admin/audit`): Admin actions with actor, entity, action, changes, IP/user agent. Generic table supports filtering, sorting, pagination, and details.
- **Login Events** (`/admin/login-events`): Global feed of admin and user logins (success/failure, reason, IP, user agent, principal name/email). Filter by principal type and success.
- **Per-user Activity Modal** (Users → “View Activity”): Combines that user’s audit actions and login history.
- **Data sources**: `audit_logs` for admin actions; `login_events` for both admin and user logins. IDs/emails/names are normalized for consistent display/export.

## Security Context

Admin operations require the admin role:

```clojure
(rf/reg-event-fx :admin/require-admin
  (fn [cofx _ [_ event]]
    (let [current-user (get-in cofx [:db :current-user])]
      (if (= "admin" (:role current-user))
        {:dispatch [event]}
        {:dispatch [:auth/require-login
                    {:message "Administrator access required"}]})))))
```

## Dashboard Overview

- Quick metrics for total users, recent activity, and system health.
- Shortcuts to Users, Audit Logs, and Login Events.

## Admin Settings

The admin settings UI is split into:

- `/admin/admin-settings` — admin panel configuration (admin scope)
- `/admin/user-settings` — domain-owned user UI config (user-facing defaults/locks)

`/admin/settings` exists as a legacy redirect to `/admin/admin-settings`.

See the complete guide in `./admin-settings.md`.

### Key Features
- **Unified Interface**: Toggle between Admin and User scopes in one UI.
- **View Options**: Control display toggles and action buttons (edit/delete/select).
- **Form Fields**: Configure field existence, requirements, and validation.
- **Table Columns**: manage visibility, ordering, widths, and sorting/filtering.
- **User Defaults**: Set default view states and lock controls for end-users.

## User Management (Quick Reference)

```clojure
;; app.admin.frontend.pages.users
(defui admin-users-page [] ($ generic-admin-entity-page :users))

;; Per-user activity
($ user-actions/admin-user-actions
   {:on-view-activity #(rf/dispatch [:admin/view-user-activity %])})
```

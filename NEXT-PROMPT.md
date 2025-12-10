# NEXT-PROMPT.md

**Date**: 2025-12-10
**Topic**: Implement Phase 3B - User Experience & Dashboard for Home Expenses Tracker
**PROMPT**: Create user-facing expense tracking experience with role-based access, mobile-first dashboard, and personal analytics

---

## Context Snapshot (Single-Tenant Template Architecture)

- **Template**: Single-tenant SaaS template with Clojure/ClojureScript + PostgreSQL, served at `http://localhost:8085`
- **Current State**: Admin expense tracking UI complete (Phase 3A), now need user-facing experience (Phase 3B)
- **Authentication**: Dual auth system - admin token auth (`/admin/api`) and user session auth (`/api/v1/auth`)
- **Frontend**: Shadow-CLJS builds (`:app` for public, `:admin` for admin console) using Re-frame + UIX
- **Backend**: Services with protocols-first approach, DI container in `app.template.di.config`
- **Expenses Domain**: Complete backend services at `/admin/api/expenses/*`, database schema migrated (8 tables), domain implemented at `src/app/domain/expenses/` (services/, routes/, frontend/)

## Task Focus

Implement comprehensive user experience for personal expense tracking including:
- Role-based access control (unassigned users vs assigned users)
- Personal expense dashboard with analytics and insights
- Mobile-optimized receipt upload and expense management
- User settings and preferences
- Integration with existing expenses backend services

## Code Map (Relevant Namespaces & Files)

### Frontend Core (`:app` build)
- `src/app/template/frontend/core.cljs` - Main app entry point
- `src/app/template/frontend/routes.cljs` - Route definitions and controllers
- `src/app/template/frontend/events/auth.cljs` - User authentication events
- `src/app/template/frontend/pages/home.cljs` - Landing page (role gating needed)
- `src/app/template/frontend/pages/login.cljs` - User login page
- `src/app/template/frontend/components/` - Reusable UI components

### User-Facing Pages to Create
- `src/app/template/frontend/pages/expenses_dashboard.cljs` - Personal dashboard (new)
- `src/app/template/frontend/pages/expenses_list.cljs` - Expense history (new)
- `src/app/template/frontend/pages/receipt_upload.cljs` - Mobile receipt capture (new)
- `src/app/template/frontend/pages/expenses_settings.cljs` - User preferences (new)

### Backend Extensions (Reuse Existing Expenses Domain)
- Extend existing `src/app/domain/expenses/services/` - Reuse suppliers, expenses, receipts, articles services with user filtering
- Extend existing `src/app/domain/expenses/routes/` - Add user-specific routes alongside admin routes
- `src/app/domain/expenses/services/user_access.clj` - User filtering and access control layer (new)
- `src/app/domain/expenses/routes/user.clj` - User-facing API routes (new)

### Configuration
- `shadow-cljs.edn` - Add new routes to `:app` build
- `resources/db/models.edn` - User permissions table (if needed)

## Commands to Run

### Development
```bash
# Frontend development (app build for user experience)
npm run watch          # :app build at http://localhost:8085
npm run watch:admin    # :admin build at http://localhost:8085/admin

# Backend compilation check (use system-logs skill)
# Check for compilation errors with system-logs skill
# Backend hot-reloads automatically on file changes

# Note: App should be running continuously during development
# Use system-logs skill to monitor backend compilation and runtime errors
```

### Testing
```bash
# Backend tests (save output first!)
bb be-test 2>&1 | tee /tmp/be-test.txt

# Frontend tests (save output first!)
npm run test:cljs 2>&1 | tee /tmp/fe-test.txt

# Node tests for faster frontend test iteration
bb fe-test-node
```

### Database
```bash
# Backup before schema changes
bb backup-db

# Migrations (if schema changes needed)
clj -X:migrations-dev/migrate

# Restore if needed
bb restore-db
```

## Gotchas & Critical Details

### Authentication & Authorization
- **User sessions** use Ring session cookies (not bearer tokens like admin)
- **Role-based access**: Users without expense tracking role see waiting/onboarding page
- **Security**: Never expose other users' data - implement proper user filtering in services
- **Mixed auth**: `/api/v1/auth` for users, `/admin/api` for admin - don't confuse

### Architecture Patterns
- **Services**: Follow protocol-first pattern (see existing expenses services)
- **Frontend**: Use existing template components (`app.template.frontend.components.*`)
- **State management**: Re-frame with proper app-db paths (don't leak data between users)
- **Routing**: Use `guarded-start` pattern for authenticated routes

### Mobile-First Requirements
- **DaisyUI classes**: Use responsive classes (`ds-*` prefixed for components)
- **Touch-friendly**: Large tap targets for receipt upload and expense entry
- **PWA features**: Consider offline support for receipt capture
- **Camera integration**: Direct mobile camera access for receipt photos

### Data Access Patterns
- **User filtering**: All backend services must filter by `user_id` from session
- **Reuse existing expenses services**: Extend `src/app/domain/expenses/services/` with user filtering - DO NOT duplicate existing logic
- **API consistency**: Follow existing JSON response patterns (`{:success true :data ...}`)
- **Service patterns**: Use existing protocol-first services (suppliers, expenses, receipts, articles) with user-scoped access layer

### Single-Tenant Assumptions
- **No RLS**: Row Level Security not implemented (unlike Hosting repo)
- **No tenant context**: No multi-tenant switching or tenant isolation
- **Port**: App serves on 8085 (not 3000)

## Implementation Checklist

### Phase 1: Foundation & Role Gating
- [ ] Create user role checking middleware/service
- [ ] Implement waiting room page for unassigned users
- [ ] Add role detection to home page routing
- [ ] Create role assignment notification system
- [ ] Test role-based access control

### Phase 2: Dashboard & Core UI
- [ ] Create personal expense dashboard page at `/dashboard` or `/expenses`
- [ ] Implement recent expenses summary with pagination
- [ ] Add monthly spending overview cards
- [ ] Create quick receipt upload button/flow
- [ ] Add pending receipts status tracker
- [ ] Implement responsive dashboard layout

### Phase 3: Expense Management
- [ ] Create user expense list page with filters (date, supplier, status)
- [ ] Implement expense detail view with line items
- [ ] Add mobile-optimized expense creation form
- [ ] Create receipt upload flow with camera integration
- [ ] Add receipt status tracking and notifications
- [ ] Implement expense editing capabilities

### Phase 4: Backend Services (Reuse Existing Domain)
- [ ] Extend existing expenses services (suppliers, expenses, receipts, articles) with user filtering layer
- [ ] Add user authentication middleware to expenses API endpoints (reuse existing auth patterns)
- [ ] Create user-specific analytics endpoints (reuse reports service patterns)
- [ ] Implement receipt upload processing for users (extend existing receipts service)
- [ ] Add user preference management endpoints (new service)
- [ ] Ensure proper security boundaries between users (user filtering in service layer)

### Phase 5: Mobile Optimization
- [ ] Optimize all components for mobile screens
- [ ] Implement touch-friendly interfaces
- [ ] Add PWA manifest and service worker
- [ ] Create offline receipt capture functionality
- [ ] Test on actual mobile devices
- [ ] Implement mobile-specific features (camera, gestures)

### Phase 6: Analytics & Reporting
- [ ] Create personal spending analytics dashboard
- [ ] Implement category-wise spending breakdowns
- [ ] Add monthly/yearly trend reports
- [ ] Create top merchants analysis
- [ ] Add budget tracking and alerts
- [ ] Implement export functionality (PDF/CSV)

### Phase 7: Settings & Preferences
- [ ] Create user settings page
- [ ] Implement notification preferences
- [ ] Add default payment methods setup
- [ ] Create currency and display preferences
- [ ] Add data privacy and export settings
- [ ] Implement profile management

### Phase 8: Testing & Polish
- [ ] Write comprehensive frontend tests for user flows
- [ ] Add integration tests for user-specific backend services
- [ ] Test role-based access control thoroughly
- [ ] Verify mobile responsiveness across devices
- [ ] Security audit for user data isolation
- [ ] Performance testing for expense analytics queries

## Next Agent Instructions

1. **Use the clojure-mcp scratch-pad tool** to track implementation progress through these phases
2. **Start with Phase 1** (Foundation & Role Gating) and complete each phase before moving to the next
3. **Test authentication flows early** - role-based access is critical
4. **Follow existing code patterns** - don't reinvent patterns from template components
5. **REUSE existing expenses domain services** - extend `src/app/domain/expenses/services/` with user filtering, don't duplicate logic
6. **Use system-logs skill** to monitor backend compilation and runtime errors instead of `bb run-app`
7. **Save test output** using the critical testing workflow mentioned in docs
8. **Document any schema changes** in migration files - never edit generated migrations
9. **Start implementation immediately** after creating the detailed plan in scratch-pad

---

**Key Success Metrics**: Users can register, get assigned expense tracking role, log in to personal dashboard, upload receipts via mobile, track expenses with mobile-optimized interface, and view personal spending analytics.

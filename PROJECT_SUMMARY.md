# Single-Tenant SaaS Template - Project Summary

## Project Overview

This is a **single-tenant SaaS template** extracted from a multi-tenant hosting platform. It provides a complete Clojure/ClojureScript web application foundation with:

- **Backend**: Ring-based Clojure server with PostgreSQL database
- **Frontend**: Shadow-CLJS compiled ClojureScript SPA using re-frame and Uix
- **Admin Panel**: Complete administrative interface for user and system management
- **Authentication**: Multi-provider auth system (local password, OAuth, email verification)
- **Database**: PostgreSQL with Row Level Security (RLS) for tenant isolation
- **Testing**: Comprehensive test suite (Kaocha for backend, Karma/Shadow-CLJS for frontend)
- **Tooling**: Extensive babashka-based development tooling and automation

**Architecture Pattern**: Single-tenant with admin/user separation, suitable for SaaS products that need user management, audit trails, and subscription billing.

## Key File Paths & Their Purpose

### Core Application Files
```
src/
├── app/backend/core.clj                    # Main application entry point, system initialization
├── app/backend/webserver.clj              # Ring web server configuration and startup
├── app/backend/routes.clj                 # Main HTTP route definitions and middleware setup
├── app/backend/middleware/               # Security, rate limiting, and request processing
├── app/backend/services/                 # Business logic layer (users, auth, audit, etc.)
├── app/template/frontend/core.cljs          # Frontend application entry point and routing
├── app/template/frontend/routes.cljs      # Re-frame event routing and page dispatch
├── app/shared/                          # Cross-platform utilities and shared components
├── app/migrations/simple_repl.clj         # Database migration management REPL
└── app/di/                          # Dependency injection container
```

### Configuration Files
```
config/
├── base.edn                              # Main configuration with profile support (dev/test/prod)
├── .secrets.edn                         # Sensitive configuration (API keys, passwords)
└── .claude/                              # Claude Code skills, commands, and agent definitions
```

### Database Schema & Models
```
resources/db/
├── models.edn                            # Canonical database models definition
├── migrations/                             # Auto-generated database migrations
└── policies/                              # Row Level Security (RLS) policies
```

### Build & Development Configuration
```
deps.edn                                    # Clojure dependencies and aliases
shadow-cljs.edn                              # ClojureScript compilation configuration
bb.edn                                       # Babashka task definitions and development automation
scripts/                                      # Development scripts, database management, and deployment
```

### Testing Infrastructure
```
test/                                         # Test files mirroring src/ structure
├── backend/                                # Kaocha backend tests
├── frontend/                               # ClojureScript/JavaScript tests
└── karma.conf.js                           # Frontend test runner configuration
```

## Important Dependencies & Their Roles

### Backend Dependencies
- **Ring** (v1.14.2): HTTP server abstraction and middleware
- **next.jdbc** (v1.3.1048): Database connection and query building
- **HikariCP** (v3.2.0): Connection pooling and performance
- **PostgreSQL** (v42.7.7): Primary database with RLS support
- **aero** (v1.1.6): Configuration management with profile support
- **buddy** (v1.12.0): Security (password hashing, JWT)
- **reitit** (v0.9.1): Data validation and coercion
- **timbre** (v6.6.2): Structured logging
- **http-kit** (v2.8.0): HTTP client for external API calls

### Frontend Dependencies
- **Shadow-CLJS** (v3.1.7): ClojureScript compilation and hot reload
- **re-frame** (v1.4.3): Frontend state management and event handling
- **Uix** (v1.4.4): Modern ClojureScript UI library (React-like)
- **Tailwind CSS** (v4.0.0+): Utility-first CSS framework
- **DaisyUI** (v4.0.0+): Component library built on Tailwind

### Development & Testing Tools
- **Babashka** (v1.12.203): Fast Clojure scripting for automation
- **Kaocha** (v1.91.1392): Backend test runner
- **clj-kondo** (v2025.06.05): Static analysis and linting
- **cljfmt** (v0.9.2): Code formatting
- **Chrome DevTools MCP**: Browser automation and debugging integration
- **Playwright** (v1.52.0): E2E browser testing
- **Etaoin** (v1.1.43): Browser automation (alternative to Playwright)

## Available Tools/APIs with Usage Examples

### Babashka Development Tasks
```bash
# Start full development stack
bb run-app

# Run backend tests
bb be-test

# Run frontend tests
bb fe-test

# Database operations
bb backup-db --dev
bb restore-db --dev backup_file.sql
bb clean-db --dev

# Code quality
bb lint                    # Run clj-kondo linting
bb cljfmt-check           # Check formatting
bb cljfmt-fix             # Fix formatting issues

# Create new application from template
bb create-new-app my-invoice-app --title "Invoice Management System"
```

### Database Migration System
```clojure
;; In REPL
(require '[app.migrations.simple-repl :as mig])

;; Generate migrations from models
(mig/make-all-migrations!)

;; Apply migrations
(mig/migrate!)

;; Check migration status
(mig/status)

;; Apply to test environment
(mig/migrate! :test)
```

### Application APIs
```clojure
;; Backend service calls
(require '[app.backend.services.admin.users :as users])
(users/create-user! {:email "test@example.com" :full_name "Test User"})

;; Frontend re-frame events
(require '[re-frame.core :as rf])
(rf/dispatch [:admin/users/load])
(rf/dispatch [:admin/users/create {:email "new@example.com"}])

;; Authentication
(require '[app.backend.services.admin.auth :as auth])
(auth/authenticate-user! "admin@example.com" "password123")

;; Database queries with HoneySQL
(require '[app.shared.query-builders :as qb])
(qb/select :users {:where [:= :email "test@example.com"]})
```

### Frontend Component System
```clojure
;; Using Uix components (React-like)
(require '[uix.core :as uix])

($ :div {:class "p-4"}
  ($ :h1 {:class "text-2xl font-bold"} "Welcome")
  ($ :button {:class "ds-btn ds-btn-primary"
                :on-click #(rf/dispatch [:some-event])}
            "Click me"))

;; Re-frame subscriptions
(rf/subscribe [:admin/users]
  (fn [users-db]
    (println "Users updated:" users-db)))
```

## Overall Architecture & Component Interaction

### High-Level Architecture
```
┌─────────────────────────────────────────────────────────────────┐
│                    Web Browser (Chrome/Firefox)              │
├─────────────────────────────────────────────────────────────────┤
│                 Frontend (Shadow-CLJS + Uix)            │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ re-frame State Management + Event System        │    │
│  │ Uix UI Components (React-like)            │    │
│  │ Template Components (Auth, Forms, Tables)   │    │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│              Backend API (Ring + Reitit)               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ HTTP Routes + Middleware                  │    │
│  │ Business Services (Users, Auth, Audit)       │    │
│  │ Security (Rate Limiting, Admin Access)      │    │
│  │ Data Access Layer (HoneySQL + JDBC)        │    │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│           PostgreSQL Database with RLS                  │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Users, Admins, Audit Logs, Login Events     │    │
│  │ Row Level Security Policies                     │    │
│  │ Auto-generated Migrations                     │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Request Flow
1. **Browser Request** → Nginx/Apache → Ring Handler
2. **Security Middleware** → Rate limiting → Admin authentication → Database context
3. **Route Handler** → Business service → Database query → JSON response
4. **Frontend Event** → re-frame dispatcher → Event handler → State update → UI re-render

### Data Flow
- **Configuration**: Aero loads profile-specific config from `config/base.edn`
- **Database**: HikariCP connection pool → next.jdbc → PostgreSQL with prepared statements
- **State Management**: re-frame app-db with subscriptions for reactive UI updates
- **Security**: Multi-layer (Rate limiting → Admin auth → RLS policies in database)

### Admin Panel Structure
```
Frontend Routes (admin-only):
├── /admin/login          # Admin authentication
├── /admin/users          # User management (CRUD + bulk operations)
├── /admin/dashboard       # System overview and metrics
├── /admin/audit          # Activity logs and security monitoring
├── /admin/entities        # Generic entity management
└── /admin/subscription    # Subscription/billing management

Backend API Routes (/admin/api/*):
├── Authentication endpoints (login, logout, session management)
├── User management (CRUD, bulk operations, search)
├── Audit logging (login events, user actions, system changes)
├── Dashboard data (system stats, user counts, activity metrics)
└── Entity management (dynamic entity handling with validation)
```

## Implementation Patterns & Conventions

### Naming Conventions
- **Database**: `snake_case` for tables/columns (`users`, `created_at`)
- **Code**: `kebab-case` for namespaces and functions (`app.backend.services.admin`)
- **API Routes**: `/admin/api/*` for admin endpoints, RESTful resource patterns
- **Frontend Events**: `:domain/action` pattern (`:admin/users/create`, `:auth/login`)
- **Configuration**: Profile-based with EDN format (`:dev`, `:test`, `:prod`)

### Code Organization Patterns
- **Protocols-first**: Define interfaces in `protocols/` directories
- **Services Layer**: Business logic in `services/` with dependency injection
- **Middleware Composition**: Security and request processing in `middleware/`
- **CRUD Pattern**: Standard operations using `app.backend.crud.service` helpers
- **Event-Driven Frontend**: re-frame for state management with event handlers
- **Component Reuse**: Template components in `app.template.frontend.components/`

### Security Patterns
- **Admin Authentication**: Middleware-based with session management
- **Rate Limiting**: In-memory tracking with Redis/database fallback
- **Database RLS**: Row Level Security for tenant data isolation
- **Input Validation**: reitit schemas with Malli specifications
- **Audit Logging**: Comprehensive action and authentication event tracking

### Error Handling Patterns
- **Backend**: Ring middleware with exception handling and JSON error responses
- **Frontend**: re-frame effects for error states and user feedback
- **Database**: Transaction rollback with detailed error logging
- **API**: Consistent error response format with proper HTTP status codes

## Development Workflow Recommendations

### Local Development Setup
```bash
# 1. Start PostgreSQL services
brew services start postgresql@14

# 2. Clone and setup
git clone <repository>
cd single-tenant-template
npm install

# 3. Run database migrations
clojure -X:migrations-dev

# 4. Start development stack
bb run-app    # Starts backend + frontend + opens browser to localhost:8080
```

### Development Tools Integration
- **Hot Reload**: Automatic frontend compilation on file changes
- **REPL Integration**: Live code evaluation with `bb dev` or Clojure tools
- **Browser DevTools**: Chrome MCP integration for UI debugging
- **Test Watchers**: Auto-rerun tests on file changes
- **Code Quality**: Pre-commit hooks with cljfmt and clj-kondo

### Database Development Workflow
```bash
# Generate new migrations after model changes
bb clean-db --dev && clojure -X:migrations-dev

# Create seed data for testing
bb seed-admin

# Backup before major changes
bb backup-db --dev

# Test migrations on separate database
bb restore-db --test backup_file.sql
```

### Frontend Development Workflow
```bash
# Start frontend test runner
npm run test:cljs:watch

# Run Karma tests once
bb fe-test

# Admin panel development
open http://localhost:8085/admin/users
# Backend hot-reloads automatically
```

## Extension Points for Future Development

### New Domain Areas
**Pattern**: Create new domain under `src/app/` following existing structure:
```
src/app/your-domain/
├── backend/
│   ├── protocols.clj           # Domain-specific interfaces
│   ├── crud/service.clj       # Standard CRUD operations
│   ├── routes.clj              # HTTP route handlers
│   └── services/              # Business logic implementation
├── frontend/
│   ├── pages/                  # Re-frame page components
│   ├── components/             # Reusable UI components
│   ├── events.clj              # Domain-specific events
│   └── subs.cljs               # Data subscriptions
└── shared/
    ├── schemas.clj             # Domain data models
    ├── validation.clj          # Reitit validation schemas
    └── crud/                 # Shared CRUD utilities
```

### Adding New API Endpoints
1. **Define routes** in `src/app/your-domain/backend/routes.clj`
2. **Implement services** in `src/app/your-domain/backend/services/`
3. **Add event handlers** in `src/app/your-domain/frontend/events.clj`
4. **Create frontend components** in `src/app/your-domain/frontend/pages/`
5. **Register routes** in main route configuration

### Database Model Extensions
```clojure
;; Add to resources/db/models.edn
{:your-entity
 {:fields
  [[:id :uuid {:primary-key true}]
   [:name :varchar 255 {:null false}]
   [:description :text]
   [:created_at :timestamptz {:default "NOW()"}]
   [:updated_at :timestamptz {:default "NOW()"}]],
  :indexes
  [[:idx_your_entity_created_at :btree {:fields [:created_at]}]}}

;; Then regenerate migrations
clojure -X:migrations-dev
(mig/make-all-migrations!)
(mig/migrate!)
```

### Admin Panel Extensions
- **New Pages**: Add to `src/app/admin/frontend/pages/` following existing patterns
- **Navigation**: Update `src/app/template/frontend/routes.cljs` route table
- **API Integration**: Add corresponding backend routes under `/admin/api/`
- **Bulk Operations**: Use existing bulk operation patterns for efficiency

### Authentication Extensions
- **OAuth Providers**: Add new providers to `config/base.edn` and implement in `src/app/backend/routes/oauth.clj`
- **Role-based Access**: Extend RLS policies and middleware for fine-grained permissions
- **Session Management**: Enhance existing session handling in `src/app/backend/middleware/user.clj`

### Security Enhancements
- **Advanced Rate Limiting**: Multi-window rate limiting with Redis backend
- **API Key Authentication**: Add API key system for external integrations
- **Audit Trail Expansion**: More granular activity logging and reporting
- **Data Encryption**: Field-level encryption for sensitive data

### Monitoring & Observability
- **Metrics Integration**: Extend Micrometer metrics in `src/app/backend/middleware/rate_limiting.clj`
- **Health Checks**: Add comprehensive health check endpoints
- **Performance Monitoring**: Database query performance tracking
- **Error Tracking**: Integration with external error tracking services

### Testing Extensions
- **Integration Tests**: Add end-to-end test scenarios
- **Browser Tests**: Expand Etaoin/Playwright test coverage
- **Performance Tests**: Database and API performance benchmarking
- **Security Tests**: Authentication bypass and permission escalation testing

## Quick Reference

### Essential Commands
```bash
# Development
bb run-app              # Start full stack
bb be-test              # Backend tests
bb fe-test              # Frontend tests
bb lint                 # Code quality check

# Database
clojure -X:migrations-dev   # Migration REPL
bb backup-db --dev       # Backup dev database
bb clean-db --dev         # Clear all tables

# Production
bb build-prod           # Production build
./scripts/sh/deployment/deploy.sh  # Deploy (if present)
```

### Environment Profiles
- **:dev**: Development (localhost:8080, PostgreSQL:5432)
- **:test**: Testing (localhost:8081, PostgreSQL:5433)
- **:prod**: Production (environment-specific configuration)

### Port Allocation
- **Main App**: 8080 (development) / Environment-specific (production)
- **Admin Panel**: 8085 (development) / Same as main app in production
- **Shadow-CLJS**: 9630 (development, watch mode)
- **Test Runner**: 9095 (Karma test server)

### Multi-tenant to Single-tenant Migration Notes
This template removes:
- Multi-tenant routing and database switching
- Tenant-specific authentication and authorization
- Per-tenant subscription management
- Tenant isolation infrastructure and middleware

It retains:
- User role management (admin vs regular users)
- Subscription/billing framework structure
- Audit logging and security patterns
- Template-based component system for easy extension

## Documentation Index

Comprehensive documentation available in `/docs/` covering:
- **Architecture**: System design and component interaction
- **Backend**: API reference, security middleware, services
- **Frontend**: Component integration, state management, routing
- **Database**: Schema reference, migration patterns
- **Development**: Environment setup, testing, debugging
- **Operations**: Deployment, monitoring, maintenance

Refer to specific documentation files for detailed implementation guidance and examples.
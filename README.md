# Single-Tenant SaaS Template

This repository is the **single-tenant template** derived from the Hosting multi-tenant app. Use it as a clean starting point for building your own product.

## Quick Start

### Prerequisites

Before running the application, ensure you have all required software installed. See the [**Complete Software Requirements**](#complete-software-requirements) section below.

### 1. Database Setup

```bash
# Start PostgreSQL service
brew services start postgresql@14

# Create development database
psql -U postgres -c "CREATE DATABASE bookkeeping;"
psql -U postgres -c "CREATE USER user WITH PASSWORD 'password';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE bookkeeping TO user;"

# Create test database
psql -U postgres -c "CREATE DATABASE bookkeeping_test;"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE bookkeeping_test TO user;"
```

### 2. Project Setup

```bash
# Clone the repository
git clone <repository-url>
cd hosting

# Install Node.js dependencies
npm install

# Run database migrations
clojure -X:migrations-dev
```

### 3. Start Your Application Stack

```bash
# Terminal 1: Start your development environment
bb run-app
```

The application will be available at:
- **Main App**: http://localhost:8080
- **Admin Panel**: http://localhost:8080/admin/users (admin auth is simplified for this template)

## Complete Software Requirements

### Core Runtime Requirements

| Software | Version | Purpose | Installation |
|----------|---------|---------|--------------|
| **OpenJDK** | 17+ | Clojure JVM runtime | `brew install openjdk@17` |
| **Clojure CLI** | Latest | Clojure application runtime | `brew install clojure/tools/clojure` |
| **Babashka** | 1.12+ | Fast Clojure scripting | `brew install borkdave/brew/babashka` |

### Database Requirements

| Software | Version | Purpose | Installation |
|----------|---------|---------|--------------|
| **PostgreSQL** | 14+ | Primary database with RLS | `brew install postgresql@14` |
| **psql** | Latest | Database administration | Comes with PostgreSQL |

> **Note**: Two database instances are required:
> - Development: `localhost:5432`
> - Test: `localhost:5433`

### Frontend & Build Tools

| Software | Version | Purpose | Installation |
|----------|---------|---------|--------------|
| **Node.js** | 22.0.0+ | Frontend build tooling | `brew install node@22` |
| **Shadow-CLJS** | 2.28.18+ | ClojureScript compiler | `npm install -g shadow-cljs` |
| **PostCSS** | 8.5.6+ | CSS processing | Via npm (auto-installed) |
| **Tailwind CSS** | 4.0.0+ | CSS framework | Via npm (auto-installed) |

### Development Tools

| Software | Version | Purpose | Installation |
|----------|---------|---------|--------------|
| **Clojure-LSP** | Latest | Code intelligence & refactoring | `brew install clojure-lsp/brew/clojure-lsp` |
| **clj-kondo** | 2025.06.05+ | Clojure linter | `brew install borkdave/brew/clj-kondo` |
| **cljfmt** | 0.9.2+ | Code formatter | Included as dependency |
| **neil** | Latest | Dependency management | `brew install borkdave/brew/neil` |

### Testing Frameworks

| Software | Version | Purpose | Installation |
|----------|---------|---------|--------------|
| **Kaocha** | 1.91.1392+ | Backend test runner | Included as dependency |
| **Karma** | 6.4.3+ | Frontend test runner | Via npm (auto-installed) |
| **Chrome/Chromium** | Latest | Browser testing | Install Chrome browser |
| **Playwright** | 1.52.0+ | E2E testing | Via npm (auto-installed) |
| **Etaoin** | 1.1.43+ | Browser automation | Included as dependency |

### Optional Tools

| Software | Purpose | Installation |
|----------|---------|--------------|
| **Git** | Version control | `brew install git` |
| **pandoc** | Documentation generation | `brew install pandoc` |
| **Fish/Zsh** | Enhanced shell experience | Install your preferred shell |

## One-Command Installation (macOS)

```bash
# Core requirements
brew install openjdk@17 clojure/tools/clojure babashka postgresql@14 node@22

# Development tools
brew install clojure-lsp/brew/clojure-lsp borkdave/brew/clj-kondo borkdave/brew/neil

# Global tools
npm install -g shadow-cljs

# Optional tools
brew install git pandoc

# Start PostgreSQL
brew services start postgresql@14
```

## Development Workflow

### Available Babashka Tasks

The project includes comprehensive task automation via Babashka. Key commands:

```bash
# Application management
bb run-app              # Start the application

# Testing
bb be-test              # Run backend tests
bb fe-test-node         # Run frontend tests
bb commit --test        # Run tests then commit changes

# Code quality
bb lint                 # Run clj-kondo linting
bb cljfmt-check         # Check code formatting
bb cljfmt-fix           # Fix code formatting
bb fix-lint             # Auto-fix lint warnings

# Database management
bb backup-db --dev      # Backup development database
bb restore-db --dev <file>  # Restore from backup
bb clean-db --dev       # Clean database tables

# Project utilities
bb tasks-pretty         # Show available tasks nicely formatted
bb clean-cache          # Clear compilation caches
bb upgrade-deps         # Upgrade all dependencies
```

### Application Architecture (Template Scope)

- **Backend**: Clojure with Ring HTTP server and PostgreSQL; DI via `app.template.di.config`
- **Frontend**: ClojureScript with Shadow-CLJS and re-frame/uix
- **Admin UI**: `/admin/users` and related template pages are included; add your own admin pages under `app.admin.frontend.pages`
- **Database**: Starts from `resources/db/models.edn`; no Hosting/Financial/Integration domain tables
- **Authentication**: Simplified admin auth for single-tenant local use (nil treated as allowed in `auth-guard`)
- **Build/Tooling**: Babashka tasks, Shadow-CLJS, nREPL/Calva-friendly dev loop

### Troubleshooting

#### Common Issues

1. **Cache issues**: `bb clean-cache` to clear compilation caches
2. **Database connection**: Ensure PostgreSQL is running on ports 5432 and 5433
3. **Missing dependencies**: Run `npm install` and check Java/Clojure installation

#### Database Reset

```bash
# Completely reset development database
bb clean-restore-db --dev <backup-file.sql>

# Or clean and reinitialize
bb clean-db --dev
clojure -X:migrations-dev
```

### Configuration

Key configuration files:
- `deps.edn` - Clojure dependencies and aliases
- `shadow-cljs.edn` - ClojureScript build configuration
- `package.json` - Node.js dependencies and scripts
- `bb.edn` - Babashka task definitions
- `resources/db/models.edn` - Database schema definitions

### Documentation

- `docs/index.md` — Single-tenant template documentation entry point
- `docs/backend/single-tenant-template.md` — What this template includes and how to extend it
- `docs/operations/README.md` — Dev/startup/testing commands
- `docs/migrations/migration-overview.md` — Models/migrations workflow for this template
- `docs/frontend/app-shell.md` — Frontend/app shell overview (with admin bootstrap notes)
- `docs/archive` — Hosting/Financial/Integration domain docs kept as reference

## License

[Add your license information here]

# Create New App Script

This directory contains scripts for creating new Clojure applications based on the hosting app template.

## Quick Start

Create a new project using the bb task:

```bash
bb create-new-app my-new-project ~/Projects/
```

Or run the script directly:

```bash
./scripts/create-new-app.sh my-new-project ~/Projects/
```

## What Gets Created

The script creates a complete, ready-to-develop Clojure/ClojureScript application with:

### 🏗️ **Full Stack Architecture**
- **Backend**: Clojure 1.12.0 + Ring/Reitit + PostgreSQL
- **Frontend**: ClojureScript + re-frame 1.4.3 + UIX 1.4.4 + Tailwind
- **Database**: PostgreSQL with automated migrations
- **Testing**: Browser automation with Playwright + Etaoin

### 📁 **Complete Project Structure**
- `src/` - Application source code (176 namespaces)
- `test/` - Comprehensive test suite
- `config/` - Configuration files
- `resources/` - Static resources and database models
- `cli-tools/` - Browser automation testing tools
- `scripts/` - Build and utility scripts
- `dev/` - Development environment setup

### 🔧 **Development Tools**
- **Babashka Tasks**: Full build system with 20+ commands
- **Browser Testing**: 30+ pre-built test scripts
- **Hot Reloading**: CSS and ClojureScript live reload
- **Code Quality**: Linting, formatting, and security checks

### 📋 **Configuration Files**
- `deps.edn` - Clojure dependencies and aliases
- `bb.edn` - Babashka task definitions
- `shadow-cljs.edn` - ClojureScript build configuration
- `package.json` - Node.js dependencies
- `postcss.config.js` - CSS processing
- `tests.edn` - Test configuration
- `.gitignore` - Git ignore patterns
- `.secrets.edn` - Database configuration template

## Usage Examples

### Basic Usage
```bash
# Create in default location (../project-name)
bb create-new-app my-bookkeeping-app

# Create in specific directory
bb create-new-app invoicing-system ~/Development/
```

### After Creation
```bash
cd path/to/your-new-project

# 1. Update database credentials in .secrets.edn
# 2. Create PostgreSQL databases
# 3. Install Node.js dependencies
npm install

# 4. Start development
bb run-app              # Start backend (terminal 1)
npm run develop         # Start CSS compilation (terminal 2)

# 5. Access app at http://localhost:8080
```

## Available Commands in New Project

### Development
- `bb run-app` - Start the application server
- `bb be-test` - Run backend tests
- `bb fe-test` - Run frontend tests
- `bb scripts` - List available test scripts

### Build & Production
- `bb build-prod` - Complete production build
- `bb lint` - Run code linting
- `bb cljfmt-fix` - Fix code formatting

### Testing & Automation
- `bb script [name]` - Run browser test scripts
- `bb record-test` - Record browser interactions

### Dependencies & Maintenance
- `bb upgrade-deps` - Upgrade all dependencies
- `bb nvd-check` - Security vulnerability check

## Features Included

### 🎯 **Ready-to-Use Components**
- **50+ UI Components** organized by feature
- **Advanced Filtering System** (5 specialized files)
- **Form System** with validation (8 files)
- **List/Table System** with sorting and pagination
- **Settings System** for application configuration

### 🧪 **Comprehensive Testing**
- **30+ Test Scripts** for browser automation
- **Backend Tests** with Kaocha
- **Frontend Tests** with Shadow CLJS
- **Integration Tests** with real database

### 🚀 **Production Ready**
- **Docker Support** with docker-compose.yml
- **Database Migrations** with automigrate
- **Security Features** with Ring middleware
- **Performance Monitoring** with Micrometer
- **Structured Logging** with Timbre

## Template Features

The hosting app template provides:

### Backend Architecture
- **HTTP Kit** - High-performance web server
- **HoneySQL** - Composable SQL queries
- **Malli** - Data validation and schemas
- **HikariCP** - Database connection pooling

### Frontend Architecture
- **re-frame** - Predictable state management
- **UIX** - Modern React wrapper
- **Tailwind + DaisyUI** - Utility-first styling
- **Hot Reload** - Development productivity

### Development Experience
- **REPL-Driven Development** - Interactive coding
- **Browser Automation** - UI testing without manual work
- **Live Reload** - Instant feedback on changes
- **Comprehensive Tooling** - Everything needed for production

## Testing the Script

Test the script functionality:

```bash
# Run the test script
./scripts/test-create-app.sh
```

This creates a temporary project, verifies all essential files, and cleans up.

## Technical Details

### What Gets Copied
- Complete source code (176 namespaces, 194 files)
- All configuration files
- Development and testing tools
- Documentation and guides

### What Gets Customized
- Project name in `package.json`
- Database names in `.secrets.edn`
- Git repository initialization
- Script permissions

### Dependencies
- Clojure CLI
- Node.js and npm
- PostgreSQL
- Babashka
- Git

## Troubleshooting

### Common Issues

**Directory already exists**
```bash
rm -rf /path/to/existing-project
bb create-new-app project-name
```

**Permission errors**
```bash
chmod +x scripts/*.sh
chmod +x cli-tools/test_scripts/*.sh
```

**Database connection**
```bash
# Update .secrets.edn with correct credentials
# Ensure PostgreSQL is running
# Create the databases as specified
```

## Support

- **Documentation**: See generated README.md in new project
- **Examples**: Check cli-tools/test_scripts/ for usage patterns
- **Configuration**: Review config/base.edn for settings

The create-new-app script provides a complete, production-ready Clojure application template that includes everything needed for modern web development.

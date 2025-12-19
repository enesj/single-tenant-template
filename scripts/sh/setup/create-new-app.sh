#!/bin/bash
set -e

# Create New Clojure App Script
# This script creates a new Clojure application based on the hosting app template

# Show usage information
show_usage() {
    echo "Usage: ./scripts/create-new-app.sh <project-name> [options]"
    echo ""
    echo "Arguments:"
    echo "  project-name              Name for the project (used as package name and default db name)"
    echo ""
    echo "Options:"
    echo "  --title \"App Title\"        Application title (default: auto-generated from project name)"
    echo "  --db-name \"database\"       Database name (default: same as project name)"
    echo "  --package-name \"pkg\"       Package name (default: same as project name)"
    echo "  --target-dir \"/path\"       Target directory (default: ../project-name)"
    echo "  --config \"/path/file.edn\"  Read configuration from EDN file"
    echo "  --help                    Show this help message"
    echo ""
    echo "Configuration File (new-app-config.edn):"
    echo "  The script can read default settings from new-app-config.edn:"
    echo "  {:defaults {:title \"My Company App\""
    echo "             :db-name-suffix \"-db\""
    echo "             :package-name-prefix \"company-\""
    echo "             :target-dir \"~/Projects\"}}"
    echo ""
    echo "Examples:"
    echo "  # Basic usage"
    echo "  ./scripts/create-new-app.sh invoice-system"
    echo ""
    echo "  # With configuration file"
    echo "  ./scripts/create-new-app.sh invoice-system --config ./my-config.edn"
    echo ""
    echo "  # With custom title and database"
    echo "  ./scripts/create-new-app.sh invoice-system --title \"Invoice Management\" --db-name \"invoices\""
    echo ""
    echo "  # Full customization"
    echo "  ./scripts/create-new-app.sh my-app --title \"My Application\" --db-name \"myapp_db\" --package-name \"my-application\" --target-dir \"~/Projects\""
}

# Read configuration from EDN file
read_config_file() {
    local config_file="$1"
    if [ -f "$config_file" ]; then
        echo "📖 Reading configuration from: $config_file"
        # Use a simple parser for EDN (basic key-value extraction)
        CONFIG_TITLE=$(grep -o ':title[[:space:]]*"[^"]*"' "$config_file" | sed 's/:title[[:space:]]*"\([^"]*\)"/\1/')
        CONFIG_DB_SUFFIX=$(grep -o ':db-name-suffix[[:space:]]*"[^"]*"' "$config_file" | sed 's/:db-name-suffix[[:space:]]*"\([^"]*\)"/\1/')
        CONFIG_PACKAGE_PREFIX=$(grep -o ':package-name-prefix[[:space:]]*"[^"]*"' "$config_file" | sed 's/:package-name-prefix[[:space:]]*"\([^"]*\)"/\1/')
        CONFIG_TARGET_DIR=$(grep -o ':target-dir[[:space:]]*"[^"]*"' "$config_file" | sed 's/:target-dir[[:space:]]*"\([^"]*\)"/\1/')
        return 0
    else
        echo "⚠️  Configuration file not found: $config_file"
        return 1
    fi
}

# Interactive configuration
interactive_config() {
    echo ""
    echo "🔧 Interactive Configuration"
    echo "==========================="

    # Show current values
    echo ""
    echo "Current settings:"
    echo "  Project Name: $PROJECT_NAME"
    echo "  App Title: $APP_TITLE"
    echo "  Database Name: $DB_NAME"
    echo "  Package Name: $PACKAGE_NAME"
    echo "  Target Directory: $TARGET_BASE_DIR"
    echo ""

    # Ask if user wants to modify
    while true; do
        read -p "Do you want to modify these settings? (y/n): " yn
        case $yn in
            [Yy]* )
                echo ""
                echo "Enter new values (press Enter to keep current value):"

                # App Title
                echo ""
                read -p "App Title [$APP_TITLE]: " input_title
                if [ ! -z "$input_title" ]; then
                    APP_TITLE="$input_title"
                fi

                # Database Name
                echo ""
                read -p "Database Name [$DB_NAME]: " input_db
                if [ ! -z "$input_db" ]; then
                    DB_NAME="$input_db"
                fi

                # Package Name
                echo ""
                read -p "Package Name [$PACKAGE_NAME]: " input_package
                if [ ! -z "$input_package" ]; then
                    PACKAGE_NAME="$input_package"
                fi

                # Target Directory
                echo ""
                read -p "Target Directory [$TARGET_BASE_DIR]: " input_dir
                if [ ! -z "$input_dir" ]; then
                    TARGET_BASE_DIR="$input_dir"
                fi

                echo ""
                echo "✅ Configuration updated!"
                break
                ;;
            [Nn]* )
                echo "✅ Using current settings"
                break
                ;;
            * )
                echo "Please answer yes or no."
                ;;
        esac
    done
}

# Check if project name is provided
if [ -z "$1" ] || [ "$1" = "--help" ]; then
    show_usage
    exit 1
fi

# Parse arguments
PROJECT_NAME="$1"
shift

# Default values
APP_TITLE=""
DB_NAME="$PROJECT_NAME"
PACKAGE_NAME="$PROJECT_NAME"
TARGET_BASE_DIR="$(pwd)/../"
CONFIG_FILE=""
USE_INTERACTIVE=false

# Configuration file variables
CONFIG_TITLE=""
CONFIG_DB_SUFFIX=""
CONFIG_PACKAGE_PREFIX=""
CONFIG_TARGET_DIR=""

# Parse options
while [[ $# -gt 0 ]]; do
    case $1 in
        --title)
            APP_TITLE="$2"
            shift 2
            ;;
        --db-name)
            DB_NAME="$2"
            shift 2
            ;;
        --package-name)
            PACKAGE_NAME="$2"
            shift 2
            ;;
        --target-dir)
            TARGET_BASE_DIR="$2"
            shift 2
            ;;
        --config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        --interactive)
            USE_INTERACTIVE=true
            shift
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            echo "❌ Error: Unknown option $1"
            show_usage
            exit 1
            ;;
    esac
done

# Check for default config file if none specified
if [ -z "$CONFIG_FILE" ] && [ -f "new-app-config.edn" ]; then
    CONFIG_FILE="new-app-config.edn"
fi

# Read configuration from file if available
if [ ! -z "$CONFIG_FILE" ]; then
    if read_config_file "$CONFIG_FILE"; then
        # Apply configuration values if not overridden by command line
        if [ -z "$APP_TITLE" ] && [ ! -z "$CONFIG_TITLE" ]; then
            APP_TITLE="$CONFIG_TITLE"
        fi

        if [ "$DB_NAME" = "$PROJECT_NAME" ] && [ ! -z "$CONFIG_DB_SUFFIX" ]; then
            DB_NAME="${PROJECT_NAME}${CONFIG_DB_SUFFIX}"
        fi

        if [ "$PACKAGE_NAME" = "$PROJECT_NAME" ] && [ ! -z "$CONFIG_PACKAGE_PREFIX" ]; then
            PACKAGE_NAME="${CONFIG_PACKAGE_PREFIX}${PROJECT_NAME}"
        fi

        if [ "$TARGET_BASE_DIR" = "$(pwd)/../" ] && [ ! -z "$CONFIG_TARGET_DIR" ]; then
            # Expand tilde
            CONFIG_TARGET_DIR="${CONFIG_TARGET_DIR/#\~/$HOME}"
            TARGET_BASE_DIR="$CONFIG_TARGET_DIR"
        fi

        echo "✅ Applied configuration from: $CONFIG_FILE"

        # Ask user if they want to use these settings or modify them
        echo ""
        echo "🔧 Configuration loaded from: $CONFIG_FILE"
        echo "   Project Name: $PROJECT_NAME"
        echo "   App Title: $APP_TITLE"
        echo "   Database Name: $DB_NAME"
        echo "   Package Name: $PACKAGE_NAME"
        echo "   Target Directory: $TARGET_BASE_DIR"
        echo ""

        while true; do
            read -p "Do you want to use these settings or modify them interactively? (use/modify): " choice
            case $choice in
                [Uu]se|[Uu]* )
                    echo "✅ Using configuration from file"
                    break
                    ;;
                [Mm]odify|[Mm]* )
                    USE_INTERACTIVE=true
                    break
                    ;;
                * )
                    echo "Please answer 'use' or 'modify'"
                    ;;
            esac
        done
    fi
fi

# Generate app title if not provided
if [ -z "$APP_TITLE" ]; then
    APP_TITLE=$(echo "$PROJECT_NAME" | sed 's/-/ /g' | awk '{for(i=1;i<=NF;i++)sub(/./,toupper(substr($i,1,1)),$i)}1')
fi

# Run interactive configuration if requested
if [ "$USE_INTERACTIVE" = true ]; then
    interactive_config
fi

TARGET_DIR="$TARGET_BASE_DIR/$PROJECT_NAME"
TEMPLATE_DIR="$(pwd)"

echo "🚀 Creating new Clojure application: $PROJECT_NAME"
echo "📁 Target directory: $TARGET_DIR"
echo "📋 Template directory: $TEMPLATE_DIR"

# Check if target directory already exists
if [ -d "$TARGET_DIR" ]; then
    echo "❌ Error: Directory $TARGET_DIR already exists"
    echo "Please choose a different project name or remove the existing directory"
    exit 1
fi

# Create target directory
echo "📁 Creating project directory..."
mkdir -p "$TARGET_DIR"

# Copy essential project structure
echo "📋 Copying project template..."

# Copy configuration files
cp "$TEMPLATE_DIR/deps.edn" "$TARGET_DIR/"
cp "$TEMPLATE_DIR/bb.edn" "$TARGET_DIR/"
cp "$TEMPLATE_DIR/shadow-cljs.edn" "$TARGET_DIR/"
cp "$TEMPLATE_DIR/package.json" "$TARGET_DIR/"
cp "$TEMPLATE_DIR/build.clj" "$TARGET_DIR/" 2>/dev/null || echo "ℹ️  build.clj not found, skipping..."

# Copy CSS and styling configuration
cp "$TEMPLATE_DIR/postcss.config.js" "$TARGET_DIR/" 2>/dev/null || echo "ℹ️  postcss.config.js not found, skipping..."

# Copy other configuration files
cp "$TEMPLATE_DIR/tests.edn" "$TARGET_DIR/" 2>/dev/null || echo "ℹ️  tests.edn not found, skipping..."
cp "$TEMPLATE_DIR/.gitignore" "$TARGET_DIR/" 2>/dev/null || echo "ℹ️  .gitignore not found, skipping..."
cp "$TEMPLATE_DIR/docker-compose.yml" "$TARGET_DIR/" 2>/dev/null || echo "ℹ️  docker-compose.yml not found, skipping..."

# Copy essential directories
echo "📂 Copying source directories..."
cp -r "$TEMPLATE_DIR/src" "$TARGET_DIR/"
cp -r "$TEMPLATE_DIR/config" "$TARGET_DIR/"
cp -r "$TEMPLATE_DIR/resources" "$TARGET_DIR/"
cp -r "$TEMPLATE_DIR/test" "$TARGET_DIR/"
cp -r "$TEMPLATE_DIR/scripts" "$TARGET_DIR/"
cp -r "$TEMPLATE_DIR/cli-tools" "$TARGET_DIR/"

# Copy development directory if it exists
if [ -d "$TEMPLATE_DIR/dev" ]; then
    cp -r "$TEMPLATE_DIR/dev" "$TARGET_DIR/"
fi

# Copy vendor directory if it exists
if [ -d "$TEMPLATE_DIR/vendor" ]; then
    cp -r "$TEMPLATE_DIR/vendor" "$TARGET_DIR/"
fi

# Copy documentation (skip original README, always create custom one)
echo "📚 Copying documentation..."
cp "$TEMPLATE_DIR/PROJECT_SUMMARY.md" "$TARGET_DIR/" 2>/dev/null || echo "ℹ️  PROJECT_SUMMARY.md not found, skipping..."
cp "$TEMPLATE_DIR/CLAUDE.md" "$TARGET_DIR/" 2>/dev/null || echo "ℹ️  CLAUDE.md not found, skipping..."

# Always create custom README for new project
echo "📝 Creating README.md with project-specific instructions..."
cat > "$TARGET_DIR/README.md" << EOF
# $APP_TITLE

A full-stack Clojure/ClojureScript application built with modern tools and practices.

## 🚀 Quick Start

### Prerequisites

Make sure you have the following installed:

- **Clojure CLI** (1.10.0 or later) - [Installation Guide](https://clojure.org/guides/install_clojure)
- **Node.js** (16+ recommended) and **npm** - [Download](https://nodejs.org/)
- **PostgreSQL** (13+ recommended) - [Download](https://www.postgresql.org/download/)
- **Babashka** (latest) - [Installation Guide](https://github.com/babashka/babashka#installation)

### Installation & Setup

1. **Clone and enter the project:**
   \`\`\`bash
   cd $PROJECT_NAME
   \`\`\`

2. **Install Node.js dependencies:**
   \`\`\`bash
   npm install
   \`\`\`

3. **Set up your database:**

   **Option A: Using Docker (Recommended)**
   \`\`\`bash
   # Start PostgreSQL with Docker Compose
   docker-compose up -d
   \`\`\`

   **Option B: Local PostgreSQL**
   \`\`\`bash
   # Create databases (adjust credentials as needed)
   createdb $DB_NAME
   createdb ${DB_NAME}-test
   \`\`\`

4. **Configure database connection:**

   Update \`.secrets.edn\` with your database credentials:
   \`\`\`clojure
   {:database {:dbtype "postgresql"
               :host "localhost"
               :port 5432
               :dbname "$DB_NAME"
               :user "your-username"
               :password "your-password"}
    :test-database {:dbtype "postgresql"
                    :host "localhost"
                    :port 5433
                    :dbname "${DB_NAME}-test"
                    :user "your-username"
                    :password "your-password"}}
   \`\`\`

5. **Start the development environment:**

   **Terminal 1 - Backend Server:**
   \`\`\`bash
   bb run-app
   \`\`\`

   **Terminal 2 - Frontend Development:**
   \`\`\`bash
   npm run develop
   \`\`\`

6. **Access your application:**

   Open your browser to [http://localhost:8080](http://localhost:8080)

## 🛠️ Development Commands

### Core Commands
- \`bb run-app\` - Start the application server
- \`npm run develop\` - Start CSS compilation with hot reload
- \`bb be-test\` - Run backend tests
- \`bb fe-test\` - Run frontend tests
- \`bb build-prod\` - Build for production

### Testing & Quality
- \`bb scripts\` - List all available test scripts
- \`bb script [name]\` - Run a specific browser automation test
- \`bb lint\` - Run code linting
- \`bb cljfmt-fix\` - Fix code formatting

### Database Management
- `(mig/migrate!)` - Run database migrations via REPL
- `(mig/migrate! :test)` - Run database migrations for test environment

### Useful Development Tools
- \`bb upgrade-deps\` - Upgrade all dependencies
- \`bb nvd-check\` - Check for security vulnerabilities
- \`bb kill-java\` - Kill all Java processes (useful when things get stuck)

## 🏗️ Architecture Overview

This application follows modern Clojure/ClojureScript best practices:

### Backend (Clojure)
- **HTTP Kit** - High-performance web server
- **Ring/Reitit** - HTTP routing and middleware
- **PostgreSQL** - Database with connection pooling (HikariCP)
- **HoneySQL** - Composable SQL queries
- **Malli** - Data validation and schema definition

### Frontend (ClojureScript)
- **re-frame** - Predictable state management
- **UIX** - Modern React wrapper for ClojureScript
- **Shadow CLJS** - Build tool with hot reloading
- **Tailwind + DaisyUI** - Utility-first CSS framework

### Development Tools
- **Browser Automation** - Comprehensive UI testing with Playwright
- **Hot Reloading** - Instant feedback for both CSS and ClojureScript
- **REPL-Driven Development** - Interactive development experience

## 📁 Project Structure

\`\`\`
$PROJECT_NAME/
├── src/app/
│   ├── backend/           # Clojure backend code
│   ├── frontend/          # ClojureScript frontend code
│   └── shared/            # Shared Clojure/ClojureScript code
├── test/                  # Test files
├── config/                # Configuration files
├── resources/
│   ├── db/               # Database models and migrations
│   └── public/           # Static assets
├── cli-tools/            # Browser automation and testing tools
├── scripts/              # Build and utility scripts
├── dev/                  # Development environment setup
└── vendor/               # Vendored dependencies
\`\`\`

## 🧪 Testing

### Backend Tests
\`\`\`bash
bb be-test
\`\`\`

### Frontend Tests
\`\`\`bash
bb fe-test
\`\`\`

### Database Migrations
Database migrations are run via the REPL for safety and visibility.

```clojure
(require '[app.template.backend.migrations.simple-repl :as mig])
(mig/migrate!)
```

### Browser Automation Tests
\`\`\`bash
# List available test scripts
bb scripts

# Run a specific test
bb script navigation_test
bb script full_workflow
\`\`\`

## 🚀 Production Deployment

### Build for Production
\`\`\`bash
bb build-prod
\`\`\`

This creates:
- \`target/${PROJECT_NAME}-standalone.jar\` - Executable JAR file
- Optimized CSS and JavaScript assets
- All dependencies bundled

### Running in Production
\`\`\`bash
# Basic execution
java -jar target/${PROJECT_NAME}-standalone.jar

# With custom configuration
java -Daero.profile=production -jar target/${PROJECT_NAME}-standalone.jar

# With JVM tuning
java -Xmx2g -Xms1g -jar target/${PROJECT_NAME}-standalone.jar
\`\`\`

## 🔧 Configuration

### Environment Profiles
- \`dev\` - Development (default)
- \`test\` - Testing
- \`production\` - Production

### Key Configuration Files
- \`config/base.edn\` - Main application configuration
- \`.secrets.edn\` - Database credentials (not in git)
- \`deps.edn\` - Clojure dependencies and aliases
- \`shadow-cljs.edn\` - ClojureScript build configuration
- \`docker-compose.yml\` - PostgreSQL setup

## 📚 Learning Resources

### Clojure/ClojureScript
- [Clojure.org](https://clojure.org/) - Official Clojure documentation
- [ClojureScript.org](https://clojurescript.org/) - ClojureScript guide
- [re-frame](https://day8.github.io/re-frame/) - Frontend state management

### Development Tools
- [Shadow CLJS User Guide](https://shadow-cljs.github.io/docs/UsersGuide.html)
- [Babashka Book](https://book.babashka.org/) - Scripting with Clojure

## 🤝 Contributing

1. Make sure all tests pass: \`bb be-test && bb fe-test\`
2. Format code: \`bb cljfmt-fix\`
3. Run linting: \`bb lint\`
4. Test browser automation: \`bb script full_workflow\`

## 📄 License

This project was generated from the hosting app template.

---

**Generated with ❤️ using the hosting app template**
EOF

# Update project-specific configurations
echo "🔧 Updating project-specific configurations..."
echo "📋 Using configuration:"
echo "   Project Name: $PROJECT_NAME"
echo "   App Title: $APP_TITLE"
echo "   Database Name: $DB_NAME"
echo "   Package Name: $PACKAGE_NAME"

# Update package.json with new package name
if [ -f "$TARGET_DIR/package.json" ]; then
    python3 -c "
import json
import sys

try:
    with open('$TARGET_DIR/package.json', 'r') as f:
        data = json.load(f)

    data['name'] = '$PACKAGE_NAME'

    with open('$TARGET_DIR/package.json', 'w') as f:
        json.dump(data, f, indent=2)

    print('✅ Updated package.json name field')
except Exception as e:
    print(f'⚠️  Could not update package.json: {e}')
    sys.exit(0)
"
fi

# Update HTML title
if [ -f "$TARGET_DIR/resources/public/index.html" ]; then
    sed -i '' "s/Bookkeeping App/$APP_TITLE/g" "$TARGET_DIR/resources/public/index.html"
    echo "✅ Updated HTML title to '$APP_TITLE'"
fi

# Update database names in config/base.edn
if [ -f "$TARGET_DIR/config/base.edn" ]; then
    sed -i '' "s/bookkeeping-test/${DB_NAME}-test/g" "$TARGET_DIR/config/base.edn"
    sed -i '' "s/bookkeeping/$DB_NAME/g" "$TARGET_DIR/config/base.edn"
    echo "✅ Updated database names in config/base.edn"
fi

# Update database init fallback name
if [ -f "$TARGET_DIR/src/app/backend/db/init.clj" ]; then
    sed -i '' "s/\"bookkeeping\"/\"$DB_NAME\"/g" "$TARGET_DIR/src/app/backend/db/init.clj"
    echo "✅ Updated database init fallback name"
fi

# Update docker-compose.yml with new database names
if [ -f "$TARGET_DIR/docker-compose.yml" ]; then
    sed -i '' "s/POSTGRES_DB: bookkeeping-test/POSTGRES_DB: ${DB_NAME}-test/g" "$TARGET_DIR/docker-compose.yml"
    sed -i '' "s/POSTGRES_DB: bookkeeping/POSTGRES_DB: $DB_NAME/g" "$TARGET_DIR/docker-compose.yml"
    echo "✅ Updated docker-compose.yml database names"
fi

# Create .secrets.edn template
echo "🔐 Creating .secrets.edn template..."
cat > "$TARGET_DIR/.secrets.edn" << EOF
{:database {:dbtype "postgresql"
            :host "localhost"
            :port 5432
            :dbname "$DB_NAME"
            :user "user"
            :password "password"}
 :test-database {:dbtype "postgresql"
                 :host "localhost"
                 :port 5433
                 :dbname "${DB_NAME}-test"
                 :user "user"
                 :password "password"}}
EOF

# Update deps.edn paths if needed
echo "🔧 Ensuring deps.edn paths are correct..."
# The paths should already be correct from the template copy

# Make scripts executable
echo "🔧 Making scripts executable..."
chmod +x "$TARGET_DIR/scripts"/*.sh 2>/dev/null || echo "ℹ️  No shell scripts found in scripts directory"
chmod +x "$TARGET_DIR/cli-tools/test_scripts"/*.sh 2>/dev/null || echo "ℹ️  No test scripts found"

# Initialize git repository
echo "📝 Initializing git repository..."
cd "$TARGET_DIR"
git init
git add .
git commit -m "Initial commit: New Clojure app based on hosting template

🚀 Generated with hosting app template
📁 Project: $PROJECT_NAME
🎯 Ready for development

Features included:
- Full-stack Clojure/ClojureScript setup
- re-frame + UIX frontend
- Ring/Reitit backend
- PostgreSQL database integration
- Browser automation testing
- Comprehensive build system
- Development tooling"

echo ""
echo "🎉 Successfully created new Clojure application: $PROJECT_NAME"
echo ""
echo "📍 Next steps:"
echo "1. cd $TARGET_DIR"
echo "2. Update .secrets.edn with your database credentials"
echo "3. Create your PostgreSQL databases:"
echo "   - CREATE DATABASE \"$PROJECT_NAME\";"
echo "   - CREATE DATABASE \"${PROJECT_NAME}-test\";"
echo "4. Install Node.js dependencies: npm install"
echo "5. Start development: bb run-app"
echo "6. In another terminal: npm run develop"
echo "7. Access your app at http://localhost:8080"
echo ""
echo "🔧 Available commands:"
echo "  bb run-app     - Start the application"
echo "  bb be-test     - Run backend tests"
echo "  bb fe-test     - Run frontend tests"
echo "  bb build-prod  - Build for production"
echo "  bb deploy-prod - Deploy to production"
echo "  bb scripts     - List test scripts"
echo ""
echo "📖 For more information, see README.md in the project directory"
echo "✅ Happy coding! 🚀"

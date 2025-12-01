# Project Management Scripts

This folder contains scripts for project creation, automation, and general project management tasks.

## Scripts

### Project Creation
- **create-new-app.clj** - Enhanced Clojure project creation script with proper EDN parsing
- **README-create-new-app.md** - Documentation for the project creation process

### Automation
- **auto_commit.clj** - Automated commit creation based on project changes

### Utilities
- **file_combiner.clj** - Combines multiple files for analysis or processing
- **md-to-pdf.clj** - Converts Markdown files to PDF format

## Usage Examples

```bash
# Create a new Clojure application
bb project-management/create-new-app.clj my-new-app

# Combine files for analysis
bb project-management/file_combiner.clj src/**/*.clj > combined-source.txt

# Convert documentation to PDF
bb project-management/md-to-pdf.clj README.md
```

## Project Creation Features

The `create-new-app.clj` script provides:
- Proper EDN configuration parsing
- Database setup with multi-tenant support
- Docker Compose configuration
- Development environment setup
- Babashka task configuration

## Automation Features

The automation scripts help with:
- Consistent commit messages
- File processing workflows
- Documentation generation

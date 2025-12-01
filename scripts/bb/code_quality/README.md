# Code Quality Scripts

This folder contains scripts for improving and maintaining code quality in the Clojure codebase.

## Scripts

### Linting & Fixes
- **fix_lint_warnings.clj** - Automatically fixes simple lint warnings like unused bindings and imports
- **fix_misplaced_docstrings.clj** - Fixes misplaced docstrings in function definitions
- **fix_str_warnings.clj** - Fixes string-related linting warnings

### Formatting
- **format_edn.clj** - Formats EDN configuration files with proper indentation

## Usage Examples

```bash
# Fix lint warnings automatically
bb code-quality/fix_lint_warnings.clj

# Fix docstring placement issues
bb code-quality/fix_misplaced_docstrings.clj

# Format EDN files
bb code-quality/format_edn.clj config/base.edn
```

## Integration

These scripts work with the project's linting setup using clj-kondo and can be run as part of the development workflow to maintain consistent code quality.

## Automation

Consider running these scripts as part of:
- Pre-commit hooks
- CI/CD pipeline
- Regular code maintenance tasks

# Testing Scripts

Scripts for testing API endpoints and application functionality.

## Scripts

### `test-api-v1.sh`
Quick verification script for API v1 endpoints to ensure the backend is running correctly.

**Usage:**
```bash
./test-api-v1.sh
```

**Tests:**
- Health endpoint (`/api/v1/health`)
- Config endpoint (`/api/v1/config`)
- Items endpoint (`/api/v1/entities/items`)
- Provides helpful feedback if endpoints return 404

### `test-create-app.sh`
Comprehensive test suite for the `create-new-app` functionality. Creates a test project and validates all components.

**Usage:**
```bash
./test-create-app.sh
```

**Validates:**
- Essential files and directories creation
- Git repository initialization
- Project customizations (HTML title, package.json, database config)
- Configuration file updates
- Automatic cleanup after testing

### `test-enhanced-create-app.sh`
Advanced test suite for the enhanced `create-new-app` implementation with additional validation features.

**Usage:**
```bash
./test-enhanced-create-app.sh
```

**Tests:**
- Basic project creation with validation
- Configuration file parsing (EDN format)
- EDN file validation system
- Configuration manifest file updates
- Invalid EDN handling
- Multiple project creation scenarios

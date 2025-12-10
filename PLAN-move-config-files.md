# Plan: Move Configuration Files into frontend/config

## Date: 2025-12-10

## Objective
Document the migration of admin UI configuration files into `src/app/admin/frontend/config` to co-locate configuration with the frontend code that uses it, improving code organization and maintainability.

## Current State Analysis

### Configuration Files Location
- **Current (post-move)**: `src/app/admin/frontend/config/`
  - `form-fields.edn` - Form field definitions and validation rules
  - `table-columns.edn` - Table column configurations and display options
  - `view-options.edn` - View-specific settings and options

- **Target**: `src/app/admin/frontend/config/`
  - Already contains: `entities.edn`, `loader.cljs`, `preload.cljs`

### How Configuration Is Loaded

1. **Preloading (Build-time)**:
   - `preload.cljs` uses `shadow.resource/inline` to embed configs at compile time
   - Provides immediate access to configs before async loading completes
   - Now references: `"app/admin/frontend/config/*.edn"`

2. **Runtime Loading**:
   - `loader.cljs` fetches configs via the authenticated admin API (`/admin/api/settings` family)
   - Falls back to preloaded configs while async loading
   - Caches configs in memory for performance

3. **Backend API**:
   - `settings.clj` reads/writes configs from `src/app/admin/frontend/config/`
   - Provides admin API to modify configurations
   - Paths are hardcoded to current location

### Key Dependencies

1. **Frontend Dependencies**:
   - `app.admin.frontend.config/preload.cljs` - Build-time preloading
   - `app.admin.frontend.config/loader.cljs` - Runtime loading
   - Shadow-CLJS resource inlining

2. **Backend Dependencies**:
   - `app.backend.routes.admin.settings` - Settings API
   - File system paths for reading/writing

3. **External References**:
   - Documentation files referencing the old path
   - API documentation showing endpoints

## Implementation Steps

### Phase 1: Prepare New Configuration Structure
1. Create new configuration files in `src/app/admin/frontend/config/`
2. Copy existing configurations from `src/app/admin/frontend/config/`
3. Update file structure to align with frontend conventions

### Phase 2: Update Frontend Loading
1. Modify `preload.cljs` to reference new file paths
2. Update `loader.cljs` to load from new location
3. Ensure backward compatibility during transition

### Phase 3: Update Backend Integration
1. Modify backend settings API to read from new location
2. Update file path constants in `settings.clj`
3. Ensure admin settings page continues working

### Phase 4: Update Documentation
1. Update all documentation referencing old paths
2. Update API documentation
3. Update developer guides and READMEs

### Phase 5: Migration and Cleanup
1. Run tests to ensure everything works
2. Remove old configuration files
3. Clean up any unused imports or references

## Detailed Tasks

### 1. File Movement and Updates
- [ ] Move `form-fields.edn` to `src/app/admin/frontend/config/`
- [ ] Move `table-columns.edn` to `src/app/admin/frontend/config/`
- [ ] Move `view-options.edn` to `src/app/admin/frontend/config/`
- [ ] Update resource references in `preload.cljs`
- [ ] Update fetch URLs in `loader.cljs`

### 2. Backend Path Updates
- [ ] Update `view-options-path` constant in `settings.clj`
- [ ] Update `form-fields-path` constant in `settings.clj`
- [ ] Update `table-columns-path` constant in `settings.clj`
- [ ] Test backend API endpoints

### 3. Frontend Integration
- [ ] Test preloaded configuration loading
- [ ] Test runtime configuration fetching
- [ ] Verify configuration caching works
- [ ] Test admin settings page functionality

### 4. Documentation Updates
- [ ] Update `docs/frontend/admin-settings.md`
- [ ] Update `docs/backend/http-api.md`
- [ ] Update any other documentation with old paths
- [ ] Update inline code comments

### 5. Testing and Validation
- [ ] Run frontend tests: `npm run test:cljs`
- [ ] Run backend tests: `bb be-test`
- [ ] Manually test admin UI
- [ ] Test configuration persistence

## Risk Mitigation

1. **Backward Compatibility**:
   - Implement fallback mechanism to check old location if new location doesn't exist
   - Keep old files during initial deployment

2. **Build Process**:
   - Ensure Shadow-CLJS can still inline resources from new location
   - Test both development and production builds

3. **Runtime Loading**:
   - Verify new paths are accessible from the browser
   - Update any static file serving configuration if needed

## Expected Benefits

1. **Better Organization**: Configuration co-located with code that uses it
2. **Improved Maintainability**: Easier to find and update configurations
3. **Clearer Separation**: Frontend config separated from public assets
4. **Better Developer Experience**: Faster navigation between code and config

## Rollback Plan

If issues arise:
1. Keep original files in place until migration is complete
2. Use feature flags to switch between old/new locations
3. Monitor error logs for configuration loading issues
4. Quickly revert path changes if needed

## Success Criteria

1. All configuration files successfully moved to new location
2. Admin UI loads and functions correctly
3. Settings API can read/write configurations
4. All tests pass
5. No runtime errors in browser console
6. Documentation updated and accurate

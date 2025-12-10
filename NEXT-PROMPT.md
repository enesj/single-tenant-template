# NEXT-PROMPT: Move Configuration Files Implementation

**Date**: 2025-12-10
**Task**: Move configuration files from `resources/public/admin/ui-config` to `src/app/admin/frontend/config`

## Context Snapshot

- Single-tenant SaaS template built with Clojure/ClojureScript and PostgreSQL
- Admin panel served at `http://localhost:8085`
- Configuration currently in `resources/public/admin/ui-config/`:
  - `form-fields.edn` - Form field definitions
  - `table-columns.edn` - Table column configurations
  - `view-options.edn` - View-specific settings
- Target location: `src/app/admin/frontend/config/` (already has `entities.edn`, `loader.cljs`, `preload.cljs`)

## Code Map

### Frontend Files to Modify
- `src/app/admin/frontend/config/preload.cljs` - Build-time config preloading (lines 44, 54, 60)
- `src/app/admin/frontend/config/loader.cljs` - Runtime config loading (line 64)
- `src/app/admin/frontend/config/loader.cljs` - Fetch URLs (lines 82-84)

### Backend Files to Modify
- `src/app/backend/routes/admin/settings.clj` - File path constants (lines 10-12)
- `src/app/backend/routes/admin/settings.clj` - All read/write functions

### Documentation to Update
- `docs/frontend/admin-settings.md`
- `docs/backend/http-api.md`
- Any other docs referencing `/admin/ui-config`

## Commands to Run

### Development
```bash

# Frontend testing
npm run test:cljs

# Backend testing
bb be-test

# Build verification
bb build:admin
```

### Manual Testing
```bash
# Check admin UI loads
open http://localhost:8085

# Test settings page
open http://localhost:8085/admin/settings
```

## Implementation Approach

### Key Insights
1. **Dual Loading Mechanism**: Configs are both preloaded at build time (via shadow.resource) and loaded at runtime (via fetch)
2. **Backend API**: Settings API directly reads/writes these files from filesystem
3. **Resource Pathing**: Moving from public assets to source directory affects both build and runtime access

### Critical Steps

1. **Phase 1 - Move Files**
   ```bash
   # Copy configs to new location
   cp resources/public/admin/ui-config/*.edn src/app/admin/frontend/config/
   ```

2. **Phase 2 - Update Preloader**
   - Change resource paths from `"public/admin/ui-config/*.edn"` to `"app/admin/frontend/config/*.edn"`
   - Shadow-CLJS will inline from source paths

3. **Phase 3 - Update Runtime Loader**
   - Change fetch URLs from `/admin/ui-config/*.edn` to new endpoint or serve from new location
   - May need to update static file serving

4. **Phase 4 - Update Backend**
   - Change file paths from `resources/public/admin/ui-config/*.edn` to `src/app/admin/frontend/config/*.edn`
   - Test all settings API endpoints

5. **Phase 5 - Testing**
   - Verify preloaded configs available immediately
   - Verify runtime async loading still works
   - Test admin settings page can modify configs
   - Check all entity pages still render correctly

## Gotchas

1. **Shadow-CLJS Resources**: Resource inlining works with source paths, not resources/public
2. **Runtime Access**: Files moved to src/ may not be accessible via HTTP - might need copy to resources during build
3. **Backend File Access**: Backend runs from project root, can access src/ directory directly
4. **Path Consistency**: Ensure preloader and loader use consistent paths
5. **Build Artifacts**: May need to update build process to copy configs to resources/public

## Checklist

- [ ] Copy configuration files to new location
- [ ] Update preload.cljs resource paths
- [ ] Update loader.cljs fetch URLs
- [ ] Update backend file path constants
- [ ] Test frontend loads configurations
- [ ] Test backend settings API
- [ ] Run all tests
- [ ] Update documentation
- [ ] Clean up old files after verification

## Additional Notes

- The `entities.edn` file is already in the target location - this shows the pattern works
- Consider implementing a migration script or build step to copy configs during development
- Monitor browser console for configuration loading errors
- The admin UI should continue working without interruption if done correctly

## Expected Outcome

After implementation:
- All configuration files will be in `src/app/admin/frontend/config/`
- Frontend will load configs from new location (both preload and runtime)
- Backend settings API will read/write from new location
- Admin UI will function normally
- Better code organization with configs co-located with frontend code

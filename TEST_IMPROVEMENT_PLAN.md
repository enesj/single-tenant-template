# Test Improvement Plan - December 18, 2025

This document outlines the identified gaps in the test suites following today's extensive refactoring and modularization efforts. It provides a roadmap for improving test reliability and coverage.

## 1. Backend Test Stability (Critical)

The backend integration tests currently exhibit intermittent failures due to how functions are stubbed.

### Problem
In `test/app/backend/test_helpers.clj`, the `build-handler` function uses `with-redefs` to stub database-hitting functions. However, `with-redefs` only changes the root binding for the duration of the `with-redefs` block. Since `build-handler` returns a handler that is executed *outside* this block in the tests, the real implementation is often called instead of the stub, leading to "Unknown dbtype" or `nil` connection errors.

### Proposed Improvement
- **Option A (Preferred):** Update `build-handler` to return a handler that wraps its execution in the necessary `with-redefs` blocks.
- **Option B:** Modify the `app-routes` and handlers to accept a `db` and `service-container` more consistently, and pass mocked versions in all integration tests.
- **Immediate Fix:** Ensure `get-simple-metrics-summary` and `get-dashboard-stats` are properly stubbed in `api_test.clj` and `dashboard_test.clj` across all relevant test cases.

## 2. Shared Logic Coverage

The `app.shared.type-conversion` namespace received a critical fix today regarding `parseFloat` and `NaN` handling.

### Problem
The fix (returning `nil` instead of `js/NaN` in CLJS when a string cannot be parsed as a float) is currently not verified by any automated tests in `test/app/shared/type_conversion_test.cljc`.

### Proposed Improvement
- Add test cases to `test/app/shared/type_conversion_test.cljc` that specifically target:
    - `parse-number` with invalid numeric strings in both CLJ and CLJS.
    - `convert-to-type` behavior with `:decimal` when input is a non-numeric string (e.g., "abc").
    - Ensure `NaN` is never returned to the frontend state from these utility functions.

## 3. Modularization Integrity

Large monolithic namespaces were split into multiple smaller modules (e.g., `app.admin.frontend.events.settings.*`).

### Problem
Existing tests might still be referencing the old namespaces or only testing a subset of the new modules. While the app builds and basic tests pass, we need to ensure the "entrypoint" events in the new structure are fully exercised.

### Proposed Improvement
- Audit `test/app/admin/frontend/events/user_settings_test.cljs` and ensure it covers the new modular structure in `src/app/admin/frontend/events/settings/`.
- Add "Smoke Tests" for each new module to verify that common event paths (e.g., loading settings, saving toggles) still function correctly across the new namespace boundaries.

## 4. Feature-Specific Gaps

### CRUD Success Highlights
Unit tests were added for `expenses` and `user-expenses` events to verify they track recently changed IDs.

- **Gap:** There is no integration test verifying that these IDs actually result in a visual highlight in the UI (e.g., a "flash" class applied to a table row).
- **Improvement:** Add a browser-based test (Karma) that dispatches a success event and asserts that the corresponding row in the expense list receives a highlight class.

### Admin Routing Consistency
A fix was implemented for sidebar active states and dashboard path aliases.

- **Gap:** We have unit tests for the router matching, but not for the *effect* of changing routes on the sidebar component's state.
- **Improvement:** Add a test that verifies the Sidebar component correctly identifies the active route name and applies the `ds-active` class.

## Verification Plan for Implementation

### Automated Tests
1. **Backend:** Run `bb be-test` and ensure 0 errors/failures.
2. **Frontend (Node):** Run `bb fe-test-node` and ensure all type-conversion cases pass.
3. **Frontend (Browser):** Run `bb run-karma` to verify UI highlights and navigation states.

### Manual Verification
- Verify that the Admin Dashboard loads without errors in the logs (proving the stats loading fallback works).
- Intentionally enter an invalid number in an expense form and verify no `NaN` errors appear in the browser console.

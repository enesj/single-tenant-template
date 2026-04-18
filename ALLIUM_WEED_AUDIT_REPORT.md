# Allium Weed Audit Report

Generated from the audit of `ALLIUM_SPECS_INVENTORY.md` on 2026-04-18.

## Scope

- Mode: **check**
- Source inventory: `ALLIUM_SPECS_INVENTORY.md`
- Validation baseline: `allium check specs/allium` → `34 file(s) checked, no issues found`
- Files changed during audit: **none**
- Audit style: static spec-to-code comparison across the current repository state

## Coverage summary

| Section | Specs audited | Specs with findings |
| --- | ---: | ---: |
| `template` | 14 | 10 |
| `shared` | 5 | 3 |
| `domain` | 4 | 4 |
| `drafts` | 11 | 5 |
| **Total** | **34** | **22** |

## Finding totals by classification

| Classification | Count |
| --- | ---: |
| **Spec bug** | 14 |
| **Code bug** | 11 |
| **Aspirational design** | 8 |
| **Intentional gap** | 3 |
| **Total material findings** | **36** |

## Post-audit updates

- 2026-04-18 — Resolved the paired users-entity code bugs (`specs/allium/template/dry-principle.allium` → `VersionedEntityRoutesKeepExplicitUsersReadException`; `specs/allium/template/platform-boundaries.allium` → `UserReadDelegationRemainsExplicit`). Fix summary: rewired `/api/v1/entities/users` onto the real served Reitit `""` child subroute, normalized live entity param handling, and replaced the unsafe generic users fallback with a tenant-scoped protected users list. Verification: focused backend regression (`{:test 1, :pass 20, :fail 0, :error 0}`) plus live browser E2E confirmed the route now returns only safe fields (`id`, `fullName`, `email`, `status`, `membershipRole`, `membershipStatus`) with no `passwordHash` or ciphertext leakage.

## Shared specs

### Shared findings

- `specs/allium/shared/form-field-options.allium` — `LegacyDuplicateRuntimeSnapshotSanitization` — **Intentional gap**: the spec describes strict duplicate rejection, while the runtime read path also sanitizes and deduplicates legacy persisted snapshots before validation.
- `specs/allium/shared/infrastructure.allium` — `SerializationSanitizationBoundary` — **Code bug**: auth and OAuth routes still use route-local serialization sanitizers instead of the shared sanitizer boundary and fallback.
- `specs/allium/shared/list-filter-query-param-forwarding.allium` — `ServerTextFilterForwardingScope` — **Spec bug**: the active `user_expenses` refresh path forwards raw text filter IDs more directly than the spec currently allows.

### Shared specs with no material divergence

- `specs/allium/shared/list-filter-pagination-reset.allium`
- `specs/allium/shared/view-options.allium`

## Template specs

### Template findings

- `specs/allium/template/authentication.allium` — `UserLogoutClearsOnlyUserAuth` — **Intentional gap**: user logout now clears the full session; preserving `:admin-token` was intentionally dropped.
- `specs/allium/template/multi-tenancy.candidate.allium` — `RemoveUserFromTenant` — **Spec bug**: tenant-member removal suspends memberships and supports reinstatement, while the spec says the membership ceases to exist.
- `specs/allium/template/admin-invitation.candidate.allium` — `ReopenAcceptedAdminInvitation`, `ReissueExpiredAdminInvitation`, `ReissueRevokedAdminInvitation` — **Aspirational design**: only resending pending invitations is implemented; reopening or reissuing non-pending invitations is not.
- `specs/allium/template/workspace-onboarding.candidate.allium` — `GuidedFirstUpload` — **Aspirational design**: the onboarding step links to the normal upload page rather than a dedicated onboarding upload surface.
- `specs/allium/template/workspace-onboarding.candidate.allium` — `RoleDifferenceNotice` — **Aspirational design**: role differences render inside onboarding instead of as a separate dismissible notice.
- `specs/allium/template/email-privacy-at-rest.candidate.allium` — `AdminUsersDirectory / NoRoutineEmailDisclosure` — **Aspirational design**: routine admin user browsing still exposes masked email data.
- `specs/allium/template/email-privacy-at-rest.candidate.allium` — `AdminAdminsDirectory / NoRoutineEmailDisclosure` — **Aspirational design**: routine admin-admin browsing still exposes masked or full email data according to privilege level.
- `specs/allium/template/deploy-railway.allium` — deployment workflow overview comment — **Spec bug**: the file header still describes Railway deploys as manual-migration-triggered, but deploy-time migrations are automatic.
- `specs/allium/template/deploy-railway.allium` — `PortIsRailwayInjectedAtRuntime` — **Spec bug**: the spec forbids fallback defaults, but production config and the container image still allow `PORT=8080`.
- `specs/allium/template/deploy-railway.allium` — `RequiredManualVariablesMustExistBeforeFirstDeploy` — **Spec bug**: not every listed Railway variable is enforced as a fatal startup precondition.
- `specs/allium/template/dry-principle.allium` — `ProtectedUsersEntityRequiresProtectedHandlingPath` — **Spec bug**: the spec says the public-entity set is empty, but code now has a non-empty `public-entities` set.
- `specs/allium/template/dry-principle.allium` — `VersionedEntityRoutesKeepExplicitUsersReadException` — **Code bug**: the explicit users read exception resolves the wrong handler namespace.
- `specs/allium/template/platform-boundaries.allium` — `UserReadDelegationRemainsExplicit` — **Code bug**: the users-collection delegation path points to the wrong handler namespace.
- `specs/allium/template/frontend-config-runtime-resolution.candidate.allium` — `SnapshotReplacement` — **Spec bug**: the candidate spec models full snapshot replacement, but the runtime settings implementation supports partial patching.
- `specs/allium/template/frontend-config-runtime-resolution.candidate.allium` — user-facing `entities` runtime management — **Intentional gap**: the spec leaves this open, but `user/entities` is already runtime-managed in code.
- `specs/allium/template/backlog-remove-priority.candidate.allium` — `DefaultListOrder` — **Code bug**: backend ordering moved away from priority, but the frontend default sort metadata was not updated to match.

### Template specs with no material divergence

- `specs/allium/template/authorization.allium`
- `specs/allium/template/external-api-audit.candidate.allium`
- `specs/allium/template/domain-architecture.allium`
- `specs/allium/template/mobile-web.candidate.allium`

## Domain specs

### Domain findings

- `specs/allium/domain/expenses/implementation.allium` — `ExpensesUserApiRouteFamilies / SettingsEndpointsRemainGrouped` — **Spec bug**: the active settings/defaults/export/delete endpoints now live under `/api/v1/profile`, while the spec still describes the old `/api/v1/expenses/settings` grouping.
- `specs/allium/domain/expenses/implementation.allium` — `ExpensesUserApiRouteFamilies / ExpensesAdminRouteFactoryBoundary` — **Spec bug**: route naming and protection around payer-related endpoints no longer match the current `/payers` route tree.
- `specs/allium/domain/expenses/price-history-integrity.candidate.allium` — co-occurring article suggestions — **Code bug**: the co-occurrence query is filtered to manual expenses only, even though the spec reserves origin-based filtering for last-price enrichment.
- `specs/allium/domain/expenses/settings-hierarchy.candidate.allium` — `DefaultPayerReadOnly` — **Spec bug**: `default_payer` is directly editable on the profile page.
- `specs/allium/domain/expenses/settings-hierarchy.candidate.allium` — `OwnerProfileSection` — **Spec bug**: the privileged profile section is effectively owner/admin power-user behavior, not owner-only behavior.
- `specs/allium/domain/expenses/settings-hierarchy.candidate.allium` — `ExportTenantExpenses / DeleteAllTenantExpenses` — **Code bug**: export/delete operations act on the current user’s data rather than tenant-wide data.
- `specs/allium/domain/expenses/settings-hierarchy.candidate.allium` — `ReceiptsAlwaysBAM` — **Code bug**: reviewed receipts can still persist non-BAM currency.
- `specs/allium/domain/expenses/workspace-dashboard.candidate.allium` — `ComputeMonthlyTrend / RankingBound` — **Aspirational design**: the dashboard does not zero-fill a fixed six-month trend window.
- `specs/allium/domain/expenses/workspace-dashboard.candidate.allium` — `ComputeUnmappedAliasCount` — **Code bug**: unmapped alias count is global rather than tenant-filtered.
- `specs/allium/domain/expenses/workspace-dashboard.candidate.allium` — `DashboardPage` — **Aspirational design**: the dashboard lacks a dedicated empty-state rendering branch.

### Domain specs with no material divergence

- None

## Draft specs

### Draft findings

- `specs/allium/drafts/expenses/expense-entry.candidate.allium` — desktop Smart Input surface — **Code bug**: the dashboard uses Smart Input, but the expenses list still renders the old standard expense form.
- `specs/allium/drafts/expenses/payer-and-defaults.candidate.allium` — default payer source — **Aspirational design**: the new per-user default payer model coexists with the older payer-row `is_default` behavior.
- `specs/allium/drafts/expenses/payer-and-defaults.candidate.allium` — `UpdateDefaultPayerOnExpenseSubmit` — **Code bug**: expense submit and receipt approval do not update the sticky default payer.
- `specs/allium/drafts/expenses/payer-and-defaults.candidate.allium` — receipt upload defaults surface — **Code bug**: upload-time category and note defaults are incomplete in the actual UI.
- `specs/allium/drafts/expenses/receipt-ocr-unit-extraction.candidate.allium` — single-letter unit suffixes — **Spec bug**: `/L` extraction is already implemented, so the open question is stale.
- `specs/allium/drafts/expenses/receipt-ocr.candidate.allium` — receipt status lifecycle — **Spec bug**: the implementation includes `parsing`, `parsed`, and `approved` states beyond the spec’s simplified lifecycle.
- `specs/allium/drafts/list-view-sort.candidate.allium` — multi-column sorting — **Spec bug**: multi-column sorting is already live.

### Draft specs with no material divergence

- `specs/allium/drafts/expenses/expense-list-item-expand.candidate.allium`
- `specs/allium/drafts/expenses/list-filter-contracts.candidate.allium`
- `specs/allium/drafts/expenses/receipt-ocr-supplier-alias-resolution.candidate.allium`
- `specs/allium/drafts/list-view-date-range-picker.candidate.allium`
- `specs/allium/drafts/list-view-filter-form.candidate.allium`
- `specs/allium/drafts/list-view-filtering.candidate.allium`

## Recommended triage order

1. **Fix clear code bugs first**
   - wrong users handler namespace — resolved 2026-04-18 (see post-audit updates)
   - backlog default sort mismatch
   - expenses list still using the old standard form
   - co-occurrence query overfiltering
   - tenant export/delete scope mismatch
   - sticky default-payer updates
2. **Then update clearly stale specs**
   - `specs/allium/template/deploy-railway.allium`
   - `specs/allium/domain/expenses/implementation.allium`
   - `specs/allium/drafts/expenses/receipt-ocr.candidate.allium`
   - `specs/allium/drafts/expenses/receipt-ocr-unit-extraction.candidate.allium`
   - `specs/allium/drafts/list-view-sort.candidate.allium`
   - `specs/allium/template/frontend-config-runtime-resolution.candidate.allium`
3. **Leave aspirational candidates for product/design choice**
   - admin invitation reopen/reissue flows
   - dedicated onboarding upload UX and role-difference notice
   - stricter email-privacy behavior
   - dashboard zero-fill and dedicated empty-state behavior

## Notes

- This report mirrors the completed check-mode weed audit performed against the repository state current on 2026-04-18.
- The report is intended as a saved artifact of the findings, not as an implementation plan or an automatically applied fix set.
- Scratch-pad tracking for this audit recorded full coverage of all 34 specs and matching inventory counts.

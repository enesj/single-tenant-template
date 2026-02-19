# Allium Specifications

This directory contains **living Allium specifications** for this system—executable contracts that model behavior precisely as it is implemented.

## What is Allium?

Allium is an LLM-native language for sharpening intent alongside implementation. Each spec is a **single source of truth** that:

- Models entities, lifecycle states, and invariants
- Captures rules and validation logic as formal constraints  
- Defines interaction surfaces (API boundaries, event contracts)
- Flags deferred/open design questions explicitly
- Can be evaluated and refined by both humans and AI

Allium specs are **companion artifacts** to code—they live alongside implementation, sync with it, and guide development.

See [`.agents/skills/allium/SKILL.md`](../../.agents/skills/allium/SKILL.md) for the full Allium language guide and best practices.

## Folder Convention

```text
specs/allium/
├── template/              # Platform-level boundaries & cross-cutting concerns
├── domain/                # Domain-specific behaviors (mirrors src/app/domain/)
│   ├── expenses/
│   ├── articles/
│   └── [domain]/
├── drafts/                # Work-in-progress specs (candidate status)
│   └── [domain]/
└── shared/                # Cross-domain entities & patterns
```

### Locating Specs

- **Platform boundaries** (e.g., HTTP middleware, DI container contracts) → `template/`
- **Complex behaviors** (e.g., receipt OCR, expense posting, article mapping) → `domain/[domain]/*.allium`
- **Draft/candidate specs** awaiting stability → `drafts/[domain]/*.allium`
- **Shared types** used across domains → `shared/`

## Spec Lifecycle

Each spec has a lifecycle:

### 🟡 Candidate

- Status: WIP, gathering feedback from implementation
- Location: `drafts/[domain]/*.candidate.allium`
- Review: Check spec against test suite and edge cases; gather open questions
- Transition: Move to `domain/[domain]/` once stable

### 🟢 Stable

- Status: Faithful model of current behavior; serves as reference
- Location: `domain/[domain]/*.allium`
- Contract: Should match implementation; divergence indicates debt or discovered bugs
- Test: Verified against test suite; open questions resolved or explicitly deferred

### ⚪ Archived

- Status: Superseded by newer spec or behavior deprecated
- Location: `archived/[domain]/` (when applicable)
- Reason: Link to successor spec or migration notes

## How to Read an Allium Spec

Each spec contains:

1. **Entity Definitions**

   ```allium
   entity Receipt {
       status: uploaded | parsing | parsed | extracting | extracted | review_required | posted | failed
       total_amount_guess: Decimal?
       effective_status: derived field (read-only calculation)
   }
   ```

   - List all fields, types, and key invariants
   - `?` denotes optional/nullable
   - Derived fields computed from other state

2. **Rules**

   ```allium
   rule HandleOCRExtractionSuccess {
       when: OCRExtractionCompleted(receipt, extraction_data)
       requires: receipt.status = extracting
       ensures: receipt.extraction = extraction_data
       ensures: receipt.status = extracted
   }
   ```

   - `when` = trigger condition or event
   - `requires` = preconditions (must all hold)
   - `ensures` = postconditions (must hold after action)

3. **Surfaces** = API/boundary contracts

   ```allium
   surface UserReceiptOperations = {
       .post("/api/v1/expenses/receipts/{id}/ocr")
           → queues OCR extraction
           → requires: receipt.owner = current_user OR receipt.owner = null
   }
   ```

   - Each surface lists endpoints, methods, inputs, visibility rules

4. **Deferred** = explicit open questions or future work

   ```allium
   deferred {
       Should mismatch in line-item totals block auto-post?
       Who can request OCR: owner only, or admin on behalf?
   }
   ```

## How to Write a Spec

1. **Extract from code**: Read source, tests, and DB schema
2. **Identify boundaries**: What state, events, and rules define this behavior?
3. **Model entities**: Fields, types, derived state, invariants
4. **Capture rules**: When/requires/ensures for each key rule
5. **Define surfaces**: API surfaces, visibility, who-calls-what
6. **Flag open questions**: Write deferred issues explicitly
7. **Validate**: Does spec match test suite? Run focused tests to verify

**Minimal example** (1-2 surfaces, 3-5 rules, 1 entity):

```allium
entity Order {
    id: UUID
    status: pending | confirmed | shipped | delivered
    total: Decimal
    items: [OrderItem]
}

rule ConfirmOrder {
    when: confirmed_by_user(order)
    requires: order.status = pending
    requires: order.total > 0
    ensures: order.status = confirmed
    ensures: OrderConfirmed(order) event raised
}

surface OrderCheckout = {
    .post("/api/v1/orders", { total, items })
        → creates Order with status=pending
        → returns Order
        → requires: user authenticated

    .patch("/api/v1/orders/{id}/confirm")
        → confirms order via ConfirmOrder rule
        → returns updated Order
        → requires: order.owner = current_user
}
```

## Current Specs

### Candidate 🟡

- **[Receipt OCR](drafts/expenses/receipt-ocr.candidate.allium)**: Upload → OCR extraction → review → optional auto-post lifecycle. Documents extraction post-processing, supplier/store resolution, 8-state finite machine.
- **[List View Filtering](drafts/list-view-filtering.candidate.allium)**: Canonical list filtering semantics (client/server mode behavior, first-character text auto-apply, conditional server refresh dispatch, and select-dropdown dismissal behavior).
- **[List View Sorting](drafts/list-view-sort.candidate.allium)**: Canonical list sorting semantics with client/server mode split and sort state propagation.

### Stable 🟢

- **[Platform Boundaries](template/platform-boundaries.allium)**: HTTP routing composition, DI container registration, middleware layering.
- **[Domain Architecture](template/domain-architecture.allium)**: Domain registry contracts, shared route-descriptor boundaries, and template/domain composition guarantees.
- **[DRY Principle](template/dry-principle.allium)**: Implementation-faithful reuse layers across template UI primitives, adapter utilities, CRUD bridges, generic backend CRUD, DI wiring, and domain registry composition (with security/domain caveats).
- **[Authentication](template/authentication.allium)**: User/admin login flows, OAuth callback behavior, session coexistence rules, password reset, and email verification lifecycle.
- **[Authorization](template/authorization.allium)**: Admin token + role guards, generic CRUD entity policy, and expenses role-gated access matrix.
- **[Expenses Implementation Wiring](domain/expenses/implementation.allium)**: Expenses manifest mount points, user/admin route-family boundaries, and route-factory boundary contracts.

### Deferred / Future 📋

- Article mapping & alias ingestion
- Supplier & store management  
- Rate limiting & session management

## Resources

- [Allium Skill Guide](../../.agents/skills/allium/SKILL.md)
- Spec review checklist: test coverage ✓, open questions ✓, invariants ✓
- Questions? Ask in spec header or flag in `deferred` section

---

**Last Updated:** 2026-02-18  
**Spec Governance:** Living artifacts; sync with implementation via code review + REPL validation.

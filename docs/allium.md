# Allium Specifications

This page provides a quick index to **Allium specifications** in this system. Allium is an LLM-native language for modeling behavior precisely—serving as a **living contract** between implementation and intent.

**Important:** Canonical Allium specs live in `specs/allium/`, not in docs/. This page serves as navigation only.

## What is Allium?

Allium specs capture:

- **Entities** with fields, types, derived state, and invariants
- **Rules** that govern state transitions (when, requires, ensures)
- **Surfaces** that define interaction boundaries and contracts
- **Deferred** questions representing open design decisions

Learn more: [Allium Skill Guide](../.agents/skills/allium/SKILL.md)

## Quick Links

### 📍 Spec Inventory

#### Stable 🟢

- [Platform Boundaries](../specs/allium/template/platform-boundaries.allium) — HTTP routing, DI container, middleware layering

#### Candidate 🟡

- [Receipt OCR](../specs/allium/drafts/expenses/receipt-ocr.candidate.allium) — Upload → OCR → extract → review → post lifecycle

### 📂 Browse by Folder

- **[specs/allium/](../../specs/allium/)** ← main spec directory
  - `template/` → Cross-cutting platform concerns
  - `domain/` → Domain behaviors (expenses, articles, etc.)
  - `drafts/` → Work-in-progress specs
  - `shared/` → Shared types and patterns

### 🚀 Getting Started

**If you want to:**

- **Understand receipt OCR behavior** → Start with [receipt-ocr.candidate.allium](../specs/allium/drafts/expenses/receipt-ocr.candidate.allium)
- **Learn the Allium language** → Read [SKILL.md](../.agents/skills/allium/SKILL.md) then a spec example
- **Write a new spec** → See [specs/allium/README.md](../specs/allium/README.md#how-to-write-a-spec)
- **Find specs by domain** → Browse `specs/allium/domain/`

## Spec Lifecycle

**🟡 Candidate** — WIP, gathering feedback → locate in `drafts/[domain]/`

**🟢 Stable** — Faithful model of current behavior → locate in `domain/[domain]/`

**⚪ Archived** — Superseded or deprecated → locate in `archived/[domain]/`

## Key Insight: Specs vs. Docs

- **Specs** (`specs/allium/`) are **executable contracts**—to be evaluated, tested, and refined
- **Docs** (`docs/`) are **narrative explanations**—to introduce, guide, and contextualize
- **This page** bridges them: it's a navigation aid, not a canonical spec

When in doubt, trust the spec over the narrative explanation. If they diverge, raise an issue or update the spec.

---

**Last Updated:** 2026-02-14

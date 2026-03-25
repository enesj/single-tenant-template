# Improvement Plan: `.claude/commands/create-articles-.md`

Based on lessons learned from the 2026-03-24 full-backlog session (466 aliases, 100% resolved).

---

## Pain Points (ranked by time wasted)

### P0 — Key normalization mismatch (cost: ~30 min)

**What happened:** The agent wrote a correction script with a naive `normalize-key` that stripped diacritics entirely (`Š` → nothing) instead of transliterating them (`Š` → `S`). This caused mapping failures because the article keys in DB didn't match the mapping file keys.

**Root cause:** The command doc doesn't document how `db/normalize-key` works (`scripts/bb/articles/db.clj:104-123`). It uses NFD decomposition + combining-mark removal, so `Š→S`, `č→c`, `Đ→D`. The only warning is "Đ/đ is dropped entirely" which is actually wrong — it becomes `D/d`.

**Fix in command doc:**
- Document the exact normalization algorithm: NFD decomposition → strip combining marks → lowercase → non-alnum to hyphens → trim/collapse
- Provide the transliteration table: `Š→S, č→c, ž→z, đ→d, ć→c`
- **Critical instruction:** "Never write your own normalize-key. Always use the output of `create_articles.clj --dry-run` or query `SELECT normalized_key FROM articles` to get actual keys."
- Remove the misleading "Đ/đ character is dropped entirely from keys" — it becomes `d`, not nothing

### P1 — Already-mapped aliases crash the mapping script (cost: ~15 min)

**What happened:** `map_aliases.clj` throws on the first already-mapped alias instead of skipping it. Had to manually query DB for 6 already-mapped IDs and filter them out of the mappings file.

**Two possible fixes:**
1. **Script fix** (preferred): Add `--skip-mapped` flag to `map_aliases.clj` that silently skips aliases where `article_id IS NOT NULL`
2. **Command doc fix**: Add a step before mapping that filters out already-mapped alias IDs:
   ```bash
   bb scripts/bb/articles/filter_mapped_aliases.clj dev \
     --mappings-file tmp/mappings-suggested.edn \
     --output tmp/mappings-unmapped-only.edn
   ```

### P2 — Local heuristic output quality is too low to use directly (cost: ~20 min)

**What happened:** The `--skip-research` pass produced articles with:
- 87% Generic (no manufacturer) — many branded items missed
- Wrong categories (coffee capsules as "Zdravlje i apoteka", essential oil as food oil)
- Wrong subcategories ("Mlijeko" under "Lična njega" for a soap)
- OCR artifact numbers in canonical names (241, 657, 197 appended to Meggle milk)
- Many duplicates that should have been merged (5 Meggle 3.2% variants, served beverages)

**Root cause:** Local heuristics in `articles_research.clj` do brand matching but don't:
- Cross-reference against existing articles in DB
- Validate category/subcategory against actual DB taxonomy
- Detect OCR suffix artifacts (trailing hex/numbers)
- Merge near-duplicates across suppliers

**Fixes (script improvements):**
1. Add `--validate-taxonomy` flag that checks suggested categories/subcategories against DB and warns on mismatches
2. Add OCR suffix stripping (trailing 2-4 char hex patterns like `6f93`, `2b3c`, `a73`)
3. Add manufacturer pattern matching for common Bosnian brands: Meggle, Dukat, Milkos, Vispak (Zlatna Džezva), Podravka (Zvijezda), Atlantic Grupa (Argeta), BAT (Dunhill), Nestlé (Nescafé), etc.
4. Add near-duplicate detection: normalize names, group by similarity, suggest merges

**Fix in command doc:**
- After local heuristics, add explicit "Quality audit checklist" step:
  - Count manufacturer coverage (target: >40% for branded items)
  - Verify category→subcategory pairs against `bb list-categories --with-subcategories`
  - Search for OCR artifacts: trailing hex (`[0-9a-f]{3,4}`), trailing numbers not part of weight/volume
  - Search for near-duplicates: same product different stores (e.g., "Kefa Potrosacka Pvc" vs "Kesa Potrosacka Pvc /ko")

### P3 — Output file overwriting between batches (cost: ~10 min)

**What happened:** Each `bb articles-research --supplier X` run overwrites `articles-suggested.edn` and `mappings-suggested.edn`. When processing BINGO, then TROPIC, then dm sequentially, each run clobbers the previous output. No issue if you create+map immediately after each research run, but error-prone.

**Fix (script improvement):** Add `--output-prefix` flag:
```bash
bb articles-research dev --supplier "BINGO" --output-prefix bingo --pretty
# Produces: tmp/bingo-articles-suggested.edn, tmp/bingo-mappings-suggested.edn
```

**Fix in command doc:** Add explicit note: "Each research run overwrites `tmp/articles-suggested.edn` and `tmp/mappings-suggested.edn`. Create articles and map aliases immediately after each supplier batch before running the next."

### P4 — Perplexity creates English categories and subcategory proliferation (cost: ~10 min)

**What happened:** Perplexity sometimes returns English category names ("Jams & Marmalades & Honey & Preserve") and creates many near-duplicate subcategories ("Maramice" vs "Maramice i papirni proizvodi", "Čokolada i bomboni" vs "Čokolada i slatkiši").

**Fix (script improvement):**
1. Pass existing categories + subcategories in the Perplexity prompt as a constraint: "Pick from these categories: [list]. Pick from these subcategories or suggest a new one in Bosnian."
2. Add post-processing that maps English category names to Bosnian equivalents
3. Add subcategory dedup/normalization pass

**Fix in command doc:** Add a "Post-Perplexity quality gate" step:
```bash
# Check for English categories
bb -e '...' < tmp/articles-suggested.edn
# Check for manufacturer names ≤ 2 chars (OCR artifacts)
# Check for subcategory near-duplicates
```

### P5 — Progress tracking confusion (cost: ~5 min)

**What happened:** `unmapped_aliases_counts.clj` groups by (raw_label, supplier) showing 200 unique combos. `articles_research.clj` deduplicates across suppliers showing 448 unique groups. After mapping a batch, the unmapped count from `unmapped_aliases_counts.clj` barely changes because the cross-supplier groups were resolved but many individual supplier combos remain.

**Fix in command doc:**
- Use `SELECT COUNT(*) FILTER (WHERE article_id IS NULL) FROM article_aliases` as the ground-truth progress metric (individual alias rows, not grouped combos)
- Add this as the primary progress indicator between phases
- Keep `unmapped_aliases_counts.clj` for backlog composition analysis only

---

## Proposed Command Doc Structure (revised)

```
Pre-phase: Resolve review-required receipts (unchanged)

Phase 0: Quick overview
  - bb scripts/bb/articles/unmapped_aliases_counts.clj dev --pretty
  - SQL: SELECT COUNT(*) FILTER (WHERE article_id IS NULL) FROM article_aliases

Phase 1: Local heuristics  (--skip-research)
  - NEW: Quality audit checklist (categories, manufacturers, OCR artifacts, duplicates)
  - NEW: Normalization key reference (exact algorithm, transliteration table)
  - NEW: "Never write your own normalize-key" warning

Phase 2: Perplexity research (per supplier or all)
  - NEW: Post-Perplexity quality gate (English cats, short mfg, subcat dedup)
  - NEW: "Create+map immediately after each supplier batch" workflow

Phase 3: Variant risk check (unchanged)

Phase 4: Create articles + map aliases
  - NEW: --skip-mapped flag or pre-filter step
  - NEW: Use dry-run keys as source of truth, not mental derivation

Phase 5: Noise + remaining (unchanged)

Phase 6: Verify + report (unchanged)
```

---

## Script Changes (separate PRs)

| Priority | Script | Change | Effort |
|----------|--------|--------|--------|
| P0 | `articles_research.clj` | Add manufacturer patterns for common Bosnian brands | M |
| P0 | `articles_research.clj` | Strip trailing OCR hex artifacts from canonical names | S |
| P1 | `map_aliases.clj` | Add `--skip-mapped` flag | S |
| P1 | `articles_research.clj` | Pass existing DB taxonomy to Perplexity prompt | M |
| P2 | `articles_research.clj` | Add `--output-prefix` flag | S |
| P2 | `articles_research.clj` | Post-process: English→Bosnian category mapping | S |
| P3 | `articles_research.clj` | Near-duplicate detection + merge suggestions | L |
| P3 | `articles_research.clj` | Validate suggested taxonomy against DB | M |

---

## Command Doc Text Changes (can do now)

1. Replace "Đ/đ character is dropped entirely from keys" with accurate transliteration docs
2. Add normalization algorithm reference section
3. Add quality audit checklist after Phase 1
4. Add post-Perplexity quality gate after Phase 2
5. Add --skip-mapped workflow or pre-filter step in Phase 4
6. Add ground-truth progress metric (SQL count, not grouped script count)
7. Add "create+map immediately per batch" workflow note
8. Add common manufacturer patterns table for manual review

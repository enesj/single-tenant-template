# `create-articles-` Prompt Improvement Analysis

Session date: 2026-03-25  
Source prompt: `.github/prompts/create-articles-.prompt.md` → `.claude/commands/create-articles-.md`

---

## All changes implemented in this session

### `.claude/commands/create-articles-.md` — 9 changes

| # | Issue / motivation | Location | What changed |
|---|-------------------|----------|--------------|
| 1 | OCR-merge artifact (mid-string hex) | Step 1c item 3 | Extended OCR checklist with mid-string hex separator pattern + explicit delete-not-map instruction |
| 2 | Perplexity encoding artifacts (fragmented Bosnian words) | Step 1d | New bullet: scan for short 2–4 char fragments after spaces; worked examples (`"Bombo ni"` → `"Bomboni"`) |
| 3 | Hallucinated / truncated manufacturers | Step 1d | Expanded from 1 failure mode (≤2 chars) to 3: truncated known brands, product-attribute-as-manufacturer |
| 4 | Manufacturer reference table gaps | Key normalization reference section | Added 6 new rows: Dunhill/BAT, NESTLE/Nestlé, Haribo, Roshen, Bayer, Babybel/Bel, Zanetti |
| 5 | Cross-batch duplicate detection | Step 1d (new bullet) + Step 4a | New bullet with 3-step remediation; dry-run note clarified with remove-and-redirect instructions |
| 6 | Noise aliases surviving into `mappings-suggested.edn` | New Step 1e (between 1d and Phase 4) | Mandatory step to purge noise entries from mappings file before running `map_aliases.clj` |
| 7 | Category/subcategory validation too late | Step 1c item 2 | Replaced one-liner `bb list-categories` hint with mandatory SQL cross-reference query + validation rule for missing subcategories |
| 8 | `--skip-mapped` not default for multi-batch sessions | Step 4c | Promoted `--skip-mapped` to default command; first-run-only case demoted to a note |
| 9 | No post-run improvement loop | New Phase 4e + Completion gate checkbox | Decision rule for when/how to update `perplexity-system-prompt.txt` and this command file after each run |
| 10 | Output contract didn't require reporting EDN corrections or prompt changes | Output contract items 7 + 8 (new) | Added required output items: concise grouped list of all `.edn` file fixes, and list of any prompt file changes with reasons |

### `scripts/bb/articles/perplexity-system-prompt.txt` — new file

The Perplexity system prompt extracted from the hardcoded inline Clojure string in
`articles_research.clj` into a standalone editable text file. Contains `{{CATEGORIES}}`
and `{{TAXONOMY}}` placeholders substituted at runtime. Three new rules added immediately
from this session's findings:

- **Bosnian word integrity** — prohibits splitting multi-byte characters across tokens
  (e.g. `"Bombo ni"` → `"Bomboni"`, `"Det erđent"` → `"Deterdžent"`)
- **Canonical manufacturer names** — prohibits abbreviations/truncations from raw labels;
  examples: `"DUNHIL"` → `"BAT"`, `"NESTLE AFTER"` → `"Nestlé"`, `"HARIBO BE"` → `"Haribo"`
- **OCR-merge artifact handling** — when a label contains two product names joined by a
  short hex/number token, identify only the dominant product and use category `"Ostalo"`

### `scripts/bb/articles/articles_research.clj` — 2 changes

- New `load-system-prompt-template` function: reads `perplexity-system-prompt.txt` from
  disk, throws with a clear hint if the file is missing
- `build-research-prompt` refactored: replaces the 20-line inline `(str ...)` prompt
  with a template load + `str/replace` injection of `{{CATEGORIES}}` and `{{TAXONOMY}}`

---

## Summary of issues encountered

Seven distinct issue classes surfaced during a full 0 → ~100% alias mapping run (287 aliases).
Each section names the issue, shows where it appeared in the session, and proposes a concrete prompt change.

---

## Issue 1 — OCR-merge artifact: two products fused into one alias

**What happened**  
The alias `EUTHYROX TABLETE 100 MCG A 100 2f5 REVALID KAPSULE A 60 63de` is two separate pharmacy products whose receipt lines got merged by OCR. Perplexity treated it as a single article and proposed a merged article name. The alias appeared in both the local-heuristics batch and the Perplexity batch.

**Why the prompt missed it**  
Phase 1c item 3 (OCR artifact detection) looks for a trailing hex pattern at the *end* of a name. A merge artifact has hex *in the middle* as a separator between two product names — entirely different visual profile.

**Proposed fix**  
Add a new bullet to the OCR artifact detection checklist in Step 1c:

> - **Mid-string hex/number separator** — two distinct product names joined by a short token
>   (e.g. `"2f5"`, `"63de"`, a bare 3-digit number). These are two OCR'd receipt lines
>   stitched together. The alias must be **deleted** (Phase 5 noise deletion), not mapped.
>   Pattern to scan for: capital product word → whitespace → 2–4 hex chars → whitespace → capital product word.

Also add it to the Step 1d post-Perplexity checklist, since Perplexity will try to resolve the merged string as a real article.

---

## Issue 2 — Perplexity-produced encoding artifacts in article / subcategory names

**What happened**  
Perplexity returned space-fragmented Bosnian words:

- Subcategory `"Bombo ni i žel e"` instead of `"Bomboni i žele"`
- Subcategory `"Tjeste nina"` instead of `"Tjestenina"`
- Article name `"Bombo ne gumene 100G"` instead of `"Bomboni gumeni 100G"`
- Article name `"Det erđent za suđe"` instead of `"Deterdžent za suđe"`
- Article name `"Keks mljev eni"` instead of `"Keks mljeveni"`

These all look like inter-word spaces inserted inside a compound character (e.g. `đ` → `d` + space + leftover). They survive the quality audit unless the reviewer knows to scan for short word fragments.

**Why the prompt missed it**  
Step 1d lists "English category names" and "hallucinated manufacturers" but says nothing about fragmented/garbled Bosnian text caused by Perplexity's tokenisation.

**Proposed fix**  
Add to Step 1d post-Perplexity checklist:

> - **Fragmented Bosnian words** — Perplexity sometimes splits a multi-byte character
>   across tokens, inserting a space inside a word.
>   Scan article names and subcategory strings for suspiciously short 2–4 character
>   fragments following a space (e.g., `"ne"`, `"ni"`, `"eni"`, `"nina"`, `"đent"`).
>   Cross-check against the raw label and known Bosnian words:
>   `"Bombo ni"` → `"Bomboni"`, `"Tjeste nina"` → `"Tjestenina"`, `"Det erđent"` → `"Deterdžent"`.

---

## Issue 3 — Hallucinated / abbreviated manufacturer names from Perplexity

**What happened**  
Perplexity returned OCR fragments from the raw label as manufacturer names:
- `"DUNHIL"` (should be `"BAT"`)
- `"HARIBO BE"` (should be `"Haribo"`)
- `"JELLY ROSHE"` (should be `"Roshen"`)
- `"NESTLE AFTER"` (should be `"Nestlé"`)
- `"RINFU"` — not a real manufacturer; belonged to the raw label context
- `"PILAV BIOFIT"` — not a manufacturer; it's a product attribute

**Why the prompt missed it**  
Step 1d says: *"Short/nonsensical manufacturer values (≤ 2 chars, all-caps fragments) are likely OCR artefacts — set to Generic."*  
This threshold (≤ 2 chars) misses multi-token abbreviations like `"HARIBO BE"` and `"NESTLE AFTER"` which are clearly wrong but longer than 2 chars.

**Proposed fix**  
Expand the hallucinated-manufacturer heuristic in Step 1d:

> - **Abbreviated or truncated known brands**: manufacturer values that are a mangled version
>   of a recognisable brand (e.g. `"NESTLE"`, `"NESTLE AFTER"`, `"DUNHIL"`, `"HARIBO BE"`,
>   `"JELLY ROSHE"`) should be resolved to the canonical manufacturer.
>   A useful signal: if the manufacturer value appears verbatim inside the raw label, it
>   came from the label, not from product knowledge. Cross-check with the known-brand table.
> - **Product attributes as manufacturers**: values like `"BIOFIT"`, `"LIGHT"`, `"CLASSIC"`
>   are product line suffixes, not manufacturers. Set them to `nil` or `"Generic"` and move
>   the attribute to the canonical name if relevant.

Also extend the manufacturer reference table in the "Key normalization reference" section:

| Pattern | Manufacturer | Notes |
|---------|--------------|-------|
| Dunhill, DUNHIL | BAT | Tobacco |
| Haribo, HARIBO BE | Haribo | Confectionery |
| Roshen, ROSHE | Roshen | Confectionery |
| Bayer, Aspirin | Bayer | Pharma |
| Voltaren | GSK / Novartis | Pharma |
| Babybel, Leerdammer | Bel | Dairy |
| Zanetti | Zanetti | Dairy/cheese |

---

## Issue 4 — Duplicate detection does not cover inter-batch duplicates

**What happened**  
Perplexity proposed three articles that were already in the DB (created in the same session's first batch):
- `LIMUN SVJEŽI` / `SVJEZI LIMUN` — both duplicates of existing article `Limun`
- `PREMIUM bezolovni benzin 95` — duplicate of `Gorivo Premium 95`
- `SOK 1.25L COCA COLA` — duplicate of `Coca-Cola 1.25L`

These weren't caught by the prompt's near-duplicate check because that check looks for duplicates *within* the current suggestion file, not against articles that were created earlier in the same session.

**Why the prompt missed it**  
Phase 1c item 4 ("Near-duplicate detection") and Phase 3 ("Variant risk check") both operate on the current batch only. There is no step that looks up just-created articles for conflicts.

**Proposed fix**  
Add a new item to the Step 1d post-Perplexity checklist:

> - **Cross-batch duplicate check**: Run:
>   ```bash
>   bb scripts/bb/articles/unmapped_aliases_counts.clj dev --pretty
>   ```
>   and also query the DB for any article that shares a normalized key with a proposed article:
>   ```sql
>   SELECT normalized_key, canonical_name FROM articles
>   WHERE normalized_key IN (
>     -- paste the normalized_key values from the dry-run output
>   );
>   ```
>   Alternatively, after running `create_articles.clj --dry-run`, any article with
>   `"already_exists": true` in the output is a duplicate. **Remove it from
>   `tmp/articles-suggested.edn`** and redirect its mappings to the existing key
>   in `tmp/mappings-suggested.edn` (change `:article-key` to the existing article's key).

Also clarify the note in Step 4a (dry-run):

> The dry-run will flag articles that already exist. For each flagged article:
> 1. Remove it from `tmp/articles-suggested.edn`.
> 2. In `tmp/mappings-suggested.edn`, change `:article-key` for all mappings that
>    pointed to the removed article to the **existing article's `normalized_key`**.
> 3. Re-run the dry-run to confirm 0 conflicts.

---

## Issue 5 — Noise aliases mixed into `mappings-suggested.edn` with no removal step

**What happened**  
The OCR-merge alias (`c246cee0` for `EUTHYROX … 2f5 REVALID …`) appeared in `tmp/mappings-suggested.edn` pointing to the (also bogus) merged article. The prompt instructs the agent to delete noise aliases in Phase 5, *after* mapping. But by then the mapping script would have tried to map the noise alias to the (now removed) article, causing an error.

The correct action is: identify the noise alias during the quality review, remove its entry from `mappings-suggested.edn`, and schedule it for deletion via `delete_unmapped_aliases.clj`.

**Why the prompt missed it**  
Phase 5 (noise deletion) is presented as a post-mapping cleanup step. There is no step that cross-references noise candidates against the mappings file *before* running `map_aliases.clj`.

**Proposed fix**  
Add a "mappings–noise cross-reference" step between Step 1d and Phase 4:

> **Step 1e — Remove noise aliases from `mappings-suggested.edn`**
>
> Before running `map_aliases.clj`, check `tmp/noise-candidates.edn` and the OCR
> artifact aliases identified during manual review. For each noise alias:
> 1. Remove its entry from `tmp/mappings-suggested.edn`.
> 2. Add its `raw-label` to a list for `delete_unmapped_aliases.clj` (run in Phase 5).
>
> This prevents `map_aliases.clj` from failing on a missing article for a noise alias.

---

## Issue 6 — Category/subcategory validation happens too late (after dry-run, not before)

**What happened**  
`tmp/articles-suggested.edn` referenced categories that needed to be verified in the DB:
- `"Svježe voće i povrće"` — existed but needed confirmation
- `"Skladištenje i organizacija"` — existed but needed confirmation  
- `"Oprema za kućne ljubimce"` with subcategory `"Hrana za životinje"` — only had `"Opste"` in DB

The dry-run script does NOT validate that categories/subcategories exist; it only checks the article's own fields. Unknown categories/subcategories silently create them or cause runtime failures depending on the script version.

**Why the prompt missed it**  
Phase 2 ("Check existing categories") comes before Phase 4 (dry-run), but Step 1c doesn't explicitly instruct the agent to cross-reference the *new* subcategory names proposed by Perplexity against the DB before proceeding.

**Proposed fix**  
Add a mandatory DB check to Step 1c item 2 (Category/subcategory validation):

> After reading the suggestion file, extract all unique `(category, subcategory)` pairs
> and verify them against the DB in a single query:
> ```sql
> SELECT c.name AS category, array_agg(s.name ORDER BY s.name) AS subcategories
> FROM categories c
> LEFT JOIN subcategories s ON s.category_id = c.id
> WHERE c.name IN ('Cat1', 'Cat2', ...)  -- insert all categories from suggestions
> GROUP BY c.name;
> ```
> For any subcategory not found in the output:
> - Either reassign the article to the closest existing subcategory.
> - Or accept that the script will create the subcategory automatically (only if it's
>   a legitimate Bosnian-language leaf name and the category exists).
> Never create new top-level categories.

---

## Issue 7 — `--skip-mapped` usage not tied to a specific trigger

**What happened**  
When Perplexity mappings included aliases that point to articles already created in batch 1 (e.g., the lemon aliases, Coca-Cola 1.25L alias), the agent needed to use `--skip-mapped`. But the prompt presents `--skip-mapped` only as an option for *large backlogs*, not as a standard flag for *multi-batch sessions*.

**Why the prompt missed it**  
The existing note under Step 4c says *"By default `map_aliases.clj` throws on the first alias where `article_id IS NOT NULL`. Use `--skip-mapped` to silently skip."* This is true but does not emphasise that `--skip-mapped` is needed for **any second batch in the same session**.

**Proposed fix**  
Add a rule at the top of Step 4c:

> **Multi-batch sessions**: If you have already mapped aliases in this session (i.e. this is
> not the first time running `map_aliases.clj` against the same DB), **always** use
> `--skip-mapped` to prevent failures on aliases that were mapped by the earlier batch
> or by redirects within `mappings-suggested.edn` that point to already-existing articles.

---

## Summary table

| # | Issue class | Affected phase | Severity | Action |
|---|-------------|----------------|----------|--------|
| 1 | OCR-merge artifact (mid-string hex) | Phase 1c/1d OCR check | High — creates false articles | Add mid-string hex pattern to OCR checklist |
| 2 | Perplexity encoding artifacts in names | Phase 1d quality gate | High — corrupted canonical names | Add fragmented Bosnian word check to Phase 1d |
| 3 | Hallucinated/truncated manufacturer names | Phase 1d quality gate | Medium — wrong taxonomy | Widen manufacturer hallucination heuristic + extend reference table |
| 4 | Duplicates against earlier-session articles | Phase 1d / dry-run | High — error on create | Add cross-batch duplicate check step |
| 5 | Noise alias present in mappings file | Phase 5 timing | Medium — map_aliases error | Add Step 1e to remove noise entries before mapping |
| 6 | Category validation too late to catch missing subcategories | Step 1c | Medium — silent bad taxonomy | Mandate DB category/subcategory query in Step 1c |
| 7 | `--skip-mapped` not treated as mandatory for multi-batch | Step 4c | Low — recoverable error | Promote `--skip-mapped` to standard rule for 2nd+ batches |

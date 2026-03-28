# Receipt OCR batch testing results

## 2026-03-27 — Dunhill structured qty/unit fix

### 2026-03-28 sweep scope

- Profile: `dev`
- OCR workflow: `llamaparse`
- Batch size: `20`
- Run mode: selected batch only (`batch-5`)
- Reset before rerun: yes (script default)
- Deferred refine: yes (script default)
- Focus receipt: `5b6bae69-dbc1-4e3a-b801-c11289c4f360` (`IMG_3985.jpeg`)
- Goal: validate the structured-table qty/unit parsing + overlay fix against the real receipt batch containing the Dunhill example

### 2026-03-28 validation evidence

Artifact:

- `tmp/receipt-ocr-extract-focused-tests-20260327-180657.txt`

Namespaces run:

- `app.domain.backend.expenses.integrations.llamaparse-test`
- `app.domain.backend.expenses.workers.receipt-ocr-extraction.items-test`
- `app.domain.backend.expenses.workers.receipt-ocr-extraction.reconciliation-test`

Summary:

- `46 tests, 199 assertions, 0 failures, 0 errors`

Notes:

- This covers the changed LlamaParse table parsing path and the structured-response reconciliation/overlay path, including the new collapsed-total regression test.

### Batch plan artifact

Artifacts:

- `tmp/receipt-ocr-batches.edn`
- `tmp/receipt-ocr-batches.txt`

Planner summary:

- statuses included: `posted`, `extracted`
- total receipts planned: `139`
- total batches: `7`
- target receipt landed in `batch-5`

`batch-5` receipt IDs:

- `f58b1e1c-5f81-4fd3-b145-bbd6b9fd8cf8`
- `92a6b706-864e-4ab1-8909-10fe46181217`
- `c7c377ab-3257-4239-8e22-6ab117e4900d`
- `be18c6e9-1c73-4f07-832d-2b5ebbee092e`
- `3dbe4043-4353-44db-a9e6-dd1b9f9d5623`
- `9e15c1f4-4d47-46c2-8a71-e6738cb40858`
- `c09f0cd8-ba4c-478f-83c9-2a7e23e2e551`
- `5b6bae69-dbc1-4e3a-b801-c11289c4f360`
- `5002f5c1-3a5b-47af-80b7-5eb2a1668550`
- `eb6eb10f-c9d8-4256-86e6-aae474ff92b7`
- `91e3d434-cb9a-4b7d-a3ce-de52f2159df9`
- `feaacf91-87bb-4133-91f6-a2c3777789fd`
- `dadf8822-aeb3-40da-9a1a-63a2f190c381`
- `f3b06f76-84be-4bbe-baa0-b9bb82b21bc6`
- `8135dd65-e5e6-42e0-a99f-47d846901d57`
- `cefb50db-7be9-4bd5-bc4a-6b19ee1c049e`
- `8927ed2b-ff04-426a-bd27-2f116905e3e3`
- `83a30944-7a0f-4dde-8bfb-fa2ff5ecd1d4`
- `b67fae2f-6680-4051-8d75-8e1bc376b2fe`
- `adf6f458-d42c-4c20-a03c-9f5d6080ce4d`

### Batch 5 reparse run

Artifact:

- `tmp/receipt-ocr-batch-5-20260327-180912.txt`

Run summary:

- batch label: `batch-5-llamaparse`
- processed: `20`
- duration: `55.357875 ms`
- summary: `{:missing-result 20}`
- status-counts reported by the script: `{nil 20}`
- review-required count: `0`

Outcome:

- The batch execution was **blocked** because the LlamaParse OCR provider had no API key in the runtime environment.
- Every receipt in the batch returned `:error :missing-api-key`.
- No receipts were reparsed, no extraction results were refreshed, and no refine work occurred.

### Batch 5 rerun after sourcing `.env`

Artifact:

- `tmp/receipt-ocr-batch-5-20260327-181646.txt`

Environment wiring (presence only; no secret values logged):

- `LLAMA_CLOUD_API_KEY`: present
- `LLAMAPARSE_API_KEY`: not present
- `LAMA_CLOUD_API_KEY`: not present

Run summary:

- batch label: `batch-5-llamaparse`
- processed: `20`
- duration: `131267.481625 ms`
- summary: `{:ok 19, :failed 1}`
- status-counts reported by the script: `{"extracted" 19, nil 1}`
- review-required count: `0`

Outcome:

- The rerun succeeded for `19` receipts.
- One receipt timed out during LlamaParse polling:
  - receipt id: `c7c377ab-3257-4239-8e22-6ab117e4900d`
  - filename: `IMG_3971.jpeg`
  - error: `LlamaParse job polling timed out`
- The focus receipt `5b6bae69-dbc1-4e3a-b801-c11289c4f360` completed successfully.

### Final DB summary after the attempted batch run

Batch-wide receipt status counts:

- `posted`: `20`

Persisted structured-response coverage on the 20 batch receipts:

- receipts with merge metadata: `20`
- total `matched-count`: `104`
- total `repaired-count`: `0`
- total `filled-count`: `0`
- total `unused-structured-count`: `0`
- `refine_pending` receipts: `0`

Focus receipt (`5b6bae69-dbc1-4e3a-b801-c11289c4f360`) after the attempted rerun:

- receipt status: `posted`
- persisted structured merge meta: `matched=7 repaired=0 filled=0 unused=0`
- persisted extracted Dunhill item still reads: `qty=1.0 unit_price=27.6 line_total=27.6`

Downstream posted expense item currently used by the UI:

- `expense_item_id`: `98adadcd-3398-4660-b9b2-36b31abb4919`
- `qty=4.000 unit_price=6.90 line_total=27.60`

### Updated DB summary after the successful rerun

Batch-wide receipt status counts:

- `extracted`: `19`
- `failed`: `1`

Persisted structured-response coverage on the rerun batch receipts:

- receipts with merge metadata: `18`
- total `matched-count`: `94`
- total `repaired-count`: `0`
- total `filled-count`: `0`
- total `unused-structured-count`: `0`
- `refine_pending` receipts: `0`

Focus receipt (`5b6bae69-dbc1-4e3a-b801-c11289c4f360`) after the successful rerun:

- receipt status: `extracted`
- persisted structured merge meta: `matched=7 repaired=0 filled=0 unused=0`
- persisted extracted Dunhill item now reads: `qty=4.0 unit_price=6.9 line_total=27.6`
- post-processing `price-repairs`: `null`

Interpretation:

- The target receipt now parses correctly from the structured table path directly.
- The new fix was exercised in real data via the parser/structured overlay flow.
- `repaired-count` stayed `0` for this receipt because the merged row was parsed correctly up front rather than needing a follow-up structured-value repair.

### Findings

1. **Code-level validation passed.** The focused backend tests covering the changed parser/reconciliation path are green.
2. **Real-batch validation is currently blocked by configuration.** The repo-side batch workflow ran correctly, but the provider refused to process receipts because the LlamaParse API key was missing.
3. **The target receipt has not yet been re-extracted under the new code.** Its stored extraction JSON still shows the old collapsed value (`1 × 27.60`).
4. **The visible article price issue is corrected downstream.** The posted `expense_items` row for the Dunhill receipt now stores `4 × 6.90 = 27.60`, which is why the duplicate/price-history UI no longer shows `27.60 BAM` as a unit price.
5. **Real data has not yet exercised the new repair path.** The batch’s persisted structured merge metadata still shows `repaired-count=0`, which is expected because the rerun never reached the provider.
6. **Follow-up rerun with `.env` loaded completed real-data validation for the target receipt.** The receipt now stores the correct extracted Dunhill row (`4.0 × 6.9 = 27.6`) in `raw_extract_json`.
7. **The repair heuristic still remains a safety net.** On this receipt the parser now handled the merged qty/unit cell correctly, so no explicit structured-value repair was needed.

### Wider regression sweep on more receipts

Additional artifacts:

- `tmp/receipt-ocr-batch-4-20260327-184022.txt`
- `tmp/receipt-ocr-batch-6-20260327-184300.txt`
- `tmp/receipt-ocr-retry-img-3971-20260327-184659.txt`

Expanded scope:

- batch `4` (`20` receipts)
- batch `6` (`20` receipts)
- retry of the prior timeout `IMG_3971.jpeg` (`1` receipt)
- total additional receipts exercised: `41`

Run outcomes:

- batch `4`: `{:ok 20}` → final statuses `20 extracted`
- batch `6`: `{:ok 20}` at runner level, final statuses `19 extracted`, `1 review_required`
- retry `IMG_3971.jpeg`: `{:ok 1}` → final status `extracted`

Combined DB summary for the additional regression sweep:

- final status counts: `40 extracted`, `1 review_required`
- receipts with structured merge metadata: `34`
- total `matched-count`: `201`
- total `repaired-count`: `0`
- total `filled-count`: `0`
- total `unused-structured-count`: `0`
- `refine_pending` receipts: `0`

Anomaly observed:

- `IMG_4043.jpeg` (`d38719fb-2d8e-4743-aaaa-e4cadc06fad4`) landed in `review_required`
- extracted total guess: `98.95`
- extracted item sum: `8.95`
- extracted items only contained one line (`x145cm`, `8.95`)
- structured merge counts were all `0`

Interpretation:

- The additional regression sweep did **not** surface a repeat of the Dunhill collapsed-unit-price bug.
- The one `review_required` receipt looks like a broader OCR extraction failure / poor source parse, not a regression in the merged qty+unit table handling added for Dunhill.
- The previously timed-out `IMG_3971.jpeg` succeeded on isolated retry, so the timeout appears transient.

### Next step

Real-data validation is now complete for the focus receipt and the broader regression sweep. The main follow-up left is to inspect `IMG_4043.jpeg` separately if you want to improve handling of receipts with severely incomplete OCR item extraction. Suggested checks:

- `tmp/receipt-ocr-batch-6-20260327-184300.txt`
- receipt `d38719fb-2d8e-4743-aaaa-e4cadc06fad4` / `IMG_4043.jpeg`
- compare `total_amount_guess` vs extracted item sum for that receipt
- the focus receipt’s `raw_extract_json -> extraction -> items`
- the receipt-level `structured_response_merge.repaired-count`

### Follow-up fix for `IMG_4043.jpeg`

Additional artifacts:

- `tmp/llamaparse-test-20260327-193100.log`
- `tmp/receipt-ocr-reparse-img-4043-20260327-193500.txt`

Root cause:

- This was primarily an **extract-path** issue, not a refinement-path issue.
- The raw OCR text contained the full product label split across two lines:
  - `VRECA VAKUM ZA ODJECU HENGER XL 70`
  - `x145cm 8,95E`
- The text-item fallback parser kept only the second line as the item label, so the stored extraction became just `x145cm`.
- Separately, the OCR total line was noisy (`ME98,95`), while the fallback payment lines and item sum consistently supported `8,95`.

Fix implemented:

- text-item fallback now combines a pending descriptive label with a short continuation-style priced line (such as `x145cm 8,95E`)
- text-item label detection now avoids over-filtering product lines that merely end with a number
- total extraction now prefers the item sum when at least two fallback payment/total lines agree with it and the preferred total candidate clearly disagrees

Validation:

- focused backend tests: `33 tests, 154 assertions, 0 failures`
- targeted real receipt rerun:
  - receipt id: `d38719fb-2d8e-4743-aaaa-e4cadc06fad4`
  - final status: `extracted`
  - `review-required-count`: `0`

Persisted result after rerun:

- status: `extracted`
- `total_amount_guess`: `8.95`
- extracted item:
  - `raw_label = VRECA VAKUM ZA ODJECU HENGER XL 70 x145cm`
  - `qty = 1.0`
  - `unit_price = 8.95`
  - `line_total = 8.95`
- `llm_refine`: `null`

Interpretation:

- This receipt is now handled correctly in extraction alone.
- No refinement fallback was needed once the text-item parsing and noisy-total handling were corrected.

### Mixed sibling + regression rerun

Additional artifacts:

- `tmp/receipt-ocr-reparse-mixed-20260327-194500.txt`

Scope:

- sibling cluster around the original anomaly:
  - `IMG_4041.jpeg`
  - `IMG_4042.jpeg`
  - `IMG_4043.jpeg`
  - `IMG_4044.jpeg`
- different regression controls from other batches:
  - `IMG_3985.jpeg` (Dunhill structured-merge case)
  - `IMG_3971.jpeg` (previous timeout/retry case)
  - `IMG_3955.jpeg`
  - `IMG_3926.HEIC`
  - `IMG_4062.jpeg`
  - `IMG_4071.jpeg`

Run outcome:

- batch label: `img-4043-regression-mixed`
- processed: `10`
- summary: `{:ok 10}`
- final status counts: `{"extracted" 10}`

DB verification after rerun:

- all 10 receipts remained `extracted`
- none of the 10 receipts stored `llm_refine`
- `IMG_4043.jpeg` persisted as:
  - `raw_label = VRECA VAKUM ZA ODJECU HENGER XL 70 x145cm`
  - `qty = 1.0`
  - `unit_price = 8.95`
  - `line_total = 8.95`
  - `total_amount_guess = 8.95`
- sibling receipts also looked sane:
  - `IMG_4041.jpeg`: `4` items, total `12.90`
  - `IMG_4042.jpeg`: `5` items, total `21.45`
  - `IMG_4044.jpeg`: `DUNHILL MASTER BLEND SILVER`, `3 × 6.90 = 20.70`
- control receipts remained stable:
  - `IMG_3985.jpeg`: `7` items, total `42.95`
  - `IMG_3971.jpeg`: `2` items, total `34.19`
  - `IMG_3926.HEIC`: `4` items, total `29.99`
  - `IMG_4071.jpeg`: `2` items, total `411.15`

Interpretation:

- The extraction fix appears localized and safe across the checked real-data sample.
- It improved the `IMG_4043.jpeg` sibling case without pushing nearby or unrelated receipts into refinement or review.
- The Dunhill fix and the new split-line-text fix coexist cleanly in the same regression set.

## 2026-03-28 — Full LlamaParse corpus regression sweep

### Sweep scope

- Profile: `dev`
- OCR workflow: `llamaparse`
- Batch size: `20`
- Requested coverage: all receipts currently in statuses `posted` + `extracted` in the dev database
- Planner result: `157` receipts across `8` batches, spanning two tenants
- Tenant split at plan time:
  - `enes-jakic`: `149` receipts
  - `jakic-enes-test`: `8` receipts
- New execution rule used during this sweep: after each workflow modification, rerun all previously completed batches before moving to the next unseen batch

### Validation evidence

Artifact:

- `tmp/receipt-ocr-extract-focused-tests.txt`

Final focused result after the last parser change:

- `40 tests, 189 assertions, 0 failures`

### Batch plan artifacts

- `tmp/receipt-ocr-batches.edn`
- `tmp/receipt-ocr-batches.txt`

Planner summary:

- statuses included: `posted`, `extracted`
- total receipts planned: `157` (database-wide, not scoped to `/t/enes-jakic/receipts`)
- total batches: `8`

### Parser improvements made during the sweep

Only `batch-1` exposed real parsing regressions. Two targeted fixes were added to the structured-text fallback in `llamaparse/receipt_extraction/text_items.clj`:

1. support for `pending label` + `qty/unit/total on next line`
2. support for `pending label` + `qty-only line` + `unit/total on following line`

These fixes were validated by focused tests and then by rerunning all previously completed batches before continuing.

Receipts directly fixed by those changes:

- `IMG_3620.jpg` → now extracts `MLIJEKO MEGGLE 3,2% 657` as `3 × 2.25 = 6.75`
- `IMG_3812.jpg` → now extracts two sugar lines totaling `6.25`
- `IMG_3814.jpg` → now extracts both the urine cup line (`2 × 0.55 = 1.10`) and the thermometer line (`7.15`)

### Batch-by-batch results

| Batch | Planned receipts | Result | Notes |
| --- | ---: | --- | --- |
| `batch-1` | 20 | completed after 2 reruns | initial run surfaced 3 structured-text parsing gaps; final rerun finished `20/20 extracted` |
| `batch-2` | 20 | clean | `{:ok 20}`, no workflow changes needed |
| `batch-3` | 20 | clean | `{:ok 20}`, no workflow changes needed |
| `batch-4` | 20 | clean | `{:ok 20}`, no workflow changes needed |
| `batch-5` | 20 | clean | `{:ok 20}`, no workflow changes needed |
| `batch-6` | 20 | clean | `{:ok 20}`, no workflow changes needed |
| `batch-7` | 20 | parser-clean with blockers | `12` receipts reran successfully; `8` linked-expense receipts were blocked by reset safety rules |
| `batch-8` | 17 | clean | `{:ok 17}`, no workflow changes needed |

### Batch artifacts

- `tmp/receipt-ocr-batch-1-*.txt`
- `tmp/receipt-ocr-batch-1-rerun-*.txt`
- `tmp/receipt-ocr-batch-1-rerun2-*.txt`
- `tmp/receipt-ocr-batch-2-*.txt`
- `tmp/receipt-ocr-batch-3-*.txt`
- `tmp/receipt-ocr-batch-4-*.txt`
- `tmp/receipt-ocr-batch-5-*.txt`
- `tmp/receipt-ocr-batch-6-*.txt`
- `tmp/receipt-ocr-batch-7-*.txt`
- `tmp/receipt-ocr-batch-7-linked-no-reset-*.txt`
- `tmp/receipt-ocr-batch-8-*.txt`

### Linked-expense blockers in batch 7

These `8` receipts are already linked to expenses, so `reset-for-ocr!` refuses to clear them by design. A retry with `--no-reset` also skipped them because the runner only processes receipts already in OCR-active statuses when reset is disabled.

All `8` blocked receipts belong to tenant `jakic-enes-test`, not tenant `enes-jakic`.

Blocked receipt ids:

- `12687b13-e28d-448e-8e92-3d005523c519`
- `800c2768-13c4-4522-aac7-954cf7d7111d`
- `d1a28a7f-4adf-4096-a195-63ca652d42e7`
- `395d230e-954a-43de-916a-36138b5e4311`
- `c1bb90fd-e22d-4431-a9ec-d0bac754e3a9`
- `7c4fc534-65f2-4dba-ba19-e1033c241025`
- `42bdc705-57e2-40f5-b012-e726f28f1dd8`
- `c650b75a-bf12-4341-b761-98f16e546f61`

Interpretation:

- this is a **workflow safety constraint**, not a parsing regression
- the corpus sweep successfully exercised every safely reparsable receipt in the target set
- finishing the remaining `8` would require either unlinking their expenses first or intentionally changing the protected reparse workflow for linked receipts

### Final sweep summary

- target receipts in sweep plan: `157`
- successfully reparsed in this run: `149`
- blocked by linked-expense safety rules: `8`
- new parser workflow changes required during the sweep: `2`
- batches that needed parser changes: `1`
- batches clean after parser stabilization: `2` through `8` (with only the batch-7 linked-expense blocker subset)

Final database status snapshot after the sweep:

- `extracted`: `151`
- `posted`: `6`
- `review_required`: `2`

Tenant-specific clarification for the user receipts page:

- `/t/enes-jakic/receipts` correctly shows `149` receipts because tenant `enes-jakic` has `149` receipts total, all `extracted`
- the remaining receipts from the database-wide sweep belong to tenant `jakic-enes-test`
- the `8` blocked linked-expense receipts are all in `jakic-enes-test`
- the `2` `review_required` receipts are also in `jakic-enes-test`

Interpretation:

- The parsing workflow fine-tuning converged after the `batch-1` fixes.
- No later batch surfaced a new parsing pattern that required additional code changes.
- The final parser handles the structured text variants that were breaking real receipts early in the sweep.
- The only incomplete portion of the corpus run is the explicitly documented set of linked-expense receipts that the current safety rails prevent from being reparsed in place.

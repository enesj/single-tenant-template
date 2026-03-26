# Receipt OCR Batch Testing Results

## Last updated

2026-03-26

## Goal

Track OCR reparse validation in batches so we can inspect extraction regressions, spot store-specific patterns, and iterate on the hybrid structured-row merge.

## Baseline after implementation

### Focused automated validation

- Status: ✅ passed
- Scope:
  - `app.domain.backend.expenses.integrations.llamaparse-test`
  - `app.domain.backend.expenses.workers.receipt-ocr-extraction.reconciliation-test`
  - `app.domain.backend.expenses.workers.receipt-ocr-extraction.items-test`
  - `app.domain.expenses.services.expenses-test`
- Result: **57 tests, 228 assertions, 0 failures**
- Saved output: `tmp/hybrid-price-consistency-kaocha.txt`

### Execution note

- Batch 1 used the original one-off runner and processed receipts sequentially.
- Batches 2-5 used the updated bounded-parallel batch runner in `tmp/reparse_receipts_batch.clj`.
- Parallelism for batches 2-5 matched the repo's UI queue pattern: `RECEIPT_OCR_UI_MAX_CONCURRENT=6`.
- In this tool session, a direct Clojure MCP eval endpoint was not exposed, so execution used the same app code path through the local Clojure runtime with the project environment loaded.

## Overall outcome

- Total receipts reparsed: **85 / 85**
- Final database state: **85 posted**, **0 extracted**
- Receipts with `structured_response_merge` metadata: **85 / 85**
- Total `matched-count` across all batches: **336**
- Total `repaired-count` across all batches: **0**
- Total `filled-count` across all batches: **0**

### Interpretation

- The hybrid structured-row overlay ran on every reparsed receipt.
- Structured rows matched many provider rows (`matched-count` > 0 in every batch), so the alignment logic is definitely active.
- None of these 85 receipts needed a `repaired-count` correction, which suggests the new hybrid repair path is available but this dataset did not reproduce the Dunhill-style quantity/unit-price mismatch after full reparse.
- Batch 4 triggered Cerebras refine activity on a small number of receipts; the receipts still completed successfully and ended in `posted` status.

## Batch summary

| Batch | Receipt count | Status | Notes |
| --- | ---: | --- | --- |
| 1 | 20 | Completed | Sequential runner, all 20 posted, observed wall-clock ~239s |
| 2 | 20 | Completed | Parallel runner (`6` workers), all 20 posted in `48733.221041 ms` |
| 3 | 20 | Completed | Parallel runner (`6` workers), all 20 posted in `43519.569958 ms` |
| 4 | 20 | Completed | Parallel runner (`6` workers), all 20 posted in `49026.204584 ms`; included Cerebras refine activity |
| 5 | 5 | Completed | Parallel runner (`6` workers), all 5 posted in `13422.290792 ms` |

## Batch details

### Batch 1

- Status: Completed
- Execution mode: Sequential
- Receipt IDs:
  - `e63f13bc-09fd-464d-9843-6b6cf09355d3`
  - `1516c6cc-74f3-4a69-a473-5c30e0456c8e`
  - `8ae5af2a-9c1f-4a6a-ba33-5b125857dbab`
  - `d63e5ae4-b659-4bbb-ae2c-f297da519d11`
  - `44a0e332-bc02-42be-ab81-6505ffe08387`
  - `bf5fe4c4-b109-4cea-bcf6-1f116b54379c`
  - `8dbdc804-13f9-441f-92b0-6c2a8cea8d17`
  - `8a7eacc2-b665-4880-8e69-977715f9d21f`
  - `1872e7e3-e82f-4844-bbdc-d52a8b7afa2d`
  - `033f4ae2-223c-4068-aee4-e1c900861cda`
  - `7f2b208a-aaa1-40ed-8251-3c641720f64f`
  - `0081d3a1-ffbd-49e2-a4b4-72ae49a6f4a9`
  - `b91037a4-03e9-4bb8-94ce-75c3aa58de29`
  - `996f9d39-ecf1-440b-99f5-3ad1fe8674c9`
  - `39d7a174-8d32-4a5e-97ab-2718b1a614e4`
  - `80282d41-c82c-446e-881e-dca422c158f4`
  - `7ff265a9-571d-46b4-b8d4-7699572c1570`
  - `b65d9163-fcd6-46f7-86ae-846f32703bae`
  - `aabfe905-f480-4158-aa8b-945122a65525`
  - `4c942852-2292-446d-ac65-cbb8f718937a`
- Output artifacts:
  - `tmp/receipt-ocr-batch-1.txt` — first failed attempt (webserver port collision)
  - `tmp/receipt-ocr-batch-1-rerun.txt` — second failed attempt (missing env in subprocess)
  - `tmp/receipt-ocr-batch-1-env.txt` — successful run with environment loaded
- Result summary:
  - Database state after completion: **20 posted / 0 extracted**
  - Structured merge coverage: **20 / 20** receipts
  - `matched-count` total: **113**
  - `repaired-count` total: **0**
  - `filled-count` total: **0**
- Findings:
  - The environment-loaded rerun succeeded.
  - This batch established the working execution path and confirmed the OCR workflow was resolving to LlamaParse in the local environment.
  - Observed log span from the successful artifact is about **239 seconds**, which made the case for moving the remaining batches to bounded parallel execution.

### Batch 2

- Status: Completed
- Execution mode: Parallel (`RECEIPT_OCR_UI_MAX_CONCURRENT=6`)
- Receipt IDs:
  - `3120bdf5-e598-4fbd-a6de-a2219185f9d0`
  - `472086e1-9cd9-4b07-832d-07614e411bba`
  - `4e7751c2-fd8e-4d73-b5dd-9297602671ae`
  - `9db03d1e-695c-4835-b8cb-377d07f71e33`
  - `97276a32-01e4-43e9-9529-d8951df67bc5`
  - `815797c2-11c9-4537-9643-6a733a4468bf`
  - `cadb962a-d342-4145-9426-3d45b33f3a69`
  - `87c1933c-b4f0-480a-abb4-cceb900857e6`
  - `cda9f83c-c436-4ba7-af77-1ac3d29c2439`
  - `435add86-d79a-4cae-9ebf-1d364ad77878`
  - `4db8bc51-6ae8-4556-b66b-4ade2e82fcc1`
  - `1fc1ac6b-faba-401d-9939-ae05d70b5bb2`
  - `c71ad713-2712-4d96-876e-7cf4aab62d4f`
  - `bc88b518-9d1d-4f73-b026-71cd5484c4c9`
  - `4136900f-7c42-43f7-9ce3-2040cb4df3ba`
  - `aae2d2f4-bb57-46bc-8042-052fb9d65cc4`
  - `98a48908-0876-47ed-831a-63eadeb78f19`
  - `0f179280-cd4c-4ed3-ac3f-2136cad65c39`
  - `3389ce66-1568-4627-b0ab-010cf546ee82`
  - `cb2cc1e3-b7a3-4f53-b37a-171a0c5ff566`
- Output artifact: `tmp/receipt-ocr-batch-2.txt`
- Result summary:
  - Batch runner summary: `{:ok 20}`
  - Duration: **48733.221041 ms**
  - Database state after completion: **20 posted / 0 extracted**
  - Structured merge coverage: **20 / 20** receipts
  - `matched-count` total: **57**
  - `repaired-count` total: **0**
  - `filled-count` total: **0**
- Findings:
  - Parallel execution cut the wall-clock time dramatically compared to Batch 1.
  - No review-required receipts remained at the end of the batch.

### Batch 3

- Status: Completed
- Execution mode: Parallel (`RECEIPT_OCR_UI_MAX_CONCURRENT=6`)
- Receipt IDs:
  - `615a8fcd-f223-4508-b299-c0474deb394c`
  - `39f96503-0c02-43c9-83aa-1796c0d07211`
  - `d7fec87b-7306-4eda-87f3-58f5e551f0e9`
  - `06c5c852-0af1-4a8d-88c7-ef4fdfdecb6c`
  - `8a0cdf37-1cba-45b3-898d-517155c4276a`
  - `e3be3d6b-f7fd-42a4-bef0-fac11a5c63c9`
  - `3e1a3332-ee0c-4682-bd8a-4d9c5c3b842e`
  - `105945ed-770e-4437-b82d-178c1d35f035`
  - `3813493a-5d83-4830-9c9b-5d22431a1298`
  - `a5cacc77-c754-4ab1-8694-04ee208d3119`
  - `59971bb2-874f-4387-b82c-42ec8bc6ba6a`
  - `43b7a755-6ad0-49e1-941b-7320fc647194`
  - `47f0705d-293b-4885-8ba2-93273b1706fa`
  - `dc5985bb-480f-4a39-bda4-cbe8e555f26c`
  - `0daa58dc-4f20-4fee-b23b-f8d41a83e2d5`
  - `5ce0fa6f-0fce-48a7-9966-df23cf28b3aa`
  - `722701de-26b5-40bb-86af-9736d4670194`
  - `0f4a23d4-1b39-4737-a399-ad9f630d8797`
  - `4d711c9b-2e70-4930-8a03-25ff590fe957`
  - `7292c092-5ea6-49f2-83eb-5e233c4ed2ac`
- Output artifact: `tmp/receipt-ocr-batch-3.txt`
- Result summary:
  - Batch runner summary: `{:ok 20}`
  - Duration: **43519.569958 ms**
  - Database state after completion: **20 posted / 0 extracted**
  - Structured merge coverage: **20 / 20** receipts
  - `matched-count` total: **40**
  - `repaired-count` total: **0**
  - `filled-count` total: **0**
- Findings:
  - This was the fastest full 20-receipt batch.
  - The hybrid matcher continued to attach structured metadata across the entire batch, but no receipt needed an explicit repair overlay.

### Batch 4

- Status: Completed
- Execution mode: Parallel (`RECEIPT_OCR_UI_MAX_CONCURRENT=6`)
- Receipt IDs:
  - `38dece6c-cd98-4407-8242-7e916d5b8345`
  - `0ea3b933-8828-49cd-95ce-c4201823653c`
  - `2e09278c-4281-45c6-b066-31d1ebde1b25`
  - `03a9505d-054d-4980-96b0-16393fc646ca`
  - `e0f4024b-b08b-4c6c-8254-a0aab774a372`
  - `7ac7b376-140b-4413-a09c-d8a2d403a6da`
  - `df1aa02d-2e90-4118-af15-e3725490e701`
  - `5caaf1cd-ebd7-4f12-acc2-bf1202a742d3`
  - `fe621e3c-dcc6-4b93-a1af-74ac6855777e`
  - `195d5e85-8162-4a3a-a8f9-e8699c6036e3`
  - `227f5ffc-4aaa-416c-9e76-13e722e99678`
  - `6539db3b-9a64-413b-8909-0ea908dc9c7c`
  - `ae878e18-c4a6-4c2a-89bc-fe83f44ded65`
  - `d788814e-4693-4bee-abfb-58d1cccf1fc5`
  - `abc9ed28-3e58-4635-8cd6-e545340f5798`
  - `861077ec-b805-4d29-92f5-f51c473050c7`
  - `901032cf-d8a2-432d-845e-0547acbca64d`
  - `5bb0f884-fb50-470c-b48b-73c41b606611`
  - `d44bd30a-0049-4038-ae10-a48043f09636`
  - `2fa064cd-b900-4442-ba31-6539f6a065c9`
- Output artifact: `tmp/receipt-ocr-batch-4.txt`
- Result summary:
  - Batch runner summary: `{:ok 20}`
  - Duration: **49026.204584 ms**
  - Database state after completion: **20 posted / 0 extracted**
  - Structured merge coverage: **20 / 20** receipts
  - `matched-count` total: **104**
  - `repaired-count` total: **0**
  - `filled-count` total: **0**
- Findings:
  - Two receipts in this batch triggered Cerebras refine activity and still completed successfully.
  - Batch 4 had the highest structured-match volume among the parallel batches.

### Batch 5

- Status: Completed
- Execution mode: Parallel (`RECEIPT_OCR_UI_MAX_CONCURRENT=6`)
- Receipt IDs:
  - `f58b1e1c-5f81-4fd3-b145-bbd6b9fd8cf8`
  - `92a6b706-864e-4ab1-8909-10fe46181217`
  - `c7c377ab-3257-4239-8e22-6ab117e4900d`
  - `be18c6e9-1c73-4f07-832d-2b5ebbee092e`
  - `3dbe4043-4353-44db-a9e6-dd1b9f9d5623`
- Output artifact: `tmp/receipt-ocr-batch-5.txt`
- Result summary:
  - Batch runner summary: `{:ok 5}`
  - Duration: **13422.290792 ms**
  - Database state after completion: **5 posted / 0 extracted**
  - Structured merge coverage: **5 / 5** receipts
  - `matched-count` total: **22**
  - `repaired-count` total: **0**
  - `filled-count` total: **0**
- Findings:
  - Final cleanup batch completed without incident.
  - The bounded parallel runner handled the smaller batch correctly without any special-casing.

## Follow-up notes

- If we want to stress the explicit repair branch further, the next step should be to identify receipts where `structured_response_merge.matched-count > 0` but the stored item arithmetic still looks suspicious, then inspect those receipts manually.
- For future reruns, the parallel batch runner in `tmp/reparse_receipts_batch.clj` is the faster operational path unless a direct Clojure MCP eval endpoint is available in-session.
- The current dataset validates the hybrid merge wiring and coverage, but not the `repaired-count` branch on real receipts; that branch is still covered by focused tests.

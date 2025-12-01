<!-- ai: {:tags [:tools :rag] :kind :reference} -->

Title: Local RAG Index for Docs

Overview
- Hybrid BM25 + cosine TF-IDF index for the docs stored at `docs/rag/index.edn`.
- No network calls, no services. Queries run in-memory via Babashka.

Build the index
- bb cli-tools/rag.clj index docs docs/rag

Query the index
- Basic: bb cli-tools/rag.clj query -q "tenant RLS policies"
- Top-k: bb cli-tools/rag.clj query -q "shadow cljs build" -k 5
- Namespace filter: bb cli-tools/rag.clj query -q "routes auth" -n app.backend.routes
- Tag filter: bb cli-tools/rag.clj query -q "pagination" -t backend
- Minimum score: bb cli-tools/rag.clj query -q "pagination" -m 0.05 (defaults to `-m 0.02` to drop zero-similarity hits)

Output format
- <score>  <path>#<anchor> — <heading> (cos 0.652 | bm25 1.844)
- Preview snippet (first ~200 chars)

Notes
- Metadata must be provided via a front-matter HTML comment block (see below) at the TOP of each doc; only the first ~40 lines are scanned when indexing.
- Tags are required for the `-t` filter and namespaces for the `-n` filter; missing metadata means those filters will skip the file.
- Chunks store both cosine and BM25 scores; final ranking blends them (0.65 cosine / 0.35 BM25) to reward exact keyword overlap while keeping semantic relevance.
- Rebuild whenever docs change significantly. The builder ignores `docs/rag/` automatically.

Metadata front matter
- Place this at the very top of any Markdown doc:
  ```markdown
  <!-- ai: {:namespaces [app.shared.pagination]
           :tags [:shared :frontend :backend]
           :kind :guide} -->
  ```
- Keep the block within the first 40 lines so the indexer can detect it.
- Add whichever fields apply:
  - `:namespaces` – symbols referenceable via `-n`
  - `:tags` – enums (e.g. `:backend`, `:frontend`, `:migrations`) for `-t`
  - `:kind` – `:guide`, `:runbook`, `:reference`, etc., mostly for reporting

Future options (optional)
- Hybrid search: combine BM25 (keyword) score with cosine for even better precision.
- Model embeddings: swap TF-IDF weights with sentence-transformers later; keep the same index schema.

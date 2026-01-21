# Cerebras Inference Documentation Summary

> **Source**: https://inference-docs.cerebras.ai/introduction
> **Purpose**: Using Cerebras models to improve MistralOCR output quality

---

## Overview

Cerebras Inference is the world's fastest AI inference platform, achieving speeds of **1,000-3,000 tokens/second** - approximately **20x faster** than competitors like Claude Sonnet 4.5.

- **Base URL**: `https://api.cerebras.ai/v1`
- **OpenAI Compatible**: Full OpenAI API compatibility for easy migration
- **Free API Key**: Available at [API Playground](https://inference-docs.cerebras.ai/)

---

## Quick Start

### 1. Get API Key
Visit [Cerebras API Keys](https://inference-docs.cerebras.ai/) and navigate to "API Keys"

### 2. Set Environment Variable
```bash
export CEREBRAS_API_KEY="your-api-key-here"
export CEREBRAS_RECEIPT_REFINE_ENABLED=true
```

### 3. Clojure (JVM) Setup

- This repo already includes suitable HTTP/JSON libs for Clojure (JVM): `clj-http` and `cheshire` in `deps.edn`.
- Receipt refinement is implemented in app code:
  - Client: `src/app/domain/backend/expenses/integrations/cerebras.clj`
  - Prompt + JSON Schema: `src/app/domain/backend/expenses/integrations/cerebras/receipt_refine.clj`

### 4. Basic Usage

**Clojure (JVM) — in-app integration:**
```clj
(ns user
  (:require [app.domain.backend.expenses.integrations.cerebras :as cerebras]
            [app.domain.backend.expenses.integrations.cerebras.receipt-refine :as receipt-refine]))

;; Build config from app-config + env (env wins)
(def cfg (cerebras/build-config {}))

;; Build messages (system + user prompt)
(def messages (receipt-refine/build-chat-messages "<mistral-ocr-markdown>"))

;; Call refine (returns app-shaped extraction + raw response)
(cerebras/refine-receipt-markdown! cfg "<mistral-ocr-markdown>")
```

**cURL:**
```bash
curl https://api.cerebras.ai/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CEREBRAS_API_KEY" \
  -d '{
    "model": "llama-3.3-70b",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

---

## Available Models

### Production Models (Stable, Production-Ready)

| Model Name | Model ID | Parameters | Speed (tokens/s) |
|---|---|---|---|
| Llama 3.1 8B | `llama3.1-8b` | 8B | ~2,200 |
| Llama 3.3 70B | `llama-3.3-70b` | 70B | ~2,100 |
| OpenAI GPT OSS | `gpt-oss-120b` | 120B | ~3,000 |
| Qwen 3 32B | `qwen-3-32b` | 32B | ~2,600 |

### Preview Models (Evaluation Only)

| Model Name | Model ID | Parameters | Speed (tokens/s) |
|---|---|---|---|
| Qwen 3 235B Instruct | `qwen-3-235b-a22b-instruct-2507` | 235B | ~1,400 |
| Z.ai GLM 4.6 | `zai-glm-4.6` | 357B | ~1,000 |

> **Note**: All models are unpruned original versions. Precision varies by model (FP16, FP8).

---

## Key Features for OCR Post-Processing

### Structured Outputs (JSON Schema Enforcement)

**Critical for OCR**: Ensures consistent, validated JSON output for receipt parsing.

#### Benefits
- **Reduced Variability**: Consistent outputs per predefined schema
- **Type Safety**: Enforces correct data types
- **Easier Parsing**: Direct programmatic use without extra processing

#### Usage Example (Clojure JVM)

```clj
(ns user
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [app.domain.backend.expenses.integrations.cerebras.receipt-refine :as receipt-refine]))

(def base-url "https://api.cerebras.ai/v1")
(def api-key (System/getenv "CEREBRAS_API_KEY"))

(defn refine-with-json-schema
  [markdown]
  (let [body {:model "llama-3.3-70b"
              :messages (receipt-refine/build-chat-messages markdown)
              :temperature 0
              :response_format {:type "json_schema"
                                :json_schema {:json_schema {:name "ReceiptExtractionCentsV1"
                                                            :strict true
                                                            :schema receipt-refine/receipt-extraction-json-schema}}}}
        resp (http/post (str base-url "/chat/completions")
                        {:headers {"Authorization" (str "Bearer " api-key)
                                   "Content-Type" "application/json"}
                         :socket-timeout 20000
                         :conn-timeout 5000
                         :throw-exceptions false
                         :body (json/generate-string body)})]
    (-> resp :body (json/parse-string true))))
```

#### Schema Features
- **Data Types**: String, Number, Boolean, Integer, Object, Array, Enum, null
- **Union Types**: `anyOf` for multiple possible types (max 5)
- **Nested Structures**: Up to 5 layers of nesting
- **Required Fields**: Enforced presence
- **Additional Properties**: Set to `false` to restrict extra fields
- **Enums**: Value constraints with `enum` keyword

#### Schema Limitations
- No recursive schemas
- Max schema length: 5,000 characters
- All properties must be in `required` array (use `anyOf` for nullable fields)
- Use `$defs` instead of `definitions`

### JSON Mode (Alternative to Structured Outputs)

For flexible JSON without strict schema enforcement (Clojure JVM):

```clj
(ns cerebras.json-mode
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def base-url "https://api.cerebras.ai/v1")

(defn parse-receipt-json-mode
  [raw]
  (let [api-key (System/getenv "CEREBRAS_API_KEY")
        body {:model "llama-3.3-70b"
              :messages [{:role "system" :content "You must respond with valid JSON only."}
                         {:role "user" :content (str "Parse this receipt: " raw)}]
              :response_format {:type "json_object"}}
        resp (http/post (str base-url "/chat/completions")
                        {:headers {"Authorization" (str "Bearer " api-key)
                                   "Content-Type" "application/json"}
                         :body (json/generate-string body)})]
    (-> resp :body (json/parse-string true))))
```

**Limitations:**
- Not compatible with streaming (`stream=false`)
- Must explicitly instruct model to generate JSON

### Comparison: Structured Outputs vs JSON Mode

| Feature | Structured Output | JSON Mode |
|---|---|---|
| Valid JSON | Yes | Yes |
| Schema Adherence | Yes (enforced) | No (flexible) |
| API Setting | `response_format: {type: "json_schema", ...}` | `response_format: {type: "json_object"}` |

---

## Streaming Responses

```clj
(ns cerebras.streaming
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]))

(def base-url "https://api.cerebras.ai/v1")

(defn stream-chat
  [model messages]
  (let [api-key (System/getenv "CEREBRAS_API_KEY")
        req {:headers {"Authorization" (str "Bearer " api-key)
                       "Content-Type" "application/json"}
             :body (cheshire.core/generate-string
                    {:model model :messages messages :stream true})
             :as :stream
             :throw-exceptions false}
        resp (http/post (str base-url "/chat/completions") req)]
    (with-open [rdr (io/reader (:body resp))]
      (doseq [line (line-seq rdr)]
        (when (and (seq line) (.startsWith ^String line "data:"))
          (let [json (subs line 5)]
            (when-not (= (clojure.string/trim json) "[DONE]")
              (print json) (flush))))))))

;; Example
(comment
  (stream-chat "llama-3.3-70b" [{:role "user" :content "Explain OCR..."}]))
```

---

## OpenAI Compatibility

Full OpenAI API compatibility means:
- Same base URL structure
- Same request/response formats
- Easy migration from OpenAI SDKs

---

## Rate Limits & Pricing

Check the [Pricing page](https://inference-docs.cerebras.ai/pricing) and [Rate Limits](https://inference-docs.cerebras.ai/rate-limits) for details.

---

## Recommended Setup for MistralOCR Enhancement

### In-App Two-Stage Pipeline

Receipt OCR now supports an optional refine step:

1. Mistral OCR produces `parsed_markdown` for the receipt.
2. If `CEREBRAS_RECEIPT_REFINE_ENABLED=true` and `CEREBRAS_API_KEY` is set, the worker calls Cerebras to extract structured data.
3. The worker still runs markdown heuristics + reconciliation to make the final stored extraction more robust.

Refine results are stored under `raw_extract_json.llm_refine` for debugging.

---

## Resources

- **Docs**: https://inference-docs.cerebras.ai/
- **API Playground**: Get free API key and test models
- **Python SDK**: https://github.com/cerebras/cerebras_cloud_sdk_python
- **Node.js SDK**: https://github.com/cerebras/cerebras_cloud_sdk_node
- **Service Status**: https://inference-docs.cerebras.ai/support/service-status
- **Error Codes**: https://inference-docs.cerebras.ai/support/error-codes

---

## Key Insights for OCR Improvement

`★ Insight ─────────────────────────────────────`
1. **Structured Outputs**: Use `json_schema` with `strict: true` to enforce validated JSON receipt data - eliminates parsing errors from messy OCR output

2. **Model Selection**: `llama-3.3-70b` offers best balance of speed (2100 tok/s) and quality for post-processing; `qwen-3-32b` is faster if needed

3. **Prompt Strategy**: Provide MistralOCR raw text + explicit field extraction instructions to leverage Cerebras models' strong instruction following for clean, structured data
`─────────────────────────────────────────────────`

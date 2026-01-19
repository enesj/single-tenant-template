<!-- ai: {:tags [:validation :overview :single-tenant] :kind :overview} -->

# Validation System Docs (Single-Tenant)

Validation is metadata-driven and lives alongside the schema in `resources/db/models.edn`. The same metadata powers backend validation and admin UI form hints.

## Files in this folder
- [overview.md](overview.md) – high-level architecture and key concepts
- [implementation-guide.md](implementation-guide.md) – how to add validation metadata and consume it in admin UI/backend
- [technical-reference.md](technical-reference.md) – core APIs and metadata shape
- [troubleshooting.md](troubleshooting.md) – common problems and fixes

## Quick start
1. Add `:validation` metadata to a field in `resources/db/models.edn`.
   ```edn
   [:email [:varchar 255]
    {:null false
     :validation {:type :email
                  :messages {:invalid "Please enter a valid email address"}
                  :ui {:placeholder "Enter your email address"}}}]
   ```
2. Backend uses the metadata to build Malli validators and return clear error messages.
3. Admin UI uses the same metadata for input types/placeholders and client-side feedback.

## Principles
- **Single source**: Define rules once in models; reuse everywhere.
- **Cross-platform**: Shared code works in Clojure and ClojureScript.
- **Non-invasive**: Validation metadata does not change schema generation.

Keep new validation rules in the model files and update these docs when APIs change.***

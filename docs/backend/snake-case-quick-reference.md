<!-- ai: {:tags [:backend] :kind :guide} -->

# Snake_case Refactoring Quick Reference

## 🔍 Detection Commands

```bash
# Find snake_case violations in frontend
rg --type cljs ":[a-z_]+_[a-z_]+" src/ | grep -v "_v2\|/full_name\|/subscription_tier"

# Find snake_case violations in backend return maps
rg --type clj ":[a-z_]+_[a-z_]+" src/app/backend/services/ | grep -v "sql\|jdbc\|db"

# Find get-in usage with snake_case
rg --type cljs "get-in.*:.*_" src/

# Find specific patterns
rg --type cljs ":.*_.*overview\|:.*_.*status\|:.*_.*data" src/
```

## 🔧 Fix Patterns

### Backend Service Pattern
```clojure
;; Add import
[app.backend.services.admin.monitoring.shared :as monitoring-shared]

;; Replace function
(defn get-data [db]
  (monitoring-shared/with-monitoring-error-handling
    "get data"
    (fn [[result1 result2]]
      {:kebab-case-key1 result1
       :kebab-case-key2 result2})
    {:kebab-case-key1 []
     :kebab-case-key2 {}}
    (fn []
      [(query1) (query2)])))
```

### Frontend Component Pattern
```clojure
;; Update data access
(:snake_case_key data)     → (:kebab-case-key data)
(get-in data [:snake_case]) → (get-in data [:kebab-case])

;; Keep database columns unchanged
(:users/full_name user)     → (:users/full_name user)     ; No change
(:transactions_v2/id tx)    → (:transactions_v2/id tx)    ; No change
```

## ✅ Testing Commands

```bash
# Test in REPL
bb repl
(require '[namespace :as ns] :reload)
(ns/function-name test-db)

# Run quality checks
bb lint && bb cljfmt-check

# Start dev server
npm run dev
```

## 📋 Refactoring Checklist

- [ ] Backup: `git checkout -b fix/snake-case-NAMESPACE`
- [ ] Find violations: Use detection commands above
- [ ] Backend: Add shared imports, convert return keys, apply normalization
- [ ] Frontend: Update structure key access, keep DB columns unchanged
- [ ] Test: REPL tests, frontend rendering, lint checks
- [ ] Commit: `git commit -m "fix: convert snake_case to kebab-case in NAMESPACE"`

## 🚨 Remember

- ✅ **Convert**: Structure keys (`:snake_case` → `:kebab-case`)
- ❌ **Don't Convert**: Database columns (`:users/full_name`, `:total_amount`)
- ✅ **Always**: Apply `normalize-audit-map-recursive` to backend returns
- ✅ **Test**: Both backend normalization and frontend rendering

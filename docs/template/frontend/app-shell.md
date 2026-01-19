<!-- ai: {:tags [:frontend :architecture :template] :kind :guide} -->

# Frontend App Shell (Template)

## Overview

Single-tenant frontend built with Shadow CLJS, Re-frame, and UIx. The app shell powers both the public build (`:app`) and the admin console (`:admin`). Admin-specific wiring and state live in [Admin App Shell](../../admin/frontend/app-shell.md).

## Build System Architecture

### Shadow CLJS Configuration (`shadow-cljs.edn`)

```clojure
{:nrepl {:port 8777 :init-ns shadow.user}
 :devtools {:http-port 9650 :http-host "localhost"}
 :source-paths ["src" "resources/db"]
 :builds
 {:app {:target :browser
        :output-dir "resources/public/js/main"
        :asset-path "/js/main"
        :devtools {:reload-strategy :full
                  :auto-refresh true
                  :after-load app.template.frontend.core/after-load}
        :dev {:compiler-options {:closure-defines {re-frame.trace.trace-enabled? true}}
              :modules {:app {:preloads [app.template.frontend.dev.tracing
                                         app.template.frontend.dev.repl-tracing]}}}
        :modules {:app {:init-fn app.template.frontend.core/init
                        :preloads [app.template.frontend.preload.silence]}}}

  :admin {:target :browser
          :output-dir "resources/public/js/admin"
          :asset-path "/js/admin"
          :devtools {:reload-strategy :full
                    :auto-refresh true}
          :modules {:app {:init-fn app.template.frontend.core/init
                          :preloads [app.template.frontend.preload.silence
                                     app.template.frontend.dev.tracing]}}}

  :test {:target :browser-test
         :test-dir "target/test"
         :ns-regexp "-test$"}
  :test-node {:target :node-test
              :output-to "target/test-node.cjs"
              :ns-regexp "-test$"}
  :karma-test {:target :karma
               :output-to "target/karma-test.js"
               :ns-regexp "-test$"}}}
```

### Build Targets

| Target | Purpose | Entry Point | Output Directory |
|--------|---------|-------------|------------------|
| `:app` | Public/shell build (optional landing) | `app.template.frontend.core/init` | `resources/public/js/main` |
| `:admin` | Admin console (users, audit, login events) | `app.template.frontend.core/init` | `resources/public/js/admin` |
| `:test`, `:test-node`, `:karma-test` | CLJS tests (browser/node/karma) | Test namespaces | `target/test*` |

### Development Workflow

```bash
# Public build (rarely touched in single-tenant)
npm run watch

# Admin console (primary surface, served at http://localhost:8085)
npm run watch:admin

# Tests
npm run test:cljs
bb fe-test-node       # node runner

# Production bundles
npm run build
npm run build:admin
```

## Application Entry Points

### Admin Console (`app.admin.frontend.core`)

Admin **module** wiring, state, events, and routes are documented in [Admin App Shell](../../admin/frontend/app-shell.md).

The admin Shadow build uses the shared SPA bootstrap (`app.template.frontend.core/init`) and then initializes the admin module lazily via `app.admin.frontend.core/init-admin!` when the current URL/route is under `/admin`.

### Public Shell (`app.template.frontend.core`)

The public build remains for lightweight landing/demo needs but carries no tenant switching. Routing and pages can be trimmed if unused; keep `init`/`after-load` wiring aligned with `shadow-cljs.edn`.

---

**Related Documentation**
- [Template Infrastructure](./template-infrastructure.md)
- [Template Component Integration](./template-component-integration.md)

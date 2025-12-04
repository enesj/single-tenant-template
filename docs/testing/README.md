# Testing Documentation

This folder contains documentation for the project's testing infrastructure.

## Frontend Testing

| Document | Description |
|----------|-------------|
| [Overview](./fe/overview.md) | Architecture, rendering strategy, and implementation details |
| [Development Guide](./fe/development-guide.md) | How to write, run, and debug tests |

### Quick Reference

```bash
# Run Node.js tests (fast, primary)
npm run test:cljs

# Run browser tests (Karma/Chrome)
npm run test:cljs:karma

# Watch mode for development
npm run test:cljs:watch
```

## Test Organization

```
test/
├── app/
│   ├── admin/frontend/     # Admin panel tests
│   │   ├── adapters/       # Data adapter tests
│   │   ├── components/     # UI component tests
│   │   ├── events/         # Re-frame event tests
│   │   └── security/       # Security wrapper tests
│   └── template/frontend/  # Shared template tests
│       ├── api/            # HTTP client tests
│       ├── components/     # Shared component tests
│       ├── events/         # Event handler tests
│       ├── pages/          # Page component tests
│       ├── subs/           # Subscription tests
│       └── utils/          # Utility tests
└── karma-adapter.js        # Karma integration
```

## Key Concepts

- **Dual Environment**: Tests run in both Node.js (jsdom) and real browser (Karma/Chrome)
- **Mock Fallback**: When DOM rendering fails, mock renderer generates expected HTML from props
- **HTTP Stubbing**: Capture and simulate HTTP requests for isolated testing
- **Subscription Testing**: Test re-frame subscriptions with registered handlers

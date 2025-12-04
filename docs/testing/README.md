# Testing Documentation

This folder contains documentation for the project's testing infrastructure.

## Backend Testing

| Document | Description |
|----------|-------------|
| [Overview](./be/overview.md) | Architecture, infrastructure, and test categories |
| [Development Guide](./be/development-guide.md) | How to write, run, and debug tests |
| [Test Patterns](./be/test-patterns.md) | Common testing patterns |
| [Fixtures Reference](./be/fixtures-reference.md) | Fixture utilities |

### Quick Reference

```bash
# Run all backend tests
bb be-test

# Verbose output
clj -M:test -m kaocha.runner --reporter documentation

# Run specific namespace
clj -M:test -m kaocha.runner --focus app.backend.routes.admin.auth-test
```

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
│   ├── backend/                # Backend tests
│   │   ├── fixtures.clj        # System lifecycle hooks
│   │   ├── test_helpers.clj    # Shared utilities
│   │   ├── routes_smoke_test.clj
│   │   └── routes/
│   │       ├── admin/          # Admin route tests
│   │       │   ├── auth_test.clj
│   │       │   ├── dashboard_test.clj
│   │       │   ├── users_test.clj
│   │       │   ├── audit_test.clj
│   │       │   ├── admins_test.clj
│   │       │   ├── login_events_test.clj
│   │       │   ├── password_test.clj
│   │       │   ├── settings_test.clj
│   │       │   ├── user_bulk_test.clj
│   │       │   ├── user_operations_test.clj
│   │       │   ├── transactions_test.clj
│   │       │   ├── entities_test.clj
│   │       │   └── integrations_test.clj
│   │       └── api_test.clj
│   ├── template/
│   │   └── backend/
│   │       └── crud/
│   │           └── service_test.clj
│   ├── admin/frontend/         # Admin panel tests
│   │   ├── adapters/           # Data adapter tests
│   │   ├── components/         # UI component tests
│   │   ├── events/             # Re-frame event tests
│   │   └── security/           # Security wrapper tests
│   └── template/frontend/      # Shared template tests
│       ├── api/                # HTTP client tests
│       ├── components/         # Shared component tests
│       ├── events/             # Event handler tests
│       ├── pages/              # Page component tests
│       ├── subs/               # Subscription tests
│       └── utils/              # Utility tests
└── karma-adapter.js            # Karma integration
```

## Key Concepts

### Backend Testing

- **Kaocha Runner**: Test framework with hooks for system lifecycle
- **Test Profile**: Separate database (port 55433) and web port (8086)
- **Mock Helpers**: Utilities for mocking requests, databases, and services
- **Transaction Rollback**: Database isolation via rollback-only transactions

### Frontend Testing

- **Dual Environment**: Tests run in both Node.js (jsdom) and real browser (Karma/Chrome)
- **Mock Fallback**: When DOM rendering fails, mock renderer generates expected HTML from props
- **HTTP Stubbing**: Capture and simulate HTTP requests for isolated testing
- **Subscription Testing**: Test re-frame subscriptions with registered handlers

## Test Statistics

| Suite | Tests | Assertions |
|-------|-------|------------|
| Backend | 121 | 498 |
| Frontend | Varies | Varies |

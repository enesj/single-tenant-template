# Template Infrastructure

This directory contains reusable SaaS infrastructure components that can be used by any domain.

## Structure

- `backend/` - Backend template services (auth, tenant, CRUD, etc.)
- `frontend/` - Frontend template components (forms, lists, auth UI, etc.)
- `shared/` - Shared template utilities
- `di/` - Dependency injection configuration

## Key Principles

- Domain-agnostic and reusable
- Metadata-driven operations
- Protocol-based interfaces
- Comprehensive validation
- Multi-tenant by design

## Usage

Template services are injected into domain services to provide infrastructure capabilities while maintaining clean separation of concerns.

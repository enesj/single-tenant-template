# Next Session Prompt: Interactive Components ID Audit

**Date**: 2025-12-16
**Task**: Find all interactive components in all cljs files in this app. Remember which interactive components don't have defined IDs and remember them in an .md file in root pad together with description of their functionality.

## Context Snapshot

- **Repository**: Single-tenant SaaS template built with Clojure/ClojureScript and PostgreSQL
- **Frontend**: Shadow CLJS, Re-frame, UIx components with two main builds (`:app` for public, `:admin` for admin console)
- **Architecture**: Admin UI served at `http://localhost:8085`, using template component system with DaisyUI CSS classes (`ds-` prefix)
- **Key Interactive Component Patterns**: UIx `defui` components, form inputs, buttons, on-click handlers, elements with `:id` attributes
- **Component Libraries**: Template components in `src/app/template/frontend/components/*`, admin components in `src/app/admin/frontend/components/*`

## Task Focus

Your mission is to comprehensively audit all interactive components across the ClojureScript codebase to identify accessibility and testing gaps. Specifically:

1. **Find ALL interactive components** - buttons, inputs, forms, clickable elements, any UI elements with user interaction handlers
2. **Check ID presence** - determine which interactive components have proper `:id` attributes for accessibility and testing
3. **Document gaps** - create a comprehensive report of components missing IDs with their functionality descriptions
4. **Generate actionable report** - output to `INTERACTIVE-COMPONENTS-ID-AUDIT.md` in repo root

## Code Map - Key Areas to Inspect

### Core Interactive Component Namespaces
- `src/app/template/frontend/components/form.cljs` - Core form rendering with field components
- `src/app/template/frontend/components/form/fields/` - Input components (input.cljs, checkbox.cljs, select.cljs, textarea.cljs, number.cljs)
- `src/app/template/frontend/components/button.cljs` - Button component system
- `src/app/template/frontend/components/list/table.cljs` - Interactive table components
- `src/app/template/frontend/components/list/` - List-related interactive components (rows.cljs, cells.cljs, ui.cljs)
- `src/app/template/frontend/components/filter/` - Filter and search components
- `src/app/template/frontend/components/batch_edit/` - Batch editing interface components
- `src/app/template/frontend/components/dropdown.cljs` - Dropdown menu components
- `src/app/template/frontend/components/modal.cljs` - Modal dialog components

### Admin-Specific Interactive Components
- `src/app/admin/frontend/components/enhanced_action_buttons.cljs` - Admin action buttons
- `src/app/admin/frontend/components/user_actions.cljs` - User management actions
- `src/app/admin/frontend/components/admin_actions.cljs` - Administrative operations
- `src/app/admin/frontend/components/user_details_modal.cljs` - User detail modal
- `src/app/admin/frontend/components/audit_actions.cljs` - Audit log interactions
- `src/app/admin/frontend/components/audit_export_controls.cljs` - Export functionality

### Domain-Specific Interactive Components
- `src/app/domain/frontend/expenses/components/` - Expenses domain interactive forms and controls
- `src/app/domain/frontend/expenses/pages/` - Expense management page components with forms and tables

### Pages with High Interactive Density
- `src/app/admin/frontend/pages/` - Admin interface pages (login.cljs, users.cljs, dashboard.cljs, settings.cljs)
- `src/app/template/frontend/pages/` - Template pages (login.cljs, register.cljs, entities.cljs)
- `src/app/domain/frontend/expenses/pages/` - Expense domain pages (expense_new.cljs, expense_detail.cljs, expenses_list.cljs)

## Commands to Run

```bash
# Start the application for manual verification if needed
bb run-app

# Compile frontend builds to ensure all components are processable
npm run build:admin
npm run build

# Run tests to understand component interaction patterns
bb fe-test
bb be-test  # For backend integration context

# Search for interactive component patterns during development
rg -n "defui|on-click|:on-click|input.*type|button.*type|form" src/**/*.cljs
```

## Gotchas & Architecture Notes

- **UIx Component System**: Components use `defui` macro, not traditional Reagent components. Look for `($ :element)` syntax for DOM elements
- **Form System**: Complex form infrastructure with dynamic field generation, field specs determine component types and attributes
- **ID Generation Pattern**: Components may generate IDs programmatically using `form-id` and `field-id` combinations (see `form.cljs:66-67`)
- **Event Handling**: Re-frame events handle interactions, look for `on-click` attributes and event dispatch patterns
- **Accessibility**: Components should have proper `aria-*` attributes and semantic HTML structure
- **Testing Support**: IDs are crucial for DOM testing - components without proper IDs are harder to test reliably

## Interactive Component Detection Patterns

Search for these patterns in ClojureScript files:

1. **UIx Components**: `(defui component-name...`, `($ :element-type {:attrs...} ...)`
2. **Click Handlers**: `:on-click`, `on-click`, event dispatch patterns
3. **Form Elements**: `input`, `textarea`, `select`, `button` elements with interaction handlers
4. **ID Attributes**: `:id` attribute assignment, programmatic ID generation
5. **Event Dispatch**: `(rf/dispatch [...])` patterns tied to user interactions
6. **Conditional Rendering**: Interactive elements that appear based on state

## Expected Deliverable

Create `INTERACTIVE-COMPONENTS-ID-AUDIT.md` in repo root with:

### Structure:
```markdown
# Interactive Components ID Audit Report

## Summary
- Total interactive components found: X
- Components with proper IDs: Y
- Components missing IDs: Z
- Coverage percentage: %

## Components Missing IDs

### Form Components
- **Component**: [component-name]
  - **File**: src/path/to/component.cljs
  - **Functionality**: [description of what the component does]
  - **Why it needs ID**: [testing/accessibility reason]

### Button Components
- **Component**: [component-name]
  - **File**: src/path/to/component.cljs
  - **Functionality**: [description]
  - **Recommended ID pattern**: [suggestion]

### Navigation Components
[Continue for each category...]

## Recommendations
1. Priority components for ID implementation
2. Suggested ID naming conventions
3. Implementation approach for systematic ID addition
```

## Implementation Checklist

1. **Scan Strategy Setup**
   - [ ] Configure search patterns for interactive components
   - [ ] Set up systematic file traversal approach
   - [ ] Initialize results tracking structure

2. **Component Discovery Phase**
   - [ ] Scan all `.cljs` files for `defui` components
   - [ ] Identify all elements with `:on-click` handlers
   - [ ] Find all form elements (input, select, textarea, button)
   - [ ] Locate modal, dropdown, and navigation components
   - [ ] Capture components with dynamic rendering and state changes

3. **ID Analysis Phase**
   - [ ] Check each interactive component for `:id` attribute presence
   - [ ] Analyze ID generation patterns (static vs dynamic)
   - [ ] Document components with programmatic ID creation
   - [ ] Identify components completely missing ID attributes

4. **Documentation Phase**
   - [ ] Create comprehensive component functionality descriptions
   - [ ] Categorize components by type and priority
   - [ ] Generate structured markdown report
   - [ ] Provide actionable recommendations for ID implementation

5. **Verification Phase**
   - [ ] Cross-reference findings with actual component behavior
   - [ ] Validate ID uniqueness and naming consistency
   - [ ] Ensure report completeness and accuracy

## Success Criteria

- **Complete Coverage**: Every interactive component in the `.cljs` codebase is analyzed
- **Clear Documentation**: Each component missing ID has clear functionality description and implementation recommendation
- **Actionable Output**: Report provides systematic approach for adding missing IDs
- **Quality Standards**: Components are categorized by testing/accessibility priority
- **Pattern Recognition**: ID generation patterns are documented for consistent implementation

Use the clojure-mcp scratch-pad tool to track your progress through each phase and maintain comprehensive notes during the component audit process.

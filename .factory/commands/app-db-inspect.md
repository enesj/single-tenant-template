# App-DB Inspect Command

**Command:** `/app-db-inspect [focus] "[task]"`

**Description:** Safely inspects the re-frame app-db state in ClojureScript applications without encountering `IDeref` protocol errors. Accepts optional focus parameters for targeted inspection and concrete task descriptions for customized analysis.

## Usage

**Basic inspection:** `/app-db-inspect`

**Focused inspection:** `/app-db-inspect [focus-area]`

**Task-focused inspection:** `/app-db-inspect [focus-area] "[specific task]"`

**Available focus areas:**
- `auth` - Authentication and session state only
- `routes` - Current route and navigation state only
- `entities` - Entity stores and data loading only
- `ui` - UI state, theme, and interface only
- `issues` - Only problems and inconsistencies detected
- `[key-path]` - Any specific app-db key path (e.g., `entities/users`, `ui/theme`)

## When to Use

Use this command when you need to:
- Check current frontend state or re-frame app-db
- Debug authentication issues (session, user info, tenant context)
- Verify data loading status or entity stores
- Check route and navigation state
- Investigate UI state, theme, or interface configuration
- Look for errors, warnings, or state inconsistencies
- Focus on specific areas without overwhelming output
- **Execute concrete analysis tasks** with targeted investigation goals

## What Happens

When you run `/app-db-inspect`, the system will:

1. **Safely access app-db** using defensive programming patterns
2. **Parse focus parameters** to determine inspection scope
3. **Interpret concrete task** to customize analysis approach
4. **Execute targeted inspection** based on your specific requirements
5. **Analyze specified sections** or full app-db if no focus provided
6. **Detect common issues** like route/auth mismatches
7. **Provide task-specific output** with actionable recommendations

**With focus parameters:**
- Single focus area: Detailed analysis of that section only
- Multiple focus areas: Sequential analysis of each area
- Custom key_path: Raw inspection of that specific data structure

**With concrete task description:**
- The task parameter guides the analysis approach
- Output is customized to address your specific investigation
- Recommendations are tailored to your stated goals
- Analysis focuses on patterns relevant to your task

## Expected Output

```
## App-DB Inspection Results

### Summary
✅ Re-frame app-db found and accessible
🕐 Timestamp: 2024-12-15T10:30:00.000Z

### Authentication State
- Authenticated: true
- Session Valid: true
- User: user@example.com (owner role)
- Tenant: Demo Tenant (ID: 123)
- Diagnosis: ✓ User is properly authenticated

### Current Route
- Template: /dashboard
- Route Name: :dashboard
- Parameters: {}

### Entity Stores Overview
- properties: 5 items loaded, not loading
- transactions: 12 items loaded, not loading
- users: 3 items loaded, not loading

### UI State
- Theme: light
- Sidebar: closed
- Pagination: {page: 1, per-page: 10}

### Issues Detected
- No authentication or data loading issues found
- Route and authentication state are consistent
```

## Key Features

- **Safe Access**: Uses `(exists? re-frame.db/app-db)` checking
- **Error Handling**: Graceful fallbacks when app-db is unavailable
- **Context Awareness**: Provides diagnosis and recommendations
- **Structured Output**: Organized sections for easy scanning

## Troubleshooting

If app-db inspection fails:
1. Ensure ClojureScript REPL is connected and running
2. Check that re-frame is initialized in the application
3. Verify the application is loaded in the browser

## Technical Details

The command uses this core inspection pattern:
```clojure
(if (exists? re-frame.db/app-db)
  (let [app-db re-frame.db/app-db]
    ;; Safe inspection logic here
    {:success true :data analysis})
  {:success false :error "app-db not found"})
```

## Integration

This command works well with:
- **Chrome DevTools**: Verify UI state matches app-db state
- **HTTP debugging**: Check if API responses update app-db correctly
- **REPL development**: Interactive frontend state exploration

## Examples

**Basic inspection:**
```
/app-db-inspect
```

**Focused inspection:**
```
/app-db-inspect auth
/app-db-inspect routes,entities
/app-db-inspect issues
/app-db-inspect entities/users
```

**Task-focused inspection with concrete goals:**
```
/app-db-inspect auth "Why is the user session timing out after 5 minutes?"
/app-db-inspect entities "Check if the properties data is loading correctly for the dashboard"
/app-db-inspect routes "Investigate why the admin route is not accessible despite having admin role"
/app-db-inspect ui "Analyze theme switching behavior and ensure it persists across page reloads"
/app-db-inspect issues "Find all state inconsistencies that could cause the login redirect loop"
/app-db-inspect properties "Verify user properties are properly filtered by tenant ID"
```

**Analysis customization based on tasks:**
- **Authentication debugging**: Focus on session tokens, expiration, role validation
- **Data loading verification**: Check loading states, error handling, pagination
- **Route access investigation**: Analyze route parameters, auth guards, navigation flow
- **UI behavior analysis**: Examine state updates, component lifecycle, user interactions
- **Performance investigation**: Identify state bottlenecks, unnecessary re-computations
- **Security audit**: Review sensitive data exposure, authorization checks

**Output format:**
- **Full inspection**: Complete analysis with all sections
- **Focused inspection**: Only the requested section(s) with enhanced detail
- **Custom key path**: Raw data structure inspection
- **Task-specific analysis**: Customized output addressing your concrete investigation goals

---

**Usage:** Type `/app-db-inspect` with optional focus parameters and concrete task descriptions to examine specific aspects of your re-frame application state with targeted analysis.

**Examples:**
- `/app-db-inspect` - Full app-db inspection
- `/app-db-inspect auth` - Only authentication state
- `/app-db-inspect entities/users` - Specific entity store
- `/app-db-inspect routes,issues "Debug why admin routes redirect to login"`
- `/app-db-inspect auth "Check session expiration and token refresh logic"`

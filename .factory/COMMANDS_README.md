# Factory Commands - Updated with Concrete Task Support

All Factory commands have been updated to accept prompt parameters with concrete task descriptions, enabling targeted analysis and customized investigations.

## Updated Command Syntax

### App-DB Inspect
- **Command:** `/app-db-inspect [focus] "[task]"`
- **Focus Areas:** `auth`, `routes`, `entities`, `ui`, `issues`, or custom key-path
- **Task Examples:**
  - `"Why is the user session timing out after 5 minutes?"`
  - `"Check if the properties data is loading correctly for the dashboard"`
  - `"Investigate why the admin route is not accessible despite having admin role"`

### Re-frame Events Analysis
- **Command:** `/reframe-events-analysis "[task]"`
- **Task Examples:**
  - `"Debug why the dashboard data isn't loading"`
  - `"Find performance bottlenecks in property loading"`
  - `"Analyze the event sequence causing form submission errors"`

### System Logs
- **Command:** `/system-logs "[task]"`
- **Task Examples:**
  - `"Debug the authentication timeout errors"`
  - `"Investigate database connection failures"`
  - `"Analyze shadow-cljs compilation warnings and fix them"`

## Benefits of Task-Based Commands

1. **Targeted Analysis:** Commands now focus on patterns relevant to your specific problem
2. **Customized Output:** Analysis is tailored to address your investigation goals
3. **Actionable Recommendations:** Provide solutions specific to your stated task
4. **Focused Data Collection:** Reduce noise by collecting only relevant log/event data
5. **Enhanced Debugging:** More efficient problem-solving with context-aware analysis

## Analysis Customization

The task parameter guides the command's approach:

- **App-DB Inspection:** Focuses on specific state inconsistencies or data patterns
- **Event Analysis:** Filters and analyzes events relevant to your debugging goal
- **System Logs:** Highlights log entries and patterns supporting your investigation

## Usage Patterns

**Basic Analysis:**
```bash
/app-db-inspect auth
/reframe-events-analysis
/system-logs
```

**Task-Focused Investigation:**
```bash
/app-db-inspect auth "Why do users get logged out after 5 minutes?"
/reframe-events-analysis "Debug why property deletion isn't updating the UI"
/system-logs "Find all database timeout errors in the last 10 minutes"
```

## Consistent Parameter Handling

All commands follow the same pattern:
- **Optional Parameters:** Focus areas, filters, or modes (where applicable)
- **Required Task (quoted):** Concrete investigation description in quotes
- **Guided Analysis:** Commands interpret tasks and customize their approach
- **Targeted Output:** Results are filtered to address your specific needs

## Example Workflows

### Authentication Debugging Workflow
1. `/app-db-inspect auth "Check session token validation and expiration logic"`
2. `/reframe-events-analysis "Trace the login/logout event sequence for timeout issues"`
3. `/system-logs "Find authentication errors and session management warnings"`

### Performance Investigation Workflow
1. `/app-db-inspect entities "Analyze data loading states and pagination bottlenecks"`
2. `/reframe-events-analysis "Identify slow events causing UI lag"`
3. `/system-logs performance "Monitor database query performance and memory usage"`

### Data Loading Issues Workflow
1. `/app-db-inspect properties "Verify property data is properly filtered by tenant"`
2. `/reframe-events-analysis "Trace properties loading events and error handling"`
3. `/system-logs "Find API errors and network timeout issues"`

## Migration from Previous Syntax

Previous command usages continue to work:
- `/app-db-inspect` → Still performs full app-db inspection
- `/reframe-events-analysis` → Still performs general event analysis
- `/system-logs` → Still performs general log analysis

Enhanced functionality is available by adding task descriptions in quotes.

---

**All Factory commands now support concrete task descriptions for targeted debugging and analysis.**

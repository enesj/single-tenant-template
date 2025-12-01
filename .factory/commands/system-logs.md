# System Logs Command

**Command:** `/system-logs "[task]"`

**Description:** Read and analyze server logs and shadow-cljs logs using the monitoring system to track real-time output from running processes. Accepts concrete task descriptions for targeted log analysis and problem investigation.

## When to Use

Use this command when you need to:
- Monitor real-time output from server and shadow-cljs processes
- Investigate errors and warnings in the application
- Track compilation progress and build issues
- Analyze database query logs and authentication events
- Identify performance bottlenecks from log patterns
- Debug issues by examining recent activity
- **Execute concrete log analysis tasks** with specific investigation goals

## What Happens

When you run `/system-logs`, the system will:

**If called with a concrete task:**
1. **Focus ONLY on the defined task** - Execute exactly what was requested
2. **No extra analysis** - Skip all unrelated log patterns and general insights
3. **Direct output** - Provide only findings relevant to the specific task
4. **Task-specific recommendations** - Tailored exclusively to the stated problem
5. **Minimal context** - Focus purely on answering the defined question

**If called without a task:**
1. **Check monitoring status** to verify active process monitoring
2. **Read log output** using the monitoring script
3. **Execute general analysis** with standard patterns
4. **Analyze log patterns** for common issues
5. **Provide structured analysis** with actionable recommendations

## Expected Output

**When called with a specific task:**
```
## System Logs Analysis: [Your Task]

✅ Found 3 database connection timeout events
🎯 Focus: Database connection failures

### Task-Specific Findings
🚨 ERROR: Connection timeout in src/app/backend/db.clj:45
   - Timestamp: 14:28:45
   - Context: During user authentication query
   - Pattern: Occurs every 2-3 minutes under load

🔍 Root Cause Analysis:
   Connection pool exhausted (max 10 connections)
   during高峰期用户登录

### Task-Specific Recommendations
1. Increase connection pool size to 20
2. Add connection retry logic with exponential backoff
3. Implement connection health monitoring

```

**When called without a task:**
```
## System Logs Analysis

### Summary
✅ Active monitoring found: bb dev:all (PID: 12345)
📊 Analyzed 250 log entries from last 5 minutes

### Recent Activity
🕐 Latest notable events identified
📦 Build status and compilation results
⚠️ Error warnings requiring attention
```

## Command Options

You can specify different monitoring modes:

- **One-time read**: `/system-logs` - Read current log state once
- **Real-time follow**: `/system-logs follow` - Stream logs in real-time
- **Error focus**: `/system-logs errors` - Filter for errors and warnings only
- **Performance focus**: `/system-logs performance` - Highlight timing and performance data
- **Task-focused analysis**: `/system-logs "[specific investigation]"` - Targeted log analysis based on concrete goals

**Task-focused examples:**
- `/system-logs "Investigate why the database connection keeps failing"`
- `/system-logs "Find all authentication errors in the last 10 minutes"`
- `/system-logs "Analyze shadow-cljs compilation warnings and fix them"`
- `/system-logs "Debug why property uploads are timing out"`
- `/system-logs "Monitor memory usage patterns during heavy load"`

## Prerequisites

For this command to work:
1. A process must be actively monitored via `monitor_terminal.sh`
2. The monitoring script must be available: `./scripts/sh/monitoring/read_output.sh`
3. Output files must exist in `/tmp/` directory
4. Proper file permissions for `/tmp/` access

## Starting Monitoring

If no active monitoring is found, you'll need to start it:

```bash
# Start monitoring full development stack
./scripts/sh/monitoring/monitor_terminal.sh 'bb dev:all'

# Start monitoring just the server
./scripts/sh/monitoring/monitor_terminal.sh 'bb dev:server'

# Start monitoring shadow-cljs only
./scripts/sh/monitoring/monitor_terminal.sh 'bb dev:frontend'
```

Then in another terminal:
```bash
# Read logs once
./scripts/sh/monitoring/read_output.sh

# Follow logs in real-time
./scripts/sh/monitoring/read_output.sh -f
```

## Analysis Patterns

### Error Investigation
- Stack traces and exception messages
- Compilation errors with file locations
- Runtime errors with context
- Repeated error patterns

### Performance Monitoring
- Database query durations
- HTTP request/response times
- Memory usage warnings
- Resource exhaustion messages

### Build/Compilation
- Shadow-cljs compilation status
- Hot-reload notifications
- Build warnings and errors
- Dependency issues

### Recent Activity
- Latest log entries with timestamps
- Event sequences leading to issues
- User actions reflected in logs

## Error Handling

If monitoring isn't active, you'll see:

```
❌ No active monitoring found

To start monitoring:
./scripts/sh/monitoring/monitor_terminal.sh 'bb dev:all'

Then read logs with:
./scripts/sh/monitoring/read_output.sh
```

## Technical Details

The monitoring system uses:
- **Metadata File**: `/tmp/active_command.txt` (symlink to current session)
- **Output Files**: `/tmp/command_output_{RUN_ID}.log`
- **Position Tracking**: `{OUTPUT_FILE}.pos` (tracks read position)
- **Buffer Files**: `{OUTPUT_FILE}.last` (recent output buffer)

## Common Use Cases

### Debugging Compilation Issues
```bash
/system-logs errors
# Focuses on shadow-cljs compilation errors and warnings
```

### Monitoring Application Performance
```bash
/system-logs performance
# Highlights slow operations, database queries, and timing data
```

### Real-time Development Monitoring
```bash
/system-logs follow
# Streams live output during active development
```

### Authentication/Authorization Debugging
```bash
/system-logs
# Look for login/logout events, session management, auth failures
```

## Integration

This command works well with:
- **REPL Tools**: Cross-reference with live system state
- **Code Analysis**: Review source code related to errors
- **Development Tools**: bb tasks, shadow-cljs, server management

---

**Usage:** Type `/system-logs` with a concrete task description to execute ONLY that specific task: `/system-logs "Debug the authentication timeout errors"` will focus exclusively on authentication timeout analysis with no extra analysis.

**Examples:**
- `/system-logs` - General log analysis (comprehensive)
- `/system-logs follow` - Real-time log monitoring
- `/system-logs "Investigate database connection failures"` - ONLY database connection analysis
- `/system-logs "Find all errors in the last 5 minutes"` - ONLY recent error analysis
- `/system-logs "Analyze memory usage patterns and identify leaks"` - ONLY memory usage analysis

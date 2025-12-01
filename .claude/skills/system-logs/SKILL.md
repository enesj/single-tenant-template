---
description: "Read and analyze server logs and shadow-cljs logs using monitoring system"
tags: ["logs", "monitoring", "debugging", "development", "server", "shadow-cljs"]
---

# system-logs

**Read server logs and shadow-cljs logs using monitoring system to track real-time output from running processes.**

## Skill Purpose

This skill helps access and analyze development logs to:
- Monitor real-time output from server and shadow-cljs processes
- Investigate errors and warnings in application
- Track compilation progress and build issues
- Analyze database query logs and authentication events
- Identify performance bottlenecks from log patterns
- Debug issues by examining recent activity

## When to Use

Use this skill when:
- User asks about server logs, build output, or compilation errors
- Debugging runtime issues or errors in application
- Monitoring active development processes
- Investigating database queries or authentication problems
- Checking compilation status or hot-reload issues
- User mentions keywords: "logs", "errors", "server output", "build", "compilation"

## Prerequisites

The project should have:
1. Active monitoring of processes via `monitor_terminal.sh`
2. Running server and/or shadow-cljs processes
3. Access to `/tmp/` directory for log files
4. The monitoring script: `./scripts/sh/monitoring/read_output.sh`

## Analysis Workflow

### 1. Understand User Intent

Analyze the user's request for context keywords:
- **Server**: "server", "backend", "api" → Focus on server-side logs and HTTP requests
- **Compilation**: "compilation", "build", "compile" → Emphasize build process and compilation output
- **Authentication**: "authentication", "auth", "login" → Look for auth and authorization logs
- **Database**: "database", "db", "sql" → Focus on database queries and connection logs
- **Performance**: "performance", "slow", "timing" → Look for performance-related messages and timing data
- **Recent**: "recent", "latest", "new" → Focus on most recent log entries
- **Errors**: "error", "warning", "exception" → Filter for error and warning messages only

### 2. Check Monitoring Status

Verify that a process is being monitored:
1. Check for active monitoring metadata at `/tmp/active_command.txt`
2. Confirm output file exists at `/tmp/command_output_{RUN_ID}.log`
3. If no monitoring active, suggest starting it with `monitor_terminal.sh`

```bash
# Check if monitoring is active
if [ -f "/tmp/active_command.txt" ]; then
    echo "✓ Active monitoring found"
else
    echo "❌ No active monitoring - suggest starting with:"
    echo "./scripts/sh/monitoring/monitor_terminal.sh 'bb dev:all'"
fi
```

### 3. Execute Log Reading

Use monitoring script to read logs with proper error handling:

**One-time Read** (default):
```bash
# Safe log reading with error handling
if [ -f "./scripts/sh/monitoring/read_output.sh" ]; then
    ./scripts/sh/monitoring/read_output.sh
else
    echo "❌ Monitoring script not found at expected location"
    echo "Looking for alternative locations..."
    find . -name "read_output.sh" -type f 2>/dev/null || echo "No read_output.sh found"
fi
```

**Follow Mode** (real-time monitoring):
```bash
# Real-time monitoring with interrupt handling
if [ -f "./scripts/sh/monitoring/read_output.sh" ]; then
    echo "Starting real-time log monitoring... (Press Ctrl+C to stop)"
    ./scripts/sh/monitoring/read_output.sh -f
else
    echo "❌ Cannot start real-time monitoring - script not found"
    exit 1
fi
```

**Using MCP Tools for Enhanced Analysis**:
```clojure
;; Check monitoring status via clojure-mcp tools
(try
  ;; Check if active monitoring exists
  (when (.exists js/window "fetch") ; Node.js environment
    (-> (js/fetch "/tmp/active_command.txt"
              {:method "GET"})
        (.then #(.text %))
        (.then #(println "Active monitoring:" %)))
  (catch js/Error e
    (println "❌ Error checking monitoring status:" (.-message e))))

;; Alternative: Use bash tool to check monitoring
(bash "if [ -f '/tmp/active_command.txt' ]; then echo 'Active: true'; else echo 'Active: false'; fi")
```

### 4. Analyze Log Content

Based on log output, provide analysis for:

**Error Patterns:**
- Stack traces and exception messages
- Compilation errors with file locations
- Runtime errors with context
- Warning messages that may indicate issues
- Repeated error patterns suggesting systemic problems

**Performance Insights:**
- Slow database queries (timing information)
- HTTP request/response times
- Compilation duration and hot-reload speed
- Memory usage warnings
- Resource exhaustion messages

**Build/Compilation:**
- Shadow-CLJS compilation status
- Hot-reload notifications
- Build warnings and errors
- Dependency issues
- ClojureScript warnings

**Server Activity:**
- HTTP request logs with status codes
- API endpoint activity
- Database query logs
- Authentication events (login, logout, session)
- Middleware processing

**Recent Activity:**
- Latest log entries with timestamps
- Event sequences leading to issues
- User actions reflected in logs
- State changes and side effects

### 5. Provide Actionable Recommendations

Give specific, actionable advice:
- **Error Resolution**: Suggest fixes for identified errors with code examples
- **Performance**: Recommend optimization strategies for slow operations
- **Debugging**: Point to likely problem areas and next debugging steps
- **Configuration**: Suggest configuration changes if issues detected
- **Best Practices**: Recommend patterns to avoid identified problems

## Common Analysis Patterns

### Error Investigation
```bash
# Read logs with error filtering
./scripts/sh/monitoring/read_output.sh 2>&1 | grep -i -E "(error|exception|failed|fatal|stack trace)"

# Enhanced error analysis with context
./scripts/sh/monitoring/read_output.sh 2>&1 | grep -i -B 5 -A 5 "error"

# Look for specific error patterns in compilation
./scripts/sh/monitoring/read_output.sh 2>&1 | grep -i -E "(compilation.*failed|build.*error|syntax.*error)"
```

### Compilation Monitoring
```bash
# Follow compilation output in real-time
./scripts/sh/monitoring/read_output.sh -f

# Monitor for specific compilation issues
./scripts/sh/monitoring/read_output.sh 2>&1 | grep -i -E "(warning|reloading|compiling|build)"

# Track hot-reload efficiency
./scripts/sh/monitoring/read_output.sh 2>&1 | grep -i "hot reload" | tail -20
```

### Performance Analysis
```bash
# Read logs and look for timing information
./scripts/sh/monitoring/read_output.sh 2>&1 | grep -i -E "(slow|timeout|duration|ms|[0-9]+\.[0-9]+s)"

# Database query performance monitoring
./scripts/sh/monitoring/read_output.sh 2>&1 | grep -i -B 2 -A 2 "query.*took"

# HTTP request timing analysis
./scripts/sh/monitoring/read_output.sh 2>&1 | grep -E "([0-9]+ms|request.*time)"
```

### Authentication/Authorization Debugging
```bash
# Focus on auth-related log entries
./scripts/sh/monitoring/read_output.sh 2>&1 | grep -i -E "(login|logout|auth|session|token|authorization)"

# Track authentication failures specifically
./scripts/sh/monitoring/read_output.sh 2>&1 | grep -i -B 3 -A 3 "auth.*fail|login.*fail|unauthorized"

# Session management debugging
./scripts/sh/monitoring/read_output.sh 2>&1 | grep -i -B 2 -A 2 "session.*create|session.*destroy"
```

### Recent Activity Summary
```bash
# Get snapshot of recent activity (last 50 lines)
./scripts/sh/monitoring/read_output.sh 2>&1 | tail -50

# Monitor real-time with timestamps
./scripts/sh/monitoring/read_output.sh -f 2>&1 | while read line; do
    echo "[$(date '+%H:%M:%S')] $line"
done
```

## Enhanced Monitoring with MCP Tools

### Using clojure-mcp for Log Analysis
```clojure
;; Enhanced log analysis with error handling
(defn analyze-log-pattern [pattern description]
  (try
    (let [result (bash (str "./scripts/sh/monitoring/read_output.sh 2>&1 | grep -i '" pattern "' | head -10"))]
      (println "\n📊" description ":")
      (println result))
    (catch js/Error e
      (println "❌ Error analyzing pattern" pattern ":" (.-message e)))))

;; Analyze common patterns
(doseq [pattern-desc [["error|exception" "Errors and Exceptions"]
                    ["warning|warn" "Warnings"]
                    ["slow|timeout" "Performance Issues"]
                    ["compiling|build" "Build Status"]]]
  (analyze-log-pattern (first pattern-desc) (second pattern-desc)))

;; Real-time monitoring with ClojureScript integration
(defn monitor-logs-with-analysis []
  (try
    (let [log-content (bash "./scripts/sh/monitoring/read_output.sh")]
          error-count (->> log-content
                       (clojure.string/split-lines)
                       (filter #(clojure.string/includes? % "ERROR"))
                       count)]
      (println "\n📈 Log Summary:")
      (println "Total lines:" (->> log-content clojure.string/split-lines count))
      (println "Errors found:" error-count)
      (when (> error-count 0)
        (println "⚠ Error rate detected - investigation recommended")))
    (catch js/Error e
      (println "❌ Monitoring failed:" (.-message e)))))
```

### Advanced Log Processing
```bash
# Multi-pattern analysis with context
patterns=("error" "warning" "slow" "timeout" "failed")
for pattern in "${patterns[@]}"; do
    echo "=== Analyzing pattern: $pattern ==="
    ./scripts/sh/monitoring/read_output.sh 2>&1 | grep -i -C 3 "$pattern" | head -15
    echo ""
done

# Time-based log analysis (last 5 minutes)
minutes_ago=5
since_time=$(date -d "$minutes_ago minutes ago" '+%Y-%m-%d %H:%M:%S')
echo "=== Logs since $since_time ==="
./scripts/sh/monitoring/read_output.sh 2>&1 | awk -v since="$since_time" '
    $0 >= since { print "[" $1 "] " $0 }
' | tail -20
```

## Monitoring System Integration

### File Structure
- **Metadata File**: `/tmp/active_command.txt` (symlink to current session)
- **Output Files**: `/tmp/command_output_{RUN_ID}.log`
- **Position Files**: `{OUTPUT_FILE}.pos` (tracks read position)
- **Buffer Files**: `{OUTPUT_FILE}.last` (recent output buffer)

### State Management
- **Position Tracking**: Uses `.pos` files to track read position
- **Incremental Reading**: Only shows new content to avoid duplication
- **Last Output Buffer**: Maintains recent output for quick access
- **PID Tracking**: Monitors process IDs for cleanup

### Process Integration
Works with commands started through `monitor_terminal.sh`:
- `bb dev:all` - Monitor full development stack
- `bb dev:server` - Monitor backend server only
- `bb dev:frontend` - Monitor shadow-cljs compilation only
- Any other command passed to `monitor_terminal.sh`

## Fallback Strategies

If monitoring system isn't active:

### 1. Check for Alternative Log Locations
```bash
# Look for common log file locations
log_locations=("logs/" "target/" ".shadow-cljs/" "tmp/")
for location in "${log_locations[@]}"; do
    if [ -d "$location" ]; then
        echo "Found log directory: $location"
        ls -la "$location" | head -10
    fi
done
```

### 2. Direct Process Output Monitoring
```bash
# Check for running Java/Clojure processes
echo "=== Active Development Processes ==="
ps aux | grep -E "(java|clojure|node)" | grep -v grep

# Monitor network ports for development servers
echo "=== Development Server Ports ==="
lsof -i :8080 -i :8081 -i :3000 -i :8085 2>/dev/null || echo "No dev servers found on standard ports"
```

### 3. Manual Log File Access
```bash
# Try to access recent log files directly
for file in /tmp/command_output_*.log; do
    if [ -f "$file" ]; then
        echo "=== Recent log file: $file ==="
        echo "Size: $(wc -c < "$file") bytes"
        echo "Modified: $(stat -c %y "$file")"
        echo "Last 10 lines:"
        tail -10 "$file"
        echo ""
    fi
done | sort -r | head -3
```

### 4. Suggest Starting Monitoring
```bash
# Provide clear instructions to start monitoring
echo "To start monitoring development processes:"
echo ""
echo "1. Full development stack:"
echo "   ./scripts/sh/monitoring/monitor_terminal.sh 'bb dev:all'"
echo ""
echo "2. Backend only:"
echo "   ./scripts/sh/monitoring/monitor_terminal.sh 'bb dev:server'"
echo ""
echo "3. Frontend only:"
echo "   ./scripts/sh/monitoring/monitor_terminal.sh 'bb dev:frontend'"
echo ""
echo "Then in another terminal:"
echo "   ./scripts/sh/monitoring/read_output.sh"
```

## Output Format

Provide analysis in clear, structured sections:

```markdown
## Log Analysis

### Summary
[High-level overview: log source, time range, key findings, overall status]

### Errors & Warnings
- [Specific error with context and line numbers]
- [Warning messages with implications]
- [Stack traces with root cause analysis]

### Performance Insights
- [Slow operations with durations]
- [Resource usage patterns]
- [Bottlenecks identified]

### Recent Activity
- [Chronological recent events]
- [User actions and system responses]
- [Notable state changes]

### Recommendations
1. [Specific fix with code example if applicable]
2. [Configuration change with rationale]
3. [Debugging steps to investigate further]

### Next Steps
- [Suggested actions to resolve issues]
- [Further monitoring to perform]
- [Code changes to investigate]
```

## Integration Points

This skill works well with:
- **REPL tools**: Cross-reference with live system state
- **Code analysis skills**: Review source code related to errors
- **Development tools**: bb tasks, shadow-cljs, server management
- **Debugging skills**: Deep dive into specific issues found in logs
- **Event history analysis**: Correlate log events with re-frame events
- **Database debugging**: Cross-reference SQL logs with database state

## Best Practices

1. **Start with Context**: Understand what the user was doing when issues occurred
2. **Recent First**: Check most recent logs first for active issues
3. **Follow for Active Issues**: Use follow mode when debugging live problems
4. **Pattern Recognition**: Look for repeated errors or warning patterns
5. **Cross-Reference**: Compare logs with code and system state
6. **Actionable Output**: Always provide specific next steps
7. **Error Recovery**: Handle monitoring system failures gracefully

## Example Interactions

**User**: "Check server logs for errors"
**Action**: Read logs, filter for ERROR/Exception/Failed patterns, analyze stack traces

**User**: "Is build working?"
**Action**: Read shadow-cljs output, check for compilation success, analyze warnings

**User**: "Show me database queries"
**Action**: Filter logs for SQL/database patterns, analyze query performance

**User**: "Monitor real-time logs"
**Action**: Start follow mode, stream output, highlight important events

**User**: "Why did authentication fail?"
**Action**: Search for auth-related logs, trace login sequence, identify failure point

**User**: "What happened in the last 5 minutes?"
**Action**: Read recent logs, summarize activity chronologically, highlight issues

## Error Handling

Handle common issues gracefully:

**No Active Command:**
```bash
No monitoring active. To start monitoring:
./scripts/sh/monitoring/monitor_terminal.sh 'bb dev:all'
```

**Missing Output File:**
```bash
Output file not found. The monitored process may have terminated.
Check if the process is still running with: ps aux | grep -E "(java|node)"
```

**Permission Issues:**
```bash
Cannot access /tmp/ directory. Check file permissions.
Run: chmod 755 /tmp/ (if you have appropriate permissions)
```

**Invalid Arguments:**
```bash
Usage: read_output.sh [-f]
  -f: Follow mode (real-time monitoring)
```

**Script Not Found:**
```bash
Monitoring script not found. Looking for alternatives:
find . -name "read_output.sh" -type f 2>/dev/null
Found scripts at: [list of found scripts]
```

## Tips for Effective Analysis

1. **Context is Key**: Always consider what operation was being performed
2. **Time Windows**: Recent logs (last few minutes) most relevant for active issues
3. **Error Context**: Look at log entries before and after errors
4. **Pattern vs. Single**: Distinguish between one-time errors and patterns
5. **Severity Assessment**: Prioritize ERRORS over WARNINGS over INFO
6. **Tool Integration**: Use both bash and clojure-mcp tools for comprehensive analysis

## Starting Monitoring System

If monitoring isn't active, guide the user:

```bash
# Start monitoring full development stack
./scripts/sh/monitoring/monitor_terminal.sh 'bb dev:all'

# Start monitoring just the server
./scripts/sh/monitoring/monitor_terminal.sh 'bb dev:server'

# Start monitoring shadow-cljs
./scripts/sh/monitoring/monitor_terminal.sh 'bb dev:frontend'
```

Then in another terminal:
```bash
# Read logs once
./scripts/sh/monitoring/read_output.sh

# Follow logs in real-time
./scripts/sh/monitoring/read_output.sh -f
```

---

**Note**: This skill adapts to the current monitoring state and log content. Always:
1. Verify monitoring is active before attempting to read logs
2. Check actual log content format and adapt analysis accordingly
3. Provide context-aware analysis based on what the user is working on
4. Suggest specific, actionable solutions based on findings
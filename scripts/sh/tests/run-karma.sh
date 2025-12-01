#!/bin/bash

# Karma Test Runner using enhanced result extraction
# More reliable than Chrome headless approach

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Configuration
KARMA_URL="http://localhost:9876"
OUTPUT_FILE="test-results/karma-results.json"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXTRACTION_SCRIPT="${SCRIPT_DIR}/extract-karma-results.sh"

echo "🧪 Karma Test Runner with Bash Extraction"
echo "============================================"

# Check dependencies
if ! command -v npm &> /dev/null; then
    print_error "npm not found"
    exit 1
fi


if [ ! -f "$EXTRACTION_SCRIPT" ]; then
    print_error "Extraction script not found: $EXTRACTION_SCRIPT"
    exit 1
fi

echo "📋 Configuration:"
echo "   - Karma server: $KARMA_URL"
echo "   - Extraction method: Bash HTML parsing"
echo "   - Output file: $OUTPUT_FILE"
echo ""


# Ensure test-results directory exists
mkdir -p test-results
# Clean up existing results
rm -f "$OUTPUT_FILE" karma-failures.json test-results/karma-failures.json extracted_test_names.txt

# Function to cleanup on exit
cleanup() {
    echo ""
    print_status "Stopping Karma server (normal cleanup)..."
    if [ -n "$KARMA_PID" ]; then
        kill $KARMA_PID 2>/dev/null || true
        # Note: "Terminated: 15" message is expected - it's SIGTERM normal shutdown
        # Wait a moment for process to die
        sleep 2
    fi
}

trap cleanup EXIT

echo "🚀 Step 1: Starting Karma server..."
echo ""

get_mtime() {
  local f="$1"
  if [ ! -e "$f" ]; then echo 0; return; fi
  if stat -f %m "$f" >/dev/null 2>&1; then
    stat -f %m "$f"
  else
    stat -c %Y "$f"
  fi
}

# Start Karma in background
KARMA_LOG=$(mktemp)
START_TIME=$(date +%s)
# Prefer line-buffered output when available to reduce grep lag
if command -v stdbuf >/dev/null 2>&1; then
  stdbuf -oL -eL npm run test:cljs:karma > "$KARMA_LOG" 2>&1 &
elif command -v script >/dev/null 2>&1; then
  # Use a PTY to encourage line-buffering on systems without stdbuf (macOS/BSD)
  script -q /dev/null npm run test:cljs:karma > "$KARMA_LOG" 2>&1 &
else
  npm run test:cljs:karma > "$KARMA_LOG" 2>&1 &
fi
KARMA_PID=$!

print_status "Karma started with PID: $KARMA_PID"
print_status "Karma log at: $KARMA_LOG (tail -f to watch)"

# Wait for Karma server to be ready
print_status "Waiting for Karma server to start..."
MAX_WAIT=45
WAIT_COUNT=0

# Try to auto-detect the actual Karma URL/port from the log (some environments randomize ports)
detect_karma_url() {
    local detected
    # Common pattern: "Karma vX server started at http://localhost:9879/"
    detected=$(grep -Eo 'server started at http://[^ ]+' "$KARMA_LOG" 2>/dev/null | tail -1 | awk '{print $4}')
    if [ -z "$detected" ]; then
        # Fallback: any http://host:port occurrences from stack traces / base URLs
        detected=$(grep -Eo 'http://[^/ ]+:[0-9]+' "$KARMA_LOG" 2>/dev/null | tail -1)
    fi
    if [ -n "$detected" ] && [ "$detected" != "$KARMA_URL" ]; then
        KARMA_URL="$detected"
        export KARMA_URL
        DEBUG_URL="$KARMA_URL/debug.html"
        export DEBUG_URL
        print_status "Detected Karma URL: $KARMA_URL"
    fi
}

# Pretty-print saved artifacts with short explanations
print_artifacts() {
  echo "📁 Saved artifacts in test-results/"
  for f in test-results/*; do
    [ -e "$f" ] || continue
    base="$(basename "$f")"
    case "$base" in
      karma-results.json)
        echo " - $base — summary JSON (total/passed/failed/successRate, extractionMethod)" ;;
      karma-failures.json)
        echo " - $base — failures-only JSON (name/suite/file:line/message/log per failure)" ;;
      *)
        echo " - $base — artifact from test run" ;;
    esac
  done
}

# Consider HTTP ready if we get any sane status code (200/301/302/404)
check_http_ready() {
  local url="$1"
  curl -sI --max-time 2 "$url" 2>/dev/null | head -n1 | grep -Eq 'HTTP/[0-9\.]+ (200|30[12]|404)'
}

while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    # Attempt port auto-detection on each iteration
    detect_karma_url || true
    # Ready if HTTP responds
    if check_http_ready "$KARMA_URL" || check_http_ready "$KARMA_URL/debug.html" || check_http_ready "$KARMA_URL/context.json"; then
        print_success "Karma server is ready!"
        break
    fi
    # Or if log shows tests already started/completed
    if [ -f "$KARMA_LOG" ] && \
       (grep -q "\[KARMA-ADAPTER\] Tests completed" "$KARMA_LOG" 2>/dev/null || \
        grep -q "Summary: .* tests" "$KARMA_LOG" 2>/dev/null); then
        print_success "Karma tests appear completed (from log)"
        break
    fi
    # Also treat early adapter messages as readiness signal
    if [ -f "$KARMA_LOG" ] && \
       (grep -q "All files loaded, ready for tests" "$KARMA_LOG" 2>/dev/null || \
        grep -q "Auto-starting tests" "$KARMA_LOG" 2>/dev/null || \
        grep -q "Calling shadow.test.karma.start" "$KARMA_LOG" 2>/dev/null); then
        print_success "Karma signals ready in log"
        break
    fi

    echo -n "."
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
    # Show a helpful hint every 5 seconds
    if [ $((WAIT_COUNT % 5)) -eq 0 ]; then
      print_status "Still waiting... checking $KARMA_URL (attempt $WAIT_COUNT/$MAX_WAIT)"
      # Surface last few log lines for visibility
      tail -n 5 "$KARMA_LOG" 2>/dev/null || true
    fi
done

echo ""

if [ $WAIT_COUNT -eq $MAX_WAIT ]; then
    print_error "Karma server failed to start within $MAX_WAIT seconds"
    echo ""
    echo "🔍 Debugging info:"
    echo "Karma log output (head):"
    sed -n '1,60p' "$KARMA_LOG" 2>/dev/null || true
    echo ""
    echo "Attempting log-based extraction anyway..."
    if KARMA_LOG="$KARMA_LOG" OUTPUT_FILE="$OUTPUT_FILE" "$EXTRACTION_SCRIPT" >/dev/null 2>&1; then
        print_success "Log extraction completed despite startup timeout"
        # fall through to final analysis
    else
        print_error "Log extraction failed"
        rm -f "$KARMA_LOG"
        exit 1
    fi
fi

echo ""
# Save timestamp for reference
RUN_TS=$(date +%Y%m%d-%H%M%S)
mkdir -p test-results

echo "🚀 Step 2: Waiting for tests to complete..."
echo ""

# Wait for tests to run (monitor Karma log for completion)
TEST_TIMEOUT=90  # allow a bit more time
TEST_WAIT=0
TESTS_COMPLETED=false

while [ $TEST_WAIT -lt $TEST_TIMEOUT ]; do
    # Consider completion if the Karma process exited (singleRun should exit)
    if ! kill -0 "$KARMA_PID" 2>/dev/null; then
        print_success "Karma process exited; treating as completion"
        TESTS_COMPLETED=true
        break
    fi

    # Consider completion if a NEW results JSON was written during this run
    if [ -s "$OUTPUT_FILE" ]; then
        of_mtime=$(get_mtime "$OUTPUT_FILE")
        if [ "$of_mtime" -ge "$START_TIME" ]; then
            print_success "Results file detected; tests completed"
            TESTS_COMPLETED=true
            break
        fi
    fi
    if [ -s "test-results/karma-failures.json" ]; then
        kf_mtime=$(get_mtime "test-results/karma-failures.json")
        if [ "$kf_mtime" -ge "$START_TIME" ]; then
            print_success "Failures file detected; tests completed"
            TESTS_COMPLETED=true
            break
        fi
    fi

    # Check if tests completed by looking at Karma log
    if grep -q "TOTAL:.*SUCCESS" "$KARMA_LOG" 2>/dev/null || \
       grep -q "Executed.*of.*SUCCESS" "$KARMA_LOG" 2>/dev/null || \
       grep -q "\[KARMA-ADAPTER\] Tests completed" "$KARMA_LOG" 2>/dev/null || \
       grep -q "\[KARMA-ADAPTER\] Summary: .* tests" "$KARMA_LOG" 2>/dev/null || \
       grep -q "Summary: .* tests" "$KARMA_LOG" 2>/dev/null; then
        print_success "Tests completed detected!"
        TESTS_COMPLETED=true
        break
    fi

    # Also check for explicit failure completion
    if grep -q "TOTAL:.*FAILED" "$KARMA_LOG" 2>/dev/null || \
       grep -q "Executed.*of.*FAILED" "$KARMA_LOG" 2>/dev/null || \
       grep -q "\[KARMA-ADAPTER\] Failed tests:" "$KARMA_LOG" 2>/dev/null; then
        print_warning "Tests completed with failures!"
        TESTS_COMPLETED=true
        break
    fi

    echo -n "⏳ Waiting for tests... ($((TEST_WAIT + 1))/${TEST_TIMEOUT})  [log: $KARMA_LOG]"
    sleep 2
    TEST_WAIT=$((TEST_WAIT + 1))
    echo -ne "\r"
done

echo ""

if [ "$TESTS_COMPLETED" != "true" ]; then
    print_warning "Test completion not detected, proceeding with extraction anyway..."
else
    print_success "Tests completed in ${TEST_WAIT} seconds"
fi

echo ""
echo "🚀 Step 3: Extracting test results with Bash..."
echo ""

# Wait a moment for results to be fully available
sleep 3

# Run the Bash extraction script (pass Karma log path)
if KARMA_LOG="$KARMA_LOG" OUTPUT_FILE="$OUTPUT_FILE" "$EXTRACTION_SCRIPT" >/dev/null 2>&1; then
    print_success "Bash extraction completed!"

    if command -v python3 >/dev/null 2>&1 && [ -f "${SCRIPT_DIR}/karma_log_parser.py" ]; then
        if python3 "${SCRIPT_DIR}/karma_log_parser.py" \
          --log "$KARMA_LOG" \
          --summary "$OUTPUT_FILE" \
          --failures "test-results/karma-failures.json"; then
            print_success "Python log parser produced detailed results"
            # Fast path: finalize immediately from JSON to avoid hangs
            if [ -s "$OUTPUT_FILE" ]; then
              if command -v jq >/dev/null 2>&1; then
                TOTAL=$(jq -r '.summary.total // 0' "$OUTPUT_FILE")
                FAILED_ASSERTIONS=$(jq -r '.summary.failedAssertions // .summary.failed // 0' "$OUTPUT_FILE")
                FAILED_TESTS=$(jq -r '.summary.failedTests // .summary.failed // 0' "$OUTPUT_FILE")
                if [ "$TOTAL" -gt 0 ]; then
                  echo ""
                  echo "🚀 Step 4: Final analysis..."
                  if [ "${JSON_ONLY:-0}" = "1" ] && [ -s "test-results/karma-failures.json" ]; then
                    cat "test-results/karma-failures.json"
                  else
                    echo "❌ $FAILED_ASSERTIONS failed assertions across ${FAILED_TESTS:-0} tests"
                  fi
                  # Always show saved artifacts (names + explanations)
                  echo ""
                  print_artifacts
                  # Exit according to failures
                  if [ "$FAILED_ASSERTIONS" -eq 0 ]; then exit 0; else exit 1; fi
                fi
              fi
            fi
    else
        print_warning "Python log parser failed; using bash-extracted summary only"
    fi
fi

    # Try extraction with debug output only if explicitly requested
    if [ "${VERBOSE_EXTRACTION:-0}" = "1" ]; then
        print_error "Bash extraction failed"
        echo ""
        echo "🔍 Debugging:"
        echo "Karma log (last 20 lines):"
        tail -20 "$KARMA_LOG"
        echo ""
        echo "Try manual extraction:"
        echo "  KARMA_LOG="$KARMA_LOG" $EXTRACTION_SCRIPT"
    else
        print_error "Main extraction failed, but results may already exist from timeout extraction (use VERBOSE_EXTRACTION=1 for debugging)"
    fi
fi

# Clean up log file unless KEEP_KARMA_LOG=1
if [ "${KEEP_KARMA_LOG:-0}" != "1" ]; then
  rm -f "$KARMA_LOG"
else
  print_status "Keeping Karma log at: $KARMA_LOG (KEEP_KARMA_LOG=1)"
fi

echo ""
echo "🚀 Step 4: Final analysis..."
echo ""

# Display results if available
if [ -f "$OUTPUT_FILE" ]; then
    if command -v jq &> /dev/null; then
        TOTAL=$(jq -r '.summary.total // 0' "$OUTPUT_FILE")
        FAILED_ASSERTIONS=$(jq -r '.summary.failedAssertions // .summary.failed // 0' "$OUTPUT_FILE")
        FAILED_TESTS=$(jq -r '.summary.failedTests // .summary.failed // 0' "$OUTPUT_FILE")

        # Print full details for ALL failed tests; omit summary as requested
        if [ "$FAILED_ASSERTIONS" -gt 0 ]; then
            if [ "${JSON_ONLY:-0}" = "1" ] && [ -s "test-results/karma-failures.json" ]; then
                # Emit pure JSON for programmatic consumption
                cat "test-results/karma-failures.json"
            elif [ "${QUIET_OUTPUT:-0}" = "1" ]; then
                # Quiet mode - just show summary
                echo "❌ $FAILED_ASSERTIONS failed assertions across $FAILED_TESTS tests - see test-results/karma-failures.json for details"
            else
                # Verbose mode - show full details (default)
                echo "❌ $FAILED_ASSERTIONS failed assertions across ${FAILED_TESTS:-0} tests"
                echo ""
                echo "❌ Failed Assertions (full details):"
                echo "-----------------------------------"
                cat "$OUTPUT_FILE" | jq -r '
                      .results[] |
                      select(.success == false) |
                      ("- Name: " + .name),
                      ("  Suite: " + (.suite // "Unknown suite")),
                      ("  File: " + (.file // "") + ((.line // 0) as $l | if $l > 0 then (":" + ($l|tostring)) else "" end)),
                      ("  Message: " + (.message // "")),
                      "  Log:",
                      ((.log // []) | map("    - " + .) | .[]),
                  ""
                '
                fi
        elif [ "$TOTAL" -eq 0 ]; then
            print_error "No tests detected in results JSON"
            echo ""
            echo "🔍 Debugging (last 80 log lines):"
            tail -n 80 "$KARMA_LOG" 2>/dev/null || true
            echo ""
            echo "Tips:"
            echo " - Ensure karma.conf.cjs includes test/capture-test-results.js and test/karma-adapter.js"
            echo " - Verify console forwarding: captureConsole + browserConsoleLogOptions in karma.conf.cjs"
            echo " - Try: KEEP_KARMA_LOG=1 $0 karma-enhanced and share the log tail if still 0"
            echo ""
            print_artifacts
            exit 1
        else
            print_success "No failed tests"
        fi

        # Determine overall status (based on failures only)
        if [ "$FAILED_ASSERTIONS" -eq 0 ]; then
            echo ""
            print_artifacts
            exit 0
        else
            echo ""
            print_artifacts
            exit 1
        fi
    else
        print_warning "jq not found - showing raw JSON:"
        cat "$OUTPUT_FILE"
    fi
else
    print_error "No results file created"
    echo ""
    echo "💡 Alternative options:"
    echo "  1. Try manual extraction: $EXTRACTION_SCRIPT"
    echo "  2. Run Node.js tests: npm run test:cljs-node"
    echo "  3. Run tests in debug mode for more information"
    exit 1
fi

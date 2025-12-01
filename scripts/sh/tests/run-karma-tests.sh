#!/bin/bash

# Complete Karma Test Runner with Result Capture
# Launches Karma tests and automatically captures all results

set -e

echo "🧪 Karma + Shadow-CLJS Complete Test Runner"
echo "=========================================="

# Check dependencies
if ! command -v npm &> /dev/null; then
    echo "❌ npm not found"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo "❌ Node.js not found"
    exit 1
fi

# Check if Chrome is available
if ! command -v "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" &> /dev/null && ! command -v google-chrome &> /dev/null && ! command -v chromium &> /dev/null; then
    echo "❌ Chrome/Chromium not found"
    exit 1
fi

echo "📋 Test Configuration:"
echo "   - Karma server: http://localhost:9876"
echo "   - Browser: Chrome headless"
echo "   - Test framework: Shadow-CLJS + Karma"
echo "   - Result capture: Enhanced monitoring"
echo ""

# Clean up any existing test results
rm -f karma-test-results.json test-failures.json

echo "🚀 Step 1: Starting Karma test server..."
echo ""

# Start Karma in background and capture output
KARMA_OUTPUT=$(mktemp)
npm run test:cljs:karma 2>&1 | tee "$KARMA_OUTPUT" &
KARMA_PID=$!

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "🛑 Cleaning up..."
    if [ -n "$KARMA_PID" ]; then
        kill $KARMA_PID 2>/dev/null || true
    fi
    rm -f "$KARMA_OUTPUT"
}
trap cleanup EXIT

echo "⏳ Waiting for Karma server to start..."
sleep 5

# Check if Karma server is running
MAX_ATTEMPTS=10
ATTEMPT=1
KARMA_READY=false

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    if curl -s --connect-timeout 2 "http://localhost:9876" > /dev/null 2>&1; then
        KARMA_READY=true
        echo "✅ Karma server is ready!"
        break
    fi

    echo "⏳ Waiting for Karma server... (attempt $ATTEMPT/$MAX_ATTEMPTS)"
    sleep 2
    ATTEMPT=$((ATTEMPT + 1))
done

if [ "$KARMA_READY" != "true" ]; then
    echo "❌ Karma server failed to start within ${MAX_ATTEMPTS} attempts"
    echo ""
    echo "🔍 Debugging info:"
    echo "   Karma output:"
    cat "$KARMA_OUTPUT" | head -20
    echo ""
    echo "💡 Check karma.conf.cjs configuration"
    exit 1
fi

echo ""
echo "🚀 Step 2: Monitoring test execution..."
echo ""

# Run the test result extraction
if [ -f "/Users/enes/Projects/hosting/scripts/extract-karma-test-results.sh" ]; then
    /Users/enes/Projects/hosting/scripts/extract-karma-test-results.sh
else
    echo "❌ Test extraction script not found"
    exit 1
fi

echo ""
echo "🚀 Step 3: Analyzing results..."
echo ""

# Wait a moment for final results
sleep 3

# Display final summary
if [ -f "karma-test-results.json" ]; then
    TOTAL_TESTS=$(cat karma-test-results.json | jq -r '.summary.total // 0')
    PASSED=$(cat karma-test-results.json | jq -r '.summary.passed // 0')
    FAILED=$(cat karma-test-results.json | jq -r '.summary.failed // 0')
    SUCCESS_RATE=$(cat karma-test-results.json | jq -r '.summary.successRate // 0')
    TIMEOUT=$(cat karma-test-results.json | jq -r '.summary.timeout // false')

    echo "🏁 FINAL TEST RESULTS:"
    echo "====================="
    echo "📊 Summary:"
    echo "   Total Tests: $TOTAL_TESTS"
    echo "   ✅ Passed:    $PASSED"
    echo "   ❌ Failed:    $FAILED"
    echo "   📈 Success Rate: $SUCCESS_RATE%"

    if [ "$TIMEOUT" = "true" ]; then
        echo "   ⚠️ Status: Timeout - may have partial results"
    else
        echo "   ✅ Status: Complete"
    fi

    # Check for specific errors you mentioned
    echo ""
    echo "🔍 Error Analysis:"

    # Count failures by type
    STRING_INCLUDE_FAILURES=$(cat karma-test-results.json | jq -r '.results[] | select(.success == false and (.failureMessage // "") | contains("str/includes?")) | .name' | wc -l || echo "0")
    ASSERTION_FAILURES=$(cat karma-test-results.json | jq -r '.results[] | select(.success == false and (.failureMessage // "") | contains("=")) | .name' | wc -l || echo "0")

    echo "   String include failures: $STRING_INCLUDE_FAILURES"
    echo "   Equality assertion failures: $ASSERTION_FAILURES"

    # Show specific failing tests if any
    if [ "$FAILED" -gt 0 ]; then
        echo ""
        echo "❌ Failed Tests:"
        cat karma-test-results.json | jq -r '.results[] | select(.success == false) | "   • \(.name): \(.failureMessage // .status // "No details")"' | head -10

        if [ "$FAILED" -gt 10 ]; then
            echo "   ... and $((FAILED - 10)) more failures"
        fi
    fi

    # Determine overall status
    if [ "$FAILED" -eq 0 ] && [ "$TOTAL_TESTS" -gt 0 ]; then
        echo ""
        echo "🎉 ALL TESTS PASSED! 🎉"
        exit 0
    elif [ "$TOTAL_TESTS" -eq 0 ]; then
        echo ""
        echo "⚠️ No tests were captured - check configuration"
        exit 1
    else
        echo ""
        echo "❌ TESTS FAILED - $FAILED out of $TOTAL_TESTS tests failed"
        exit 1
    fi

else
    echo "❌ No test results were captured"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "   1. Check karma.conf.cjs configuration"
    echo "   2. Verify shadow-cljs.edn has correct test settings"
    echo "   3. Check Chrome availability"
    echo "   4. Try running tests in verbose mode for more debugging"
    echo ""
    echo "💡 Alternative: Run Node.js tests for immediate results:"
    echo "   npm run test:cljs-node"
    exit 1
fi

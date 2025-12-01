#!/bin/bash

# Comprehensive Test Runner for Frontend Tests
# Provides multiple ways to run and monitor tests

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

# Function to display help
show_help() {
    echo "🧪 Frontend Test Runner"
    echo "======================"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  karma         Run tests with Karma (browser environment, Chrome-based)"
    echo "  karma-enhanced      Run tests with Karma (browser environment, enhanced extraction)"
    echo "  node          Run tests with Node.js (fast, immediate results)"
    echo "  watch         Start test server in watch mode"
    echo "  monitor       Monitor currently running tests"
    echo "  extract       Extract results from currently running tests"
    echo "  all           Run both Karma and Node.js tests"
    echo "  help          Show this help message"
    echo ""
    echo "Quick Examples:"
    echo "  $0 karma      # Run browser tests (Chrome-based)"
    echo "  $0 node       # Run Node.js tests (recommended for fast feedback)"
    echo "  $0 karma-enhanced   # Run browser tests (enhanced extraction)"
    echo "  $0 monitor    # Monitor test results in real-time"
    echo ""
    echo "File Locations:"
    echo "  Karma config: karma.conf.cjs"
    echo "  Shadow config: shadow-cljs.edn"
    echo "  Test results: karma-test-results.json"
    echo "  Test failures: test-failures.json"
}

# Function to run Karma tests
run_karma_tests() {
    print_status "Running Karma + Shadow-CLJS tests..."
    echo ""

    if [ -f "/Users/enes/Projects/hosting/scripts/sh/tests/run-karma-tests.sh" ]; then
        /Users/enes/Projects/hosting/scripts/sh/tests/run-karma-tests.sh
    else
        print_error "Karma test runner script not found"
        print_status "Falling back to npm command..."
        npm run test:cljs:karma
    fi
}

# Function to run Node.js tests
run_node_tests() {
    print_status "Running Node.js tests (immediate results)..."
    echo ""

    if npm run test:cljs-node 2>&1; then
        print_success "Node.js tests completed successfully"
        return 0
    else
        print_error "Node.js tests failed"
        return 1
    fi
}

# Function to start watch mode
start_watch_mode() {
    print_status "Starting test server in watch mode..."
    print_status "Open http://localhost:8091 to view tests in browser"
    echo ""

    npm run test:cljs:watch
}

# Function to monitor tests
monitor_tests() {
    print_status "Monitoring test results..."
    echo ""

    # Check if Karma server is running
    if curl -s --connect-timeout 3 "http://localhost:9876" > /dev/null 2>&1; then
        print_status "Karma server detected - extracting Karma results..."
        if [ -f "/Users/enes/Projects/hosting/scripts/sh/tests/extract-karma-results.sh" ]; then
            /Users/enes/Projects/hosting/scripts/sh/tests/extract-karma-results.sh
        else
            print_error "Karma extraction script not found"
        fi
    elif curl -s --connect-timeout 3 "http://localhost:8091" > /dev/null 2>&1; then
        print_status "Browser test server detected - using headless monitoring..."
        if [ -f "/Users/enes/Projects/hosting/scripts/sh/tests/headless-test-monitor-v2.sh" ]; then
            /Users/enes/Projects/hosting/scripts/sh/tests/headless-test-monitor-v2.sh
        else
            print_error "Headless monitor script not found"
        fi
    else
        print_error "No test server running"
        print_status "Start tests first with:"
        echo "  $0 karma     # Start Karma tests"
        echo "  $0 watch     # Start watch mode"
        echo "  $0 node      # Run Node.js tests directly"
        return 1
    fi
}

# Function to extract results
extract_results() {
    print_status "Extracting test results..."
    echo ""

    # Try different extraction methods
    if [ -f "karma-test-results.json" ]; then
        print_success "Found Karma test results:"
        TOTAL=$(cat karma-test-results.json | jq -r '.summary.total // 0')
        PASSED=$(cat karma-test-results.json | jq -r '.summary.passed // 0')
        FAILED=$(cat karma-test-results.json | jq -r '.summary.failed // 0')
        SUCCESS_RATE=$(cat karma-test-results.json | jq -r '.summary.successRate // 0')

        echo "   Total: $TOTAL, Passed: $PASSED, Failed: $FAILED, Success Rate: $SUCCESS_RATE%"

        if [ "$FAILED" -gt 0 ]; then
            echo ""
            print_warning "Failed tests:"
            cat karma-test-results.json | jq -r '.results[] | select(.success == false) | "   • \(.name): \(.message // .failureMessage // "No details")"'
        fi

    elif [ -f "test-failures.json" ]; then
        print_success "Found test failure analysis:"
        TOTAL=$(cat test-failures.json | jq -r '.summary.totalTests // 0')
        FAILURES=$(cat test-failures.json | jq -r '.summary.failures // 0')
        EXTRACTED=$(cat test-failures.json | jq -r '.totalFailures // 0')

        echo "   Total: $TOTAL, Failures: $FAILURES, Extracted: $EXTRACTED"

    else
        print_warning "No existing test results found"
        print_status "Running live extraction..."
        monitor_tests
    fi
}

# Function to run all tests
run_all_tests() {
    print_status "Running comprehensive test suite..."
    echo ""

    print_status "1/2: Running Node.js tests..."
    if run_node_tests; then
        NODE_SUCCESS=0
    else
        NODE_SUCCESS=1
    fi
    echo ""

    print_status "2/2: Running Karma tests..."
    if run_karma_tests; then
        KARMA_SUCCESS=0
    else
        KARMA_SUCCESS=1
    fi
    echo ""

    # Final summary
    print_status "Test Suite Summary:"
    echo "   Node.js tests: $([ $NODE_SUCCESS -eq 0 ] && echo '✅ PASSED' || echo '❌ FAILED')"
    echo "   Karma tests:   $([ $KARMA_SUCCESS -eq 0 ] && echo '✅ PASSED' || echo '❌ FAILED')"
    echo ""

    if [ $NODE_SUCCESS -eq 0 ] && [ $KARMA_SUCCESS -eq 0 ]; then
        print_success "All test suites passed! 🎉"
        return 0
    else
        print_error "Some test suites failed"
        return 1
    fi
}

# Main script logic
case "${1:-help}" in
    "karma")
        run_karma_tests
        ;;
    "karma-enhanced")
        /Users/enes/Projects/hosting/scripts/sh/tests/run-karma-enhanced.sh
        ;;
    "node")
        run_node_tests
        ;;
    "watch")
        start_watch_mode
        ;;
    "monitor")
        monitor_tests
        ;;
    "extract")
        extract_results
        ;;
    "all")
        run_all_tests
        ;;
    "help"|"-h"|"--help")
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        echo ""
        show_help
        exit 1
        ;;
esac

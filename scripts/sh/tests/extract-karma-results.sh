#!/bin/bash

# Simple Karma test result extractor using curl and grep
# More reliable than Chrome headless approach

KARMA_URL="${KARMA_URL:-http://localhost:9876}"
DEBUG_URL="${DEBUG_URL:-http://localhost:9876/debug.html}"
OUTPUT_FILE="${OUTPUT_FILE:-karma-test-results.json}"

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

# Function to check if Karma server is running
check_karma_server() {
    if curl -s --connect-timeout 3 "$KARMA_URL" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to fetch debug page
fetch_debug_page() {
    local temp_file=$(mktemp)
    if curl -s "$DEBUG_URL" > "$temp_file" 2>&1; then
        echo "$temp_file"
    else
        rm -f "$temp_file"
        return 1
    fi
}

# Function to extract from Karma server log (preferred)
extract_from_log() {
    local log_file="$1"
    [ -r "$log_file" ] || return 1

    local temp_results=$(mktemp)
    local summary_line
    local norm_log=$(mktemp)

    # Normalize lines: keep only text after "[KARMA-ADAPTER] ", strip quotes, control chars, and ANSI SGR like [39m
    sed -n 's/.*\[KARMA-ADAPTER\] \(.*\)$/\1/p' "$log_file" \
      | sed -e "s/^'//; s/'$//; s/^\"//; s/\"$//" \
      | tr -d '\r\t\033' \
      | sed -E 's/\[[0-9;]*m//g' > "$norm_log"

    # Capture individual test results from console logs (normalized)
    # Example: Test result received: suite name test name - PASS
    grep -o '^Test result received: .*' "$norm_log" 2>/dev/null | \
      sed 's/^Test result received: //' > "$temp_results"

    # Parse pass/fail counts from the individual lines if present
    local total_tests=$(wc -l < "$temp_results" | tr -d ' ')
    local passed_from_lines=$(grep -c ' - PASS\s*$' "$temp_results" 2>/dev/null || true)
    local failed_from_lines=$(grep -c ' - FAIL\s*$' "$temp_results" 2>/dev/null || true)

    # Try to find a summary line emitted by the adapter (normalized)
    summary_line=$(grep -o '^Summary:[^\r\n]*' "$norm_log" 2>/dev/null | tail -1)

    local tests_count=0 passed=0 failures=0 errors=0 success_rate=0

    if [ -n "$summary_line" ]; then
        # [KARMA-ADAPTER] Summary: 176 tests, 176 passed, 0 failed, 100% success rate
        if [[ "$summary_line" =~ Summary:[[:space:]]*([0-9]+)[[:space:]]+tests ]]; then
            tests_count="${BASH_REMATCH[1]}"
        fi
        if [[ "$summary_line" =~ ([0-9]+)[[:space:]]+passed ]]; then
            passed="${BASH_REMATCH[1]}"
        fi
        if [[ "$summary_line" =~ ([0-9]+)[[:space:]]+failed ]]; then
            failures="${BASH_REMATCH[1]}"
        fi
        if [[ "$summary_line" =~ ([0-9]+)%[[:space:]]+success[[:space:]]+rate ]]; then
            success_rate="${BASH_REMATCH[1]}"
        fi
    fi

    # Fallback to derived counts from lines if summary missing
    if [ "${tests_count:-0}" -eq 0 ]; then
        tests_count=$total_tests
    fi
    if [ "${passed:-0}" -eq 0 ] && [ "$tests_count" -gt 0 ]; then
        passed=$passed_from_lines
    fi
    if [ "${failures:-0}" -eq 0 ] && [ "$tests_count" -gt 0 ]; then
        failures=$failed_from_lines
    fi
    if [ "${success_rate:-0}" -eq 0 ] && [ "$tests_count" -gt 0 ]; then
        success_rate=$((passed * 100 / tests_count))
    fi

    # Build detailed failure results (name, suite, message, log)
    local results_json="["
    local suite_map=$(mktemp)
    # Map from summary list (normalized): "  - name (suite)"
    # Strip trailing quotes before mapping to avoid pattern miss
    grep -o '^  - .*' "$norm_log" 2>/dev/null \
      | sed -e "s/'$//; s/\"$//" \
      | sed -E 's/^  - (.*) \(([^()]*)\)\s*$/\1\t\2/' > "$suite_map" || true

    # Prepare list of failed test names
    local fail_names_file=$(mktemp)
    # Prefer explicit FAILED TEST markers from adapter logs
    grep -o '^FAILED TEST: .*' "$norm_log" 2>/dev/null \
      | sed -e 's/^FAILED TEST: //' -e "s/'$//; s/\"$//" > "$fail_names_file" || true
    # Fallback to names from the summary list if none captured above
    if [ ! -s "$fail_names_file" ]; then
      cut -f1 "$suite_map" 2>/dev/null >> "$fail_names_file" || true
    fi
    # De-duplicate
    sort -u -o "$fail_names_file" "$fail_names_file" 2>/dev/null || true

    # Iterate each failure and collect details
    while IFS= read -r fail_name; do
        [ -z "$fail_name" ] && continue
        # Lookup suite if available
        local suite
        suite=$(grep -F "$fail_name	" "$suite_map" | head -1 | cut -f2)
        # Collect error lines for this failure from normalized adapter output
        local err_lines
        err_lines=$(awk -v n="$fail_name" '
            BEGIN{collect=0}
            /^FAILED TEST: /{
                if (index($0,n)>0) {collect=1; next} else {collect=0}
            }
            {
                if (collect==1) {
                    if ($0 ~ /^FAILED TEST: / || $0 ~ /^Summary:/ || $0 ~ /^Failed tests:/ || $0 ~ /^  - /) {collect=0}
                    else if ($0 ~ /^  /) { sub(/^  /,""); print }
                    else if ($0 ~ /^(Exception:|message:|at\s|TypeError|ReferenceError|Uncaught|FAIL in\s)/) { print }
                }
            }
        ' "$norm_log")

        # Harvest nearby RAW Karma log lines for additional context (even when err_lines present)
        local raw_context=""
        local start_line
        start_line=$(grep -n -F "[KARMA-ADAPTER] FAILED TEST: $fail_name" "$log_file" 2>/dev/null | head -1 | cut -d: -f1)
        if [ -z "$start_line" ]; then
          start_line=$(grep -n -F "[KARMA-ADAPTER]   - $fail_name" "$log_file" 2>/dev/null | head -1 | cut -d: -f1)
        fi
        if [ -n "$start_line" ]; then
          raw_context=$(awk -v s=$((start_line+1)) 'NR>=s{
              if ($0 ~ /\[KARMA-ADAPTER\] (FAILED TEST:|Summary:|Failed tests:|\s+- )/) { exit }
              print
              if (++c>=120) exit
          }' "$log_file")
        fi

        # Determine message and build log JSON array
        local message=""
        local log_json="["
        if [ -n "$err_lines" ]; then
            while IFS= read -r l; do
                local clean="$(echo "$l" | tr -d '\r\t\033' | sed -E 's/\[[0-9;]*m//g' | sed 's/^\s*//; s/\s*$//' | tr -d '\000-\010\013-\014\016-\037' | sed 's/\\/\\\\/g; s/"/\\"/g')"
                if [ -z "$message" ] && echo "$clean" | grep -q '^Message: '; then
                    message="$(echo "$clean" | sed 's/^Message: //')"
                fi
                [ -n "$clean" ] && log_json+="\"$clean\","
            done << EOF_LINES
$err_lines
EOF_LINES
        fi
        if [ -n "$raw_context" ]; then
            while IFS= read -r l; do
                # Normalize to content after [KARMA-ADAPTER] if present; else keep raw
                local norm_part
                norm_part=$(echo "$l" | sed -n 's/.*\[KARMA-ADAPTER\] \(.*\)$/\1/p')
                if [ -n "$norm_part" ]; then
                  l="$norm_part"
                fi

                # Sanitize and strip browser prefixes like "Chrome Headless ... (LOG|ERROR): "
                local clean="$(echo "$l" \
                  | sed -E 's/^Chrome Headless[^:]*:( LOG:| ERROR:)?\s*//; s/^Firefox[^:]*:( LOG:| ERROR:)?\s*//' \
                  | sed -E 's/\x1B\[[0-9;]*[mK]//g' \
                  | tr -d '\r\t' \
                  | tr -d '\000-\010\013-\014\016-\037' \
                  | sed 's/^\s*//; s/\s*$//' \
                  | sed 's/\\/\\\\/g; s/"/\\"/g')"
                [ -z "$clean" ] && continue

                # Skip known non-detail lines (summary bullets and headings)
                if echo "$clean" | grep -Eq '^(Failed tests:|Summary:|\s*\-\s)'; then
                  continue
                fi

                # Optionally hide expected/actual/diff lines only if HIDE_EXPECTED_ACTUAL=1 (default is to include)
                if [ "${HIDE_EXPECTED_ACTUAL:-0}" = "1" ] && echo "$clean" | grep -Eq '^(Expected|Actual|expected:|actual:|diff:)'; then
                  continue
                fi

                # Prefer meaningful first message line
                if [ -z "$message" ]; then
                    if echo "$clean" | grep -Eq '^(\(=|\(not|▶|Error|Assertion|TypeError|ReferenceError|Uncaught|Exception:|message:|FAIL in[[:space:]])'; then
                        message="$clean"
                    fi
                else
                    # Prefer explicit error lines over generic FAIL header if encountered later
                    if echo "$clean" | grep -Eq '^(TypeError|ReferenceError|Uncaught|Exception:|message:)'; then
                        message="$clean"
                    fi
                fi

                log_json+="\"$clean\","
            done << EOF_RAW
$raw_context
EOF_RAW
        fi
        # If still no message, try to derive from FAIL header if present in err_lines or raw_context
        if [ -z "$message" ]; then
            local fail_hdr
            fail_hdr=$(printf "%s\n%s\n" "$err_lines" "$raw_context" | grep -m1 -E '^[[:space:]]*FAIL in[[:space:]]*\(')
            if [ -n "$fail_hdr" ]; then
              message=$(echo "$fail_hdr" | tr -d '\r\t\033' | sed -E 's/\[[0-9;]*m//g' | tr -d '\000-\010\013-\014\016-\037' | sed 's/^\s*//; s/\s*$//; s/\\/\\\\/g; s/"/\\"/g')
            fi
        fi
        # Trim trailing comma and close
        log_json="${log_json%,}]"

        # Attempt to extract file and line (first match from err_lines)
        local file_path="" line_no=""
        local file_line
        file_line=$(echo "$err_lines" | grep -Eo '([A-Za-z0-9_./-]+\.(clj|cljc|cljs|js):[0-9]+)' | head -1 || true)
        if [ -z "$file_line" ] && [ -n "$raw_context" ]; then
            # Also match browser-style URLs ending with .cljs:NN
            file_line=$(echo "$raw_context" | grep -Eo '((https?://[^ ]+)?[A-Za-z0-9_./:-]+\.(clj|cljc|cljs|js):[0-9]+)' | head -1 || true)
        fi
        if [ -n "$file_line" ]; then
            file_path="${file_line%:*}"
            line_no="${file_line##*:}"
        fi

        # Escape for JSON
        local esc_name="$(echo "$fail_name" | tr -d '\r\t\033' | sed -E 's/\[[0-9;]*m//g' | tr -d '\000-\010\013-\014\016-\037' | sed 's/\\/\\\\/g; s/"/\\"/g')"
        # If suite is empty/Unknown, try inferring from file path
        local inferred_suite="${suite}"
        if [ -z "$inferred_suite" ] || [ "$inferred_suite" = "Unknown suite" ]; then
          if [ -n "$file_path" ]; then
            # Convert path to namespace-ish: slashes -> dots, underscores -> hyphens, strip extension
            inferred_suite=$(echo "$file_path" \
              | sed -E 's/\.(clj|cljc|cljs|js)$//' \
              | sed 's#/#.#g' \
              | sed 's/_/-/g')
          fi
          [ -z "$inferred_suite" ] && inferred_suite="Unknown suite"
        fi
        local esc_suite="$(echo "$inferred_suite" | tr -d '\r\t\033' | sed -E 's/\[[0-9;]*m//g' | tr -d '\000-\010\013-\014\016-\037' | sed 's/\\/\\\\/g; s/"/\\"/g')"
        local esc_msg="$(echo "${message:-}" | tr -d '\r\t\033' | sed -E 's/\[[0-9;]*m//g' | tr -d '\000-\010\013-\014\016-\037' | sed 's/\\/\\\\/g; s/"/\\"/g')"

        # Escape file path as needed
        local esc_file="$(echo "$file_path" | tr -d '\r\t\033' | sed -E 's/\[[0-9;]*m//g' | tr -d '\000-\010\013-\014\016-\037' | sed 's/\\/\\\\/g; s/"/\\"/g')"
        local line_json=${line_no:-0}
        results_json+="{\"name\":\"$esc_name\",\"suite\":\"$esc_suite\",\"success\":false,\"message\":\"$esc_msg\",\"log\":$log_json,\"file\":\"$esc_file\",\"line\":$line_json,\"source\":\"karma-log\"},"
    done < "$fail_names_file"

    # If we didn't capture any failure blocks but failures exist, fall back to summary list
    if [ "$failures" -gt 0 ] && [ "$results_json" = "[" ]; then
        while IFS=$'\t' read -r map_name map_suite; do
            [ -z "$map_name" ] && continue
            # If mapping failed (no tab), attempt inline parse
            if [ -z "$map_suite" ]; then
                # Try to extract trailing (suite)
                local try_suite try_name
                try_suite=$(echo "$map_name" | sed -n 's/^  - .* (\([^()]*\))\s*$/\1/p')
                try_name=$(echo "$map_name" | sed -n 's/^  - \(.*\) (.*)\s*$/\1/p')
                if [ -n "$try_suite" ] && [ -n "$try_name" ]; then
                    map_suite="$try_suite"
                    map_name="$try_name"
                else
                    # Remove leading bullet if still present
                    map_name=$(echo "$map_name" | sed 's/^  - \s*//')
                fi
                # Drop any trailing quotes
                map_name=$(echo "$map_name" | sed -e "s/'$//; s/\"$//")
            fi

            local esc_name="$(echo "$map_name" | tr -d '\r\t\033' | sed -E 's/\[[0-9;]*m//g' | sed 's/\\/\\\\/g; s/"/\\"/g')"
            local esc_suite="$(echo "${map_suite:-Unknown suite}" | tr -d '\r\t\033' | sed -E 's/\[[0-9;]*m//g' | sed 's/\\/\\\\/g; s/"/\\"/g')"
            results_json+="{\"name\":\"$esc_name\",\"suite\":\"$esc_suite\",\"success\":false,\"message\":\"\",\"log\":[],\"source\":\"karma-log\"},"
        done < "$suite_map"
    fi

    # Trim trailing comma and close array
    results_json="${results_json%,}]"

    # If nothing meaningful was captured, signal failure to allow fallback
    if [ "${tests_count:-0}" -eq 0 ] && [ ! -s "$fail_names_file" ] && [ "${total_tests:-0}" -eq 0 ] && [ -z "$summary_line" ]; then
      print_warning "No adapter patterns found in Karma log; falling back to HTML extraction"
      rm -f "$OUTPUT_FILE" 2>/dev/null || true
      return 1
    fi

    # Produce JSON
    local escaped_summary="$(echo "$summary_line" | tr -d '\r\t\033' | sed -E 's/\[[0-9;]*m//g' | sed 's/\\/\\\\/g; s/"/\\"/g')"
    cat > "$OUTPUT_FILE" << EOF
{
  "summary": {
    "timestamp": "$(date -Iseconds)",
    "total": ${tests_count:-0},
    "passed": ${passed:-0},
    "failed": ${failures:-0},
    "errors": ${errors:-0},
    "successRate": ${success_rate:-0},
    "source": "bash-extraction"
  },
  "results": $results_json,
  "extractionMethod": "bash-log-grep",
  "htmlSummary": "$escaped_summary"
}
EOF

    print_status "Parsed Karma log: ${tests_count:-0} total, ${passed:-0} passed, ${failures:-0} failed"

    # Export for main control flow
    TOTAL_TESTS="$tests_count"
    HTML_SUMMARY="$summary_line"
    RESULTS_FILE="$temp_results"

    return 0
}

# Function to extract test results from HTML
extract_test_results() {
    local html_file="$1"
    local temp_results=$(mktemp)

    # Extract Karma adapter messages
    grep -o '\[KARMA-ADAPTER\] Test result received: [^<]*' "$html_file" 2>/dev/null | \
        sed 's/\[KARMA-ADAPTER\] Test result received: //' | \
        sed 's/ - \(PASS\|FAIL\)//' > "$temp_results"

    # Count total tests found
    local total_tests=$(wc -l < "$temp_results" | tr -d ' ')

    # Extract test completion message
    local completion_msg=$(grep -o '\[KARMA-ADAPTER\] Tests completed' "$html_file" 2>/dev/null)

    # Extract summary info from HTML
    local html_summary=$(grep -o '\[KARMA-ADAPTER\] Summary:[^<]*' "$html_file" 2>/dev/null | head -1)

    # Fallback pattern used by some reporters
    if [ -z "$html_summary" ]; then
        html_summary=$(grep -o '([0-9]\+ Tests\?, [0-9]\+ failures\?, [0-9]\+ errors\?, [0-9]\+ Assertions\?)' "$html_file" 2>/dev/null | head -1)
    fi

    TOTAL_TESTS="$total_tests"
    COMPLETION_MSG="$completion_msg"
    HTML_SUMMARY="$html_summary"
    RESULTS_FILE="$temp_results"

    # Copy test names to a file for reference
    if [ "$total_tests" -gt 0 ]; then
        cp "$temp_results" "extracted_test_names.txt"
        print_status "Test names saved to extracted_test_names.txt"
    fi
}

# Function to parse summary and create JSON
create_json_output() {
    local total_tests="$1"
    local html_summary="$2"

    # Parse HTML summary for numbers
    local tests_count=0
    local passed=0
    local failures=0
    local errors=0
    local success_rate=0

    if [ -n "$html_summary" ]; then
        if [[ "$html_summary" =~ Summary:[[:space:]]*([0-9]+)[[:space:]]+tests ]]; then
            tests_count="${BASH_REMATCH[1]}"
        fi
        if [[ "$html_summary" =~ ([0-9]+)[[:space:]]+passed ]]; then
            passed="${BASH_REMATCH[1]}"
        fi
        if [[ "$html_summary" =~ ([0-9]+)[[:space:]]+failed ]]; then
            failures="${BASH_REMATCH[1]}"
        fi
        if [[ "$html_summary" =~ ([0-9]+)[[:space:]]+errors ]]; then
            errors="${BASH_REMATCH[1]}"
        fi
        if [[ "$html_summary" =~ ([0-9]+)%[[:space:]]+success[[:space:]]+rate ]]; then
            success_rate="${BASH_REMATCH[1]}"
        fi

        # Fallback parsing for alternative format
        if [ "$tests_count" -eq 0 ]; then
            tests_count=$(echo "$html_summary" | grep -o '[0-9]\+\s*Tests\?' | grep -o '[0-9]\+' | head -1)
            tests_count=${tests_count:-0}
        fi
        if [ "$failures" -eq 0 ]; then
            failures=$(echo "$html_summary" | grep -o '[0-9]\+\s*failures\?' | grep -o '[0-9]\+' | head -1)
            failures=${failures:-0}
        fi
        if [ "$errors" -eq 0 ]; then
            errors=$(echo "$html_summary" | grep -o '[0-9]\+\s*errors\?' | grep -o '[0-9]\+' | head -1)
            errors=${errors:-0}
        fi
    fi

    # Use extracted count if HTML parsing failed
    if [ "$tests_count" -eq 0 ]; then
        tests_count="$total_tests"
    fi

    if [ "$passed" -eq 0 ] && [ "$tests_count" -gt 0 ]; then
        passed=$((tests_count - failures - errors))
    fi

    if [ "$success_rate" -eq 0 ] && [ "$tests_count" -gt 0 ]; then
        success_rate=$((passed * 100 / tests_count))
    fi

    # Escape quotes in summary to keep JSON valid
    local escaped_summary="$(echo "$html_summary" | sed 's/"/\\"/g')"

    # Create JSON output
    cat > "$OUTPUT_FILE" << EOF
{
  "summary": {
    "timestamp": "$(date -Iseconds)",
    "total": $tests_count,
    "passed": $passed,
    "failed": $failures,
    "errors": $errors,
    "successRate": $success_rate,
    "source": "bash-extraction"
  },
  "results": [],
  "extractionMethod": "bash-html-grep",
  "htmlSummary": "$escaped_summary"
}
EOF

    print_success "JSON results created: $tests_count total, $passed passed, $failures failed, $success_rate% success rate"
}

# Main execution
main() {
    print_status "Bash Karma Test Result Extractor"
    print_status "================================="

    # Prefer extracting from Karma server log if available
    if [ -n "$KARMA_LOG" ] && [ -r "$KARMA_LOG" ]; then
        print_status "Karma log detected: $KARMA_LOG"
        if extract_from_log "$KARMA_LOG"; then
            print_success "JSON results created from Karma log"
            echo ""
            echo "📊 Test Summary:"
            if command -v jq &> /dev/null && [ -f "$OUTPUT_FILE" ]; then
                jq -r '"   Total Tests: \(.summary.total)"' "$OUTPUT_FILE"
                jq -r '"   ✅ Passed:    \(.summary.passed)"' "$OUTPUT_FILE"
                jq -r '"   ❌ Failed:    \(.summary.failed)"' "$OUTPUT_FILE"
                jq -r '"   💥 Errors:    \(.summary.errors)"' "$OUTPUT_FILE"
                jq -r '"   📈 Success Rate: \(.summary.successRate)%"' "$OUTPUT_FILE"
            else
                echo "   Results saved to: $OUTPUT_FILE"
            fi
            return 0
        fi
    fi

    # If no log (or parsing failed), fallback to HTTP extraction
    # Check if Karma server is running
    if ! check_karma_server; then
        print_error "Karma server not running at $KARMA_URL"
        print_status "Start it with: npm run test:cljs:karma"
        exit 1
    fi

    print_status "Karma server detected, extracting results..."

    # Fetch debug page
    local html_file
    if html_file=$(fetch_debug_page); then
        print_status "Fetched debug page ($(wc -c < "$html_file") characters)"

        # Extract test results
        if extract_test_results "$html_file"; then
            local total_tests="$TOTAL_TESTS"
            local html_summary="$HTML_SUMMARY"
            local results_file="$RESULTS_FILE"

            print_status "Extracted $total_tests test results"

            # Create JSON output
            create_json_output "$total_tests" "$html_summary"

            # Display summary
            echo ""
            echo "📊 Test Summary:"
            if command -v jq &> /dev/null && [ -f "$OUTPUT_FILE" ]; then
                jq -r '"   Total Tests: \(.summary.total)"' "$OUTPUT_FILE"
                jq -r '"   ✅ Passed:    \(.summary.passed)"' "$OUTPUT_FILE"
                jq -r '"   ❌ Failed:    \(.summary.failed)"' "$OUTPUT_FILE"
                jq -r '"   💥 Errors:    \(.summary.errors)"' "$OUTPUT_FILE"
                jq -r '"   📈 Success Rate: \(.summary.successRate)%"' "$OUTPUT_FILE"
            else
                echo "   Results saved to: $OUTPUT_FILE"
            fi

            echo ""
            echo "💾 Files created:"
            echo "   - $OUTPUT_FILE (JSON results)"
            if [ -f "extracted_test_names.txt" ]; then
                echo "   - extracted_test_names.txt (test names)"
            fi

            echo ""
            echo "🔧 Quick Commands:"
            echo "   View results: cat $OUTPUT_FILE"
            if command -v jq &> /dev/null; then
                echo "   Parse with jq: cat $OUTPUT_FILE | jq ."
            fi

        else
            print_error "Failed to extract test results"
        fi

        # Clean up
        rm -f "$html_file" "$results_file"

    else
        print_error "Failed to fetch debug page"
        exit 1
    fi
}

# Run main function
main "$@"

// Enhanced test result capture script for shadow-cljs + karma
// This script properly parses shadow-cljs test results and captures completion status

(function() {
  'use strict';

  var testResults = [];
  var testStarted = true; // enable capturing conservatively; gating proved brittle across runners
  var captureConsole = true;
  var lastTestIdx = -1;

  // Parse the Object{...} format that shadow-cljs uses
  function parseObjectString(objectString) {
    try {
      // Remove "Object{" prefix and "}" suffix
      var content = objectString.replace(/^Object\{|\}$/g, '');

      // Parse key-value pairs manually
      var result = {};
      var pairs = content.split(', ');

      for (var i = 0; i < pairs.length; i++) {
        var pair = pairs[i].trim();
        var colonIndex = pair.indexOf(':');

        if (colonIndex > 0) {
          var key = pair.substring(0, colonIndex).trim();
          var value = pair.substring(colonIndex + 1).trim();

          // Handle different value types
          if (value === 'true') {
            result[key] = true;
          } else if (value === 'false') {
            result[key] = false;
          } else if (value === 'null' || value === 'undefined') {
            result[key] = null;
          } else if (value === '[]') {
            result[key] = [];
          } else if (value.startsWith('[') && value.endsWith(']')) {
            // Parse array format like ['app.admin.frontend.adapters.users-test']
            var arrayContent = value.substring(1, value.length - 1);
            if (arrayContent.trim() === '') {
              result[key] = [];
            } else {
              result[key] = arrayContent.split(',').map(function(item) {
                return item.replace(/^['"]|['"]$/g, '').trim();
              });
            }
          } else if (value.startsWith("'") || value.startsWith('"')) {
            // String value
            result[key] = value.replace(/^['"]|['"]$/g, '');
          } else if (/^\d+$/.test(value)) {
            // Number value
            result[key] = parseInt(value, 10);
          } else {
            // Treat as string
            result[key] = value;
          }
        }
      }

      return result;
    } catch (e) {
      console.warn('Failed to parse object string:', objectString, e);
      return null;
    }
  }

  // Enhanced console.log override
  var originalLog = console.log;
  console.log = function() {
    // Call original log first
    var args = Array.prototype.slice.call(arguments);
    originalLog.apply(console, args);

    if (!captureConsole) return;

    // Convert arguments to string for processing
    var message = args.join(' ');

    // Detect cljs.test failure header lines to open a failure bucket when adapter hooks are absent
    try {
      var mFail = message.match(/^(FAIL|ERROR) in \(([^\)]+)\)/);
      if (mFail) {
        var nsTest = mFail[2];
        var suiteName = nsTest.indexOf('/') > -1 ? nsTest.split('/')[0] : 'Unknown suite';
        var testName = nsTest.indexOf('/') > -1 ? nsTest.split('/').slice(1).join('/') : nsTest;
        testResults.push({
          suite: suiteName,
          description: testName,
          name: testName,
          success: false,
          skipped: false,
          time: 0,
          log: [],
          file: null,
          line: null,
          message: null
        });
        lastTestIdx = testResults.length - 1;
      }
    } catch (e) {}

    // Capture test results - enhanced pattern matching
    if (message.includes('Test result:')) {
      try {
        var match = message.match(/Test result: (Object\{[^}]+\})/);
        if (match) {
          var parsedResult = parseObjectString(match[1]);
          if (parsedResult) {
            testResults.push({
              suite: parsedResult.suite ? parsedResult.suite.join(' ') : 'Unknown Suite',
              description: parsedResult.description || 'Unknown Test',
              success: parsedResult.success !== false,
              skipped: parsedResult.skipped === true,
              time: parsedResult.time || 0,
              log: parsedResult.log || []
            });

            // Log capture progress
            originalLog.call(console, '[CAPTURE] Test result captured: ' + parsedResult.description + ' - ' + (parsedResult.success ? 'PASS' : 'FAIL'));
          }
        }
      } catch (e) {
        originalLog.call(console, '[CAPTURE ERROR] Failed to parse test result:', e);
      }
    }

    // Also capture from karma adapter format
    if (message.includes('[KARMA-ADAPTER] Test result received:')) {
      try {
        var after = message.split('[KARMA-ADAPTER] Test result received: ')[1];
        if (after) {
          var m = after.trim().match(/^(.*) - (PASS|FAIL)\s*$/);
          var testName = (m ? m[1] : after).trim();
          var status = (m ? m[2] : 'PASS');
          var isSuccess = status === 'PASS';

          testResults.push({
            suite: 'shadow-cljs tests',
            description: testName,
            name: testName,
            success: isSuccess,
            skipped: false,
            time: 0,
            log: [],
            file: null,
            line: null,
            message: null
          });
          lastTestIdx = testResults.length - 1;

          originalLog.call(console, '[CAPTURE] Karma adapter test captured: ' + testName + ' - ' + status);
        }
      } catch (e) {
        originalLog.call(console, '[CAPTURE ERROR] Failed to parse karma adapter result:', e);
      }
    }

    // Detect test completion with multiple patterns
    if (message.includes('Tests completed:') ||
        message.includes('Tests completed') ||
        message.includes('Ran') && message.includes('tests') ||
        message.includes('Testing completed') ||
        message.includes('Test suite finished')) {

      // Finalize test capture
      captureConsole = false;

      // Calculate statistics
      var passed = testResults.filter(function(r) { return r.success; }).length;
      var failed = testResults.filter(function(r) { return !r.success && !r.skipped; }).length;
      var skipped = testResults.filter(function(r) { return r.skipped; }).length;
      var total = testResults.length;

      // Save results globally
      window.capturedTestResults = {
        timestamp: new Date().toISOString(),
        total: total,
        passed: passed,
        failed: failed,
        skipped: skipped,
        successRate: total > 0 ? Math.round((passed / total) * 100) : 0,
        results: testResults
      };

      // Report summary
      originalLog.call(console, '\n=== ENHANCED TEST RESULT CAPTURE ===');
      originalLog.call(console, '✅ Total tests captured:', total);
      originalLog.call(console, '🎉 Passed:', passed);
      originalLog.call(console, '❌ Failed:', failed);
      originalLog.call(console, '⏭️ Skipped:', skipped);
      originalLog.call(console, '📈 Success Rate:', window.capturedTestResults.successRate + '%');
      originalLog.call(console, '📋 Results available in: window.capturedTestResults');

      // Save to file if possible (Node.js environment)
      if (typeof require !== 'undefined') {
        try {
          var fs = require('fs');
          fs.writeFileSync('captured-test-results.json', JSON.stringify(window.capturedTestResults, null, 2));
          originalLog.call(console, '💾 Results saved to: captured-test-results.json');
        } catch (e) {
          // Ignore file save errors in browser environment
        }
      }
    }

    // Detect test start
    if (message.includes('Running tests') || message.includes('Starting test run')) {
      testStarted = true;
      originalLog.call(console, '[CAPTURE] Test run started, capturing results...');
    }

    // Capture assertion-like lines into the current test context (if any)
    if (testStarted && lastTestIdx >= 0) {
      try {
        // Ignore our own adapter/capture chatter
        if (!(/\[KARMA-ADAPTER\]|\[CAPTURE\]/.test(message))) {
          if (/^(\(=|\(not|▶|Expected|Actual|at\s)/.test(message)) {
            // record file:line if present
            var fl2 = message.match(/([A-Za-z0-9_./-]+\.(clj|cljc|cljs|js):(\d+))/);
            if (fl2) {
              testResults[lastTestIdx].file = fl2[1].split(':')[0];
              testResults[lastTestIdx].line = parseInt(fl2[3], 10);
            }
            testResults[lastTestIdx].log = testResults[lastTestIdx].log || [];
            testResults[lastTestIdx].log.push({ type: 'log', message: message, timestamp: new Date().toISOString() });
            if (!testResults[lastTestIdx].message && (message.includes('▶') || /^(\(=|\(not|Expected|Actual)/.test(message))) {
              testResults[lastTestIdx].message = message;
            }
          }
        }
      } catch (e) {}
    }
  };

  // Also capture console.error for error details
  var originalError = console.error;
  console.error = function() {
    var args = Array.prototype.slice.call(arguments);
    originalError.apply(console, args);

    if (captureConsole) {
      var message = args.join(' ');

      // Attach to the most recent test if available
      var idx = lastTestIdx >= 0 ? lastTestIdx : (testResults.length - 1);
      if (idx < 0) {
        // If no current test, create a placeholder when we see assertion-like content
        if (/^(\(=|\(not|▶|Expected|Actual|at\s)/.test(message)) {
          testResults.push({ suite: 'Unknown suite', description: 'Unknown test', name: 'Unknown test', success: false, skipped: false, time: 0, log: [], file: null, line: null, message: null });
          idx = testResults.length - 1;
          lastTestIdx = idx;
        }
      }
      if (idx >= 0) {
        // Record file:line if present
        try {
          var fl = message.match(/([A-Za-z0-9_./-]+\.(clj|cljc|cljs|js):(\d+))/);
          if (fl) {
            testResults[idx].file = fl[1].split(':')[0];
            testResults[idx].line = parseInt(fl[3], 10);
          }
        } catch (e) {}

        // Save the error/log line as-is
        testResults[idx].log.push({
          type: 'error',
          message: message,
          timestamp: new Date().toISOString()
        });

        // If we don't have a primary message yet, set it from a meaningful line
        if (!testResults[idx].message) {
          if (/^(Error|Assertion|FAIL|Expected|Actual|TypeError|ReferenceError|Uncaught|\(=|\(not)/i.test(message) || message.includes('▶')) {
            testResults[idx].message = message;
          }
        }

        // Also emit adapter-tagged log lines so the bash extractor can pick them up from Karma logs
        try {
          if (/^(\(=|\(not|Expected|Actual)/.test(message) || message.includes('▶')) {
            originalLog.apply(console, ['[KARMA-ADAPTER]', '  ' + message]);
          }
          // Emit file line as adapter-tagged if detected
          if (fl) {
            originalLog.apply(console, ['[KARMA-ADAPTER]', '  File: ' + testResults[idx].file + (testResults[idx].line ? (':' + testResults[idx].line) : '')]);
          }
        } catch (e) {}
      }
    }
  };

console.log('Enhanced test result capture initialized');
console.log('Supported patterns: Test result: Object{...}, Tests completed:, Ran X tests');
})();

// Karma adapter for shadow-cljs - Fixed recursion issues
// This adapter provides the Karma interface and triggers shadow-cljs test execution

(function() {
  'use strict';

  // Store test results and state
  var testResults = [];
  var testComplete = false;
  var karmaInstance = null;

  // Find the actual Karma instance to avoid recursion
  function findKarmaInstance() {
    if (karmaInstance) return karmaInstance;

    // Try different ways to find the Karma instance
    if (typeof window !== 'undefined' && window.__karma__) {
      karmaInstance = window.__karma__;
    } else if (typeof window !== 'undefined' && window.top && window.top.__karma__) {
      karmaInstance = window.top.__karma__;
    }

    return karmaInstance;
  }

  // Safe logging that won't trigger recursion
  function safeLog(method, message) {
    try {
      var originalMethod = console[method];
      if (originalMethod && originalMethod.apply) {
        originalMethod.apply(console, ['[KARMA-ADAPTER]', message]);
      }
    } catch (e) {
      // Fallback - just in case
    }
  }

  // Karma interface that satisfies both Karma client and shadow-cljs requirements
  var karmaInterface = {
    // === Karma client methods (required by Karma context.html) ===

    // Called by Karma client when all files are loaded
    loaded: function() {
      safeLog('log', 'All files loaded, ready for tests');

      // Auto-start tests after a brief delay to ensure shadow-cljs is fully loaded
      setTimeout(function() {
        safeLog('log', 'Auto-starting tests...');
        if (window.shadow && window.shadow.test && window.shadow.test.karma && window.shadow.test.karma.start) {
          safeLog('log', 'Calling shadow.test.karma.start()');
          window.shadow.test.karma.start();
        } else {
          safeLog('error', 'shadow.test.karma.start not found!');
          console.log('Available shadow properties:', window.shadow);
          console.log('shadow.test exists:', !!(window.shadow && window.shadow.test));
          console.log('shadow.test.karma exists:', !!(window.shadow && window.shadow.test && window.shadow.test.karma));
        }
      }, 100);
    },

    // === Shadow-cljs methods (required by generated test code) ===

    // This method should NOT be called since shadow-cljs generates its own start function
    start: function() {
      safeLog('warn', '__karma__.start() called - this should not happen with shadow-cljs');
    },

    // Called by shadow-cljs for individual test results
    result: function(result) {
      var testName = result.description || result.suite || 'Unknown test';
      var status = result.success !== false ? 'PASS' : 'FAIL';

      safeLog('log', 'Test result received: ' + testName + ' - ' + status);

      // Enhanced result logging for failure detection
      if (result.success === false) {
        safeLog('error', 'FAILED TEST: ' + testName);
        if (result.log && result.log.length > 0) {
          result.log.forEach(function(logEntry) {
            safeLog('error', '  ' + (typeof logEntry === 'string' ? logEntry : JSON.stringify(logEntry)));
          });
        }
        if (result.message) {
          safeLog('error', '  Message: ' + result.message);
        }
      }

      // Store detailed result for analysis
      testResults.push({
        name: testName,
        success: result.success !== false,
        status: status,
        time: result.time || 0,
        suite: result.suite || 'Unknown suite',
        log: result.log || [],
        message: result.message,
        timestamp: new Date().toISOString()
      });

      // Forward to real Karma instance if available
      var karma = findKarmaInstance();
      if (karma && karma.result && karma.result !== this.result) {
        try {
          karma.result(result);
        } catch (e) {
          safeLog('error', 'Failed to forward result to Karma: ' + e.message);
        }
      }
    },

    // Called by shadow-cljs when all tests are complete
    complete: function(result) {
      var totalTests = testResults.length;
      var passedTests = testResults.filter(function(r) { return r.success; }).length;
      var failedTests = totalTests - passedTests;
      var successRate = totalTests > 0 ? Math.round((passedTests / totalTests) * 100) : 0;

      safeLog('log', '[KARMA-ADAPTER] Tests completed');
      safeLog('log', '[KARMA-ADAPTER] Summary: ' + totalTests + ' tests, ' + passedTests + ' passed, ' + failedTests + ' failed, ' + successRate + '% success rate');

      if (failedTests > 0) {
        safeLog('error', '[KARMA-ADAPTER] Failed tests:');
        var failedList = testResults.filter(function(r) { return !r.success; });
        failedList.forEach(function(test) {
          safeLog('error', '[KARMA-ADAPTER]   - ' + test.name + ' (' + test.suite + ')');
        });

        // Emit full details based on captured console data if available
        try {
          var emitted = false;
          if (typeof window !== 'undefined' && window.capturedTestResults && Array.isArray(window.capturedTestResults.results)) {
            var capturedFailed = window.capturedTestResults.results.filter(function(r) { return r && r.success === false; });
            if (capturedFailed.length > 0) {
              emitted = true;
              capturedFailed.forEach(function(cf) {
                var cname = cf.description || cf.name || 'Unknown test';
                // Start a new failure block so extractors can capture details
                safeLog('error', '[KARMA-ADAPTER] FAILED TEST: ' + cname);
                if (cf.file) {
                  safeLog('error', '[KARMA-ADAPTER]   File: ' + cf.file + (cf.line ? (':' + cf.line) : ''));
                }
                if (cf.message) {
                  safeLog('error', '[KARMA-ADAPTER]   Message: ' + cf.message);
                }
                if (Array.isArray(cf.log)) {
                  cf.log.forEach(function(entry) {
                    var msg = (entry && entry.message) ? entry.message : (typeof entry === 'string' ? entry : JSON.stringify(entry));
                    safeLog('error', '[KARMA-ADAPTER]     ' + msg);
                  });
                }
              });
            }
          }
          // Fallback: emit details from adapter's own results if capture is unavailable
          if (!emitted && failedList.length > 0) {
            failedList.forEach(function(test) {
              safeLog('error', '[KARMA-ADAPTER] FAILED TEST: ' + (test.name || 'Unknown test'));
              if (test.file) {
                safeLog('error', '[KARMA-ADAPTER]   File: ' + test.file + (test.line ? (':' + test.line) : ''));
              }
              if (test.message) {
                safeLog('error', '[KARMA-ADAPTER]   Message: ' + test.message);
              }
              if (Array.isArray(test.log) && test.log.length > 0) {
                test.log.forEach(function(entry) {
                  var msg2 = (entry && entry.message) ? entry.message : (typeof entry === 'string' ? entry : JSON.stringify(entry));
                  safeLog('error', '[KARMA-ADAPTER]     ' + msg2);
                });
              }
            });
          }
        } catch (e) {
          safeLog('error', 'Error emitting detailed failure logs: ' + e.message);
        }
      }

      testComplete = true;

      // Forward to real Karma instance if available
      var karma = findKarmaInstance();
      if (karma && karma.complete && karma.complete !== this.complete) {
        try {
          karma.complete(result);
        } catch (e) {
          safeLog('error', 'Failed to forward completion to Karma: ' + e.message);
        }
      }
    },

    // Called by shadow-cljs for info messages - FIXED RECURSION
    info: function(info) {
      // Only log to console, don't forward to Karma to prevent recursion
      safeLog('log', 'Info: ' + JSON.stringify(info));

      // Store the info but don't forward it to prevent infinite loops
      // This is the key fix - we don't call karma.info() here
    },

    // Called by shadow-cljs for errors
    error: function(error) {
      safeLog('error', 'Error: ' + JSON.stringify(error));

      // Forward to real Karma instance if available
      var karma = findKarmaInstance();
      if (karma && karma.error && karma.error !== this.error) {
        try {
          karma.error(error);
        } catch (e) {
          safeLog('error', 'Failed to forward error to Karma: ' + e.message);
        }
      }
    },

    // Additional methods Karma might expect
    config: {},
    startSpec: function(spec) {
      safeLog('log', 'Starting spec: ' + spec);
    },
    endSpec: function(spec) {
      safeLog('log', 'Ending spec: ' + spec);
    }
  };

  // Set up the karma interface safely
  if (typeof window !== 'undefined') {
    // Store any existing __karma__ to preserve functionality
    var existingKarma = window.__karma__;

    // Create our __karma__ interface
    window.__karma__ = karmaInterface;

    // Preserve any important existing methods
    if (existingKarma) {
      ['loaded', 'start', 'result', 'complete', 'info', 'error'].forEach(function(method) {
        if (existingKarma[method] && typeof existingKarma[method] === 'function') {
          safeLog('log', 'Preserving existing karma.' + method + ' method');
        }
      });
    }

    safeLog('log', 'Karma adapter loaded and __karma__ interface configured');

    // Detect if we're in a Karma environment
    if (window.karma) {
      safeLog('log', 'Karma environment detected');
      karmaInstance = window.karma;
    }
  } else {
    console.error('Karma adapter: window object not available');
  }
})();

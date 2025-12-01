// karma.conf.cjs - Fixed configuration for shadow-cljs
module.exports = function(config) {
  config.set({
    // Use no frameworks - we have our own custom setup
    frameworks: [],

    // Browser configuration
    browsers: ['ChromeHeadless'],

    // Files to load - order matters!
    files: [
      // Load React and ReactDOM first - using browser-compatible production files
      'node_modules/react/cjs/react.production.min.js',
      'node_modules/react-dom/cjs/react-dom.production.min.js',
      // Load test result capture first
      'test/capture-test-results.js',
      // Load our custom adapter to setup __karma__ interface
      'test/karma-adapter.js',
      // Then load the shadow-cljs compiled tests
      'target/karma-test.js'
    ],

    // Exclude the config file itself
    exclude: [
      'karma.conf.cjs'
    ],

    // Reporters
    reporters: ['progress'],

    // Single run for CI/automated testing
    singleRun: true,

    // AutoWatch can be enabled for development
    autoWatch: false,

    // Concurrency level
    concurrency: 1,

    // Plugins
    plugins: [
      require('karma-chrome-launcher')
    ],

    // Proper error handling and reliable console forwarding
    captureConsole: true,
    browserConsoleLogOptions: {
      level: 'log',
      terminal: true
    },
    client: {
      captureConsole: true
    },

    // Timeout settings
    browserNoActivityTimeout: 60000,

    // Karma log level
    logLevel: config.LOG_INFO,

    // Custom launcher config for better stability
    customLaunchers: {
      ChromeHeadless: {
        base: 'Chrome',
        flags: [
          '--no-sandbox',
          '--headless',
          '--disable-gpu',
          '--disable-web-security',
          '--disable-features=VizDisplayCompositor'
        ]
      }
    }
  });
};

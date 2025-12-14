#!/usr/bin/env node

/**
 * CLI script for using optimized WarpGrep configuration
 *
 * Usage:
 *   node scripts/warpgrep-search.js "pattern"
 *   node scripts/warpgrep-search.js "pattern" --type clj
 *   node scripts/warpgrep-search.js "pattern" --source-only
 */

const {
  createOptimizedWarpGrep,
  createFileTypeSpecificWarpGrep,
  createSourceCodeWarpGrep
} = require('../config/warpgrep-config');

// Parse command line arguments
const args = process.argv.slice(2);
const pattern = args[0];

if (!pattern) {
  console.error('Error: Search pattern is required');
  console.log('Usage: node scripts/warpgrep-search.js "pattern" [options]');
  console.log('');
  console.log('Options:');
  console.log('  --type <ext>     Search only files with specified extension (e.g., clj, cljs)');
  console.log('  --source-only    Search only source files (exclude tests/docs)');
  console.log('  --context <n>    Number of context lines (default: 3)');
  console.log('  --help           Show this help message');
  process.exit(1);
}

// Parse options
const options = {
  type: null,
  sourceOnly: false,
  context: 3
};

for (let i = 1; i < args.length; i++) {
  switch (args[i]) {
    case '--type':
      options.type = args[++i];
      break;
    case '--source-only':
      options.sourceOnly = true;
      break;
    case '--context':
      options.context = parseInt(args[++i]) || 3;
      break;
    case '--help':
      console.log('Usage: node scripts/warpgrep-search.js "pattern" [options]');
      console.log('');
      console.log('Options:');
      console.log('  --type <ext>     Search only files with specified extension (e.g., clj, cljs)');
      console.log('  --source-only    Search only source files (exclude tests/docs)');
      console.log('  --context <n>    Number of context lines (default: 3)');
      console.log('  --help           Show this help message');
      process.exit(0);
    default:
      console.error(`Unknown option: ${args[i]}`);
      process.exit(1);
  }
}

async function runSearch() {
  try {
    console.log(`Searching for: "${pattern}"`);
    console.log(`Context lines: ${options.context}`);

    let grepTool;
    let searchType = 'Standard';

    // Create appropriate tool based on options
    if (options.sourceOnly) {
      grepTool = createSourceCodeWarpGrep();
      searchType = 'Source-only';
    } else if (options.type) {
      const ext = options.type.startsWith('.') ? options.type : `.${options.type}`;
      grepTool = createFileTypeSpecificWarpGrep([ext]);
      searchType = `${ext} files only`;
    } else {
      grepTool = createOptimizedWarpGrep();
    }

    console.log(`Search type: ${searchType}`);
    console.log('');

    // Perform search
    const results = await grepTool.search({
      pattern: pattern,
      context: options.context
    });

    if (results && results.length > 0) {
      console.log(`Found ${results.length} results:\n`);

      results.forEach((result, index) => {
        console.log(`${index + 1}. ${result.file}:${result.line || 0}`);
        if (result.context) {
          console.log(result.context);
        }
        console.log('');
      });
    } else {
      console.log('No results found');
    }
  } catch (error) {
    console.error('Search failed:', error.message);

    if (error.message.includes('MORPH_API_KEY')) {
      console.error('\nMake sure MORPH_API_KEY environment variable is set');
    }

    process.exit(1);
  }
}

// Run the search
runSearch();

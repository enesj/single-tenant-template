# WarpGrep Configuration Guide

This guide explains the optimized WarpGrep setup for the single-tenant template project.

## Overview

WarpGrep is a powerful code search tool that integrates with the MorphLLM platform. This project includes an optimized configuration that excludes noisy directories and files to provide cleaner, more relevant search results.

## Configuration Files

### 1. MCP Server Configuration
 **Location**: `.vscode/mcp.json`
- **Purpose**: Configures the MorphLLM MCP server with WarpGrep
- **Key Settings**:
  ```json
  "WARPGREP_EXCLUDES": ".git,node_modules,target,.shadow-cljs,.clj-kondo,.cpcache,dist,build,out,.next,.idea,.vscode,.DS_Store,npm-debug.log*,yarn-debug.log*,yarn-error.log*"
  ```

### 2. JavaScript Configuration Module
- **Location**: `config/warpgrep-config.js`
- **Purpose**: Provides reusable WarpGrep configurations for custom implementations
- **Exports**:
  - `createOptimizedWarpGrep()`: Standard optimized configuration
  - `createFileTypeSpecificWarpGrep()`: For searching specific file types
  - `createSourceCodeWarpGrep()`: For source-only searches (excludes tests/docs)

## Excluded Directories and Files

The configuration excludes the following patterns to improve search quality:

### Build & Dependency Artifacts
- `.git/` - Version control metadata
- `node_modules/` - Node.js dependencies
- `target/` - Clojure compilation output
- `.shadow-cljs/` - Shadow-CLJS build cache
- `.clj-kondo/` - Linter cache
- `dist/`, `build/`, `out/` - JavaScript build outputs

### Log & Temporary Files
- `npm-debug.log*`, `yarn-debug.log*` - Package manager logs
- `*.tmp`, `*.temp` - Temporary files
- `.DS_Store`, `Thumbs.db` - OS-specific files

### IDE & Editor Files
- `.vscode/`, `.idea/` - IDE configuration
- `*.swp`, `*.swo` - Vim swap files

## Usage Examples

### Basic Usage with MCP
The MorphLLM MCP server automatically uses the configured exclusions when you use WarpGrep tools in Claude Code.

### Custom JavaScript Implementation
```javascript
const { createOptimizedWarpGrep } = require('../config/warpgrep-config');

// Create optimized tool
const grepTool = createOptimizedWarpGrep();

// Use for searching
const results = await grepTool.search({
  pattern: 'defn.*database',
  context: 3
});
```

### File-Type Specific Search
```javascript
const { createFileTypeSpecificWarpGrep } = require('../config/warpgrep-config');

// Search only Clojure files
const cljGrep = createFileTypeSpecificWarpGrep(['.clj', '.cljs']);
const results = await cljGrep.search({
  pattern: 're-frame.*event',
  context: 3
});
```

### Source-Only Search
```javascript
const { createSourceCodeWarpGrep } = require('../config/warpgrep-config');

// Search only source files (exclude tests and docs)
const sourceGrep = createSourceCodeWarpGrep();
const results = await sourceGrep.search({
  pattern: 'TODO|FIXME',
  context: 3
});
```

## Performance Benefits

The optimized configuration provides:

1. **Faster Searches**: Excludes large directories like `node_modules/` and `target/`
2. **Cleaner Results**: Removes noise from build artifacts and dependency files
3. **Reduced Noise**: Filters out logs, cache files, and IDE configuration
4. **Focused Context**: Concentrates on actual source code and configuration files

## Environment Variables

The MCP configuration uses these environment variables:

- `MORPH_API_KEY`: Your MorphLLM API key (recommended: keep it in an ignored local file like `.env` or your shell env; never commit it)
- `WARPGREP_EXCLUDES`: Comma-separated list of exclusion patterns
- `ENABLED_TOOLS`: Must include `warpgrep_codebase_search`

## Troubleshooting

### If searches are still including excluded files:
1. Restart your MCP server
2. Verify the environment variables are set correctly
3. Check that the exclusion patterns match your directory structure

### If searches are too slow:
1. Verify exclusions are working (check first few results)
2. Consider adding more specific patterns for your project
3. Use file-type specific configurations for targeted searches

## Customization

To add more exclusions:

1. Edit `WARPGREP_EXCLUDES` in `.vscode/mcp.json`
2. Update `PROJECT_EXCLUDES` in `config/warpgrep-config.js`
3. Restart the MCP server

To create domain-specific configurations:
```javascript
// Example: Backend-only search
function createBackendWarpGrep() {
  const backendExcludes = [
    ...PROJECT_EXCLUDES,
    "src/app/frontend/**",
    "src/app/admin/frontend/**",
    "resources/public/**"
  ];

  return createWarpGrepTool({
    repoRoot: '.',
    morphApiKey: process.env.MORPH_API_KEY,
    remoteCommands: {
      excludes: backendExcludes
    }
  });
}
```

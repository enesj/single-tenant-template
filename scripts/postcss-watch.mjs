#!/usr/bin/env node

import { spawn } from 'child_process';
import chokidar from 'chokidar';
import path from 'path';
import fs from 'fs';
import http from 'http';

const srcFile = 'src/app/frontend/ui/style.css';
const outFile = 'resources/public/assets/css/style.css';
const triggerFile = 'resources/public/assets/css/.build-complete';

let buildTimeout = null;
let isBuilding = false;
let shadowProcess = null;

// Debounced build function
function scheduleBuild() {
  if (buildTimeout) {
    clearTimeout(buildTimeout);
  }

  buildTimeout = setTimeout(() => {
    runBuild();
  }, 500);
}

function runBuild() {
  if (isBuilding) {
    scheduleBuild();
    return;
  }

  isBuilding = true;
  const startTime = Date.now();
  console.log(`Processing ${srcFile}...`);

  const postcss = spawn('npx', [
    'postcss',
    srcFile,
    '-o', outFile,
    '--verbose'
  ], {
    env: {
      ...process.env,
      NODE_ENV: 'development',
      TAILWIND_MODE: 'watch'
    },
    stdio: 'inherit'
  });

  postcss.on('close', (code) => {
    isBuilding = false;
    const duration = Date.now() - startTime;
    if (code === 0) {
      console.log(`Finished ${srcFile} in ${duration} ms`);

      console.log('✅ CSS is ready - you can refresh your browser now');
    } else {
      console.error(`PostCSS exited with code ${code}`);
    }
    console.log('Waiting for file changes...');
  });

  postcss.on('error', (err) => {
    isBuilding = false;
    console.error('Failed to start PostCSS:', err);
  });
}

console.log('Starting debounced CSS watcher...');
console.log('Debounce delay: 500ms');

// Initial build
runBuild();

// Watch patterns - watch directories and filter files in event handlers
const watchPatterns = [
  'src/app',  // Watch entire src directory
  'resources/public/index.html',  // Watch specific HTML files
  'resources/public/admin.html'
];

console.log('\nWatching patterns:');
watchPatterns.forEach(pattern => console.log(`  • ${pattern}`));

const watcher = chokidar.watch(watchPatterns, {
  ignored: [
    '**/node_modules/**',
    '**/.shadow-cljs/**',
    '**/target/**',
    '**/resources/public/js/**',
    '**/resources/public/assets/css/style.css'
  ],
  persistent: true,
  ignoreInitial: false,  // Changed to false to see initial files
  awaitWriteFinish: {
    stabilityThreshold: 100,
    pollInterval: 100
  },
  cwd: process.cwd()  // Ensure patterns are relative to project root
});

let changeCount = 0;

// Function to check if file should trigger CSS rebuild
function shouldTriggerBuild(filePath) {
  const ext = path.extname(filePath);
  const relevantExtensions = ['.cljs', '.cljc', '.html', '.css'];
  return relevantExtensions.includes(ext) || filePath.endsWith('index.html') || filePath.endsWith('admin.html');
}

watcher
  .on('change', filePath => {
    if (shouldTriggerBuild(filePath)) {
      changeCount++;
      console.log(`[${changeCount}] File changed: ${filePath}`);
      scheduleBuild();
    }
  })
  .on('add', filePath => {
    // Don't increment changeCount or trigger build for initial scan
    if (changeCount > 0 && shouldTriggerBuild(filePath)) {
      changeCount++;
      console.log(`[${changeCount}] File added: ${filePath}`);
      scheduleBuild();
    }
  })
  .on('unlink', filePath => {
    changeCount++;
    console.log(`[${changeCount}] File removed: ${filePath}`);
    scheduleBuild();
  })
  .on('error', error => console.error(`Watcher error: ${error}`))
  .on('ready', () => {
    const watched = watcher.getWatched();
    let fileCount = 0;

    if (watched && typeof watched === 'object') {
      for (const [dir, files] of Object.entries(watched)) {
        fileCount += files.length;
      }
    }

    console.log('\n=== CSS Watcher Ready ===');
    console.log(`Total files being watched: ${fileCount}`);
    console.log('Watching all ClojureScript files in src/**/*.{cljs,cljc}');
    console.log('\nDebouncing enabled: Multiple rapid changes will trigger only one rebuild');
    console.log('========================\n');
  });

// Handle graceful shutdown
process.on('SIGINT', () => {
  console.log('\nShutting down CSS watcher...');
  if (buildTimeout) {
    clearTimeout(buildTimeout);
  }
  // Clean up trigger file
  try {
    fs.unlinkSync(triggerFile);
  } catch (err) {
    // Ignore errors, file may not exist
  }
  watcher.close().then(() => process.exit(0));
});

process.on('SIGTERM', () => {
  console.log('\nShutting down CSS watcher...');
  if (buildTimeout) {
    clearTimeout(buildTimeout);
  }
  // Clean up trigger file
  try {
    fs.unlinkSync(triggerFile);
  } catch (err) {
    // Ignore errors, file may not exist
  }
  watcher.close().then(() => process.exit(0));
});

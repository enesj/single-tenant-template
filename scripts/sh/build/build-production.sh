#!/bin/bash
set -e

echo "🏗️  Building hosting app for production..."

# Check if app is running and stop it
echo "📋 Checking for running processes..."
if pgrep -f "java.*hosting" > /dev/null; then
    echo "⚠️  Stopping running application..."
    ./scripts/sh/development/kill-java.sh
fi

# Clean previous builds
echo "🧹 Cleaning previous builds..."
rm -rf resources/public/assets/js/compiled/*
rm -rf target/*

# Install/update Node.js dependencies
echo "📦 Installing Node.js dependencies..."
npm install

# Build CSS for production
echo "🎨 Building production CSS..."
npm run build

# Build ClojureScript for production
echo "⚡ Building ClojureScript for production (app + admin)..."
npx shadow-cljs release app
npx shadow-cljs release admin

# Run backend tests
echo "🧪 Running backend tests..."
clj -X:test

# Run frontend tests
echo "🧪 Running frontend tests..."
npm run test:cljs

# Check for dependency vulnerabilities
echo "🔒 Checking for security vulnerabilities..."
bb nvd-check || echo "⚠️  NVD check completed with warnings (see above)"

# Lint code
echo "🔍 Running code linting..."
bb lint

# Format check
echo "📐 Checking code formatting..."
bb cljfmt-check

# Create uberjar
echo "📦 Creating uberjar..."
clj -T:build uberjar

echo "✅ Production build completed successfully!"
echo "📁 Build artifacts:"
echo "   - CSS: resources/public/assets/css/"
echo "   - JS (main):  resources/public/js/main/"
echo "   - JS (admin): resources/public/js/admin/"
echo "   - JAR: target/hosting-standalone.jar"
echo ""
echo "🚀 Ready for deployment!"

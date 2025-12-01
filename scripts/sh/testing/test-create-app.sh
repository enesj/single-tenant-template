#!/bin/bash
set -e

# Test script for create-new-app functionality
# This creates a test app and then cleans it up

TEST_PROJECT_NAME="test-hosting-app"
TEST_DIR="/tmp/$TEST_PROJECT_NAME"

echo "🧪 Testing create-new-app script..."

# Clean up any existing test directory
if [ -d "$TEST_DIR" ]; then
    echo "🧹 Cleaning up existing test directory..."
    rm -rf "$TEST_DIR"
fi

# Run the create script
echo "🚀 Creating test project..."
bb create-new-app "$TEST_PROJECT_NAME" "/tmp"

# Verify the project was created
if [ -d "$TEST_DIR" ]; then
    echo "✅ Project directory created successfully"

    # Check essential files
    essential_files=(
        "deps.edn"
        "bb.edn"
        "shadow-cljs.edn"
        "package.json"
        "README.md"
        ".secrets.edn"
        "src"
        "config"
        "resources"
        "test"
        "scripts"
        "cli-tools"
    )

    echo "🔍 Checking essential files and directories..."
    all_good=true

    for file in "${essential_files[@]}"; do
        if [ -e "$TEST_DIR/$file" ]; then
            echo "  ✅ $file"
        else
            echo "  ❌ Missing: $file"
            all_good=false
        fi
    done

    if [ "$all_good" = true ]; then
        echo "✅ All essential files and directories present"

        # Check if git repo was initialized
        if [ -d "$TEST_DIR/.git" ]; then
            echo "✅ Git repository initialized"
        else
            echo "❌ Git repository not initialized"
            all_good=false
        fi

        # Check if customizations were applied
        echo "🔍 Checking project customizations..."

        # Check HTML title
        if grep -q "Test Hosting App" "$TEST_DIR/resources/public/index.html"; then
            echo "  ✅ HTML title updated"
        else
            echo "  ❌ HTML title not updated"
            all_good=false
        fi

        # Check package.json name
        if grep -q "\"test-hosting-app\"" "$TEST_DIR/package.json"; then
            echo "  ✅ package.json name updated"
        else
            echo "  ❌ package.json name not updated"
            all_good=false
        fi

        # Check database names in config
        if grep -q "test-hosting-app" "$TEST_DIR/config/base.edn"; then
            echo "  ✅ Database names in config updated"
        else
            echo "  ❌ Database names in config not updated"
            all_good=false
        fi

        # Check database init fallback
        if grep -q "\"test-hosting-app\"" "$TEST_DIR/src/app/backend/db/init.clj"; then
            echo "  ✅ Database init fallback updated"
        else
            echo "  ❌ Database init fallback not updated"
            all_good=false
        fi

        # Check .secrets.edn
        if grep -q "test-hosting-app" "$TEST_DIR/.secrets.edn"; then
            echo "  ✅ .secrets.edn database names updated"
        else
            echo "  ❌ .secrets.edn database names not updated"
            all_good=false
        fi

        if [ "$all_good" = true ]; then
            echo "🎉 create-new-app script test PASSED!"
        else
            echo "❌ create-new-app script test FAILED!"
        fi
    else
        echo "❌ create-new-app script test FAILED!"
    fi

    # Clean up test directory
    echo "🧹 Cleaning up test directory..."
    rm -rf "$TEST_DIR"
    echo "✅ Cleanup complete"

else
    echo "❌ Test project directory was not created"
    echo "❌ create-new-app script test FAILED!"
fi

echo "🏁 Test complete"

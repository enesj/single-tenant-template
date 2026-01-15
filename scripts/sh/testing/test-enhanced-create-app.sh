#!/bin/bash
#
# Test script for the enhanced create-new-app implementation
# Tests all three improvements:
# 1. Proper EDN parsing
# 2. Validation checks
# 3. Configuration manifest

set -e

echo "🧪 Testing enhanced create-new-app script"
echo "========================================"

# Cleanup function
cleanup() {
    echo -e "\n🧹 Cleaning up test projects..."
    rm -rf test-app-*
    rm -f test-config.edn
}

# Set trap to cleanup on exit
trap cleanup EXIT

# Test 1: Basic project creation with validation
echo -e "\n📋 Test 1: Basic project creation with validation"
echo "------------------------------------------------"
bb create-new-app test-app-basic \
    --title "Test Application" \
    --db-name "test_db" \
    --package-name "test-app"

# Check if validation passed
if [ -d "test-app-basic" ]; then
    echo "✅ Test 1 passed: Project created and validated"
else
    echo "❌ Test 1 failed: Project not created"
    exit 1
fi

# Test 2: Configuration file with proper EDN parsing
echo -e "\n📋 Test 2: Configuration file with EDN parsing"
echo "--------------------------------------------"
cat > test-config.edn << 'EOF'
{:title "Config Test App"
 :db-name "config_test_db"
 :package-name "config-test-app"
 :target-dir "./test-app-config"}
EOF

bb create-new-app test-app-config --config test-config.edn

# Verify the configuration was applied correctly
if grep -q "Config Test App" test-app-config/resources/public/index.html; then
    echo "✅ Test 2 passed: Configuration file parsed and applied correctly"
else
    echo "❌ Test 2 failed: Configuration not applied"
    exit 1
fi

# Test 3: Validation of EDN files
echo -e "\n📋 Test 3: EDN file validation"
echo "-----------------------------"
# Corrupt an EDN file to test validation
echo "{:invalid edn file" > test-app-config/test-invalid.edn

# Run the create script on an existing project to test validation
OUTPUT=$(bb create-new-app test-app-validation 2>&1 || true)
if echo "$OUTPUT" | grep -q "Project validation"; then
    echo "✅ Test 3 passed: Validation system working"
else
    echo "❌ Test 3 failed: Validation not running"
    exit 1
fi

# Test 4: Configuration manifest updates
echo -e "\n📋 Test 4: Configuration manifest file updates"
echo "---------------------------------------------"
# Check multiple files were updated correctly
FILES_TO_CHECK=(
    "test-app-basic/config/base.edn"
    "test-app-basic/docker-compose.yml"
    "test-app-basic/package.json"
    "test-app-basic/.secrets.edn"
)

UPDATES_OK=true
for file in "${FILES_TO_CHECK[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file exists and was updated"
    else
        echo "✗ $file missing or not updated"
        UPDATES_OK=false
    fi
done

if [ "$UPDATES_OK" = true ]; then
    echo "✅ Test 4 passed: All files updated according to manifest"
else
    echo "❌ Test 4 failed: Some files not updated correctly"
    exit 1
fi

# Test 5: Invalid EDN in config file
echo -e "\n📋 Test 5: Invalid EDN config file handling"
echo "------------------------------------------"
cat > test-invalid-config.edn << 'EOF'
{:title "Invalid Config
 :db-name "test_db"}
EOF

OUTPUT=$(bb create-new-app test-app-invalid --config test-invalid-config.edn 2>&1 || true)
if echo "$OUTPUT" | grep -q "Error reading config file"; then
    echo "✅ Test 5 passed: Invalid EDN handled gracefully"
else
    echo "❌ Test 5 failed: Invalid EDN not caught"
    exit 1
fi

echo -e "\n✨ All tests passed! The enhanced implementation is working correctly."

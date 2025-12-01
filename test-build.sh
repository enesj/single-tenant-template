#!/bin/bash
cd /Users/enes/Projects/hosting
echo "Building CSS with Tailwind v4 and DaisyUI v5..."
mkdir -p resources/public/assets/css
npx postcss src/app/frontend/ui/style.css -o resources/public/assets/css/style.css
echo "Done!"
echo ""
echo "Checking if DaisyUI classes are present in output..."
if grep -q "ds-btn" resources/public/assets/css/style.css 2>/dev/null; then
    echo "✅ DaisyUI classes found! (ds-btn present)"
else
    echo "❌ DaisyUI classes not found in output"
fi
echo ""
echo "CSS file size:"
ls -lh resources/public/assets/css/style.css 2>/dev/null || echo "File not created"

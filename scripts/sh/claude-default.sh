#!/bin/bash

# Run Claude Code with default settings
# Equivalent to: bb set-model (without any arguments)

set -e

# Default settings from bb.edn set-model task
export ANTHROPIC_BASE_URL="https://api.z.ai/api/anthropic"
export ANTHROPIC_DEFAULT_OPUS_MODEL="glm-4.6"
export ANTHROPIC_DEFAULT_SONNET_MODEL="glm-4.6"
export ANTHROPIC_DEFAULT_HAIKU_MODEL="glm-4.6"
export ANTHROPIC_AUTH_TOKEN="6189b03087e74486bff8234fcab6d678.JOWIHK3u9TANnRhO"

echo "🔧 Starting Claude Code with default settings (glm-4.6)..."
echo "Environment variables exported successfully"

# Run Claude Code with any additional arguments passed to this script
exec claude "$@"

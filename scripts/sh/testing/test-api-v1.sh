#!/bin/bash
# Quick test script to verify API versioning is working

# Get port from configuration (default to 8085 for development)
PORT=$(clojure -M -e "(require '[aero.core :as aero]) (-> (aero/read-config (java.io.File. \"config/base.edn\") {:profile :dev}) :webserver :port) (println)" 2>/dev/null | tail -1)
if [ -z "$PORT" ] || [ "$PORT" = "nil" ]; then
    PORT=8085
fi

echo "Testing API v1 endpoints on port $PORT..."
echo ""

# Test health endpoint
echo "1. Testing health endpoint:"
curl -s -i http://localhost:$PORT/api/v1/health | head -n 15
echo ""

# Test config endpoint
echo "2. Testing config endpoint:"
curl -s http://localhost:$PORT/api/v1/config | jq '.' 2>/dev/null || curl -s http://localhost:$PORT/api/v1/config
echo ""

# Test entities endpoint
echo "3. Testing items endpoint:"
curl -s http://localhost:$PORT/api/v1/entities/items | jq '.' 2>/dev/null || curl -s http://localhost:$PORT/api/v1/entities/items | head -c 200
echo ""

echo "If you see 404 errors above, please restart your backend server!"

#!/usr/bin/env bash

echo "Waiting for Keycloak to be ready..."
for i in {1..30}; do
    if curl -sSf http://localhost:8080/ > /dev/null; then
        echo "localhost:8080 is ready!"
        break
    fi
    echo "Waiting... ($i)"
    sleep 1
done

#!/usr/bin/env bash

cd extension
mkdir -p ./.internal/data/
chmod 777 ./.internal/data/
./.internal/scripts/import.sh

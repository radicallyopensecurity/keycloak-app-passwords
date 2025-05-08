#!/usr/bin/env bash

set -e

cz bump --get-next --yes 2>/dev/null || exit 0

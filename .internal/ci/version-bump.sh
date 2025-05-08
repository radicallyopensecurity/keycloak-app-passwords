#!/usr/bin/env bash

NEXT_VERSION=$1

(cd theme && npm version "$NEXT_VERSION")
(cd extension && mvn versions:set -DnewVersion="$NEXT_VERSION" -DgenerateBackupPoms=false)
cz bump --yes

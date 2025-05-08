#!/usr/bin/env bash

(cd extension; mvn clean install)
(cd theme; npm install)

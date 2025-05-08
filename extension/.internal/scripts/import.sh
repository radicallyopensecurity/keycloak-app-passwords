#!/usr/bin/env bash

docker compose run --rm \
  --entrypoint "sh -c ' \
    rm -rf /opt/keycloak/data/{h2,transaction-logs} && \
    /opt/keycloak/bin/kc.sh start --import-realm'" \
  keycloak

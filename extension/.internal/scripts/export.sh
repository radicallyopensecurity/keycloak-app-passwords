#!/usr/bin/env bash

docker compose run --rm \
  keycloak \
  export --dir /opt/keycloak/data --realm myrealm --users realm_file

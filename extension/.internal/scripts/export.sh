#!/usr/bin/env bash

docker compose up -d openldap

docker compose run --rm \
  keycloak \
  export --dir /opt/keycloak/data --realm myrealm --users realm_file

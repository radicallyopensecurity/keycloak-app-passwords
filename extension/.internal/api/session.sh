#!/usr/bin/env bash

# Login to keycloak realm via web browser, at your own url or http://localhost:8080
# Fill in these variables
#export IDENTITY=""
#export SESSION=""

# List app passwords
curl -X GET http://localhost:8080/realms/myrealm/app-password \
  -H "Cookie: KEYCLOAK_IDENTITY=$IDENTITY; KEYCLOAK_SESSION=$SESSION"
#[
#  {
#    "attribute": "emailPassword",
#    "created": "2025-05-03T13:13:31Z"
#  },
#  {
#    "attribute": "smsPassword",
#    "created": null
#  }
#]

# Generate new app password
curl -X POST http://localhost:8080/realms/myrealm/app-password \
  -H "Content-Type: application/json" \
  -H "Cookie: KEYCLOAK_IDENTITY=$IDENTITY; KEYCLOAK_SESSION=$SESSION" \
  -d '{"name":"appPasswordEmail"}'
#{
#  "name": "emailPassword",
#  "password": "]0EB81l2Ak3%+[+xr&=IwLF%Krh&ZsQL",
#  "created": "2025-05-07T11:26:18.826883894Z"
#}

# Delete app password
curl -X DELETE http://localhost:8080/realms/myrealm/app-password \
  -H "Content-Type: application/json" \
  -H "Cookie: KEYCLOAK_IDENTITY=$IDENTITY; KEYCLOAK_SESSION=$SESSION" \
  -d '{"name": "emailPassword"}'
# nocontent

# Check app password
curl -X POST http://localhost:8080/realms/myrealm/app-password/check \
  -H "Content-Type: application/json" \
  -H "Cookie: KEYCLOAK_IDENTITY=$IDENTITY; KEYCLOAK_SESSION=$SESSION" \
  -d '{"name": "emailPassword", "password": "t+pz4!gina%[jeXTCVkX(sdf(AP8t"}'
# {"success":false}
# or
# {"success":true}

# Check app passwords is enabled
curl -X GET http://localhost:8080/realms/myrealm/app-password/enabled \
  -H "Cookie: KEYCLOAK_IDENTITY=$IDENTITY; KEYCLOAK_SESSION=$SESSION"
# {"enabled":false}
# or
# {"enabled":true}

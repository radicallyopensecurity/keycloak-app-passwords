#!/usr/bin/env bash

# Get Token
# Note: the token is short lived and may need to be re-requested
export TOKEN=$(curl -s \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  http://localhost:8080/realms/myrealm/protocol/openid-connect/token \
| jq -r .access_token)

# List app passwords
curl -X GET -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/realms/myrealm/app-password
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
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "emailPassword"}'
#{
#  "name": "emailPassword",
#  "password": "]0EB81l2Ak3%+[+xr&=IwLF%Krh&ZsQL",
#  "created": "2025-05-07T11:26:18.826883894Z"
#}

# Delete app password
curl -X DELETE http://localhost:8080/realms/myrealm/app-password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "emailPassword"}'
# nocontent

# Check app password
curl -X POST http://localhost:8080/realms/myrealm/app-password/check \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "emailPassword", "password": "t+pz4!gina%[jeXTCVkX(sdf(AP8t"}'
# {"success":false}
# or
# {"success":true}

# Check app passwords is enabled
curl -X GET http://localhost:8080/realms/myrealm/app-password/enabled \
  -H "Authorization: Bearer $TOKEN"
# {"enabled":false}
# or
# {"enabled":true}

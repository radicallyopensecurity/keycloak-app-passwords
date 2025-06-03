#!/usr/bin/env bash

# For some reason when importing the realm in CI it doesn't import the admin user for myrealm
# I spent too much time trying to fix it

export TOKEN=$(curl -s \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  http://localhost:8080/realms/master/protocol/openid-connect/token \
| jq -r .access_token)

curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "firstName": "admin",
    "lastName": "admin",
    "email": "admin@example.org",
    "emailVerified": true,
    "enabled": true
  }' \
  "http://localhost:8080/admin/realms/myrealm/users"

USER_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/myrealm/users?username=admin" \
  | jq -r '.[0].id')

CLIENT_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/myrealm/clients?clientId=realm-management" \
  | jq -r '.[0].id')

ROLE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/myrealm/clients/$CLIENT_ID/roles/realm-admin" \
  | jq -r '{id: .id, name: .name}')

curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "[$ROLE]" \
  "http://localhost:8080/admin/realms/myrealm/users/$USER_ID/role-mappings/clients/$CLIENT_ID"

curl -s -X PUT -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"password","value":"admin","temporary":false}' \
  "http://localhost:8080/admin/realms/myrealm/users/$USER_ID/reset-password"

GROUP_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/myrealm/groups?search=staff" \
  | jq -r '.[0].id')

curl -s -X PUT -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/myrealm/users/$USER_ID/groups/$GROUP_ID"

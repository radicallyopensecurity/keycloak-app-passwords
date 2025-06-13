#!/usr/bin/env bash

cd extension

docker compose up -d openldap

echo "Waiting for LDAP to be ready..."
for i in {1..30}; do
    if docker exec openldap ldapsearch -x -H ldap://localhost:1389 -b "dc=example,dc=org" '(objectClass=*)' dn >/dev/null 2>&1; then
        echo "LDAP is ready!"
        break
    fi
    echo "Waiting for LDAP... ($i)"
    sleep 2
done

docker exec openldap sh -c 'cd /tmp/ldapscripts && sh add-schema.sh'

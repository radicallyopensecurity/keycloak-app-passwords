#!/usr/bin/env bash

# Add OpenLDAP schema to ldap server
ldapadd -Y EXTERNAL -H ldapi:/// -f email.ldif

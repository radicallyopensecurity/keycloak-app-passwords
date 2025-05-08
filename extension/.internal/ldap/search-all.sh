#!/usr/bin/env bash

# Query all objects
ldapsearch -Y EXTERNAL -H ldapi:/// -b dc=example,dc=org \* +

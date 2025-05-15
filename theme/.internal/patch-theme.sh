#!/usr/bin/env bash

# https://github.com/keycloakify/keycloakify/issues/848

JAR_FILE="./dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar"

TMP_DIR=$(mktemp -d)
TMP_FTL_FILE="$TMP_DIR/index.ftl"

unzip -p "$JAR_FILE" theme/keycloak-app-passwords/account/index.ftl > "$TMP_FTL_FILE"

awk '
  /<\/head>/ {
    print "        <#if properties.styles?has_content>";
    print "            <#list properties.styles?split(\" \") as style>";
    print "                <link rel=\"stylesheet\" href=\"${resourceUrl}/${style}\">";
    print "            </#list>";
    print "        </#if>";
  }
  { print }
' "$TMP_FTL_FILE" > "$TMP_FTL_FILE.modified" && mv "$TMP_FTL_FILE.modified" "$TMP_FTL_FILE"

ORIGINAL_DIR="$(pwd)"

mkdir -p "$TMP_DIR/theme/keycloak-app-passwords/account"
mv "$TMP_FTL_FILE" "$TMP_DIR/theme/keycloak-app-passwords/account/index.ftl"

cd "$TMP_DIR"
zip -ur "$ORIGINAL_DIR/$JAR_FILE" theme
cd "$ORIGINAL_DIR"

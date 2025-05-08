.PHONY: dev export import build

make:
	mvn clean package

build-dev:
	mvn clean package -Pdev

dev:
	THEME_PATH=$$(realpath ../theme/dist_keycloak/theme/keycloak-app-passwords) docker compose up

test:
	mvn test

import:
	./.internal/scripts/import.sh

export:
	./.internal/scripts/export.sh
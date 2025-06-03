.PHONY: dev export import build dev import export test

make:
	mvn clean package

build-dev:
	mvn clean package -Pdev

dev:
	THEME_PATH=$$(realpath ../theme/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar) \
	docker compose up

test:
	mvn test

import:
	THEME_PATH=$$(realpath ../theme/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar) \
	./.internal/scripts/import.sh

export:
	THEME_PATH=$$(realpath ../theme/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar) \
	./.internal/scripts/export.sh

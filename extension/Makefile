.PHONY: dev export import build dev import export test

make:
	mvn clean package

init-data:
	mkdir -p .internal/data/keycloak
	mkdir -p .internal/data/openldap
	chmod 777 .internal/data/openldap -R
	chmod 777 .internal/data/keycloak -R

build-dev:
	mvn clean package -Pdev

dev:
	THEME_PATH=$$(realpath ../theme/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar) \
	docker compose up

test:
	mvn clean verify

test-unit:
	mvn test

test-integration:
	mvn test-compile failsafe:integration-test failsafe:verify

import:
	THEME_PATH=$$(realpath ../theme/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar) \
	./.internal/scripts/import.sh

export:
	THEME_PATH=$$(realpath ../theme/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar) \
	./.internal/scripts/export.sh

check-deps:
	mvn versions:display-dependency-updates -DprocessDependencyManagement=false -DshowVersionless=false

upgrade-deps:
	mvn versions:use-latest-versions -DgenerateBackupPoms=false

check-plugins:
	mvn versions:display-plugin-updates

.PHONY: all install build-dev build-prod build-extension-prod build-theme-prod

all: install build-prod

install:
	./.internal/install.sh

build-dev:
	./.internal/dev/build-extension.sh
	./.internal/dev/build-theme.sh

build-prod: build-extension-prod build-theme-prod

build-extension-prod:
	./.internal/prod/build-extension.sh

build-theme-prod:
	./.internal/prod/build-theme.sh

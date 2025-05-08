.PHONY: all install build-dev build

all: install build

install:
	./.internal/install.sh

build-dev:
	./.internal/build-dev-extension.sh
	./.internal/build-dev-theme.sh

build:
	./.internal/build-extension.sh
	./.internal/build-theme.sh

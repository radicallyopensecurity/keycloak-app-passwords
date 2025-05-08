# keycloak-app-passwords

Enable generated app passwords in `keycloak`.

[![Release](https://github.com/radicallyopensecurity/keycloak-app-passwords/actions/workflows/push-main.yml/badge.svg)](https://github.com/radicallyopensecurity/keycloak-app-passwords/actions/workflows/push-main.yml)

## Dependencies

- Java 17
- Maven
- NodeJS >= 18
- NPM
- Make
- Docker

## Description

This extension comes in 2 packages:

- [Java Extension](./extension/README.md): A `keycloak` extension that adds API routes to manage app passwords.
- [Keycloakify Theme](./theme/README.md): A `keycloakify` based theme that allows users to manage their app passwords.

## Usage

For running the packages, see the `README` docs for each package.

To run the extension and theme together, you can use or base your deployment off the [`docker-compose`](./extension/docker-compose.yml) file in the `extension` folder.

> WARNING: The `docker-compose` file is extremely insecure and shoud only be used in development.

The important parts in the `docker-compose` file are the volume mounts:

```yml
volumes:
  - ./extension/target/keycloak-app-passwords-DEV.jar:/opt/keycloak/providers/keycloak-app-passwords-DEV.jar
  - ./extension/target/keycloak-app-passwords.config.json:/opt/keycloak/providers/keycloak-app-passwords.config.json
  - ./theme/dist/theme/keycloak-app-passwords:/opt/keycloak/themes/keycloak-app-passwords
```

Placing the built extension, its config and the theme in the `/opt/keycloak/{providers,themes}` will load the extension and the theme. After starting `keycloak`, you should then set the `theme` in `realm settings` to `keycloak-app-passwords`.

You can also place the theme `jar` file in `/opt/keycloak/providers` to make the theme available in `keycloak`.

If you're already using a custom theme, then the only way to add the theme extensions from this repo. Is to copy and paste the files from the [`theme`](./theme/) folder into your own theme.

`Keycloak` and `keycloakify` do not offer methods to make this any easier. Anything clever to track this repo and merge with your custom theme will have to come from your side.

## Development

See [`theme`](./theme/README.md) on how to run the theme in development mode.

See [`extension`](./extension/README.md) on how to run the extension in development mode.

In both setups:

- The theme and extension are loaded into `keycloak`.
- You can attach a debugger to the keycloak extension on port `8787`.

It's most convenient to work in the specific package that you're working on.

In the `extension` setup you won't have live reload when edditing the theme. In the `theme` setup, you'll have slower rebuilds when you change something in the extension.

To test the `OpenLDAP` integration you'll have to use the `extension` package.

## Build

```sh
# build for prod
make

# install dependencies for dev
make install
# build for dev
make build-dev
```

## License

[MIT](./LICENSE.md)

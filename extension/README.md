# keycloak-app-passwords-extension

This extension adds a REST API to `Keycloak` that allows application-specific passwords to be stored as user attributes in a secure way.

## Dependencies

- Java 17
- Maven
- Make
- Docker

## Description

This extension adds 4 new routes to each realm:

- `GET /realms/myrealm/app-passwords`: Get a list of all app passwords that the user has
- `POST /realms/myrealm/app-passwords`: Generate a new secure app password
- `DELETE /realms/myrealm/app-passwords`: Delete an app password
- `POST /realms/myrealm/app-passwords/check`: Check if a password matches the value stored in the user attribute.
- `GET /realms/myrealm/app-passwords/enabled`: Check if app passwords feature is enabled for this user.

The [accompanying theme](../theme) allows users to manage their own app password from the `Account UI`.

## Usage

> tl;dr see [`docker-compose.yml`](./docker-compose.yml)

Build:

```sh
make
```

Place the built `jar` file in `/opt/keycloak/providers` or your custom path. The extension will now load on start.

Optionally copy [`keycloak-app-passwords.config.json`](keycloak-app-passwords.config.json) and modify the settings.

You can place the `keycloak-app-passwords.config.json` either in `/opt/keycloak/providers` or use the environment variable `KC_EXT_APP_PASSWORDS_CONFIG` to set the path somewhere else.

> Note: Modifying the Keycloak data model is unsupported according to the [Server Development Guide](https://www.keycloak.org/docs/latest/server_development/#_extensions_jpa). Therefore, we've opted to use a `json` config file.

You can now call the REST API using either `bearer` or `session` authentication:

See the scripts for [`bearer`](.internal/api/bearer.sh) and [`session`](.internal/api/session.sh) for how to call the API.

By default, app passwords is enabled for all users. You can control this by setting the `groups` key in the `config.json`:

```text
"groups": null                 // default - enabled for all
"groups": []                   // disabled for all
"groups": ["group1", "group2"] // enabled for users in group1 or group2   
```

### LDAP

This extension was built for [radicallyopensecurity](https://radicallyopensecurity.com) to allow `dovecot` and `postfix` to authenticate using a password separate from the `Keycloak` credentials.

To use LDAP some configuration is required.

In this repository we use `OpenLDAP`, first we add the attribute we want to use to LDAP, `emailUser`, by adding a schema fragment.

This file is located in [`email.ldif`](.internal/ldap/email.ldif). To add it to `OpenLDAP`, see [`add-schema.sh`](.internal/ldap/add-schema.sh).

After configuring `LDAP`, we configure `Keycloak` by going to `Realm settings->User federation` and add an `LDAP` provider. In the development version in this repo, this is already configured.

In this repo we use the following configuration in `Keycloak`, modify these as needed:

```
Connection URL: ldap://openldap:1389
Bind DN: cn=admin,dc=example,dc=org
Bind Credential: admin
Edit mode: Writable
Users DN: ou=users,dc=example,dc=org
User object classes: inetOrgPerson, organizationalPerson,emailUser
```

Now go to `mappers` under `Realm settings->User federation->ldap` and create a mapper for your attribute. For the `emailPassword` we create a mapper with the following configuration:

```
Name: emailPasswordMapper
Mapper type: user-attribute-ldap-mapper
User Model Attribute: emailPassword
LDAP Attribute: emailPassword
```

Now any new users will get added to `OpenLDAP` and any app passwords that are generated will be stored in `LDAP`.

See [`search-all.sh`](.internal/ldap/search-all.sh) to see how to validate that your app passwords are stored in `LDAP`.

## Development

Build the package for development:

```shell
make build-dev
```

First time starting, you can use the import script to import realm settings needed for initial development.

Initialize LDAP:

```sh
docker compose up openldap
docker exec -it openldap /bin/sh
sh /tmp/ldapscripts/add-schema.sh
```

```sh
# make sure openldap is running
docker compose up openldap 

make import
# or
./.internal/scripts/import.sh
```

After that you can run the app via `Docker`:

```sh
make dev
```

If you make changes to the realm and want to keep this for future runs, or to commit them, use the export script:

```sh
# make sure keycloak is not running
docker compose down keycloak
# and make sure openldap is running
docker compose up openldap 

make export
# or
./.internal/scripts/export.sh
```

An `Intellij` debugger configuration is available called `attach to docker`.

## Test

```sh
make test
```

## Known Issues

### Warning during startup

During startup, you'll see this warning:

```
WARN  [org.keycloak.services] (build-19) KC-SERVICES0047: app-password (org.radicallyopensecurity.keycloak.app_passwords.AppPasswordRestResourceProviderFactory) is implementing the internal SPI realm-restapi-extension. This SPI is internal and may change without notice
```

This [GitHub issue](https://github.com/keycloak/keycloak/issues/11114) suggests, while realm-restapi-extension SPI is internal, it's unlikely to change. And hasn't changed at least in the last 3 years.

## Resources Used

- https://www.keycloak.org/docs/latest/server_development/index.html#_providers
- https://github.com/keycloak/keycloak/
- https://github.com/cloudtrust/keycloak-rest-api-extensions/
- https://github.com/sventorben/keycloak-session-model-jpa/
- https://github.com/thomasdarimont/keycloak-extension-playground

## License

[MIT](./LICENSE.md)

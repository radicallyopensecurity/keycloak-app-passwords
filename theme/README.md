# keycloak-app-passwords-theme

This theme allows interaction with the `keycloak-app-passwords` extension through the account ui.

## Dependencies

-   NodeJS >= 18
-   Maven
-   Docker

## Description

This theme adds a new menu item to the account ui sidebar called `App Passwords`. This page interacts with the custom REST API exposed by the `keycloak-app-passwords` extension. It enables users to (re)generate and delete app passwords.

## Usage

Build:

```sh
npm run build-keycloak-theme
```

This will output 2 `.jar` files in `dist_keycloak`. Place the correct one for your version of `keycloak` in `/opt/keycloak/providers`.

After restarting your `keycloak` server, you'll see the `keycloak-app-passwords` theme in `Realm settings->theme`

## Development

```sh
npm run start
```

## License

[MIT](../LICENSE.md)

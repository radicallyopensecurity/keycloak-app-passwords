/**
 * WARNING: Before modifying this file, run the following command:
 *
 * $ npx keycloakify own --path "shared/keycloak-ui-shared/utils/darkMode.ts"
 *
 * This file is provided by @keycloakify/keycloak-ui-shared version 260200.0.0.
 * It was copied into your repository by the postinstall script: `keycloakify sync-extensions`.
 */

/* eslint-disable */

// @ts-nocheck

const DARK_MODE_CLASS = "pf-v5-theme-dark";
const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

function updateDarkMode(isEnabled: boolean) {
    const { classList } = document.documentElement;

    if (isEnabled) {
        classList.add(DARK_MODE_CLASS);
    } else {
        classList.remove(DARK_MODE_CLASS);
    }
}

export function initializeDarkMode() {
    updateDarkMode(mediaQuery.matches);
    mediaQuery.addEventListener("change", event => updateDarkMode(event.matches));
}

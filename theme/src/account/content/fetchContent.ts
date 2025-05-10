/**
 * This file has been claimed for ownership from @keycloakify/keycloak-account-ui version 260200.0.0.
 * To relinquish ownership and restore this file to its original content, run the following command:
 *
 * $ npx keycloakify own --path "account/content/fetchContent.ts" --revert
 */

/* eslint-disable */

// @ts-nocheck

import type { CallOptions } from "../api/methods";
import { request } from "../api/request";
import { appPasswordClient } from "../app-passwords/app-password-client";
import type { MenuItem } from "../root/PageNav";

export default async function fetchContentJson(opts: CallOptions): Promise<MenuItem[]> {
    const [{ content }, appPasswordEnabled] = await Promise.all([
        import("../assets/content"),
        appPasswordClient.isEnabled(opts.signal, opts.context)
    ]);

    let result = content;

    if (appPasswordEnabled.enabled) {
        result = content.concat([
            {
                label: "appPasswords",
                path: "app-passwords"
            }
        ]);
    }

    return result;
}

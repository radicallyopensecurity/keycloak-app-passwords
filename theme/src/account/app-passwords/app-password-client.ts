import { BaseEnvironment, KeycloakContext } from "../../shared/keycloak-ui-shared";
import { parseResponse } from "../api/parse-response";
import { request } from "../api/request";
import { joinPath } from "../utils/joinPath";

const listAppPasswords = async (
    signal: AbortSignal,
    context: KeycloakContext<BaseEnvironment>
) => {
    const path = `/realms/${context.environment.realm}/app-password`;
    const url = new URL(joinPath(context.environment.serverBaseUrl, path));

    const response = await request(path, context, { signal }, new URL(url));

    return parseResponse<AppPasswordListRepresentation[]>(response);
};

const generateAppPassword = async (
    context: KeycloakContext<BaseEnvironment>,
    appPasswordName: string
) => {
    const path = `/realms/${context.environment.realm}/app-password`;
    const url = new URL(joinPath(context.environment.serverBaseUrl, path));
    const body = {
        name: appPasswordName
    };

    const response = await request(
        path,
        context,
        {
            method: "POST",
            body
        },
        url
    );

    return parseResponse<AppPasswordGenerateRepresentation>(response);
};

const deleteAppPassword = async (
    context: KeycloakContext<BaseEnvironment>,
    appPasswordName: string
) => {
    const path = `/realms/${context.environment.realm}/app-password`;
    const url = new URL(joinPath(context.environment.serverBaseUrl, path));
    const body = {
        name: appPasswordName
    };

    await request(
        path,
        context,
        {
            method: "DELETE",
            body
        },
        url
    );

    return;
};

const isEnabled = async (
    signal: AbortSignal,
    context: KeycloakContext<BaseEnvironment>
) => {
    const path = `/realms/${context.environment.realm}/app-password/enabled`;
    const url = new URL(joinPath(context.environment.serverBaseUrl, path));

    const response = await request(path, context, { signal }, new URL(url));

    return parseResponse<{ enabled: boolean }>(response);
};

export const appPasswordClient = {
    list: listAppPasswords,
    generate: generateAppPassword,
    delete: deleteAppPassword,
    isEnabled
};

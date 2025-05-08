import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { keycloakify } from "keycloakify/vite-plugin";
import { resolve } from "node:path";

// https://vitejs.dev/config/
export default defineConfig(() => {
    const targetPath = resolve(__dirname, "..", "extension", "target");

    const jarFile = "keycloak-app-passwords-DEV.jar";
    const jarPath = resolve(targetPath, jarFile);

    const configFile = "keycloak-app-passwords.config.json";
    const configPath = resolve(targetPath, configFile);

    const javaOpts =
        "JAVA_OPTS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8787'";

    return {
        plugins: [
            react(),
            keycloakify({
                accountThemeImplementation: "Single-Page",
                startKeycloakOptions: {
                    dockerExtraArgs: [
                        "-p",
                        "8787:8787",
                        "-v",
                        `${jarPath}:/opt/keycloak/providers/${jarFile}`,
                        "-v",
                        `${configPath}:/opt/keycloak/providers/${configFile}`,
                        "-e",
                        javaOpts
                        // "-e",
                        // "KC_LOG_LEVEL=debug"
                    ]
                }
            })
        ]
    };
});

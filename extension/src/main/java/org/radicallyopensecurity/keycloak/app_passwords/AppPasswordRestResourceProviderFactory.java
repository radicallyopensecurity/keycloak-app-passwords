package org.radicallyopensecurity.keycloak.app_passwords;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfig;

import java.util.Optional;

public final class AppPasswordRestResourceProviderFactory implements RealmResourceProviderFactory {
    static final String ID = "app-password";
    private AppPasswordConfig config;

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new AppPasswordRestResourceProvider(session, config);
    }

    @Override
    public void init(Config.Scope scope) {
        String configPath = Optional.ofNullable(System.getenv("KC_EXT_APP_PASSWORDS_CONFIG"))
                .filter(path -> !path.isBlank())
                .orElse("/opt/keycloak/providers/keycloak-app-passwords.config.json");

        this.config = AppPasswordUtils.createConfig(configPath);
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }
}

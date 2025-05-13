package org.radicallyopensecurity.keycloak.app_passwords;

import com.password4j.Argon2Function;
import com.password4j.types.Argon2;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfig;

public class AppPasswordRestResourceProviderFactory implements RealmResourceProviderFactory {
    static final String ID = "app-password";
    private AppPasswordConfig config;

    @Override
    public RealmResourceProvider create(KeycloakSession keycloakSession) {
        return new AppPasswordRestResourceProvider(keycloakSession, config);
    }

    @Override
    public void init(Config.Scope scope) {
        String configPath = System.getenv("KC_EXT_APP_PASSWORDS_CONFIG");
        if (configPath == null || configPath.isEmpty()) {
            configPath = "/opt/keycloak/providers/keycloak-app-passwords.config.json";
        }
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

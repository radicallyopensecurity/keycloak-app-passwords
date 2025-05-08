package org.radicallyopensecurity.keycloak.app_passwords;

import com.password4j.Argon2Function;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfig;

public class AppPasswordRestResourceProvider implements RealmResourceProvider {
    private final KeycloakSession session;
    private final AppPasswordConfig config;

    public AppPasswordRestResourceProvider(
            KeycloakSession keycloakSession,
            AppPasswordConfig config
    ) {
        this.session = keycloakSession;
        this.config = config;
    }

    @Override
    public Object getResource() {
        return new AppPasswordRestResource(session, config);
    }

    @Override
    public void close() {
    }
}

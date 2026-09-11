package org.radicallyopensecurity.keycloak.app_passwords;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfig;

public final class AppPasswordRestResourceProvider implements RealmResourceProvider {
    private final KeycloakSession session;
    private final AppPasswordConfig config;

    public AppPasswordRestResourceProvider(
            KeycloakSession session,
            AppPasswordConfig config
    ) {
        this.session = session;
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

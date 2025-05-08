package org.radicallyopensecurity.keycloak.app_passwords.config;

import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class AppPasswordConfig {
    public List<AppPasswordConfigAttribute> attributes;
    public Integer length;

    public static AppPasswordConfig withDefaults() {
        AppPasswordConfig config = new AppPasswordConfig();
        config.attributes = List.of(
                new AppPasswordConfigAttribute("emailPassword", "emailPasswordCreated")
        );
        config.length = 32;
        return config;
    }
}

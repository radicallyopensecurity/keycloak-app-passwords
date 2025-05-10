package org.radicallyopensecurity.keycloak.app_passwords.dtos;

public class AppPasswordEnabledResponseDto {
    public boolean enabled;

    public AppPasswordEnabledResponseDto(boolean enabled) {
        this.enabled = enabled;
    }
}

package org.radicallyopensecurity.keycloak.app_passwords.dtos;

import jakarta.validation.constraints.NotBlank;

public class AppPasswordRequestDto {
    @NotBlank
    public String name;
}
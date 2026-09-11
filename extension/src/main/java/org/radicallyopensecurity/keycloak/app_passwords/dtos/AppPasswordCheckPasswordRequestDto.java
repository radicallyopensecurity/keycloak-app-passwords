package org.radicallyopensecurity.keycloak.app_passwords.dtos;

import jakarta.validation.constraints.NotBlank;

public class AppPasswordCheckPasswordRequestDto {
    @NotBlank
    public String name;

    @NotBlank
    public String password;
}

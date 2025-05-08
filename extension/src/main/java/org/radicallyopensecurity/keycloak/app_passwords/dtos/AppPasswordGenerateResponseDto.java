package org.radicallyopensecurity.keycloak.app_passwords.dtos;

public class AppPasswordGenerateResponseDto {
    public String name;
    public String password;
    public String created;

    public AppPasswordGenerateResponseDto(String name, String password, String created) {
        this.name = name;
        this.password = password;
        this.created = created;
    }
}

package org.radicallyopensecurity.keycloak.app_passwords.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS) // make sure nulls are output in the json
public class AppPasswordListResponseDto {
    public String name;
    public String created;

    public AppPasswordListResponseDto(String attribute, String created) {
        this.name = attribute;
        this.created = created;
    }
}

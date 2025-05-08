package org.radicallyopensecurity.keycloak.app_passwords.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AppPasswordConfigAttribute {
    public String password;
    public String created;

    public AppPasswordConfigAttribute(
        @JsonProperty("password") String password,
        @JsonProperty("created") String created
    ) {
        this.password = password;
        this.created = created;
    }
}

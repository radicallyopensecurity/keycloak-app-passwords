package org.radicallyopensecurity.keycloak.app_passwords;

import jakarta.ws.rs.BadRequestException;
import org.radicallyopensecurity.keycloak.app_passwords.dtos.AppPasswordCheckPasswordRequestDto;
import org.radicallyopensecurity.keycloak.app_passwords.dtos.AppPasswordRequestDto;

final class AppPasswordValidator {

    private AppPasswordValidator() {
    }

    static void validate(AppPasswordRequestDto request) {
        if (request == null || isBlank(request.name)) {
            throw new BadRequestException();
        }
    }

    static void validate(AppPasswordCheckPasswordRequestDto request) {
        if (request == null
                || isBlank(request.name)
                || isBlank(request.password)) {
            throw new BadRequestException();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

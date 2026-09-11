package org.radicallyopensecurity.keycloak.app_passwords;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.events.EventBuilder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfig;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfigAttribute;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AppPasswordUtilsTest {

    @Test
    void createConfigLoadOverrides() throws IOException {
        Path tempConfig = Files.createTempFile("app-password-test", ".json");

        try {
            String configJson = """
                    {
                      "attributes": [
                        {
                          "password": "somePassword",
                          "created": "somePasswordCreatedAt"
                        }
                      ],
                      "length": 24
                    }
                    """;

            Files.writeString(tempConfig, configJson);

            AppPasswordConfig config =
                    AppPasswordUtils.createConfig(tempConfig.toString());

            assertEquals(24, config.length);
            assertEquals(1, config.attributes.size());
            assertEquals("somePassword", config.attributes.getFirst().password);
            assertEquals("somePasswordCreatedAt", config.attributes.getFirst().created);
        } finally {
            Files.deleteIfExists(tempConfig);
        }
    }

    @Test
    void createConfigLoadPartialOverrides() throws IOException {
        Path tempConfig = Files.createTempFile("app-password-test", ".json");

        try {
            String configJson = """
                    {
                      "attributes": [
                        {
                          "password": "somePassword",
                          "created": "somePasswordCreatedAt"
                        }
                      ]
                    }
                    """;

            Files.writeString(tempConfig, configJson);

            AppPasswordConfig config =
                    AppPasswordUtils.createConfig(tempConfig.toString());

            assertEquals(32, config.length);
            assertEquals(1, config.attributes.size());
            assertEquals("somePassword", config.attributes.getFirst().password);
            assertEquals("somePasswordCreatedAt", config.attributes.getFirst().created);
        } finally {
            Files.deleteIfExists(tempConfig);
        }
    }

    @Test
    void createConfigLoadLengthOnlyOverride() throws IOException {
        Path tempConfig = Files.createTempFile("app-password-test", ".json");

        try {
            String configJson = """
                    {
                      "length": 34
                    }
                    """;

            Files.writeString(tempConfig, configJson);

            AppPasswordConfig config =
                    AppPasswordUtils.createConfig(tempConfig.toString());

            assertEquals(34, config.length);
            assertEquals(1, config.attributes.size());
            assertEquals("emailPassword", config.attributes.getFirst().password);
            assertEquals("emailPasswordCreated", config.attributes.getFirst().created);
        } finally {
            Files.deleteIfExists(tempConfig);
        }
    }

    @Test
    void getAttributeFindsAttribute() {
        AppPasswordConfig config = configWithAttribute(
                "somePassword1",
                "somePasswordCreated1"
        );

        AppPasswordConfigAttribute attribute =
                AppPasswordUtils.getAttribute(config, "somePassword1");

        assertNotNull(attribute);
        assertEquals("somePassword1", attribute.password);
        assertEquals("somePasswordCreated1", attribute.created);
    }

    @Test
    void getAttributeReturnsNullWhenMissing() {
        AppPasswordConfig config = configWithAttribute(
                "somePassword2",
                "somePasswordCreated2"
        );

        AppPasswordConfigAttribute attribute =
                AppPasswordUtils.getAttribute(config, "otherPassword");

        assertNull(attribute);
    }

    @Test
    void generateSecurePasswordReturnsRequestedLength() {
        String password = AppPasswordUtils.generateSecurePassword(32);

        assertEquals(32, password.length());
    }

    @Test
    void generateSecurePasswordRejectsTooShortPassword() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AppPasswordUtils.generateSecurePassword(19)
        );

        assertEquals(
                "Password length must be at least 20",
                exception.getMessage()
        );
    }

    @Test
    void generateSecurePasswordContainsRequiredCharacterTypes() {
        String password = AppPasswordUtils.generateSecurePassword(32);

        long uppercase = password.chars()
                .filter(Character::isUpperCase)
                .count();

        long lowercase = password.chars()
                .filter(Character::isLowerCase)
                .count();

        long digits = password.chars()
                .filter(Character::isDigit)
                .count();

        assertTrue(uppercase >= 2);
        assertTrue(lowercase >= 2);
        assertTrue(digits >= 2);
    }

    @Test
    void hasValidGroupNullGroupsTrue() {
        AppPasswordConfig config = new AppPasswordConfig();
        config.groups = null;

        boolean result = AppPasswordUtils.hasValidGroup(
                config,
                Stream.of("staff", "admin")
        );

        assertTrue(result);
    }

    @Test
    void hasValidGroupEmptyFalse() {
        AppPasswordConfig config = new AppPasswordConfig();
        config.groups = Collections.emptyList();

        boolean result = AppPasswordUtils.hasValidGroup(
                config,
                Stream.of("staff", "admin")
        );

        assertFalse(result);
    }

    @Test
    void hasValidGroupNonMatchingFalse() {
        AppPasswordConfig config = new AppPasswordConfig();
        config.groups = List.of("marketing");

        boolean result = AppPasswordUtils.hasValidGroup(
                config,
                Stream.of("staff", "admin")
        );

        assertFalse(result);
    }

    @Test
    void hasValidGroupMatchingTrue() {
        AppPasswordConfig config = new AppPasswordConfig();
        config.groups = List.of("staff");

        boolean result = AppPasswordUtils.hasValidGroup(
                config,
                Stream.of("staff", "admin")
        );

        assertTrue(result);
    }

    @Test
    void requireAttributeReturnsAttribute() {
        AppPasswordConfig config = new AppPasswordConfig();
        config.attributes = List.of(
                new AppPasswordConfigAttribute("somePassword", "somePasswordCreated")
        );

        EventBuilder event = mock(EventBuilder.class);

        AppPasswordConfigAttribute attribute =
                AppPasswordUtils.requireAttribute(
                        config,
                        "somePassword",
                        event
                );

        assertEquals("somePassword", attribute.password);
        verifyNoInteractions(event);
    }

    private static AppPasswordConfig configWithAttribute(
            String password,
            String created
    ) {
        AppPasswordConfig config = new AppPasswordConfig();
        config.attributes = List.of(
                new AppPasswordConfigAttribute(password, created)
        );
        return config;
    }
}

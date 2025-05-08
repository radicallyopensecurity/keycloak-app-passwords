package org.radicallyopensecurity.keycloak.app_passwords;

import org.junit.Test;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfig;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfigAttribute;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class AppPasswordUtilsTest {

    @Test
    public void createConfigLoadOverrides() throws IOException {
        Path tempConfig = Files.createTempFile("app-password-test", ".json");
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

        AppPasswordConfig config = AppPasswordUtils.createConfig(tempConfig.toString());

        assertEquals(24, (int) config.length);
        assertEquals(1, config.attributes.size());
        assertEquals("somePassword", config.attributes.get(0).password);
        assertEquals("somePasswordCreatedAt", config.attributes.get(0).created);

        Files.deleteIfExists(tempConfig);
    }

    @Test
    public void createConfigLoadPartialOverrides() throws IOException {
        Path tempConfig = Files.createTempFile("app-password-test2", ".json");
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

        AppPasswordConfig config = AppPasswordUtils.createConfig(tempConfig.toString());

        assertEquals(32, (int) config.length);
        assertEquals(1, config.attributes.size());
        assertEquals("somePassword", config.attributes.get(0).password);
        assertEquals("somePasswordCreatedAt", config.attributes.get(0).created);

        Files.deleteIfExists(tempConfig);
    }

    @Test
    public void createConfigLoadPartialOverrides2() throws IOException {
        Path tempConfig = Files.createTempFile("app-password-test2", ".json");
        String configJson = """
            {
              "length": 34
            }
            """;
        Files.writeString(tempConfig, configJson);

        AppPasswordConfig config = AppPasswordUtils.createConfig(tempConfig.toString());

        assertEquals(34, (int) config.length);
        assertEquals(1, config.attributes.size());
        assertEquals("emailPassword", config.attributes.get(0).password);
        assertEquals("emailPasswordCreated", config.attributes.get(0).created);

        Files.deleteIfExists(tempConfig);
    }

    @Test
    public void getAttributeFindsAttribute() {
        AppPasswordConfig config = new AppPasswordConfig();
        config.attributes = List.of(
                new AppPasswordConfigAttribute("somePassword", "somePasswordCreated")
        );

        AppPasswordConfigAttribute attribute = AppPasswordUtils.getAttribute(config, "somePassword");

        assertNotNull(attribute);
    }

    @Test
    public void getAttributeDoesNotFindAttribute() {
        AppPasswordConfig config = new AppPasswordConfig();
        config.attributes = List.of(
                new AppPasswordConfigAttribute("somePassword", "somePasswordCreated")
        );

        AppPasswordConfigAttribute attribute = AppPasswordUtils.getAttribute(config, "otherPassword");

        assertNull(attribute);
    }
}
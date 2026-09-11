package org.radicallyopensecurity.keycloak.app_passwords;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.passay.data.EnglishCharacterData;
import org.passay.generate.PasswordGenerator;
import org.passay.rule.CharacterRule;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfig;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfigAttribute;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class AppPasswordUtils {
    private static final List<CharacterRule> PasswordRules = List.of(
            new CharacterRule(EnglishCharacterData.UpperCase, 2),
            new CharacterRule(EnglishCharacterData.LowerCase, 2),
            new CharacterRule(EnglishCharacterData.Digit, 2));

    /**
     * Create the extension config by parsing the config JSON
     * Otherwise use default values
     * @param path Path to extension config
     * @return Extension config overridden by path as required
     */
    static AppPasswordConfig createConfig(String path) {
        Path configPath = Paths.get(path);
        AppPasswordConfig config = AppPasswordConfig.withDefaults();

        if (Files.exists(configPath)) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                AppPasswordConfig overrides = mapper.readValue(configPath.toFile(), AppPasswordConfig.class);

                if (overrides.attributes != null) {
                    config.attributes = overrides.attributes;
                }

                if (overrides.length != null) {
                    config.length = overrides.length;
                }

                if (overrides.groups != null) {
                    config.groups = overrides.groups;
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load config from " + configPath, e);
            }
        }

        return config;
    }

    /**
     * Generate a secure password with defined length
     * @param length Length of password to generate
     * @return Secure password with defined length
     */
    static String generateSecurePassword(int length) {
        if (length < 20) {
            throw new IllegalArgumentException("Password length must be at least 20");
        }

        PasswordGenerator generator = new PasswordGenerator(length, AppPasswordUtils.PasswordRules);
        return generator.generate().toString();
    }

    /**
     * Parse request and extract user
     * @param session Current session
     * @return User making the request
     * @throws WebApplicationException If unauthorized
     */
    static Auth requireAuth(KeycloakSession session) {
        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .authenticate();

        if (authResult == null) {
            throw new NotAuthorizedException("Bearer");
        }

        RealmModel realm = session.getContext().getRealm();
        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);

        return new Auth(
                realm,
                authResult.token(),
                authResult.user(),
                client,
                authResult.session(),
                false);
    }

    /**
     * Parse request and extract which attribute is requested
     * @param config The extension config
     * @param attributeName The attribute name
     * @return Extracted attribute
     */
    static AppPasswordConfigAttribute getAttribute(AppPasswordConfig config, String attributeName) {
        return config
                .attributes
                .stream()
                .filter(item -> item.password.equals(attributeName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Require an attribute to be present
     * @param config The extension config
     * @param attributeName The attribute name
     * @return Extracted attribute
     * @throws WebApplicationException If request invalid
     */
    static AppPasswordConfigAttribute requireAttribute(
            AppPasswordConfig config,
            String attributeName,
            EventBuilder event
    ) {
        AppPasswordConfigAttribute attribute = getAttribute(config, attributeName);

        if (attribute == null) {
            event.error("Not Found");
            throw new NotFoundException();
        }

        return attribute;
    }

    /**
     * App Passwords is enabled for user
     */
    public static boolean hasValidGroup(AppPasswordConfig config, Stream<String> userGroups) {
        List<String> allowedGroups = config.groups;

        if (config.groups == null) {
            // Enabled for everyone
            return true;
        }

        if (config.groups.isEmpty()) {
            // Disabled for everyone
            return false;
        }

        // user must have group membership
        return userGroups.anyMatch(allowedGroups::contains);
    }
}

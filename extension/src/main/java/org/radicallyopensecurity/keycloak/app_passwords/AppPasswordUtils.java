package org.radicallyopensecurity.keycloak.app_passwords;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import org.keycloak.models.*;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfig;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfigAttribute;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class AppPasswordUtils {
    private static final PasswordGenerator Generator = new PasswordGenerator();
    private static final List<CharacterRule> PasswordRules = List.of(
            new CharacterRule(EnglishCharacterData.UpperCase, 2),
            new CharacterRule(EnglishCharacterData.LowerCase, 2),
            new CharacterRule(EnglishCharacterData.Digit, 2),
            new CharacterRule(EnglishCharacterData.SpecialAscii, 2));

    /**
     * Create the extension config by parsing the config json
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
        return AppPasswordUtils.Generator.generatePassword(length, PasswordRules);
    }

    /**
     * Parse request and extract user
     * @param session Current session
     * @return User making the request
     * @throws NotAuthorizedException If unauthorized
     */
    static Auth validateAuth(KeycloakSession session) {
        boolean hasCookie = false;

        AppAuthManager.BearerTokenAuthenticator bearerAuthenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        AuthenticationManager.AuthResult authResult = bearerAuthenticator
                .setConnection(session.getContext().getConnection())
                .setHeaders(session.getContext().getRequestHeaders())
                .authenticate();

        if (authResult == null) {
            // fallback to browser session (identity cookie)
            hasCookie = true;
            authResult = AuthenticationManager.authenticateIdentityCookie(
                    session,
                    session.getContext().getRealm(),
                    true  // check active session
            );
        }

        if (authResult == null) {
            throw new NotAuthorizedException("User not authenticated");
        }

        RealmModel realm = session.getContext().getRealm();
        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        Auth auth = new Auth(
                session.getContext().getRealm(),
                authResult.getToken(),
                authResult.getUser(),
                client,
                authResult.getSession(),
                hasCookie);

        if (auth == null) {
            throw new NotAuthorizedException("Invalid Token");
        }

        return auth;
    }

    /**
     * Parse request and extract which attribute is requested
     * @param config The extension config
     * @param attributeName The attribute name
     * @return extracted attribute
     * @throws BadRequestException If request invalid
     */
    static AppPasswordConfigAttribute validateAttribute(AppPasswordConfig config, String attributeName) {
        if (attributeName == null) {
            throw new BadRequestException("Missing attribute name");
        }

        if (attributeName == null) {
            throw new BadRequestException("Missing 'name' field in request");
        }

        AppPasswordConfigAttribute attribute = getAttribute(config, attributeName);

        if (attribute == null) {
            throw new BadRequestException("Invalid attribute");
        }

        return attribute;
    }

    /**
     * Get attribute by name
     * @param config Config
     * @param attributeName Name
     * @return Attribute from config
     */
    public static AppPasswordConfigAttribute getAttribute(AppPasswordConfig config, String attributeName) {
        AppPasswordConfigAttribute attribute = config
                .attributes
                .stream()
                .filter(item -> item.password.equals(attributeName))
                .findFirst()
                .orElse(null);
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

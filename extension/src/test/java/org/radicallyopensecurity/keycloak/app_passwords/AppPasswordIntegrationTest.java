package org.radicallyopensecurity.keycloak.app_passwords;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@KeycloakIntegrationTest(
        config = AppPasswordIntegrationTest.ServerConfig.class
)
class AppPasswordIntegrationTest {

    private static final String CLIENT_ID = "app-password-test-client";

    private static final String ALLOWED_USER = "allowed-user";
    private static final String DENIED_USER = "denied-user";
    private static final String USER_PASSWORD = "test-password";

    private static final String ALLOWED_GROUP = "staff";

    private static final String ATTRIBUTE_NAME = "emailPassword";
    private static final String CREATED_ATTRIBUTE = "emailPasswordCreated";

    private static final String ALLOWED_ORIGIN = "https://app.example.test";
    private static final String DENIED_ORIGIN = "https://evil.example.test";

    @InjectRealm(config = TestRealm.class)
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp http;

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(
                KeycloakServerConfigBuilder config
        ) {
            return config.dependencyCurrentProject();
        }
    }

    public static class TestRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .name("app-password-test")
                    .groups(ALLOWED_GROUP)
                    .users(
                            UserBuilder.create(ALLOWED_USER)
                                    .enabled(true)
                                    .email("allowed@example.test")
                                    .firstName("Allowed")
                                    .lastName("User")
                                    .password(USER_PASSWORD)
                                    .requiredActions()
                                    .build(),

                            UserBuilder.create(DENIED_USER)
                                    .enabled(true)
                                    .email("denied@example.test")
                                    .firstName("Denied")
                                    .lastName("User")
                                    .password(USER_PASSWORD)
                                    .requiredActions()
                                    .build()
                    )
                    .clients(
                            ClientBuilder.create(CLIENT_ID)
                                    .publicClient(true)
                                    .directAccessGrantsEnabled(true)
                                    .build()
                    );
        }
    }

    private void configureUserProfile() {
        UPConfig config = realm.admin()
                .users()
                .userProfile()
                .getConfiguration();

        List<UPAttribute> attributes = new ArrayList<>(config.getAttributes());

        addManagedAttribute(attributes, ATTRIBUTE_NAME);
        addManagedAttribute(attributes, CREATED_ATTRIBUTE);

        config.setAttributes(attributes);

        realm.admin()
                .users()
                .userProfile()
                .update(config);
    }

    private void addManagedAttribute(List<UPAttribute> attributes, String name) {
        boolean exists = attributes.stream()
                .anyMatch(attribute -> name.equals(attribute.getName()));

        if (exists) {
            return;
        }

        UPAttribute attribute = new UPAttribute();
        attribute.setName(name);

        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setView(Set.of("admin"));
        permissions.setEdit(Set.of("admin"));

        attribute.setPermissions(permissions);
        attributes.add(attribute);
    }

    private String generate(String token) throws IOException {
        try (SimpleHttpResponse response = http.doPost(endpoint())
                .auth(token)
                .json(Map.of(
                        "name", ATTRIBUTE_NAME
                ))
                .asResponse()) {

            assertEquals(200, response.getStatus());

            JsonNode body = response.asJson();

            assertNotNull(body.get("password"));

            return body.get("password").asText();
        }
    }

    private String obtainToken(String username) throws IOException {
        String tokenUrl =
                realm.getBaseUrl() + "/protocol/openid-connect/token";

        try (SimpleHttpResponse response = http.doPost(tokenUrl)
                .param("grant_type", "password")
                .param("client_id", CLIENT_ID)
                .param("username", username)
                .param("password", USER_PASSWORD)
                .asResponse()) {

            assertEquals(
                    200,
                    response.getStatus(),
                    () -> "Token request failed for " + username
            );

            JsonNode json = response.asJson();

            assertNotNull(json.get("access_token"));

            return json.get("access_token").asText();
        }
    }

    private void configureClientOrigins() {
        ClientRepresentation client = realm.admin()
                .clients()
                .findByClientId(CLIENT_ID)
                .stream()
                .findFirst()
                .orElseThrow();

        client.setWebOrigins(List.of(ALLOWED_ORIGIN));

        realm.admin()
                .clients()
                .get(client.getId())
                .update(client);
    }

    private void configureGroupMembership() {
        GroupRepresentation group = realm.admin()
                .groups()
                .groups()
                .stream()
                .filter(g -> ALLOWED_GROUP.equals(g.getName()))
                .findFirst()
                .orElseThrow();

        UserRepresentation allowed = findUser(ALLOWED_USER);
        UserRepresentation denied = findUser(DENIED_USER);

        realm.admin()
                .users()
                .get(allowed.getId())
                .joinGroup(group.getId());

        realm.admin()
                .users()
                .get(denied.getId())
                .leaveGroup(group.getId());
    }

    private void clearAppPasswordAttributes() {
        clearAppPasswordAttributes(ALLOWED_USER);
        clearAppPasswordAttributes(DENIED_USER);
    }

    private void clearAppPasswordAttributes(String username) {
        UserRepresentation representation = findUser(username);

        Map<String, List<String>> attributes =
                representation.getAttributes() == null
                        ? new HashMap<>()
                        : new HashMap<>(representation.getAttributes());

        attributes.remove(ATTRIBUTE_NAME);
        attributes.remove(CREATED_ATTRIBUTE);

        representation.setAttributes(attributes);

        realm.admin()
                .users()
                .get(representation.getId())
                .update(representation);
    }

    private UserRepresentation allowedUser() {
        return findUser(ALLOWED_USER);
    }

    private UserRepresentation findUser(String username) {
        UserRepresentation user = realm.admin()
                .users()
                .searchByUsername(username, true)
                .stream()
                .findFirst()
                .orElseThrow();

        return realm.admin()
                .users()
                .get(user.getId())
                .toRepresentation();
    }

    private String passwordHash() {
        UserRepresentation user = allowedUser();

        if (user.getAttributes() == null) {
            return null;
        }

        List<String> values =
                user.getAttributes().get(ATTRIBUTE_NAME);

        return values == null || values.isEmpty()
                ? null
                : values.getFirst();
    }

    private String createdTimestamp() {
        UserRepresentation user = allowedUser();

        if (user.getAttributes() == null) {
            return null;
        }

        List<String> values =
                user.getAttributes().get(CREATED_ATTRIBUTE);

        return values == null || values.isEmpty()
                ? null
                : values.getFirst();
    }

    private String endpoint() {
        return realm.getBaseUrl() + "/app-password";
    }

    private String endpoint(String path) {
        return endpoint() + path;
    }

    private HttpResponse<String> delete(String token, String name) throws Exception {
        String body = """
            {"name":"%s"}
            """.formatted(name);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint()))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method(
                        "DELETE",
                        HttpRequest.BodyPublishers.ofString(body)
                )
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
        }
    }

    @BeforeEach
    void setup() {
        configureClientOrigins();
        configureGroupMembership();
        configureUserProfile();
        clearAppPasswordAttributes();
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        try (SimpleHttpResponse response = http.doGet(endpoint())
                .asResponse()) {

            assertEquals(401, response.getStatus());
        }
    }

    @Test
    void invalidBearerTokenReturns401() throws Exception {
        try (SimpleHttpResponse response = http.doGet(endpoint())
                .auth("not-a-valid-token")
                .asResponse()) {

            assertEquals(401, response.getStatus());
        }
    }

    @Test
    void userOutsideAllowedGroupReturns403() throws Exception {
        String token = obtainToken(DENIED_USER);

        try (SimpleHttpResponse response = http.doGet(endpoint())
                .auth(token)
                .asResponse()) {

            assertEquals(403, response.getStatus());
        }
    }

    @Test
    void enabledReturnsTrueForAllowedUser() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        try (SimpleHttpResponse response = http.doGet(endpoint("/enabled"))
                .auth(token)
                .asResponse()) {

            assertEquals(200, response.getStatus());

            JsonNode body = response.asJson();

            assertTrue(body.get("enabled").asBoolean());
        }
    }

    @Test
    void enabledReturnsFalseForDeniedUser() throws Exception {
        String token = obtainToken(DENIED_USER);

        try (SimpleHttpResponse response = http.doGet(endpoint("/enabled"))
                .auth(token)
                .asResponse()) {

            assertEquals(200, response.getStatus());

            JsonNode body = response.asJson();

            assertFalse(body.get("enabled").asBoolean());
        }
    }

    @Test
    void generateCreatesPasswordAndAttributes() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        JsonNode body;

        try (SimpleHttpResponse response = http.doPost(endpoint())
                .auth(token)
                .json(Map.of(
                        "name", ATTRIBUTE_NAME
                ))
                .asResponse()) {

            assertEquals(200, response.getStatus());

            body = response.asJson();
        }

        assertNotNull(body);

        UserRepresentation user = allowedUser();

        Map<String, List<String>> attributes = user.getAttributes();

        assertNotNull(attributes);
        assertTrue(attributes.containsKey(ATTRIBUTE_NAME));
        assertTrue(attributes.containsKey(CREATED_ATTRIBUTE));

        String storedHash = attributes.get(ATTRIBUTE_NAME).getFirst();
        String created = attributes.get(CREATED_ATTRIBUTE).getFirst();

        assertNotNull(storedHash);
        assertFalse(storedHash.isBlank());

        assertNotNull(created);
        assertFalse(created.isBlank());

        String plainText = body.get("password").asText();

        assertNotNull(plainText);
        assertFalse(plainText.isBlank());
        assertNotEquals(plainText, storedHash);
    }

    @Test
    void regenerateReplacesExistingPassword() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        String firstPassword = generate(token);
        String firstHash = passwordHash();

        String secondPassword = generate(token);
        String secondHash = passwordHash();

        assertNotEquals(firstPassword, secondPassword);
        assertNotEquals(firstHash, secondHash);
    }

    @Test
    void generateUnknownAttributeReturns404() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        try (SimpleHttpResponse response = http.doPost(endpoint())
                .auth(token)
                .json(Map.of(
                        "name", "does-not-exist"
                ))
                .asResponse()) {

            assertEquals(404, response.getStatus());
        }
    }

    @Test
    void generateBlankAttributeReturns400() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        try (SimpleHttpResponse response = http.doPost(endpoint())
                .auth(token)
                .json(Map.of(
                        "name", ""
                ))
                .asResponse()) {

            assertEquals(400, response.getStatus());
        }
    }

    @Test
    void generateMissingAttributeReturns400() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        try (SimpleHttpResponse response = http.doPost(endpoint())
                .auth(token)
                .json(Map.of())
                .asResponse()) {

            assertEquals(400, response.getStatus());
        }
    }

    @Test
    void checkCorrectPasswordReturnsTrue() throws Exception {
        String token = obtainToken(ALLOWED_USER);
        String password = generate(token);

        try (SimpleHttpResponse response = http.doPost(endpoint("/check"))
                .auth(token)
                .json(Map.of(
                        "name", ATTRIBUTE_NAME,
                        "password", password
                ))
                .asResponse()) {

            assertEquals(200, response.getStatus());

            JsonNode body = response.asJson();

            assertTrue(body.get("success").asBoolean());
        }
    }

    @Test
    void checkWrongPasswordReturnsFalse() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        generate(token);

        try (SimpleHttpResponse response = http.doPost(endpoint("/check"))
                .auth(token)
                .json(Map.of(
                        "name", ATTRIBUTE_NAME,
                        "password", "definitely-wrong"
                ))
                .asResponse()) {

            assertEquals(200, response.getStatus());

            JsonNode body = response.asJson();

            assertFalse(body.get("success").asBoolean());
        }
    }

    @Test
    void checkWithoutGeneratedPasswordReturnsFalse() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        try (SimpleHttpResponse response = http.doPost(endpoint("/check"))
                .auth(token)
                .json(Map.of(
                        "name", ATTRIBUTE_NAME,
                        "password", "anything"
                ))
                .asResponse()) {

            assertEquals(200, response.getStatus());

            JsonNode body = response.asJson();

            assertFalse(body.get("success").asBoolean());
        }
    }

    @Test
    void checkUnknownAttributeReturns404() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        try (SimpleHttpResponse response = http.doPost(endpoint("/check"))
                .auth(token)
                .json(Map.of(
                        "name", "does-not-exist",
                        "password", "anything"
                ))
                .asResponse()) {

            assertEquals(404, response.getStatus());
        }
    }

    @Test
    void checkDeniedUserReturns403() throws Exception {
        String token = obtainToken(DENIED_USER);

        try (SimpleHttpResponse response = http.doPost(endpoint("/check"))
                .auth(token)
                .json(Map.of(
                        "name", ATTRIBUTE_NAME,
                        "password", "anything"
                ))
                .asResponse()) {

            assertEquals(403, response.getStatus());
        }
    }

    @Test
    void listReturnsConfiguredPasswordMetadata() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        generate(token);

        try (SimpleHttpResponse response = http.doGet(endpoint())
                .auth(token)
                .asResponse()) {

            assertEquals(200, response.getStatus());

            JsonNode body = response.asJson();

            assertTrue(body.isArray());
            assertFalse(body.isEmpty());

            JsonNode entry = body.get(0);

            assertEquals(ATTRIBUTE_NAME, entry.get("name").asText());
            assertFalse(entry.get("created").isNull());
            assertFalse(entry.get("created").asText().isBlank());
        }
    }

    @Test
    void deleteRemovesStoredPassword() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        generate(token);

        assertNotNull(passwordHash());
        assertNotNull(createdTimestamp());

        HttpResponse<String> response =
                delete(token, ATTRIBUTE_NAME);

        assertEquals(204, response.statusCode());

        UserRepresentation user = allowedUser();

        Map<String, List<String>> attributes = user.getAttributes();

        if (attributes != null) {
            assertFalse(attributes.containsKey(ATTRIBUTE_NAME));
            assertFalse(attributes.containsKey(CREATED_ATTRIBUTE));
        }
    }

    @Test
    void deleteUnknownAttributeReturns404() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        HttpResponse<String> response =
                delete(token, "does-not-exist");

        assertEquals(404, response.statusCode());
    }

    @Test
    void deleteDeniedUserReturns403() throws Exception {
        String token = obtainToken(DENIED_USER);

        HttpResponse<String> response =
                delete(token, ATTRIBUTE_NAME);

        assertEquals(403, response.statusCode());
    }

    @Test
    void allowedCorsOriginIsReturned() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        try (SimpleHttpResponse response = http.doGet(endpoint("/enabled"))
                .auth(token)
                .header("Origin", ALLOWED_ORIGIN)
                .asResponse()) {

            assertEquals(200, response.getStatus());

            assertEquals(
                    ALLOWED_ORIGIN,
                    response.getFirstHeader("Access-Control-Allow-Origin")
            );
        }
    }

    @Test
    void forbiddenCorsOriginReturns403() throws Exception {
        String token = obtainToken(ALLOWED_USER);

        try (SimpleHttpResponse response = http.doGet(endpoint("/enabled"))
                .auth(token)
                .header("Origin", DENIED_ORIGIN)
                .asResponse()) {

            assertEquals(403, response.getStatus());
        }
    }

    @Test
    void rootPreflightSupportsExpectedMethods() throws Exception {
        try (SimpleHttpResponse response = http.doOptions(endpoint())
                .header("Origin", ALLOWED_ORIGIN)
                .header(
                        "Access-Control-Request-Method",
                        "POST"
                )
                .header(
                        "Access-Control-Request-Headers",
                        "Authorization, Content-Type"
                )
                .asResponse()) {

            assertEquals(200, response.getStatus());

            String methods =
                    response.getFirstHeader("Access-Control-Allow-Methods");

            assertNotNull(methods);
            assertTrue(methods.contains("GET"));
            assertTrue(methods.contains("POST"));
            assertTrue(methods.contains("DELETE"));
        }
    }

    @Test
    void checkPreflightSupportsPost() throws Exception {
        try (SimpleHttpResponse response = http.doOptions(endpoint("/check"))
                .header("Origin", ALLOWED_ORIGIN)
                .header(
                        "Access-Control-Request-Method",
                        "POST"
                )
                .header(
                        "Access-Control-Request-Headers",
                        "Authorization, Content-Type"
                )
                .asResponse()) {

            assertEquals(200, response.getStatus());

            String methods =
                    response.getFirstHeader("Access-Control-Allow-Methods");

            assertNotNull(methods);
            assertTrue(methods.contains("POST"));
        }
    }

    @Test
    void enabledPreflightSupportsGet() throws Exception {
        try (SimpleHttpResponse response = http.doOptions(endpoint("/enabled"))
                .header("Origin", ALLOWED_ORIGIN)
                .header(
                        "Access-Control-Request-Method",
                        "GET"
                )
                .header(
                        "Access-Control-Request-Headers",
                        "Authorization"
                )
                .asResponse()) {

            assertEquals(200, response.getStatus());

            String methods =
                    response.getFirstHeader("Access-Control-Allow-Methods");

            assertNotNull(methods);
            assertTrue(methods.contains("GET"));
        }
    }
}

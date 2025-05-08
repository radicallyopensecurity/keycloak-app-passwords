package org.radicallyopensecurity.keycloak.app_passwords;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import com.password4j.Argon2Function;
import com.password4j.Password;
import com.password4j.types.Argon2;
import jakarta.ws.rs.*;
import org.jboss.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.Auth;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfig;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfigAttribute;
import org.radicallyopensecurity.keycloak.app_passwords.dtos.*;

@jakarta.ws.rs.ext.Provider
public class AppPasswordRestResource {
    private static final Logger log = Logger.getLogger(AppPasswordRestResource.class);
    private final KeycloakSession session;
    private final AppPasswordConfig config;
    private static final Argon2Function hashFunction = Argon2Function.getInstance(
            65536,
            5,
            1,
            64,
            Argon2.ID,
            Argon2Function.ARGON2_VERSION_13
    );

    /**
     * Custom REST API to manage app passwords. Can be used to validate login in places where you want a password
     * separate from the users credentials.
     *
     * The OPTIONS and CORS settings should be set in the reverse proxy for correct function.
     * @param session Current session
     * @param config Runtime config
     */
    public AppPasswordRestResource(KeycloakSession session, AppPasswordConfig config) {
        this.session = session;
        this.config = config;
    }

    /**
     * Should allow POST, GET, DELETE and any CORS requirements.
     * Those should be set in a proxy
     * @return Empty
     */
    @OPTIONS
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response optionsRoot() {
        return Cors
                .builder()
                .preflight()
                .allowedMethods("GET", "POST", "DELETE")
                .auth()
                .add(Response.ok());
    }

    /**
     * Get a list of all app passwords for a user
     * @return List of app passwords
     */
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        Auth auth = AppPasswordUtils.validateAuth(session);
        UserModel user = auth.getUser();

        List<AppPasswordListResponseDto> result = config.attributes.stream()
                .map(item -> {
                    String created = user.getFirstAttribute(item.created);
                    return new AppPasswordListResponseDto(item.password, created);
                })
                .collect(Collectors.toList());

        return Cors
                .builder()
                .allowedOrigins(auth.getToken())
                .allowedMethods("GET")
                .add(Response.ok(result));
    }

    /**
     * Generate or re-generate an app password for a user
     * @param request Current request
     * @return Plain text app password
     */
    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate(AppPasswordRequestDto request) {
        Auth auth = AppPasswordUtils.validateAuth(session);
        AppPasswordConfigAttribute attribute = AppPasswordUtils.validateAttribute(config, request.name);

        String plainText = AppPasswordUtils.generateSecurePassword(config.length);
        String hashed = Password
                .hash(plainText)
                .addRandomSalt()
                .with(hashFunction)
                .getResult();

        String now = OffsetDateTime.now(ZoneOffset.UTC).toString();

        UserModel user = auth.getUser();
        user.setSingleAttribute(attribute.password, hashed);
        user.setSingleAttribute(attribute.created, now);

        AppPasswordGenerateResponseDto result = new AppPasswordGenerateResponseDto(
                attribute.password,
                plainText,
                now
        );

        return Cors
                .builder()
                .allowedOrigins(auth.getToken())
                .allowedMethods("POST")
                .add(Response.ok(result));
    }

    /**
     * Delete app password for a user
     * Removes the password itself and the created value
     * @param request Current request
     * @return Empty
     */
    @DELETE
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response delete(AppPasswordRequestDto request) {
        Auth auth = AppPasswordUtils.validateAuth(session);
        AppPasswordConfigAttribute attribute = AppPasswordUtils.validateAttribute(config, request.name);

        UserModel user = auth.getUser();
        user.removeAttribute(attribute.password);
        user.removeAttribute(attribute.created);

        return Cors
                .builder()
                .allowedOrigins(auth.getToken())
                .allowedMethods("DELETE")
                .add(Response.noContent());
    }

    /**
     * Preflight check password
     * @return Empty
     */
    @OPTIONS
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    public Response optionsCheck() {
        return Cors
                .builder()
                .preflight()
                .allowedMethods("POST")
                .auth()
                .add(Response.ok());
    }

    /**
     * Check if app password is correct
     * @param request Current request
     * @return Empty
     */
    @POST
    @Path("/check")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response check(AppPasswordCheckPasswordRequestDto request) {
        Auth auth = AppPasswordUtils.validateAuth(session);
        AppPasswordUtils.validateAttribute(config, request.name);

        UserModel user = auth.getUser();

        String hash = user.getFirstAttribute(request.name);
        boolean verified = Password.check(request.password, hash).with(hashFunction);

        AppPasswordCheckPasswordResponseDto result = new AppPasswordCheckPasswordResponseDto(verified);

        return Cors
                .builder()
                .allowedOrigins(auth.getToken())
                .allowedMethods("POST")
                .add(Response.ok(result));
    }
}

package org.radicallyopensecurity.keycloak.app_passwords;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.password4j.Argon2Function;
import com.password4j.Password;
import com.password4j.types.Argon2;
import jakarta.ws.rs.*;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.Auth;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfig;
import org.radicallyopensecurity.keycloak.app_passwords.config.AppPasswordConfigAttribute;
import org.radicallyopensecurity.keycloak.app_passwords.dtos.*;

@Provider
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
     * <p>
     * The OPTIONS and CORS settings should be set in the reverse proxy for correct function.
     *
     * @param session Current session
     * @param config  Runtime config
     */
    public AppPasswordRestResource(KeycloakSession session, AppPasswordConfig config) {
        this.session = session;
        this.config = config;
    }

    /**
     * Should allow POST, GET, DELETE and any CORS requirements.
     * Those should be set in a proxy
     *
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
     *
     * @return List of app passwords
     */
    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        Auth auth = AppPasswordUtils.validateAuth(session);
        UserModel user = auth.getUser();
        Stream<String> userGroups = user.getGroupsStream().map(GroupModel::getName);

        if (!AppPasswordUtils.hasValidGroup(config, userGroups)) {
            KeycloakContext context = session.getContext();
            new EventBuilder(session.getContext().getRealm(), session)
                    .event(EventType.CUSTOM_REQUIRED_ACTION)
                    .detail("Endpoint", "List App Passwords")
                    .user(user.getId())
                    .ipAddress(context.getConnection().getRemoteAddr())
                    .client(auth.getClient())
                    .error("Unauthorized");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

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
     *
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

        UserModel user = auth.getUser();
        Stream<String> userGroups = user.getGroupsStream().map(GroupModel::getName);

        KeycloakContext context = session.getContext();
        EventBuilder event = new EventBuilder(context.getRealm(), session)
                .event(EventType.UPDATE_PROFILE)
                .ipAddress(context.getConnection().getRemoteAddr())
                .client(auth.getClient())
                .user(user.getId());

        if (!AppPasswordUtils.hasValidGroup(config, userGroups)) {
            event.detail(attribute.password, "SECRET")
                    .detail(attribute.created, "EMPTY").error("Unauthorized");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        String plainText = AppPasswordUtils.generateSecurePassword(config.length);
        String hashed = Password
                .hash(plainText)
                .addRandomSalt()
                .with(hashFunction)
                .getResult();

        String now = OffsetDateTime.now(ZoneOffset.UTC).toString();

        user.setSingleAttribute(attribute.password, hashed);
        user.setSingleAttribute(attribute.created, now);

        AppPasswordGenerateResponseDto result = new AppPasswordGenerateResponseDto(
                attribute.password,
                plainText,
                now
        );

        event.detail(attribute.password, "SECRET")
                .detail(attribute.created, now).success();
        return Cors
                .builder()
                .allowedOrigins(auth.getToken())
                .allowedMethods("POST")
                .add(Response.ok(result));
    }

    /**
     * Delete app password for a user
     * Removes the password itself and the created value
     *
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

        KeycloakContext context = session.getContext();
        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session).event(EventType.UPDATE_PROFILE)
                .user(user.getId())
                .ipAddress(context.getConnection().getRemoteAddr())
                .client(auth.getClient());

        Stream<String> userGroups = user.getGroupsStream().map(GroupModel::getName);

        if (!AppPasswordUtils.hasValidGroup(config, userGroups)) {
            event.detail(attribute.password, "UNKNOWN")
                    .detail(attribute.created, "UNKNOWN").error("Unauthorized");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());

        }

        user.removeAttribute(attribute.password);
        user.removeAttribute(attribute.created);

        event.detail(attribute.password, "DELETED")
                .detail(attribute.created, "DELETED").success();
        return Cors
                .builder()
                .allowedOrigins(auth.getToken())
                .allowedMethods("DELETE")
                .add(Response.noContent());
    }

    /**
     * Preflight check password
     *
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
     *
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
        Stream<String> userGroups = user.getGroupsStream().map(GroupModel::getName);

        KeycloakContext context = session.getContext();
        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session)
                .event(EventType.CUSTOM_REQUIRED_ACTION)
                .detail("Endpoint", "Check App Password")
                .detail("Attribute", request.name)
                .user(user.getId())
                .ipAddress(context.getConnection().getRemoteAddr())
                .client(auth.getClient());

        if (!AppPasswordUtils.hasValidGroup(config, userGroups)) {
            event.error("Unauthorized");
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        String hash = user.getFirstAttribute(request.name);
        boolean verified = Password.check(request.password, hash).with(hashFunction);

        AppPasswordCheckPasswordResponseDto result = new AppPasswordCheckPasswordResponseDto(verified);

        event.detail("Verified", String.valueOf(verified)).success();
        return Cors
                .builder()
                .allowedOrigins(auth.getToken())
                .allowedMethods("POST")
                .add(Response.ok(result));
    }

    /**
     * Preflight is enabled
     *
     * @return Empty
     */
    @OPTIONS
    @Path("/enabled")
    @Produces(MediaType.APPLICATION_JSON)
    public Response optionsEnabled() {
        return Cors
                .builder()
                .preflight()
                .allowedMethods("GET")
                .auth()
                .add(Response.ok());
    }

    /**
     * Check whether app passwords are enabled for user
     *
     * @return Whether app passwords are enabled for user
     */
    @GET
    @Path("/enabled")
    @Produces(MediaType.APPLICATION_JSON)
    public Response enabled() {
        Auth auth = AppPasswordUtils.validateAuth(session);
        UserModel user = auth.getUser();
        Stream<String> userGroups = user.getGroupsStream().map(GroupModel::getName);

        boolean isEnabled = AppPasswordUtils.hasValidGroup(config, userGroups);
        AppPasswordEnabledResponseDto result = new AppPasswordEnabledResponseDto(isEnabled);

        return Cors
                .builder()
                .allowedOrigins(auth.getToken())
                .allowedMethods("GET")
                .add(Response.ok(result));
    }
}

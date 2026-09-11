package org.radicallyopensecurity.keycloak.app_passwords;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import com.password4j.Argon2Function;
import com.password4j.Password;
import com.password4j.types.Argon2;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.ext.Provider;

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
    private final KeycloakSession session;
    private final AppPasswordConfig config;
    private static final Argon2Function HASH_FUNCTION = Argon2Function.getInstance(
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
        Auth auth = AppPasswordUtils.requireAuth(session);

        Cors cors = Cors.builder()
                .checkAllowedOrigins(auth.getToken())
                .auth();

        UserModel user = auth.getUser();
        Stream<String> userGroups =
                user.getGroupsStream().map(GroupModel::getName);
        KeycloakContext context = session.getContext();

        EventBuilder event = new EventBuilder(context.getRealm(), session)
                .event(EventType.CUSTOM_REQUIRED_ACTION)
                .detail("operation", "keycloak-app-passwords_list")
                .user(user.getId())
                .ipAddress(context.getConnection().getRemoteAddr())
                .client(auth.getClient());

        if (!AppPasswordUtils.hasValidGroup(config, userGroups)) {
            event.error("Forbidden");
            throw new ForbiddenException();
        }


        List<AppPasswordListResponseDto> result = config.attributes.stream()
                .map(item -> new AppPasswordListResponseDto(
                        item.password,
                        user.getFirstAttribute(item.created)
                ))
                .toList();

        event.success();
        return cors.add(Response.ok(result));
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
        Auth auth = AppPasswordUtils.requireAuth(session);
        AppPasswordValidator.validate(request);

        Cors cors = Cors.builder()
                .checkAllowedOrigins(auth.getToken())
                .auth();

        UserModel user = auth.getUser();
        Stream<String> userGroups =
                user.getGroupsStream().map(GroupModel::getName);

        KeycloakContext context = session.getContext();

        EventBuilder event = new EventBuilder(context.getRealm(), session)
                .event(EventType.UPDATE_PROFILE)
                .detail("operation", "app-password-generate")
                .detail("attribute", request.name)
                .ipAddress(context.getConnection().getRemoteAddr())
                .client(auth.getClient())
                .user(user.getId());

        if (!AppPasswordUtils.hasValidGroup(config, userGroups)) {
            event.error("Forbidden");
            throw new ForbiddenException();
        }

        AppPasswordConfigAttribute attribute = AppPasswordUtils.requireAttribute(config, request.name, event);

        String plainText =
                AppPasswordUtils.generateSecurePassword(config.length);

        String hashed = Password.hash(plainText)
                .addRandomSalt()
                .with(HASH_FUNCTION)
                .getResult();

        String now = OffsetDateTime.now(ZoneOffset.UTC).toString();

        user.setSingleAttribute(attribute.password, hashed);
        user.setSingleAttribute(attribute.created, now);

        AppPasswordGenerateResponseDto result =
                new AppPasswordGenerateResponseDto(
                        attribute.password,
                        plainText,
                        now
                );

        event.success();
        return cors.add(Response.ok(result));
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
        Auth auth = AppPasswordUtils.requireAuth(session);
        AppPasswordValidator.validate(request);

        Cors cors = Cors.builder()
                .checkAllowedOrigins(auth.getToken())
                .auth();

        UserModel user = auth.getUser();
        KeycloakContext context = session.getContext();
        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session)
                .event(EventType.REMOVE_CREDENTIAL)
                .detail("operation", "app-password-delete")
                .detail("attribute", request.name)
                .user(user.getId())
                .ipAddress(context.getConnection().getRemoteAddr())
                .client(auth.getClient());

        Stream<String> userGroups = user.getGroupsStream().map(GroupModel::getName);

        if (!AppPasswordUtils.hasValidGroup(config, userGroups)) {
            event.error("Forbidden");
            throw new ForbiddenException();
        }

        AppPasswordConfigAttribute attribute = AppPasswordUtils.requireAttribute(config, request.name, event);

        user.removeAttribute(attribute.password);
        user.removeAttribute(attribute.created);

        event.success();

        return cors.add(Response.noContent());
    }

    /**
     * Preflight check password
     *
     * @return Empty
     */
    @OPTIONS
    @Path("/check")
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
        Auth auth = AppPasswordUtils.requireAuth(session);
        AppPasswordValidator.validate(request);

        Cors cors = Cors.builder()
                .checkAllowedOrigins(auth.getToken())
                .auth();

        UserModel user = auth.getUser();
        Stream<String> userGroups = user.getGroupsStream().map(GroupModel::getName);

        KeycloakContext context = session.getContext();

        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session)
                .event(EventType.CUSTOM_REQUIRED_ACTION)
                .detail("operation", "app-password-check")
                .detail("attribute", request.name)
                .user(user.getId())
                .ipAddress(context.getConnection().getRemoteAddr())
                .client(auth.getClient());

        if (!AppPasswordUtils.hasValidGroup(config, userGroups)) {
            event.error("Forbidden");
            throw new ForbiddenException();
        }

        AppPasswordConfigAttribute attribute = AppPasswordUtils.requireAttribute(config, request.name, event);
        String hash = user.getFirstAttribute(attribute.password);

        boolean verified = hash != null && Password.check(request.password, hash).with(HASH_FUNCTION);

        event.detail("verified", Boolean.toString(verified)).success();
        return cors.add(Response.ok(new AppPasswordCheckPasswordResponseDto(verified)));
    }

    /**
     * Preflight is enabled
     *
     * @return Empty
     */
    @OPTIONS
    @Path("/enabled")
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
        Auth auth = AppPasswordUtils.requireAuth(session);

        Cors cors = Cors.builder()
                .checkAllowedOrigins(auth.getToken())
                .auth();

        UserModel user = auth.getUser();

        boolean enabled = AppPasswordUtils.hasValidGroup(
                config,
                user.getGroupsStream().map(GroupModel::getName)
        );

        return cors.add(
                Response.ok(new AppPasswordEnabledResponseDto(enabled))
        );
    }
}

package dev.yakovlev_alexey.keycloak.authentication.authenticators.browser;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

import dev.yakovlev_alexey.keycloak.authentication.actiontoken.emaillink.EmailLinkActionToken;

public class EmailLinkAuthenticator implements Authenticator {
    public static final String EMAIL_LINK_VERIFIED = "EMAIL_LINK_VERIFIED";

    private static final Logger logger = Logger.getLogger(EmailLinkAuthenticator.class);

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // the method gets called when first reaching this authenticator and after page
        // refreshes
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();

        // cant really do anything without smtp server
        if (realm.getSmtpConfig().isEmpty()) {
            ServicesLogger.LOGGER.smtpNotConfigured();
            context.attempted();
            return;
        }

        // if email was verified allow the user to continue
        if (Objects.equals(authSession.getAuthNote(EMAIL_LINK_VERIFIED), user.getEmail())) {
            context.success();
            return;
        }

        // do not allow resending e-mail by simple page refresh
        if (!Objects.equals(authSession.getAuthNote(Constants.VERIFY_EMAIL_KEY), user.getEmail())) {
            authSession.setAuthNote(Constants.VERIFY_EMAIL_KEY, user.getEmail());
            sendVerifyEmail(session, context, user);
        } else {
            showEmailSentPage(context, user);
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // this method gets called when user submits the form
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        KeycloakSession session = context.getSession();
        UserModel user = context.getUser();

        // if link was already open continue authentication
        if (Objects.equals(authSession.getAuthNote(EMAIL_LINK_VERIFIED), user.getEmail())) {
            context.success();
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String action = formData.getFirst("submitAction");

        // if the form was submitted with an action of `resend` resend the email
        // otherwise just show the same page
        if (action != null && action.equals("resend")) {
            sendVerifyEmail(session, context, user);
        } else {
            showEmailSentPage(context, user);
        }
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.getEmail() != null;
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // TODO Auto-generated method stub
    }

    private void sendVerifyEmail(KeycloakSession session, AuthenticationFlowContext context, UserModel user)
            throws UriBuilderException, IllegalArgumentException {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        RealmModel realm = session.getContext().getRealm();

        // use the same lifespan as other tokens by getting from realm configuration
        int validityInSecs = realm.getActionTokenGeneratedByUserLifespan(EmailLinkActionToken.TOKEN_TYPE);
        long expirationInMinutes = TimeUnit.SECONDS.toMinutes(validityInSecs);

        String link = buildEmailLink(session, context, user, validityInSecs);

        // event is used to achieve better observability over what happens in Keycloak
        EventBuilder event = getSendVerifyEmailEvent(context, user);

        Map<String, Object> attributes = getMessageAttributes(user, realm.getDisplayName(), link, expirationInMinutes);

        try {
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .setUser(user)
                    .setAuthenticationSession(authSession)
                    // hard-code some of the variables - we will return here later
                    .send("emailLinkSubject", "email-link-email.ftl", attributes);

            event.success();
        } catch (EmailException e) {
            logger.error("Failed to send verification email", e);
            event.error(Errors.EMAIL_SEND_FAILED);
        }

        showEmailSentPage(context, user);
    }

    /**
     * Generates an action token link by encoding `EmailLinkActionToken` with user
     * and session data
     */
    protected String buildEmailLink(KeycloakSession session, AuthenticationFlowContext context, UserModel user,
            int validityInSecs) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        RealmModel realm = session.getContext().getRealm();
        UriInfo uriInfo = session.getContext().getUri();

        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

        String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authSession).getEncodedId();
        EmailLinkActionToken token = new EmailLinkActionToken(user.getId(), absoluteExpirationInSecs,
                authSessionEncodedId, user.getEmail(), authSession.getClient().getClientId());
        UriBuilder builder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo),
                authSession.getClient().getClientId(), authSession.getTabId());
        String link = builder.build(realm.getName()).toString();

        return link;
    }

    /**
     * Creates a Map with context required to render email message
     */
    protected Map<String, Object> getMessageAttributes(UserModel user, String realmName, String link,
            long expirationInMinutes) {
        Map<String, Object> attributes = new HashMap<>();

        attributes.put("user", user);
        attributes.put("realmName", realmName);
        attributes.put("link", link);
        attributes.put("expirationInMinutes", expirationInMinutes);

        return attributes;
    }

    /**
     * Creates a builder for `SEND_VERIFY_EMAIL` event
     */
    protected EventBuilder getSendVerifyEmailEvent(AuthenticationFlowContext context, UserModel user) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        EventBuilder event = context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL)
                .user(user)
                .detail(Details.USERNAME, user.getUsername())
                .detail(Details.EMAIL, user.getEmail())
                .detail(Details.CODE_ID, authSession.getParentSession().getId())
                .removeDetail(Details.AUTH_METHOD)
                .removeDetail(Details.AUTH_TYPE);

        return event;
    }

    /**
     * Displays email link form
     */
    protected void showEmailSentPage(AuthenticationFlowContext context, UserModel user) {
        String accessCode = context.generateAccessCode();
        URI action = context.getActionUrl(accessCode);

        Response challenge = context.form()
                .setStatus(Response.Status.OK)
                .setActionUri(action)
                .setExecution(context.getExecution().getId())
                .createForm("email-link-form.ftl");

        context.forceChallenge(challenge);
    }

}

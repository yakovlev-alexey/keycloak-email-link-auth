package dev.yakovlev_alexey.keycloak.authentication.actiontoken.emaillink;

import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.authentication.actiontoken.*;
import org.keycloak.events.*;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

import dev.yakovlev_alexey.keycloak.authentication.authenticators.browser.EmailLinkAuthenticator;

import java.util.Collections;

import javax.ws.rs.core.Response;

public class EmailLinkActionTokenHandler extends AbstractActionTokenHandler<EmailLinkActionToken> {

    public EmailLinkActionTokenHandler() {
        super(
                EmailLinkActionToken.TOKEN_TYPE,
                EmailLinkActionToken.class,
                Messages.STALE_VERIFY_EMAIL_LINK,
                EventType.VERIFY_EMAIL,
                Errors.INVALID_TOKEN);
    }

    @Override
    public Predicate<? super EmailLinkActionToken>[] getVerifiers(
            ActionTokenContext<EmailLinkActionToken> tokenContext) {
        // this is different to VerifyEmailActionTokenHandler implementation because
        // since its implementation a helper was added
        return TokenUtils.predicates(verifyEmail(tokenContext));
    }

    @Override
    public Response handleToken(EmailLinkActionToken token, ActionTokenContext<EmailLinkActionToken> tokenContext) {
        AuthenticationSessionModel authSession = tokenContext.getAuthenticationSession();
        UserModel user = authSession.getAuthenticatedUser();
        KeycloakSession session = tokenContext.getSession();
        EventBuilder event = tokenContext.getEvent();
        RealmModel realm = tokenContext.getRealm();

        event.event(EventType.VERIFY_EMAIL)
                .detail(Details.EMAIL, user.getEmail())
                .success();

        // verify user email as we know it is valid as this entry point would never have
        // gotten here
        user.setEmailVerified(true);

        // fresh auth session means that the link was open in a different browser window
        // or device
        if (!tokenContext.isAuthenticationSessionFresh()) {
            // link was opened in the same browser session (session is not fresh) - save the
            // user a click and continue authentication in the new (current) tab
            // previous tab will be thrown away
            String nextAction = AuthenticationManager.nextRequiredAction(tokenContext.getSession(), authSession,
                    tokenContext.getRequest(), tokenContext.getEvent());
            return AuthenticationManager.redirectToRequiredActions(tokenContext.getSession(), tokenContext.getRealm(),
                    authSession, tokenContext.getUriInfo(), nextAction);
        }

        AuthenticationSessionCompoundId compoundId = AuthenticationSessionCompoundId
                .encoded(token.getCompoundAuthenticationSessionId());

        AuthenticationSessionManager asm = new AuthenticationSessionManager(session);
        asm.removeAuthenticationSession(realm, authSession, true);

        ClientModel originalClient = realm.getClientById(compoundId.getClientUUID());
        // find the original authentication session
        // (where the tab is waiting to confirm)
        authSession = asm.getAuthenticationSessionByIdAndClient(realm, compoundId.getRootSessionId(),
                originalClient, compoundId.getTabId());

        if (authSession != null) {
            authSession.setAuthNote(EmailLinkAuthenticator.EMAIL_LINK_VERIFIED, user.getEmail());
        } else {
            // if no session was found in the same instance it might still be in the same
            // cluster if you have multiple replicas of Keycloak
            session.authenticationSessions().updateNonlocalSessionAuthNotes(
                    compoundId,
                    Collections.singletonMap(EmailLinkAuthenticator.EMAIL_LINK_VERIFIED,
                            token.getEmail()));
        }

        // show success page
        return session.getProvider(LoginFormsProvider.class)
                .setAuthenticationSession(authSession)
                .setSuccess(Messages.EMAIL_VERIFIED, token.getEmail())
                .createInfoPage();
    }

    // we do not really want users to authenticate using the same link multiple
    // times
    @Override
    public boolean canUseTokenRepeatedly(EmailLinkActionToken token,
            ActionTokenContext<EmailLinkActionToken> tokenContext) {
        return false;
    }
}
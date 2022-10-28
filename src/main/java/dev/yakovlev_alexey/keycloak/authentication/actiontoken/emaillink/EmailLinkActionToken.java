package dev.yakovlev_alexey.keycloak.authentication.actiontoken.emaillink;

import org.keycloak.authentication.actiontoken.DefaultActionToken;

public class EmailLinkActionToken extends DefaultActionToken {
    public static final String TOKEN_TYPE = "email-link";

    public EmailLinkActionToken(String userId, int absoluteExpirationInSecs, String compoundAuthenticationSessionId,
            String email, String clientId) {
        super(userId, TOKEN_TYPE, absoluteExpirationInSecs, null, compoundAuthenticationSessionId);
        setEmail(email);
        this.issuedFor = clientId;
    }

    private EmailLinkActionToken() {
    }
}

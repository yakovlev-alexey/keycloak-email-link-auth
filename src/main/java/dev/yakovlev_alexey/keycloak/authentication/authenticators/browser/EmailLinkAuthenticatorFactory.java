package dev.yakovlev_alexey.keycloak.authentication.authenticators.browser;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class EmailLinkAuthenticatorFactory implements AuthenticatorFactory {
	public static final EmailLinkAuthenticator SINGLETON = new EmailLinkAuthenticator();

	@Override
	public String getId() {
		return "email-link-authenticator";
	}

	@Override
	public String getDisplayType() {
		return "Email Link Authentication";
	}

	@Override
	public String getHelpText() {
		return "Authenticates the user with a link sent to their email";
	}

	@Override
	public String getReferenceCategory() {
		return null;
	}

	@Override
	public boolean isConfigurable() {
		return false;
	}

	@Override
	public boolean isUserSetupAllowed() {
		return false;
	}

	@Override
	public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
		return new AuthenticationExecutionModel.Requirement[] {
				AuthenticationExecutionModel.Requirement.REQUIRED,
				AuthenticationExecutionModel.Requirement.DISABLED,
		};
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		return null;
	}

	@Override
	public void init(Config.Scope config) {
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public void close() {
	}

	@Override
	public Authenticator create(KeycloakSession session) {
		return SINGLETON;
	}

}

package dev.yakovlev_alexey.keycloak.emaillink;

import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

import static org.keycloak.provider.ProviderConfigProperty.*;

public final class ConfigurationProperties {
	public static final String EMAIL_TEMPLATE = "EMAIL_TEMPLATE";
	public static final String PAGE_TEMPLATE = "PAGE_TEMPLATE";

	public static final String RESEND_ACTION = "RESEND_ACTION";

	public static final List<ProviderConfigProperty> PROPERTIES = Arrays.asList(
			new ProviderConfigProperty(EMAIL_TEMPLATE,
					"FTL email template name",
					"Will be used as the template for emails with the link",
					STRING_TYPE, "email-link-email.ftl"),
			new ProviderConfigProperty(PAGE_TEMPLATE,
					"FTL page template name",
					"Will be used as the template for email link page",
					STRING_TYPE, "email-link-form.ftl"),
			new ProviderConfigProperty(RESEND_ACTION,
					"Resend Email Link action",
					"Action which corresponds to user manually asking to resend email with link",
					STRING_TYPE, "resend"));

	private ConfigurationProperties() {
	}
}

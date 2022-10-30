<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("emailLinkTitle")}
    <#elseif section = "form">
        <form id="kc-register-form" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="submitAction" id="confirm" value="confirm">${msg("emailLinkCheck")}</button>
                <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="submitAction" id="resend" value="resend">${msg("emailLinkResend")}</button>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>

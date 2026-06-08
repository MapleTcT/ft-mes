package com.supcon.supfusion.authkeycloak;


import com.supcon.supfusion.authkeycloak.constant.KeyCloakErrorEnum;
import com.supcon.supfusion.authkeycloak.util.ResponseUtil;
import lombok.extern.jbosslog.JBossLog;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.directgrant.AbstractDirectGrantAuthenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.List;


@JBossLog
public class ValidateSimulatedLogin extends AbstractDirectGrantAuthenticator {
    public static final String PROVIDER_ID = "grant-simulated-login";

    public void authenticate(AuthenticationFlowContext context) {
        HttpRequest httpRequest = context.getHttpRequest();
        MultivaluedMap<String, String> formParams = httpRequest.getDecodedFormParameters();
        String username = formParams.getFirst("username");
        String companyId = formParams.getFirst("companyId");
        if (StringUtils.isBlank(username)) {
            ResponseUtil.sendTokenResponse(context, KeyCloakErrorEnum.GET_USERNAME_FAILED);
        }
        if (StringUtils.isBlank(companyId) || !StringUtils.isNumeric(companyId)) {
            ResponseUtil.sendTokenResponse(context, KeyCloakErrorEnum.GET_COMPANY_ID_FAILED);
        }
        log.info("Validate simulated login, username is " + username);
        UserModel userModel = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);
        if (userModel == null) {
            ResponseUtil.sendTokenResponse(context, KeyCloakErrorEnum.USER_NOT_FOUND);
        }
        context.setUser(userModel);
        context.success();
    }

    public boolean requiresUser() {
        return false;
    }

    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        //nothing to do
    }

    public boolean isUserSetupAllowed() {
        return false;
    }

    public String getDisplayType() {
        return "simulated-login";
    }

    public String getReferenceCategory() {
        return null;
    }

    public boolean isConfigurable() {
        return false;
    }

    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    public String getHelpText() {
        return "Simulated Login without password authentication";
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    public String getId() {
        return PROVIDER_ID;
    }
}

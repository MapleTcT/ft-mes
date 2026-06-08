package com.supcon.supfusion.authkeycloak;

import com.supcon.supfusion.authkeycloak.constant.KeyCloakConstants;
import com.supcon.supfusion.authkeycloak.constant.KeyCloakErrorEnum;
import com.supcon.supfusion.authkeycloak.util.ResponseUtil;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.directgrant.AbstractDirectGrantAuthenticator;
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import javax.ws.rs.core.MultivaluedMap;
import java.util.LinkedList;
import java.util.List;

public class ValidatePassword extends AbstractDirectGrantAuthenticator {
    public static final String PROVIDER_ID = "lfy-grant-validate-password";

    public void authenticate(AuthenticationFlowContext context) {
        String password = this.retrievePassword(context);
        if (StringUtils.isEmpty(password)) {
            context.attempted();
            return;
        }
        boolean valid = context.getSession().userCredentialManager().isValid(context.getRealm(), context.getUser(), new CredentialInput[]{UserCredentialModel.password(password)});
        if (!valid) {
            ResponseUtil.sendTokenResponse(context, KeyCloakErrorEnum.INVALID_USER_CREDENTIALS);
            return;
        }
        context.success();
    }

    public boolean requiresUser() {
        return true;
    }

    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // nothing to do
    }

    public boolean isUserSetupAllowed() {
        return false;
    }

    public String getDisplayType() {
        return "lfy-Password";
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
        return "Validates the password supplied as a 'password' form parameter in direct grant request";
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return new LinkedList();
    }

    public String getId() {
        return PROVIDER_ID;
    }

    protected String retrievePassword(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
        return inputData.getFirst(KeyCloakConstants.PASSWORD);
    }
}

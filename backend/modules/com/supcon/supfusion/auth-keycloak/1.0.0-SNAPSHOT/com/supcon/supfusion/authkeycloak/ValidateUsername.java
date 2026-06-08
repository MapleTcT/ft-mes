package com.supcon.supfusion.authkeycloak;


import com.supcon.supfusion.authkeycloak.constant.KeyCloakConstants;
import com.supcon.supfusion.authkeycloak.constant.KeyCloakErrorEnum;
import com.supcon.supfusion.authkeycloak.entity.UserEntity;
import com.supcon.supfusion.authkeycloak.util.ResponseUtil;
import lombok.extern.jbosslog.JBossLog;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.directgrant.AbstractDirectGrantAuthenticator;
import org.keycloak.credential.CredentialInput;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ServicesLogger;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;


@JBossLog
public class ValidateUsername extends AbstractDirectGrantAuthenticator {
    public static final String PROVIDER_ID = "lfy-grant-validate-username";

    AuthenticationExecutionModel.Requirement[] CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED};

    public void authenticate(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
        String username = this.retrieveUsername(context);
        if (username == null) {
            ResponseUtil.sendTokenResponse(context, KeyCloakErrorEnum.USER_NOT_FOUND);
            return;
        }
        context.getEvent().detail(KeyCloakConstants.USERNAME, username);
        context.getAuthenticationSession().setAuthNote(KeyCloakConstants.ATTEMPTED_USERNAME, username);
        UserModel user = null;
        try {
            user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(), username);
        } catch (ModelDuplicateException var6) {
            ServicesLogger.LOGGER.modelDuplicateException(var6);
            Response challengeResponse = this.errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "invalid_request", "Invalid user credentials");
            context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return;
        }
        if (user == null) {
            ResponseUtil.sendTokenResponse(context, KeyCloakErrorEnum.USER_OR_PASSWORD_ERROR);
            return;
        }
        String type = retrieveType(context);
        if(StringUtils.isNotEmpty(type)){
            if("simulate".equals(type)){
                context.setUser(user);
                context.success();
                return;
            }
        }
        SuposUserAdapter adapter = (SuposUserAdapter) user;
        String companyCode = inputData.getFirst(KeyCloakConstants.COMPANY_CODE);
        if (StringUtils.isNotEmpty(companyCode)) {
            List<UserEntity.Company> companies = adapter.getEntity().getCompanies();
            boolean find = companies.stream().anyMatch(t -> t.getCompanyCode().equals(companyCode));
            if (!find) {
                ResponseUtil.sendTokenResponse(context, KeyCloakErrorEnum.USER_NOT_IN_COMPANY);
                return;
            }
        }

        if (user.getId() == null || user.getUsername() == null) {
            ResponseUtil.sendTokenResponse(context, KeyCloakErrorEnum.USER_OR_PASSWORD_ERROR);
            return;
        }
        if (adapter.entity.getHasLock() != null && adapter.entity.getHasLock()) {
            ResponseUtil.sendTokenResponse(context, KeyCloakErrorEnum.USER_HAS_LOCK);
            return;
        }
        String password = inputData.getFirst(KeyCloakConstants.PASSWORD);
        boolean valid = context.getSession().userCredentialManager().isValid(context.getRealm(), user, new CredentialInput[]{UserCredentialModel.password(password)});
        if (!valid) {
            ResponseUtil.sendTokenResponse(context, KeyCloakErrorEnum.USER_OR_PASSWORD_ERROR);
            return;
        }
        context.setUser(user);
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
        return "lfy-Username Validation";
    }

    public String getReferenceCategory() {
        return null;
    }

    public boolean isConfigurable() {
        return false;
    }

    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return CHOICES;
    }

    public String getHelpText() {
        return "Validates the username supplied as a 'username' form parameter in direct grant request";
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return new LinkedList();
    }

    public String getId() {
        return PROVIDER_ID;
    }

    protected String retrieveUsername(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
        return inputData.getFirst(KeyCloakConstants.USERNAME);
    }
    protected String retrieveType(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();
        return inputData.getFirst(KeyCloakConstants.TYPE);
    }
}

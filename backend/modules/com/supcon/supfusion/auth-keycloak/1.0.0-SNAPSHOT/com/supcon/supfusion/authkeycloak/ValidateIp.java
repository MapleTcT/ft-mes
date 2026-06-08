package com.supcon.supfusion.authkeycloak;


import com.supcon.supfusion.authkeycloak.constant.KeyCloakConstants;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.directgrant.AbstractDirectGrantAuthenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.LinkedList;
import java.util.List;


@JBossLog
public class ValidateIp extends AbstractDirectGrantAuthenticator {
    public static final String PROVIDER_ID = "lfy-grant-validate-ip";

    public void authenticate(AuthenticationFlowContext context) {
//        String ip = retrieveCurrentIp(context);
//        if (ip == null) {
//            ResponseUtil.sendTokenResponse(context, KeyCloakErrorEnum.GET_IP_FAILED);
//            return;
//        }
//        Long companyId = retrieveCompanyId(context);
//        boolean isLegal = false;
//        try {
//            if (companyId != null) {
//                String group = PropertiesConfigure.getProperties().getProperty("nacos.group", "DEFAULT_GROUP");
//                Instance auth = Registry.getNamingService().selectOneHealthyInstance(KeyCloakConstants.AUTH, group);
//                String tenantId = retrieveTenantId(context);
//                String url = String.format("http://%s:%d%s", auth.getIp(), auth.getPort(), KeyCloakConstants.USER_INFO_URI);
//                HashMap<String, String> header = new HashMap<>();
//                header.put(KeyCloakConstants.X_TENANT_IP, tenantId);
//                header.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
//                HashMap<String, String> param = new HashMap<>();
//                param.put("ip", ip);
//                if (companyId != null) {
//                    param.put("companyId", companyId.toString());
//                }
//
//                isLegal = HttpClient.verifyIp(auth.getIp(), auth.getPort(), KeyCloakConstants.VERIFY_IP_URI, ip, companyId, tenantId);
//            }
//        } catch (NacosException | IOException e) {
//            log.error("Connect auth server failed!", e);
//        }
//        if (!isLegal) {
//            ResponseUtil.sendTokenResponse(context, KeyCloakErrorEnum.ACCESS_IP_FORBIDDEN);
//            return;
//        }
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
        return "lfy-ip Validation";
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
        return "Validates the ip supplied as a 'ip' form parameter in direct grant request";
    }

    public List<ProviderConfigProperty> getConfigProperties() {
        return new LinkedList();
    }

    public String getId() {
        return PROVIDER_ID;
    }

    protected Long retrieveCompanyId(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user instanceof SuposUserAdapter) {
            SuposUserAdapter adapter = (SuposUserAdapter) user;
            return adapter.getEntity().getCompanies().get(0).getCompanyId();
        }
        return null;
    }

    protected String retrieveTenantId(AuthenticationFlowContext context) {
        return context.getHttpRequest().getHttpHeaders().getHeaderString(KeyCloakConstants.X_TENANT_IP);
    }

    private String retrieveCurrentIp(AuthenticationFlowContext context) {
        return context.getHttpRequest().getHttpHeaders().getHeaderString(KeyCloakConstants.X_REAL_IP);
    }
}

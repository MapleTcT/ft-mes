package com.supcon.supfusion.auth.manager.listener;

import com.supcon.supfusion.auth.manager.KeycliandAdminClient;
import com.supcon.supfusion.framework.cloud.common.events.TenantAddEvent;
import com.supcon.supfusion.framework.cloud.common.events.TenantDestroyEvent;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TenantEventListListener implements ApplicationListener<ApplicationEvent> {

    @Resource
    private KeycliandAdminClient keycliandAdminClient;
    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof TenantAddEvent) {
            TenantAddEvent addEvent = (TenantAddEvent) applicationEvent;
            log.info("tenantId=====>"+addEvent.getTenant().getId());
            try{
                List<RealmRepresentation> all = keycliandAdminClient.getKeycloak().realms().findAll();
                boolean exist = all.stream().anyMatch(t -> t.getRealm().equals(addEvent.getTenant().getId()));
                if(!exist){
                    keycliandAdminClient.createRealm(addEvent.getTenant().getId());
                }else {
                    List<AuthenticationFlowRepresentation> flows = keycliandAdminClient.getKeycloak().realm(addEvent.getTenant().getId()).flows().getFlows();
                    boolean grant = flows.stream().anyMatch(flow -> flow.getAlias().equals("lfy grant"));
                    if (!grant) {
                        AuthenticationFlowRepresentation authenticationFlowRepresentation = authenticationFlowRepresentation(addEvent.getTenant().getId());
                        keycliandAdminClient.getKeycloak().realm(addEvent.getTenant().getId()).flows().createFlow(authenticationFlowRepresentation);
                        Map<String, String> map = new HashMap<>();
                        map.put("provider", "lfy-grant-validate-username");
                        keycliandAdminClient.getKeycloak().realm(addEvent.getTenant().getId()).flows().addExecution("lfy grant", map);
                    }
                    RealmRepresentation realmRepresentation1 = keycliandAdminClient.getKeycloak().realm(addEvent.getTenant().getId()).partialExport(false, true);
                    if (!"lfy grant".equals(realmRepresentation1.getDirectGrantFlow())) {
                        realmRepresentation1.setDirectGrantFlow("lfy grant");
                        keycliandAdminClient.getKeycloak().realm(addEvent.getTenant().getId()).update(realmRepresentation1);
                    }
                }
            }catch (Exception e){
                throw e;
            }
        } else if (applicationEvent instanceof TenantDestroyEvent) {
            TenantDestroyEvent destroyEvent = (TenantDestroyEvent) applicationEvent;
            keycliandAdminClient.destroyRealm(destroyEvent.getTenant().getId());
        }
    }

    private AuthenticationFlowRepresentation authenticationFlowRepresentation(String realm) {
        AuthenticationFlowRepresentation authenticationFlowRepresentation = new AuthenticationFlowRepresentation();
        authenticationFlowRepresentation.setAlias("lfy grant");
        authenticationFlowRepresentation.setDescription("OpenID Connect Resource Owner Grant");
        authenticationFlowRepresentation.setBuiltIn(false);
        authenticationFlowRepresentation.setTopLevel(true);
        authenticationFlowRepresentation.setProviderId("basic-flow");
        return authenticationFlowRepresentation;
    }

}

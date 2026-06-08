package com.supcon.supfusion.auth.service.rpc;

import com.supcon.supfusion.auth.api.AuthenticationApiService;
import com.supcon.supfusion.auth.api.dto.SimulatedLoginTokenDTO;
import com.supcon.supfusion.auth.manager.KeyCloakServiceAdapter;
import com.supcon.supfusion.auth.manager.bo.AuthorizationDTO;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author caokele
 */
@ServiceApiService
public class AuthenticationApiServiceImpl extends BaseController implements AuthenticationApiService {
    @Autowired
    private KeyCloakServiceAdapter keyCloakServiceAdapter;

    @Value("${keycloak.simulated-client:ms-simulated-login}")
    private String simulatedClient;

    @Override
    public SimulatedLoginTokenDTO simulatedLoginToken(String tenantId, String username, Long companyId) {
        AuthorizationDTO authorizationDTO = keyCloakServiceAdapter.simulatedLoginToken(tenantId, "pc_"+tenantId, username, companyId);
        if (authorizationDTO == null || authorizationDTO.getAccessToken() == null) {
            return null;
        }
        SimulatedLoginTokenDTO simulatedLoginTokenDTO = new SimulatedLoginTokenDTO();
        simulatedLoginTokenDTO.setTokenType(authorizationDTO.getTokenType());
        simulatedLoginTokenDTO.setAccessToken(authorizationDTO.getAccessToken());
        simulatedLoginTokenDTO.setExpiresIn(authorizationDTO.getExpiresIn());
        return simulatedLoginTokenDTO;
    }
}

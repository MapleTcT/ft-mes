package com.supcon.supfusion.auth.manager.Impl;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.auth.manager.KeyCloakServiceAdapter;
import com.supcon.supfusion.auth.manager.bo.AuthorizationDTO;
import com.supcon.supfusion.auth.manager.feign.client.TokenClient;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author caokele
 */
@Slf4j
@Service
public class KeyCloakServiceAdapterImpl implements KeyCloakServiceAdapter {
    @Autowired
    private TokenClient tokenClient;

    @Override
    public void logout(String clientId, String accessToken, String refreshToken) {
        HashMap<String, String> map = new HashMap<>();
        map.put("client_id", clientId);
        map.put("refresh_token", refreshToken);
        tokenClient.keycloakLogout(RpcContext.getContext().getTenantId(), map, accessToken);
    }

    @Override
    public AuthorizationDTO simulatedLoginToken(String tenantId, String clientId, String username, Long companyId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("client_id", clientId);
        map.put("username", username);
        map.put("companyId", companyId);
        map.put("grant_type", "password");
        map.put("type","simulate");
        String responseJson = tokenClient.keycloakLogin(RpcContext.getContext().getTenantId(), map);
        log.info("json parse:" + responseJson);
        AuthorizationDTO authorizationDTO = JSON.parseObject(responseJson, AuthorizationDTO.class);
        return authorizationDTO;
    }
}

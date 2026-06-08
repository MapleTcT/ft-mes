package com.supcon.supfusion.auth.manager;

import com.supcon.supfusion.auth.manager.bo.AuthorizationDTO;

/**
 * @author caokele
 */
public interface KeyCloakServiceAdapter {
    /**
     * 登出
     */
    void logout(String clientId, String accessToken, String refreshToken);

    /**
     * 模拟登录获取token
     */
    AuthorizationDTO simulatedLoginToken(String tenantId, String clientId, String username, Long companyId);
}

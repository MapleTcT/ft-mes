package com.supcon.supfusion.auth.api;

import com.supcon.supfusion.auth.api.dto.SimulatedLoginTokenDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 认证接口
 *
 * @author caokele
 */
@FeignClient(name = "auth", contextId = "authentication")
public interface AuthenticationApiService {

    /**
     * 模拟认证
     *
     * @param tenantId  租户ID
     * @param username  用户名
     * @param companyId 企业ID
     * @return 模拟登录令牌
     */
    @GetMapping("/service-api/auth/v1/authentication/simulated-login")
    SimulatedLoginTokenDTO simulatedLoginToken(@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId, @RequestParam("username") String username, @RequestParam("companyId") Long companyId);
}

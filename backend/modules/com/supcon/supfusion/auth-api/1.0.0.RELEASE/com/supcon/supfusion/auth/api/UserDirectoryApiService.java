package com.supcon.supfusion.auth.api;

import com.supcon.supfusion.auth.api.dto.UserDirectoryAuthenticateDTO;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author caokele
 */
@FeignClient(name = "auth", contextId = "user-directory")
public interface UserDirectoryApiService {

    String API_PREFIX = "/service-api/auth";

    /**
     * 认证LDAP用户
     */
    @PostMapping(API_PREFIX + "/v1/user-directories/authenticate")
    void authenticateUserDirectory(@RequestBody UserDirectoryAuthenticateDTO userDirectoryAuthenticateDTO);
}

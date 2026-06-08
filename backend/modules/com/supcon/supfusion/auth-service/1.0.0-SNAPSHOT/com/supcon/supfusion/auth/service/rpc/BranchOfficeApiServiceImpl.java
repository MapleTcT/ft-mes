package com.supcon.supfusion.auth.service.rpc;

import com.supcon.supfusion.auth.api.BranchOfficeApiService;
import com.supcon.supfusion.auth.common.utils.Base64Util;
import com.supcon.supfusion.auth.service.config.BranchOfficeProperties;
import com.supcon.supfusion.auth.service.config.HeadOfficeProperties;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author caokele
 */
@ServiceApiService
public class BranchOfficeApiServiceImpl extends BaseController implements BranchOfficeApiService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BranchOfficeProperties branchOfficeProperties;

    @Autowired
    private HeadOfficeProperties headOfficeProperties;

    @Override
    public String authorizeUrl(String originUrl, String hostUrl) {
        StringBuilder authUrlBuilder = new StringBuilder();
        String state = UUID.randomUUID().toString().replace("-", "");
        String decodeOriginUrl = Base64Util.decode(originUrl);
        String decodeHostUrl = Base64Util.decode(hostUrl);
        stringRedisTemplate.opsForValue().set(state, decodeOriginUrl, 10, TimeUnit.MINUTES);
        String redirectUri = decodeHostUrl + branchOfficeProperties.getRedirectUri();
        authUrlBuilder.append(headOfficeProperties.getAddress()).append(headOfficeProperties.getAuthorize())
                .append("?client_id=").append(branchOfficeProperties.getClientId())
                .append("&redirect_uri=").append(Base64Util.encode(redirectUri))
                .append("&state=").append(state)
                .append("&grant_type=code");
        String authUrl = authUrlBuilder.toString();
        return authUrl;
    }
}

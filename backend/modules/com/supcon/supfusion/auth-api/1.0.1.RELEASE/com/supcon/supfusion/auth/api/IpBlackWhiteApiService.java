package com.supcon.supfusion.auth.api;

import com.supcon.supfusion.auth.api.dto.IpBlackWhiteDTO;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

/**
 * @author caokele
 */
@FeignClient(name = "auth", contextId = "ip-black-white")
public interface IpBlackWhiteApiService {

    String API_PREFIX = "/service-api/auth";

    /**
     * 校验ip
     */
    @PostMapping(API_PREFIX + "/v1/ip-black-white/verify")
    Result<Boolean> verifyIp(@Valid @RequestBody IpBlackWhiteDTO ipBlackWhiteDTO);
}

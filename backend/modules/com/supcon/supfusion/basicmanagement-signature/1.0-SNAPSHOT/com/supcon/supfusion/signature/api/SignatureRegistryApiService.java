package com.supcon.supfusion.signature.api;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author: fukun
 * @Date: 2020/9/24 13:28
 * @since
 */
@FeignClient(name = "signature", contextId = "signature")
public interface SignatureRegistryApiService {

}

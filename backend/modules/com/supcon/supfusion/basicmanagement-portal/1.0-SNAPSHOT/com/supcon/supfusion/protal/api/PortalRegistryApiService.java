package com.supcon.supfusion.protal.api;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author: fukun
 * @Date: 2020/9/24 13:28
 * @since
 */
@FeignClient(name = "portal", contextId = "portal")
public interface PortalRegistryApiService {

}

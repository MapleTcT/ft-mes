package com.supcon.supfusion.theme.api;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author: fukun
 * @Date: 2020/9/24 13:28
 * @since
 */
@FeignClient(name = "theme", contextId = "theme")
public interface ThemeRegistryApiService {

}

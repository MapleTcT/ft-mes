package com.supcon.supfusion.printer.api;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author: fukun
 * @Date: 2020/9/24 13:28
 * @since
 */
@FeignClient(name = "printer", contextId = "printer")
public interface PrinterRegistryApiService {

}

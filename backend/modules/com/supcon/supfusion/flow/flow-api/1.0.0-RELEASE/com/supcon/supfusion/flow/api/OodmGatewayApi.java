/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;

/**
 * @author: zhuangmh
 * @date: 2020年9月3日 下午3:49:47
 */
@FeignClient(name = "supngin-oodm-gateway", url = "dt.56.dev.supos.net")
@ServiceApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "supos/oodm/")
public interface OodmGatewayApi {
    /**
     * 
     * @param templateNamespace 对象模板命名空间
     * @param templateName 对象模板名称
     * @param instanceName 对象实例名称
     * @param serviceNamespace 服务命名空间
     * @param serviceName 服务名
     */
    @PostMapping("v2/template/{templateNamespace}/{templateName}/instance/{instanceName}/service/{serviceNamespace}/{serviceName}")
    void executeService(@PathVariable("templateNamespace") String templateNamespace, @PathVariable("templateName") String templateName,
            @PathVariable("instanceName") String instanceName, @PathVariable("serviceNamespace") String serviceNamespace,
            @PathVariable("serviceName") String serviceName);
}

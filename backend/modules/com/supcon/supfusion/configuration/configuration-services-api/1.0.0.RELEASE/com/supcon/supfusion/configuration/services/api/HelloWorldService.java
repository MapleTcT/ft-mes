package com.supcon.supfusion.configuration.services.api;

import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.configuration.services.api.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * RPC内部接口
 * <ul>
 *     <li>FeignClient的值必须和spring.application.name的值一致</li>
 *     <li>内部接口统一梠式为：/internal-api/{spring.application.name}/{version}/**</li>
 * </ul>
 *
 * @author tomcat - <huangjianbo@supos.com>
 * @date 20-4-9 下午2:14
 */
//@FeignClient(name = "demo-provider")
//@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "demo-provider")
public interface HelloWorldService {

    /**
     * HELLO
     *
     * @param user
     * @return
     */
    @GetMapping(value = "/hello")
    @ResponseBody
    Result<UserDTO> helloWorld(@RequestParam("user") String user);
}

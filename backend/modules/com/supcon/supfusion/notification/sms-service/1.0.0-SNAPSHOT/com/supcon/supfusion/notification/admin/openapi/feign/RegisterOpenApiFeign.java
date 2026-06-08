package com.supcon.supfusion.notification.admin.openapi.feign;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.admin.openapi.vo.ProtocolConfigVO;
import com.supcon.supfusion.notification.admin.openapi.vo.RegisterResponseVO;
import com.supcon.supfusion.notification.sms.service.runner.ClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * RegisterOpenApiFeign
 *
 * @author OpenAI
 * @date 2021-04-12 18:25:09
 */
@FeignClient(name = "notification-admin", url = "${supfusion.supos.supos-host}", configuration = ClientConfiguration.class)
public interface RegisterOpenApiFeign {


   /**
    * @return Result<RegisterResponseVO>
    * @author OpenAI
    * @date 2021-04-12 18:25:09
    */
   @PostMapping(value = "/open-api/notification-admin/v2/register")
   Result<RegisterResponseVO> register(@RequestBody ProtocolConfigVO arg0);


}

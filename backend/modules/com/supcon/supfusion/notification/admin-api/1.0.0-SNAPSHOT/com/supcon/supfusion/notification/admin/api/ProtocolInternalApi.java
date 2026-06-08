package com.supcon.supfusion.notification.admin.api;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.notification.admin.api.dto.NoticeProtocolDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 通知中心协议注册器
 *
 * @param
 * @return
 */
@FeignClient(name = "notification-admin", contextId = "ProtocolInternalApi")
@Api(tags = {"内部协议注册接口", "service-api"})
public interface ProtocolInternalApi {

    /**
     * 获取所有协议类型
     *
     * @return
     */
    @GetMapping(value = HttpConstants.URL_SERVICEAPI + "/notification-admin/v1/protocols")
    @ResponseBody
    @ApiOperation("获取所有协议类型")
    ListResult<NoticeProtocolDTO> protocols();

}

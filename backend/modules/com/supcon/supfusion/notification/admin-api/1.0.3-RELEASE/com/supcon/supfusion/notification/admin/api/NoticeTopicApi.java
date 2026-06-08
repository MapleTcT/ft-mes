package com.supcon.supfusion.notification.admin.api;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.admin.api.dto.NoticeTemplateDTO;
import com.supcon.supfusion.notification.admin.api.dto.NoticeTopicDTO;
import com.supcon.supfusion.notification.admin.api.dto.ProtocolDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 模板新增接口
 *
 * @param
 * @return
 */
@FeignClient(name = "notification-admin", contextId = "NoticeTopicApi")
@Api(tags = {"主题接口", "internal-api"})
public interface NoticeTopicApi {


    /**
     * 主题新增接口
     *
     * @param noticeTopicDTO
     * @return
     */
    @PostMapping(value = HttpConstants.URL_SERVICEAPI + "/notification-admin/v1/topic")
    @ResponseBody
    @ApiOperation("主题新增接口")
    void topicAddORUpdate(@RequestBody @Valid @ApiParam(name = "主题新增", required = true) NoticeTopicDTO noticeTopicDTO);


    /**
     * 获取主题绑定协议
     *
     * @param topicCode
     * @return
     */
    @GetMapping(value = HttpConstants.URL_SERVICEAPI + "/notification-admin/v1/topic/protocols")
    @ResponseBody
    @ApiOperation("获取主题绑定协议")
    List<ProtocolDTO> getTopicProtocols(@RequestParam("topicCode") @NotEmpty(message = "主题编码不能为空") @ApiParam(name = "主题编码", required = true) String topicCode);

}

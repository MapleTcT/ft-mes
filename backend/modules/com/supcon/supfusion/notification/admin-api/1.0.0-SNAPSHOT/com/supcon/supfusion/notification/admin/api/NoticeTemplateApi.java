package com.supcon.supfusion.notification.admin.api;

import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.notification.admin.api.dto.NoticeTemplateDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;

/**
 * 模板新增接口
 *
 * @param
 * @return
 */
@FeignClient(name = "notification-admin", contextId = "NoticeTemplateApi")
@Api(tags = {"模板接口", "internal-api"})
public interface NoticeTemplateApi {


    /**
     * 模板新增接口
     *
     * @param noticeTemplateDTO
     * @return
     */
    @PostMapping(value = HttpConstants.URL_SERVICEAPI + "/notification-admin/v1/template")
    @ResponseBody
    @ApiOperation("模板新增接口")
    void addTemplateORUpdate(@RequestBody @Valid @ApiParam(name = "模板新增", required = true) NoticeTemplateDTO noticeTemplateDTO);

}

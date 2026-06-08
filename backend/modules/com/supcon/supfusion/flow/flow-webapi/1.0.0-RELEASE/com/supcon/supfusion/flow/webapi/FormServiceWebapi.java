/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.webapi;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.supcon.supfusion.flow.common.vo.webapi.FormRequestVO;
import com.supcon.supfusion.flow.taskcenter.service.FormService;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.controller.BaseController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * @author: zhuangmh
 * @date: 2020年9月2日 上午10:04:48
 */
@RestController
@InternalApi(path = HttpConstants.URL_SPLITER + "inter-api" + HttpConstants.URL_SPLITER + "flow-service")
@Api(value = "表单相关文档", tags = "表单")
public class FormServiceWebapi extends BaseController {
    
    @Autowired
    private FormService formService;
    
    /**
     * 保存表单数据
     * @param formRequest
     * @return
     */
    @PostMapping(value = "/v1/form")
    @ResponseBody
    @ApiOperation(value="保存表单数据V1接口", httpMethod="POST")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void saveForm(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) FormRequestVO formRequest) {
        formService.saveForm(Long.parseLong(formRequest.getTaskId()), formRequest.getFormData());
    }
    
    /**
     * 重置表单数据
     */
    @DeleteMapping(value = "/v1/form")
    @ResponseBody
    @ApiOperation(value="重置表单数据V1接口", httpMethod="DELETE")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "taskId", value = "taskId", required = true, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void resetForm(@RequestParam("taskId") String taskId) {
        formService.resetForm(Long.parseLong(taskId));
    }
    
}

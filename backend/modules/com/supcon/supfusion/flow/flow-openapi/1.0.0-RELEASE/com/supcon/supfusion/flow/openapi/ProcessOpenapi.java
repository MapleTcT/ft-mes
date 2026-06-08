/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.openapi;

import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import com.supcon.supfusion.flow.common.vo.openapi.*;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.supcon.supfusion.flow.common.dto.DiagramDTO;
import com.supcon.supfusion.flow.common.dto.SimpleTaskDTO;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.enumeration.ProcessStatusEnum;
import com.supcon.supfusion.flow.common.exception.IllegalParameterException;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.taskcenter.service.OpenapiService;
import com.supcon.supfusion.flow.taskcenter.service.ProcessService;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.controller.BaseController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * @author: zhuangmh
 * @date: 2020年11月6日 下午3:50:11
 */
@RestController
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "p/workflow")
@Api(value = "对外开放接口", tags = "Open-api")
@Validated
public class ProcessOpenapi extends BaseController {
    
    @Autowired
    private OpenapiService openapiService;
    @Autowired
    private ProcessService processService;
    
    
    @GetMapping("/v2/processes")
    @ResponseBody
    @ApiOperation(value="查询用户的流程列表", httpMethod="GET", nickname = "queryProcess")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "appId", value = "APP ID", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "username", value = "用户名", required = true, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "status", value = "88-进行中(默认), 77-暂停, 99-作废, 66-已完成", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "pageSize", value = "当前页号-默认10", required = false, dataType = "String", paramType = "query")
    })
    public PageResult<ProcessVO> query(HttpServletRequest request, @Nullable @RequestParam("appId") String appId
            , @Nullable @RequestParam("username") String username, @Validated @Pattern(regexp = "\\d+", message = "非法参数status") @Nullable @RequestParam("status") String status
            , @Nullable @RequestParam("current") String page, @Nullable @RequestParam("pageSize") String size) {
        int current = page == null ? Constants.DEFAULT_PAGE : Integer.parseInt(page);
        int pageSize = size == null ? Constants.DEFAULT_PAGE_SIZE : Integer.parseInt(size);
        if (StringUtils.isEmpty(username)) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        if (pageSize > Constants.MAX_PAGE_SIZE) {
            throw new IllegalParameterException(FlowErrorEnum.PAGE_MAXIMUM_LIMIT_ERROR);
        }
        Long userId = UserContext.getUserContext().getUserId();
        int statusInt = status == null ? 0 : Integer.parseInt(status);
        return openapiService.queryProcess(appId, userId, statusInt, new Pagination(0, pageSize, current));
    }
    
    @GetMapping("/v2/processes/{processId}")
    @ResponseBody
    @ApiOperation(value="查询流程详情", httpMethod="GET", nickname = "getDetail")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "processId", value = "流程ID", required = true, dataType = "String", paramType = "path"),
        @ApiImplicitParam(name = "username", value = "用户名", required = true, dataType = "String", paramType = "query")
    })
    public ProcessVO getDetail(HttpServletRequest request, @PathVariable("processId") String processId, @Nullable @RequestParam("username") String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        return openapiService.getProcess(processId);
    }
    
    @GetMapping(value = "/v2/processes/{processKey}/startInfo")
    @ResponseBody
    @ApiOperation(value="查询流程启动详情", httpMethod="GET", nickname = "getStartInfo")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "appId", value = "app id", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "processKey", value = "流程编号", required = true, dataType = "String", paramType = "path")
    })
    public ProcessStartInfoVO getStartInfo(HttpServletRequest request, @Nullable @RequestParam("appId") String appId, @PathVariable("processKey") String processKey) throws DocumentException {
        return openapiService.getProcessStartInfo(appId, processKey);
    }
    
    /**
     * 查询流程日志
     * @return
     */
    @GetMapping(value = "/v2/processes/{processId}/logs")
    @ResponseBody
    @ApiOperation(value="查询流程日志", httpMethod="GET", nickname = "queryLog")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "processId", value = "流程ID", required = true, dataType = "String", paramType = "path")
    })
    public List<ProcessLogVO> queryLog(HttpServletRequest request, @PathVariable("processId") String processId) {
        return openapiService.queryProcessLogs(processId);
    }
    
    /**
     * 发起流程
     * @param processRequest
     * @return 流程实例ID
     * @throws DocumentException 
     */
    @PostMapping(value = "/v2/processes")
    @ResponseBody
    @ApiOperation(value="发起流程", httpMethod="POST", nickname = "startProcess")
    public ProcessStartResponseVO startProcess(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) ProcessStartRequestVO processRequest) throws DocumentException {
        if (StringUtils.isEmpty(processRequest.getUsername())) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        if (StringUtils.isEmpty(processRequest.getProcessKey())) {
            throw new IllegalParameterException(FlowErrorEnum.PROCESS_KEY_NOT_EMPTY);
        }
        DiagramDTO diagramDto = new DiagramDTO(processRequest.getAppId(), processRequest.getProcessName(), processRequest.getProcessKey());
        String processId = openapiService.startProcess(diagramDto, processRequest.getFormData(),  processRequest.getComment()
                , processRequest.getAssigns(), processRequest.getAudit());
        return new ProcessStartResponseVO(processId, null);
    }
    
    
    @PutMapping(value = "/v2/processes")
    @ResponseBody
    @ApiOperation(value="保存流程", httpMethod="PUT", nickname = "save")
    public ProcessStartResponseVO save(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) ProcessSaveRequestVO processRequest) throws DocumentException {
        if (StringUtils.isEmpty(processRequest.getUsername())) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        if (StringUtils.isEmpty(processRequest.getProcessKey())) {
            throw new IllegalParameterException(FlowErrorEnum.PROCESS_KEY_NOT_EMPTY);
        }
        SimpleTaskDTO st = processService.startProcessBySave(processRequest.getAppId(), processRequest.getProcessKey(), processRequest.getFormData());
        return new ProcessStartResponseVO(st.getProcessId(), st.getTaskId());
    }
    
    /**
     * 流程作废
     * @return
     */
    @PostMapping("/v2/processes/{processId}/cancellation")
    @ResponseBody
    @ApiOperation(value="作废流程", httpMethod="POST", nickname = "cancel")
    public void cancel(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) ProcessCancelRequestVO cancelRequest,  @PathVariable("processId") String processId) {
        if (StringUtils.isEmpty(cancelRequest.getUsername())) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        processService.cancel(processId, false);
    }
    
    @GetMapping("/v2/processes/{processId}/urgeInfo")
    @ResponseBody
    @ApiOperation(value="查询催办信息", httpMethod="GET", nickname = "getUrgeInfo")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "processId", value = "流程ID", required = true, dataType = "String", paramType = "path")
    })
    public List<UrgeInfoVO> getUrgeInfo(HttpServletRequest request, @PathVariable("processId") String processId) {
        return openapiService.listUrgeInfo(processId);
    }
}

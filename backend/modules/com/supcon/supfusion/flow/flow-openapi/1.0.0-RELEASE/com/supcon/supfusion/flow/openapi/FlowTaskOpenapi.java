/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.openapi;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

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

import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.IllegalParameterException;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.vo.openapi.CompleteTaskListResponseVO;
import com.supcon.supfusion.flow.common.vo.openapi.CompletedTaskResponseVO;
import com.supcon.supfusion.flow.common.vo.openapi.EntrustRequestVO;
import com.supcon.supfusion.flow.common.vo.openapi.FormRequestVO;
import com.supcon.supfusion.flow.common.vo.openapi.PendingTaskListResponseVO;
import com.supcon.supfusion.flow.common.vo.openapi.PendingTaskResponseVO;
import com.supcon.supfusion.flow.common.vo.openapi.TaskRevokeRequestVO;
import com.supcon.supfusion.flow.common.vo.openapi.TaskSubmitRequestVO;
import com.supcon.supfusion.flow.taskcenter.service.FormService;
import com.supcon.supfusion.flow.taskcenter.service.OpenapiService;
import com.supcon.supfusion.flow.taskcenter.service.TaskCenterService;
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
public class FlowTaskOpenapi extends BaseController {
    
    @Autowired
    private OpenapiService openapiService;
    @Autowired
    private TaskCenterService taskCenterService;
    @Autowired
    private FormService formService;
    
    /**
     * 查询待办列表
     * @param username
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/v2/tasks/pending")
    @ResponseBody
    @ApiOperation(value="查询用户的待办列表", httpMethod="GET", nickname = "queryPending")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "appId", value = "APP ID", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "username", value = "用户名称", required = true, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "pageSize", value = "当前页号-默认10", required = false, dataType = "String", paramType = "query")
    })
    public PageResult<PendingTaskListResponseVO> queryPending(HttpServletRequest request
            , @Nullable @RequestParam("appId") String appId, @Nullable @RequestParam("username") String username
            , @Nullable @RequestParam("current") String page, @Nullable @RequestParam("pageSize") String size) {
        Long userId = UserContext.getUserContext().getUserId();
        int current = page == null ? Constants.DEFAULT_PAGE : Integer.parseInt(page);
        int pageSize = size == null ? Constants.DEFAULT_PAGE_SIZE : Integer.parseInt(size);
        if (StringUtils.isEmpty(username)) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        if (pageSize > Constants.MAX_PAGE_SIZE) {
            throw new IllegalParameterException(FlowErrorEnum.PAGE_MAXIMUM_LIMIT_ERROR);
        }
        return openapiService.queryPendingTask(appId, userId, new Pagination(0, pageSize, current));
    }
    
    /**
     * 查询待办列表
     * @param username
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/v2/tasks/completive")
    @ResponseBody
    @ApiOperation(value="查询用户的已办列表", httpMethod="GET", nickname = "queryCompletive")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "appId", value = "APP ID", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "username", value = "用户ID", required = true, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "pageSize", value = "当前页号-默认10", required = false, dataType = "String", paramType = "query")
    })
    public PageResult<CompleteTaskListResponseVO> queryCompletive(HttpServletRequest request
            , @Nullable @RequestParam("appId") String appId, @Nullable @RequestParam("username") String username
            , @Nullable @RequestParam("current") String page, @Nullable @RequestParam("pageSize") String size) {
        Long userId = UserContext.getUserContext().getUserId();
        int current = page == null ? Constants.DEFAULT_PAGE : Integer.parseInt(page);
        int pageSize = size == null ? Constants.DEFAULT_PAGE_SIZE : Integer.parseInt(size);
        if (StringUtils.isEmpty(username)) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        if (pageSize > Constants.MAX_PAGE_SIZE) {
            throw new IllegalParameterException(FlowErrorEnum.PAGE_MAXIMUM_LIMIT_ERROR);
        }
        return openapiService.queryCompletive(appId, userId, new Pagination(0, pageSize, current));
    }
    
    /**
     * 查看待办详情
     * @param taskId
     * @param username
     * @return
     * @throws DocumentException 
     */
    @GetMapping("/v2/tasks/{taskId}/pending")
    @ResponseBody
    @ApiOperation(value="查询待办详情", httpMethod="GET", nickname = "getPendingTask")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "username", value = "用户名称", required = true, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "taskId", value = "待办任务ID", required = true, dataType = "String", paramType = "path")
    })
    public PendingTaskResponseVO getPendingTask(HttpServletRequest request
            , @Validated @Pattern(regexp = "^[1-9]\\d+", message = "非法参数taskId") @PathVariable("taskId") String taskId
            , @Nullable @RequestParam("username") String username) throws DocumentException {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        Long userId = UserContext.getUserContext().getUserId();
        return openapiService.getPendingTask(userId, Long.parseLong(taskId));
    }
    
    /**
     * 查看已办详情
     * @param taskId
     * @param username
     * @return
     */
    @GetMapping("/v2/tasks/{taskId}/completive")
    @ResponseBody
    @ApiOperation(value="获取已办详情", httpMethod="GET", nickname = "getCompletiveTask")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "username", value = "用户名", required = true, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "taskId", value = "待办任务ID", required = true, dataType = "String", paramType = "path")
    })
    public CompletedTaskResponseVO getCompletiveTask(HttpServletRequest request, 
            @Validated @Pattern(regexp = "^[1-9]\\d+", message = "非法参数taskId") @PathVariable("taskId") String taskId,
            @Nullable @RequestParam("username") String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        Long userId = UserContext.getUserContext().getUserId();
        return openapiService.getCompleteTask(userId, Long.parseLong(taskId));
    }
    
    /**
     * 提交待办
     * @return
     * @throws DocumentException 
     */
    @PostMapping("/v2/tasks/pending")
    @ResponseBody
    @ApiOperation(value="待办提交", httpMethod="POST", nickname = "submitTask")
    public void submit(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) TaskSubmitRequestVO submitRequest) throws DocumentException {
        if (StringUtils.isEmpty(submitRequest.getUsername())) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        if (StringUtils.isEmpty(submitRequest.getTaskId())) {
            throw new IllegalParameterException(FlowErrorEnum.TASK_ID_NOT_EMPTY);
        }
        openapiService.submitTask(submitRequest);
    }
    
    /**
     * 保存表单数据
     * @param saveRequest
     * @return
     */
    @PutMapping(value = "/v2/tasks/pending")
    @ResponseBody
    @ApiOperation(value="保存表单数据", httpMethod="POST", nickname = "saveTaskForm")
    public void saveForm(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) FormRequestVO saveRequest) {
        if (StringUtils.isEmpty(saveRequest.getUsername())) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        if (StringUtils.isEmpty(saveRequest.getTaskId())) {
            throw new IllegalParameterException(FlowErrorEnum.TASK_ID_NOT_EMPTY);
        }
        formService.saveForm(Long.parseLong(saveRequest.getTaskId()), saveRequest.getFormData());
    }
    
    /**
     * 查询待办总数
     * @return
     */
    @GetMapping("/v2/tasks/pending/total")
    @ResponseBody
    @ApiOperation(value="查询待办总数", httpMethod="GET", nickname = "queryTotal")
    public int queryTotal(HttpServletRequest request, @Nullable @RequestParam("username") String username) {
        if (StringUtils.isEmpty(username)) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        Long userId = UserContext.getUserContext().getUserId();
        return taskCenterService.queryTotals(userId);
    }
    
    /**
     * 撤回
     * @return
     */
    @PostMapping("/v2/tasks/revocation")
    @ResponseBody
    @ApiOperation(value="撤回", httpMethod="POST", nickname = "revoke")
    public void revoke(@RequestBody @Validated @ApiParam(name = "请求内容", value = "传入json格式", required = true) TaskRevokeRequestVO request) throws DocumentException {
        if (StringUtils.isEmpty(request.getUsername())) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        if (StringUtils.isEmpty(request.getTaskId())) {
            throw new IllegalParameterException(FlowErrorEnum.TASK_ID_NOT_EMPTY);
        }
        taskCenterService.revoke(Long.parseLong(request.getTaskId()));
    }
    
    /**
     * 委托
     * @return
     */
    @PostMapping(value = "/v2/tasks/entrust")
    @ResponseBody
    @ApiOperation(value="委托", httpMethod="POST", nickname = "entrust")
    public void entrust(@RequestBody @Validated @ApiParam(name = "请求内容", value = "传入json格式", required = true) EntrustRequestVO request) {
        if (StringUtils.isEmpty(request.getUsername())) {
            throw new IllegalParameterException(FlowErrorEnum.USERNAME_NOT_EMPTY);
        }
        if (StringUtils.isEmpty(request.getTaskId())) {
            throw new IllegalParameterException(FlowErrorEnum.TASK_ID_NOT_EMPTY);
        }
        if (StringUtils.isEmpty(request.getMandatary())) {
            throw new IllegalParameterException(FlowErrorEnum.MANDATARY_NOT_EMPTY);
        }
        openapiService.entrust(request.getTaskId(), request.getMandatary(), request.getReason());
    }
    
}

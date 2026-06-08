
package com.supcon.supfusion.flow.webapi;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.supcon.supfusion.flow.common.vo.webapi.*;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.util.DateUtil;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.supcon.supfusion.flow.common.dto.CompleteTaskQueryContractDTO;
import com.supcon.supfusion.flow.common.dto.EntrustQueryContractDTO;
import com.supcon.supfusion.flow.common.dto.PendingQueryContractDTO;
import com.supcon.supfusion.flow.common.dto.PendingQueryContractDTO.Builder;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.vo.webapi.CompletedTaskResponseVO;
import com.supcon.supfusion.flow.common.vo.webapi.EntrustRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.EntrustResponseVO;
import com.supcon.supfusion.flow.common.vo.webapi.IdRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.PendingTaskResponseVO;
import com.supcon.supfusion.flow.common.vo.webapi.PendingTaskResponseVO2;
import com.supcon.supfusion.flow.common.vo.webapi.TaskJoinRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.TaskRevokeRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.TaskSubmitRequestVO;
import com.supcon.supfusion.flow.taskcenter.service.TaskCenterService;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * @author: zhuangmh
 * @date: 2020年6月3日 下午5:22:19
 */
@RestController
@InternalApi(path = HttpConstants.URL_SPLITER + "inter-api" + HttpConstants.URL_SPLITER + "flow-service")
@Api(value = "待办中心相关文档", tags = "待办任务管理")
public class TaskCenterWebapi {

    @Autowired
    private TaskCenterService taskCenterService;

    /**
     * 查询待办列表
     *
     * @param request
     * @return
     */
    @GetMapping(value = "/v1/tasks/pending/admin")
    @ResponseBody
    @ApiOperation(value = "获取所有待办列表V1接口", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "待办ID", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "taskName", value = "待办名称", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "processName", value = "流程名称", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "initiator", value = "发起者名字模糊查询", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "assignee", value = "待办接收者", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "version", value = "流程版本", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = Constants.PAGE_CURRENT, value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = Constants.PAGE_SIZE, value = "当前页号-默认10", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PageResult<PendingTaskResponseVO2> listAllPendingTask(HttpServletRequest request) {
        String taskName = request.getParameter("taskName");
        String processName = request.getParameter("processName");
        String id = request.getParameter("id");
        String initiator = request.getParameter("initiator");
        String assignee = request.getParameter("assignee");
        String version = request.getParameter("version");
        String page = request.getParameter(Constants.PAGE_CURRENT); // 当前页号
        String size = request.getParameter(Constants.PAGE_SIZE); // 每页条数

        int current = page == null ? Constants.DEFAULT_PAGE : Integer.parseInt(page);
        int pageSize = size == null ? Constants.DEFAULT_PAGE_SIZE : Integer.parseInt(size);
        Builder parameterBuilder = new PendingQueryContractDTO.Builder()
                .setTaskNames(taskName)
                .setProcessNames(processName)
                .setIds(id)
                .setAssignees(assignee)
                .setInitiators(initiator)
                .setVersions(version);
        return taskCenterService.queryAllPendingTask(parameterBuilder.build(), new Pagination(0, pageSize, current));
    }
    
    @GetMapping(value = "/v1/tasks/pending")
    @ResponseBody
    @ApiOperation(value = "获取我的待办列表V1接口", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "appId", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "taskName", value = "待办名称", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "processName", value = "流程名称", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "formNo", value = "单据编号", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "initiator", value = "发起者名字模糊查询", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "begin", value = "待办接收时间-时间戳(起点)", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end", value = "待办接收时间-时间戳(终点)", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = Constants.PAGE_CURRENT, value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = Constants.PAGE_SIZE, value = "当前页号-默认10", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PageResult<PendingTaskResponseVO> listMyPendingTask(HttpServletRequest request) {
        String appId = request.getParameter("appId");
        String taskName = request.getParameter("taskName");
        String processName = request.getParameter("processName");
        String formNo = request.getParameter("formNo");
        String initiator = request.getParameter("initiator");
        String begin = request.getParameter("begin"); // 按接收时间查询起点
        String end = request.getParameter("end"); // 按接收时间查询终点
        String status = request.getParameter("status");
        String page = request.getParameter(Constants.PAGE_CURRENT); // 当前页号
        String size = request.getParameter(Constants.PAGE_SIZE); // 每页条数

        int current = page == null ? Constants.DEFAULT_PAGE : Integer.parseInt(page);
        int pageSize = size == null ? Constants.DEFAULT_PAGE_SIZE : Integer.parseInt(size);
        Builder parameterBuilder = new PendingQueryContractDTO.Builder()
                .setTaskNames(taskName)
                .setProcessNames(processName)
                .setFormNos(formNo)
                .setInitiators(initiator)
                .setAppId(appId)
                .setStatus(status);
        if (begin != null) {
            parameterBuilder.setStartFrom(new Date(Long.parseLong(begin)));
        }
        if (end != null) {
            parameterBuilder.setStartTo(new Date(Long.parseLong(end)));
        }
        return taskCenterService.queryMyPendingTask(parameterBuilder.build(), new Pagination(0, pageSize, current));
    }

    /**
     * 查询已办列表
     *
     * @param request
     * @return
     */
    @GetMapping(value = "/v1/tasks/completive")
    @ResponseBody
    @ApiOperation(value = "获取已办列表V1接口", httpMethod = "GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "appId", value = "app id", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "taskName", value = "待办名称", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "processName", value = "流程名称", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "formNo", value = "单据编号", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "initiator", value = "发起者名字模糊查询", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "startFrom", value = "待办接收时间-时间戳(起点)", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "startTo", value = "待办接收时间-时间戳(终点)", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "completeFrom", value = "待办结束时间-时间戳(起点)", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "completeTo", value = "待办结束时间-时间戳(终点)", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = Constants.PAGE_CURRENT, value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = Constants.PAGE_SIZE, value = "当前页号-默认10", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PageResult<CompletedTaskResponseVO> listCompleteTask(HttpServletRequest request) {
        String appId = request.getParameter(Constants.APP_ID);
        String taskName = request.getParameter("taskName");
        String processName = request.getParameter("processName");
        String formNo = request.getParameter("formNo");
        String initiator = request.getParameter("initiator");
        String startFrom = request.getParameter("startFrom"); // 按接收时间查询起点
        String startTo = request.getParameter("startTo"); // 按接收时间查询终点
        String completeFrom = request.getParameter("completeFrom"); // 按接收时间查询起点
        String completeTo = request.getParameter("completeTo"); // 按接收时间查询终点
        String page = request.getParameter(Constants.PAGE_CURRENT); // 当前页号
        String size = request.getParameter(Constants.PAGE_SIZE); // 每页条数

        int current = page == null ? Constants.DEFAULT_PAGE : Integer.parseInt(page);
        int pageSize = size == null ? Constants.DEFAULT_PAGE_SIZE : Integer.parseInt(size);

        com.supcon.supfusion.flow.common.dto.CompleteTaskQueryContractDTO.Builder parameterBuilder = new CompleteTaskQueryContractDTO.Builder()
                .setTaskNames(taskName)
                .setProcessNames(processName)
                .setFormNos(formNo)
                .setAppId(appId)
                .setInitiators(initiator);
        if (startFrom != null) {
            parameterBuilder.setStartFrom(new Date(Long.parseLong(startFrom)));
        }
        if (startTo != null) {
            parameterBuilder.setStartTo(new Date(Long.parseLong(startTo)));
        }
        if (completeFrom != null) {
            parameterBuilder.setCompleteFrom(new Date(Long.parseLong(completeFrom)));
        }
        if (completeTo != null) {
            parameterBuilder.setCompleteTo(new Date(Long.parseLong(completeTo)));
        }
        return taskCenterService.queryCompleteTask(parameterBuilder.build(), new Pagination(0, pageSize, current));
    }

    /**
     * 获取待办详情
     *
     * @param taskId
     * @return
     * @throws DocumentException 
     * @throws  
     */
    @GetMapping(value = "/v1/task/pending")
    @ResponseBody
    @ApiOperation(value = "获取待办详情V1接口", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "待办ID", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PendingTaskResponseVO getPendingTask(@RequestParam("taskId") String taskId) throws DocumentException {
        Long userId = UserContext.getUserContext().getUserId();
        return taskCenterService.getPendingTask(userId, Long.parseLong(taskId));
    }

    /**
     * 获取待办详情
     *
     * @param taskId
     * @return
     */
    @GetMapping(value = "/v1/task/completive")
    @ResponseBody
    @ApiOperation(value = "获取已办详情V1接口", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "已办ID", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public CompletedTaskResponseVO getCompleteTask(@RequestParam("taskId") String taskId) {
        Long userId = UserContext.getUserContext().getUserId();
        return taskCenterService.getCompleteTask(userId, Long.parseLong(taskId));
    }

    /**
     * 获取当前登录人员的待办总数
     *
     * @return
     */
    @GetMapping(value = "/v1/task/pending/total")
    @ResponseBody
    @ApiOperation(value = "获取当前登录用户待办总数V1接口", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public Pagination getTaskTotalCount() {
        Long userId = UserContext.getUserContext().getUserId();
        int total = taskCenterService.queryTotals(userId);
        return new Pagination(total, 0, 0);
    }

    /**
     * 当前登录人员当天数量\最近一星期\最近一月 的数据
     *
     * @param
     * @return
     */
    @GetMapping(value = "/v1/task/pending/group/total")
    @ResponseBody
    @ApiOperation(value = "分组获取当前用户待办数量", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public Result<GroupTotalVO> getLoginTaskTotalsByTime()  {
        GroupTotalVO groupTotalVO = new GroupTotalVO();
        Date todayStartTime = DateUtil.parse(DateUtil.formatDate(new Date()),DateUtil.DATE_FORMAT);
        Date todayEndTime = DateUtil.setDays(todayStartTime,1);
        Date weekDate = DateUtil.setDays(todayStartTime,-7);
        Long userId = UserContext.getUserContext().getUserId();
        //当天待办数量
        int today = taskCenterService.queryTotalsByTime(todayStartTime, todayEndTime );
        groupTotalVO.setToday(today);
        //一周内的待办数量
        int week = taskCenterService.queryTotalsByTime(weekDate,  todayEndTime);
        groupTotalVO.setWeek(week);
        //所有待办数量
        groupTotalVO.setTotal(taskCenterService.queryTotals(userId));
        Result success = Result.success();
        success.setCode(HttpStatus.OK.value());
        success.setData(groupTotalVO);
        return success ;
    }

    /**
     * 提交待办
     *
     * @param submitRequest
     * @return
     */
    @PostMapping(value = "/v1/task/pending")
    @ResponseBody
    @ApiOperation(value = "提交待办V1接口", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void submitTask(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) TaskSubmitRequestVO submitRequest) {
        taskCenterService.submit(Long.parseLong(submitRequest.getTaskId()), submitRequest.getFormData(), submitRequest.getComment()
                , submitRequest.getAssigns(), submitRequest.getAudit());
    }
    
    /**
     * 查询委托详情
     * @param id
     * @return
     */
    @GetMapping(value = "/v1/task/entrust/detail")
    @ResponseBody
    @ApiOperation(value = "获取委托详情V1接口", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "委托ID", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PendingTaskResponseVO getEntrustDetail(@RequestParam("id") String id) {
        return taskCenterService.getEntrustDetail(Long.parseLong(id));
    }

    /**
     * 查询委托列表
     *
     * @param request
     * @return
     */
    @GetMapping(value = "/v1/task/entrust")
    @ResponseBody
    @ApiOperation(value = "查询委托列表V1接口", httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "app id", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "taskName", value = "待办任务名称", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "processName", value = "流程名称", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "mandatary", value = "受托者(用户ID)", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "begin", value = "委托时间-UTC时间戳(起点)", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "end", value = "委托时间-UTC时间戳(终点)", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "当前页号-默认10", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PageResult<EntrustResponseVO> queryEntrusts(HttpServletRequest request) {
        String appId = request.getParameter(Constants.APP_ID); // 
        String taskName = request.getParameter("taskName"); // 
        String processName = request.getParameter("processName"); // 
        String mandatary = request.getParameter("mandatary"); // 受托者
        String begin = request.getParameter("begin"); // 委托时间查询起点
        String end = request.getParameter("end"); // 委托时间查询终点
        String page = request.getParameter("current"); // 当前页号
        String size = request.getParameter("pageSize"); // 需要返回的条数

        int current = page == null ? Constants.DEFAULT_PAGE : Integer.parseInt(page);
        int pageSize = size == null ? Constants.DEFAULT_PAGE_SIZE : Integer.parseInt(size);

        Long userId = UserContext.getUserContext().getUserId();
        EntrustQueryContractDTO.Builder parameterBuilder = new EntrustQueryContractDTO.Builder()
                .setAppId(appId)
                .setProcessNames(processName)
                .setMandatarys(mandatary)
                .setPrincipal(userId.toString())
                .setTaskNames(taskName);
        if (begin != null) {
            parameterBuilder.setFrom(new Date(Long.parseLong(begin)));
        }
        if (end != null) {
            parameterBuilder.setTo(new Date(Long.parseLong(end)));
        }
        return taskCenterService.queryEntrust(parameterBuilder.build(), new Pagination(0, pageSize, current));
    }

    /**
     * 全权委托
     *
     * @param entrustRequest
     * @return
     */
    @PostMapping(value = "/v1/task/entrust")
    @ResponseBody
    @ApiOperation(value = "委托待办V1接口", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void entrustTask(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) EntrustRequestVO entrustRequest) {
        Long principal = UserContext.getUserContext().getUserId();
        Long mandatary = Long.parseLong(entrustRequest.getMandatary());
        taskCenterService.entrust(entrustRequest.getTaskId(), entrustRequest.getReason(), mandatary, principal);
    }
    
    /**
     * 管理员全权委托
     *
     * @param entrustRequest
     * @return
     */
    @PostMapping(value = "/v1/task/proxyEntrust")
    @ResponseBody
    @ApiOperation(value = "管理员委托待办V1接口", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void proxyEntrustTask(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) EntrustRequestVO entrustRequest) {
        Long mandatary = Long.parseLong(entrustRequest.getMandatary());
        taskCenterService.proxyEntrust(Long.parseLong(entrustRequest.getTaskId()), entrustRequest.getReason(), mandatary);
    }

    /**
     * 取消委托
     *
     * @param request
     * @return
     */
    @PostMapping(value = "/v1/task/cancelEntrust")
    @ResponseBody
    @ApiOperation(value = "取消委托V1接口", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "id", required = true, dataType = "String", paramType = "body"),
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void cancelEntrust(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) IdRequestVO request) {
        taskCenterService.cancelEntrust(Long.parseLong(request.getId()));
    }

    /**
     * 撤回
     *
     * @param request
     * @return
     * @throws DocumentException 
     * @throws  
     */
    @PostMapping(value = "/v1/task/completive/revoke")
    @ResponseBody
    @ApiOperation(value = "待办任务撤回V1接口", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void revokeTask(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) TaskRevokeRequestVO request) throws DocumentException {
        taskCenterService.revoke(Long.parseLong(request.getTaskId()));
    }

    /**
     * 加入会签
     *
     * @param request
     * @return
     */
    @PostMapping(value = "/v1/task/join")
    @ResponseBody
    @ApiOperation(value = "加签V1接口", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void joinMultiTask(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) TaskJoinRequestVO request) {
        taskCenterService.joinMultiTask(request.getInvitee(), Long.parseLong(request.getTaskId()));
    }

    /**
     * 迁移待办
     *
     * @param taskId        待办ID
     * @param targetTaskKey 目标待办key
     * @return
     */
    @PutMapping(value = "/v1/task/migrate")
    @ResponseBody
    @ApiOperation(value = "迁移待办V1接口", httpMethod = "PUT")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header"),
            @ApiImplicitParam(name = "taskId", value = "待办ID", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "targetTaskKey", value = "目标待办key", required = true, dataType = "String", paramType = "query")
    })
    public void migrateTask(@Valid String taskId, @Valid String targetTaskKey) {
        taskCenterService.migrateTask(Long.parseLong(taskId), targetTaskKey);
    }
    
}

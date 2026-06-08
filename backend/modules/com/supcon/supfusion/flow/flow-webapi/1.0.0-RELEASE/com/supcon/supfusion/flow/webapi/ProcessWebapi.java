/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.webapi;

import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.supcon.supfusion.flow.common.dto.DiagramDTO;
import com.supcon.supfusion.flow.common.dto.ProcessQueryContractDTO;
import com.supcon.supfusion.flow.common.dto.SimpleTaskDTO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.vo.webapi.ProcessBaseRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.ProcessLogVO;
import com.supcon.supfusion.flow.common.vo.webapi.ProcessSaveRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.ProcessStartInfoVO;
import com.supcon.supfusion.flow.common.vo.webapi.ProcessStartRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.ProcessStartResponseVO;
import com.supcon.supfusion.flow.common.vo.webapi.ProcessVO;
import com.supcon.supfusion.flow.common.vo.webapi.UrgeInfoVO;
import com.supcon.supfusion.flow.common.vo.webapi.UrgeRequestVO;
import com.supcon.supfusion.flow.taskcenter.service.ProcessService;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
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
 * @date: 2020年6月2日 下午1:48:14
 */
@RestController
@InternalApi(path = HttpConstants.URL_SPLITER + "inter-api" + HttpConstants.URL_SPLITER + "flow-service")
@Api(value = "流程实例相关文档", tags = "流程实例管理")
public class ProcessWebapi extends BaseController {
    
    @Autowired
    private ProcessService processService;
    
    /**
     * 获取流程列表
     * @return
     */
    @GetMapping(value = "/v1/processes")
    @ResponseBody
    @ApiOperation(value="获取流程列表V1接口", httpMethod="GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = Constants.APP_ID, value = "app id", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "initiator", value = "流程发起者", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "processName", value = "流程名称", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "formNo", value = "单据编号", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = Constants.PAGE_CURRENT, value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = Constants.PAGE_SIZE, value = "当前页号-默认10", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PageResult<ProcessVO> queryMy(HttpServletRequest request) {
        String initiator = request.getParameter("initiator");
        String processName = request.getParameter("processName");
        String formNo = request.getParameter("formNo");
        String appId = request.getParameter("appId");
        ProcessQueryContractDTO queryContract = new ProcessQueryContractDTO.Builder()
                .setInitiators(initiator)
                .setProcessNames(processName)
                .setFormNos(formNo)
                .setAppId(appId)
                .build();
        String page = request.getParameter(Constants.PAGE_CURRENT); // 当前页号
        String size = request.getParameter(Constants.PAGE_SIZE); // 每页条数

        int current = page == null ? Constants.DEFAULT_PAGE : Integer.parseInt(page);
        int pageSize = size == null ? Constants.DEFAULT_PAGE_SIZE : Integer.parseInt(size);
        return processService.queryMy(queryContract, new Pagination(0, pageSize, current));
    }
    
    /**
     * 获取流程列表
     * @return
     */
    @GetMapping(value = "/v1/processes/admin")
    @ResponseBody
    @ApiOperation(value="获取流程列表V1接口", httpMethod="GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", value = "流程ID", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "initiator", value = "流程发起者", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "processName", value = "流程名称", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "version", value = "版本", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = Constants.PAGE_CURRENT, value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = Constants.PAGE_SIZE, value = "当前页号-默认10", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PageResult<ProcessVO> queryAll(HttpServletRequest request) {
        String id = request.getParameter("id");
        String initiator = request.getParameter("initiator");
        String processName = request.getParameter("processName");
        String version = request.getParameter("version");
        ProcessQueryContractDTO queryContract = new ProcessQueryContractDTO.Builder()
                .setInitiators(initiator)
                .setProcessNames(processName)
                .setIds(id)
                .setVersions(version)
                .build();
        String page = request.getParameter(Constants.PAGE_CURRENT); // 当前页号
        String size = request.getParameter(Constants.PAGE_SIZE); // 每页条数

        int current = page == null ? Constants.DEFAULT_PAGE : Integer.parseInt(page);
        int pageSize = size == null ? Constants.DEFAULT_PAGE_SIZE : Integer.parseInt(size);
        return processService.queryAll(queryContract, new Pagination(0, pageSize, current));
    }
    
    /**
     * 获取我的关注流程列表
     * @return
     */
    @GetMapping(value = "/v1/process/attention")
    @ResponseBody
    @ApiOperation(value="获取我的关注流程列表V1接口", httpMethod="GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = Constants.APP_ID, value = "app id", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = Constants.PAGE_CURRENT, value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = Constants.PAGE_SIZE, value = "当前页号-默认10", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PageResult<ProcessVO> queryMyAttention(HttpServletRequest request) {
        String appId = request.getParameter(Constants.APP_ID); // 
        String page = request.getParameter(Constants.PAGE_CURRENT); // 当前页号
        String size = request.getParameter(Constants.PAGE_SIZE); // 每页条数
        int current = page == null ? Constants.DEFAULT_PAGE : Integer.parseInt(page);
        int pageSize = size == null ? Constants.DEFAULT_PAGE_SIZE : Integer.parseInt(size);
        return processService.queryMyAttention(appId, new Pagination(0, pageSize, current));
    }
    
    /**
     * 关注
     * @param requestVO
     */
    @PostMapping(value = "/v1/process/attention")
    @ResponseBody
    @ApiOperation(value="关注流程V1接口", httpMethod="POST")
    public void followProcess(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) ProcessBaseRequestVO requestVO) {
        processService.follow(requestVO.getProcessId());
    }
    
    /**
     * 取消关注
     * @param requestVO
     */
    @PutMapping(value = "/v1/process/attention")
    @ResponseBody
    @ApiOperation(value="取消关注流程V1接口", httpMethod="PUT")
    public void cancelFollowProcess(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) ProcessBaseRequestVO requestVO) {
        processService.cancelFollow(requestVO.getProcessId());
    }
    
    /**
     * 获取流程操作日志
     * @param processId 流程实例ID
     * @return
     */
    @GetMapping(value = "/v1/process/logs")
    @ResponseBody
    @ApiOperation(value="获取流程操作记录V1接口", httpMethod="GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "processId", value = "流程实例ID", required = true, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PageResult<ProcessLogVO> querylogs(@RequestParam("processId") String processId) {
        List<ProcessLogVO> logs = processService.queryProcessLogs(processId);
        return new PageResult<>(logs, 0, 0, 0);
    }
    
    @GetMapping(value = "/v1/process/startInfo")
    @ResponseBody
    @ApiOperation(value="获取流程启动信息V1接口", httpMethod="GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "appId", value = "app id", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "processKey", value = "流程编号", required = true, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public ProcessStartInfoVO getProcessStartInfo(@Nullable @RequestParam("appId") String appId, @RequestParam("processKey") String processKey) throws DocumentException {
        return processService.getProcessStartInfo(appId, processKey);
    }
    
    /**
     * 发起流程
     * @param processRequestVO
     * @return 流程实例ID
     * @throws DocumentException 
     */
    @PostMapping(value = "/v1/process")
    @ResponseBody
    @ApiOperation(value="发起流程V1接口", httpMethod="POST")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public ProcessStartResponseVO startProcess(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) ProcessStartRequestVO processRequestVO) throws DocumentException {
        DiagramDTO diagramDto = new DiagramDTO(processRequestVO.getAppId(), processRequestVO.getProcessName(), processRequestVO.getProcessKey());
        String processId = processService.startProcess(diagramDto, processRequestVO.getFormData(),  processRequestVO.getComment(), processRequestVO.getAssigns(), processRequestVO.getAudit());
        return new ProcessStartResponseVO(processId, null);
    }
    
    /**
     * 发起流程
     * @param processRequestVO
     * @return 流程实例ID
     * @throws DocumentException 
     */
    @PostMapping(value = "/v1/process/save")
    @ResponseBody
    @ApiOperation(value="保存流程待发V1接口", httpMethod="POST")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public ProcessStartResponseVO saveProcess(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) ProcessSaveRequestVO request) throws DocumentException {
        SimpleTaskDTO st = processService.startProcessBySave(request.getAppId(), request.getProcessKey(), request.getFormData());
        return new ProcessStartResponseVO(st.getProcessId(), st.getTaskId());
    }
    
    /**
     * 暂停流程
     * @param request
     * @return 流程实例ID
     */
    @PostMapping(value = "/v1/process/suspend")
    @ResponseBody
    @ApiOperation(value="暂停流程V1接口", httpMethod="POST")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void suspendProcess(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) ProcessBaseRequestVO request) {
        processService.suspendProcess(request.getProcessId());
    }
    
    /**
     * 恢复流程
     * @param request
     * @return 流程实例ID
     */
    @PostMapping(value = "/v1/process/active")
    @ResponseBody
    @ApiOperation(value="恢复流程V1接口", httpMethod="POST")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void activeProcess(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) ProcessBaseRequestVO request) {
        processService.activeProcess(request.getProcessId());
    }
    
    
    @GetMapping(value = "/v1/process/urge")
    @ResponseBody
    @ApiOperation(value="获取催办详情V1接口", httpMethod="GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "processId", value = "流程实例ID", required = true, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PageResult<UrgeInfoVO> listUrgeInfo(@RequestParam("processId") String processId) {
        List<UrgeInfoVO> urgeInfos = processService.listUrgeInfo(processId);
        return new PageResult<>(urgeInfos, 0, 0, 0);
    }
    
    /**
     * 催办
     * @param request
     * @return 流程实例ID
     */
    @PostMapping(value = "/v1/process/urge")
    @ResponseBody
    @ApiOperation(value="流程催办V1接口", httpMethod="POST")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void urgeProcess(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) UrgeRequestVO request) {
        processService.urge(request.getProcessId(), request.getUrgeList(), request.getNoticeType());
    }
    
    /**
     * 终止流程
     * @param request
     * @return 流程实例ID
     */
    @PostMapping(value = "/v1/process/terminate")
    @ResponseBody
    @ApiOperation(value="作废流程V1接口", httpMethod="POST")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void cancelProcess(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) ProcessBaseRequestVO request) {
        processService.cancel(request.getProcessId(), true);
    }
}

/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.webapi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.supcon.supfusion.flow.common.dto.DiagramQueryContractDTO;
import com.supcon.supfusion.flow.common.dto.DiagramQueryContractDTO.Builder;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.IllegalParameterException;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.vo.webapi.DiagramCreateRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.DiagramEditRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.DiagramListWrapper;
import com.supcon.supfusion.flow.common.vo.webapi.DiagramPublishRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.DiagramResponseVO;
import com.supcon.supfusion.flow.common.vo.webapi.DiagramUpdateRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.DiagramUpgradeRequestVO;
import com.supcon.supfusion.flow.common.vo.webapi.FlowChartResponseVO;
import com.supcon.supfusion.flow.common.vo.webapi.IdRequestVO;
import com.supcon.supfusion.flow.taskcenter.service.DiagramService;
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
 * @date: 2020年5月19日 下午3:24:32
 */
@RestController
@InternalApi(path = HttpConstants.URL_SPLITER + "inter-api" + HttpConstants.URL_SPLITER + "flow-service")
@Api(value = "流程组态相关文档", tags = "流程组态")
public class DiagramWebapi extends BaseController {
    
    @Autowired
    private DiagramService diagramService;
    @Autowired
    private ProcessService processService;
    
    /**
     * 查询流程列表
     * @param request
     * @return
     */
    @GetMapping(value = "/v1/diagrams")
    @ResponseBody
    @ApiOperation(value="获取流程列表V1接口", httpMethod="GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "processName", value = "流程名称", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "enable", value = "是否只查询启用版本 true-启用 false-全部", required = false, dataType = "String", paramType = "query", example = "false"),
        @ApiImplicitParam(name = "appId", value = "app id", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "multiCompany", value = "是否跨公司 true,false", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "creator", value = "创建者", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "publisher", value = "发布者", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "current", value = "当前页号-默认1", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "pageSize", value = "当前页号-默认10", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public PageResult<DiagramResponseVO> listDiagram(HttpServletRequest request) {
        String processName = request.getParameter("processName");
        String enable = request.getParameter("enable"); // 是否只查询启用版本
        String history = request.getParameter("history"); // 是否查询历史版本
        String appId = request.getParameter("appId");
        String multiCompany = request.getParameter("multiCompany");
        String creator = request.getParameter("creator");
        String publisher = request.getParameter("publisher");
        String page = request.getParameter(Constants.PAGE_CURRENT); // 当前页号
        String size = request.getParameter(Constants.PAGE_SIZE); // 每页条数
        
        int current = page == null ? Constants.DEFAULT_PAGE : Integer.parseInt(page);
        int pageSize = size == null ? Constants.DEFAULT_PAGE_SIZE : Integer.parseInt(size);
        
        Builder builder = new DiagramQueryContractDTO.Builder()
                .setProcessName(processName)
                .setEnable(enable)
                .setAppId(appId)
                .setCreator(creator)
                .setPublisher(publisher)
                .setHistory(history)
    	        .setMultiCompany(multiCompany);
        return diagramService.queryDiagrams(builder.build(), new Pagination(0, pageSize, current));
    }
    
    /**
     * 查询所有版本
     * @param request
     * @return
     */
    @GetMapping(value = "/v1/diagram/versions")
    @ResponseBody
    @ApiOperation(value="获取所有版本V1接口", httpMethod="GET")
    public PageResult<Integer> queryAllVersion(HttpServletRequest request) {
        return diagramService.queryAllVersion();
    }
    
    /**
     * 查询流程详情
     * @param id
     * @return
     */
    @GetMapping(value = "/v1/diagram")
    @ResponseBody
    @ApiOperation(value="获取流程详情V1接口", httpMethod="GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", value = "id", required = true, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public DiagramResponseVO getDiagram(@RequestParam("id") String id) {
        return diagramService.getById(Long.parseLong(id));
    }
    
    /**
     * 查询流程图数据
     * @param id
     * @return
     */
    @GetMapping(value = "/v1/diagram/flowChart")
    @ResponseBody
    @ApiOperation(value="查询流程图数据V1接口", httpMethod="GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "processKey", value = "流程编号", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "processId", value = "流程实例ID", required = false, dataType = "String", paramType = "query"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public FlowChartResponseVO generateDiagram(@Nullable @RequestParam("processKey") String processKey, @Nullable @RequestParam("processId") String processId) {
        if (StringUtils.isEmpty(processKey) && StringUtils.isEmpty(processId)) {
            throw new IllegalParameterException(FlowErrorEnum.FLOW_CHART_PARAMETER_ERROR);
        }
        return processService.generateDiagram(processKey, processId);
    }
    
    /**
     * 创建流程
     * @param createDiagramRequest
     * @return
     */
    @PostMapping(value = "/v1/diagram")
    @ResponseBody
    @ApiOperation(value="创建流程V1接口", httpMethod="POST")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public DiagramResponseVO createDiagram(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) DiagramCreateRequestVO createDiagramRequest) {
        return diagramService.createDiagram(createDiagramRequest.getAppId()
                , createDiagramRequest.getProcessName(), createDiagramRequest.getMultiCompany());
    }
    
    /**
     * 编辑流程,修改名称等
     * @param updateDiagramRequest
     * @return
     */
    @PutMapping(value = "/v1/diagram/edit")
    @ResponseBody
    @ApiOperation(value="编辑流程V1接口", httpMethod="PUT")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void editDiagram(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) DiagramEditRequestVO editDiagramRequest) {
        Long id = Long.parseLong(editDiagramRequest.getId());
        diagramService.editDiagram(id, editDiagramRequest.getProcessName(), editDiagramRequest.getMultiCompany());
    }

    /**
     * 更新流程组态
     * @param updateDiagramRequest
     * @return
     */
    @PutMapping(value = "/v1/diagramJson/save")
    @ResponseBody
    @ApiOperation(value="更新流程组态数据V1接口", httpMethod="PUT")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void saveDiagramJson(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) DiagramUpdateRequestVO updateDiagramRequest) {
        diagramService.saveDiagramJson(Long.parseLong(updateDiagramRequest.getId()), updateDiagramRequest.getJson());
    }
    
    /**
     * 发布流程 
     * @param publishDiagramRequest
     * @return
     * @throws UnsupportedEncodingException
     * @throws NumberFormatException 
     * @throws DocumentException
     */
    @PutMapping(value = "/v1/diagram/publish")
    @ResponseBody
    @ApiOperation(value="流程组态数据发布V1接口", httpMethod="PUT")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void publish(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) DiagramPublishRequestVO publishDiagramRequest) throws UnsupportedEncodingException, NumberFormatException, DocumentException {
        if (publishDiagramRequest.getAutoSave() != null && publishDiagramRequest.getAutoSave() == Boolean.TRUE) {
            diagramService.saveDiagramJson(Long.parseLong(publishDiagramRequest.getId()), publishDiagramRequest.getJson());
        }
        diagramService.publish(Long.parseLong(publishDiagramRequest.getId()), publishDiagramRequest.getBpmnXml());
    }
    
    /**
     * 升版
     * @param updateDiagramRequest
     * @return
     */
    @PostMapping(value = "/v1/diagram/upgrade")
    @ResponseBody
    @ApiOperation(value="流程组态升版V1接口", httpMethod="POST")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public DiagramResponseVO upgradeDiagram(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) DiagramUpgradeRequestVO upgradeRequest) {
        boolean multiCompany = upgradeRequest.getMultiCompany() != null && upgradeRequest.getMultiCompany();
        return diagramService.upgradeDiagram(Long.parseLong(upgradeRequest.getId()), upgradeRequest.getProcessName(), multiCompany);
    }
    
    /**
     * 启用流程
     * @param updateDiagramRequest
     * @return
     */
    @PutMapping(value = "/v1/diagram/enabled")
    @ResponseBody
    @ApiOperation(value="启用流程V1接口", httpMethod="PUT")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void enableDiagram(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) IdRequestVO updateDiagramRequest) {
        diagramService.enableDiagram(Long.parseLong(updateDiagramRequest.getId()));
    }
    
    /**
     * 停用流程
     * @param updateDiagramRequest
     * @return
     */
    @PutMapping(value = "/v1/diagram/disabled")
    @ResponseBody
    @ApiOperation(value="停用流程V1接口", httpMethod="PUT")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void disableDiagram(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) IdRequestVO updateDiagramRequest) {
        diagramService.disableDiagram(Long.parseLong(updateDiagramRequest.getId()));
    }
    
    /**
     * 重置流程, 回到上一次发布的状态
     * @param updateDiagramRequest
     * @return
     */
    @PutMapping(value = "/v1/diagram/reset")
    @ResponseBody
    @ApiOperation(value="重置流程组态V1接口", httpMethod="PUT")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void resetDiagram(@RequestBody @Valid @ApiParam(name = "请求内容", value = "传入json格式", required = true) IdRequestVO updateDiagramRequest) {
        diagramService.resetDiagram(Long.parseLong(updateDiagramRequest.getId()));
    }
    
    /**
     * 删除流程
     * @param id
     * @param request
     * @return
     */
    @DeleteMapping(value = "/v1/diagram")
    @ResponseBody
    @ApiOperation(value="删除流程V1接口", httpMethod="DELETE")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "id", value = "id", required = true, dataType = "String", paramType = "query", example = "1,2,3"),
        @ApiImplicitParam(name = "onlyDiagram", value = "是否只删除组态数据", required = false, dataType = "String", paramType = "query", example = "true"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public void deleteDiagram(HttpServletRequest request) {
        String id = request.getParameter("id");
        String onlyDiagram = request.getParameter("onlyDiagram");
        diagramService.batchDeleteDiagram(id, Boolean.valueOf(onlyDiagram));
    }
    
    /**
     * 导出流程
     * @param appId
     * @param diagramCodes
     * @return
     */
    @GetMapping(value = "/v1/diagram/exports")
    @ResponseBody
    @ApiOperation(value="流程导出V1接口", httpMethod="GET")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "appId", value = "appId", required = true, dataType = "String", paramType = "query", example = "app_123456789"),
        @ApiImplicitParam(name = "processKeys", value = "流程编号", required = true, dataType = "String", paramType = "query", example = "K1234,K2346"),
        @ApiImplicitParam(name = "X-Tenant-Id", value = "租户ID", required = false, dataType = "String", paramType = "header")
    })
    public DiagramListWrapper exportDiagram(@RequestParam("appId") String appId, @RequestParam("processKeys") String processKeys) {
        String[] processKeyArray = processKeys.split(",");
        return diagramService.exports(appId, Arrays.asList(processKeyArray));
    }

    /**
     * 导入, 文件名格式: XX_appId.data
     * @param file
     * @throws IOException
     */
    @PostMapping(value = "/v1/diagram/imports")
    @ResponseBody
    public void importDiagram(@RequestParam("file") MultipartFile file, @RequestParam("appId") String appId) throws IOException {
        String diagramJson = new String(file.getBytes(), Constants.ENCODE_UTF8);
        diagramService.imports(appId, diagramJson);
    }
}

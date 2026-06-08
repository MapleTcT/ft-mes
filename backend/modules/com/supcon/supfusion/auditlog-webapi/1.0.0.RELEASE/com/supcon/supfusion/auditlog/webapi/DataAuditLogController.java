package com.supcon.supfusion.auditlog.webapi;

import com.supcon.supfusion.auditlog.common.constant.AuditLogConstants;
import com.supcon.supfusion.auditlog.common.exception.AuditLogErrorEnum;
import com.supcon.supfusion.auditlog.common.exception.AuditLogException;
import com.supcon.supfusion.auditlog.service.DataAuditLogService;
import com.supcon.supfusion.auditlog.service.bo.*;
import com.supcon.supfusion.auditlog.webapi.vo.*;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * 数据审计业务日志
 *
 * @author caokele
 */
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + AuditLogConstants.SERVER_NAME)
@Api(tags = "数据审计日志", description = "数据审计日志文档说明")
public class DataAuditLogController extends BaseController {
    @Autowired
    private DataAuditLogService dataAuditLogService;

    @ApiOperation(value = "查询数据日志列表V1接口")
    @PostMapping(value = "/v1/data-logs")
    public PageResult<DataAuditLogResponseVO> queryDataLogs(@ApiParam(name = "current", value = "当前页号-默认1", required = false) @RequestParam(required = false, defaultValue = "1") Integer current,
                                                            @ApiParam(name = "pageSize", value = "每页数量-默认10", required = false) @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                            @ApiParam(name = "queryVO", value = "筛选条件", required = false) @RequestBody(required = false) DataAuditLogQueryVO queryVO) {
        DataAuditLogQueryBO dataAuditLogQueryBO = new DataAuditLogQueryBO();
        if (queryVO != null) {
            BeanUtils.copyProperties(queryVO, dataAuditLogQueryBO);
        }
        PageResult<DataAuditLogBO> boPageResult = dataAuditLogService.queryDataLogs(current, pageSize, dataAuditLogQueryBO);
        PageResult<DataAuditLogResponseVO> voPageResult = new PageResult<>();
        BeanUtils.copyProperties(boPageResult, voPageResult);
        return voPageResult;
    }

    @ApiOperation(value = "查询数据日志模型列表V1接口")
    @PostMapping(value = "/v1/data-log/{traceId}")
    public ListResult<DataAuditLogModelResponseVO> queryDataModels(@ApiParam(name = "traceId", value = "链路跟踪id", required = true) @PathVariable Long traceId,
                                                                   @ApiParam(name = "queryVO", value = "筛选条件", required = false) @RequestBody(required = false) DataModelQueryVO queryVO) {
        DataModelQueryBO dataModelQueryBO = new DataModelQueryBO();
        if (queryVO != null) {
            BeanUtils.copyProperties(queryVO, dataModelQueryBO);
        }
        ListResult<DataModelBO> boListResult = dataAuditLogService.queryDataModels(traceId, dataModelQueryBO);
        ListResult<DataAuditLogModelResponseVO> listResult = new ListResult<>();
        BeanUtils.copyProperties(boListResult, listResult);
        return listResult;
    }

    @ApiOperation(value = "查询导入导出日志模型列表V1接口")
    @PostMapping(value = "/v1/data-log/{traceId}/import")
    public PageResult<DataAuditLogModelResponseVO> queryImportDataModels(@ApiParam(name = "current", value = "当前页号-默认1", required = false) @RequestParam(required = false, defaultValue = "1") Integer current,
                                                                         @ApiParam(name = "pageSize", value = "每页数量-默认10", required = false) @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                                         @ApiParam(name = "traceId", value = "链路跟踪id", required = true) @PathVariable Long traceId,
                                                                         @ApiParam(name = "queryVO", value = "筛选条件", required = false) @RequestBody(required = false) DataModelQueryVO queryVO) {
        DataModelQueryBO dataModelQueryBO = new DataModelQueryBO();
        if (queryVO != null) {
            BeanUtils.copyProperties(queryVO, dataModelQueryBO);
        }
        PageResult<DataModelBO> boListResult = dataAuditLogService.queryImportDataModels(current, pageSize, traceId, dataModelQueryBO);
        PageResult<DataAuditLogModelResponseVO> pageResult = new PageResult<>();
        BeanUtils.copyProperties(boListResult, pageResult);
        return pageResult;
    }

    @ApiOperation(value = "查询导入导出审计日志模型属性列表V1接口")
    @GetMapping(value = "/v1/data-log/{traceId}/model/{modelCode}/code/{code}")
    public ListResult<DataModelPropertyResponseVO> queryImportModelProperties(@ApiParam(name = "traceId", value = "链路跟踪id", required = true) @PathVariable Long traceId,
                                                                              @ApiParam(name = "modelCode", value = "模块编码", required = true) @PathVariable String modelCode,
                                                                              @ApiParam(name = "code", value = "模型编码", required = true) @PathVariable String code) {
        ListResult<DataModelPropertyBO> modelPropertyBOList = dataAuditLogService.queryImportDataModelProperty(traceId, modelCode, code);
        ListResult<DataModelPropertyResponseVO> listResult = new ListResult<>();
        BeanUtils.copyProperties(modelPropertyBOList, listResult);
        return listResult;
    }

    @ApiOperation(value = "下载日志内文件V1接口")
    @GetMapping(value = "/v1/data-log/{traceId}/file")
    public ResponseEntity<byte[]> downloadFile(@ApiParam(name = "traceId", value = "链路跟踪id", required = true) @PathVariable Long traceId, HttpServletResponse response) throws IOException {
        ResponseEntity<byte[]> res = dataAuditLogService.queryDataLogRes(traceId);
        if (res == null) {
            // 查询审计业务数据日志失败!
            throw new AuditLogException(AuditLogErrorEnum.AUDIT_BUSSINESS_LOG_FIELD);
        }
        return res;
    }

    @ApiOperation(value = "导出审计日志")
    @PostMapping(value = "/v1/data-log/export")
    public void exportDataAuditLog(@ApiParam(name = "queryVO", value = "筛选条件") @RequestBody(required = true) DownloadDataQueryVO queryVO,
                                   HttpServletResponse response) {
        Boolean all = queryVO.getAll() != null ? queryVO.getAll() : false;
        List<Long> traceIds = queryVO != null ? queryVO.getTraceIds() : null;
        // 导出审计日志Excel
        dataAuditLogService.exportAuditLogExcelData(traceIds, all, response);
    }

    @ApiOperation(value = "导出审计模型日志")
    @PostMapping(value = "/v1/data-log/{traceId}/model/export")
    public void exportDataAuditLogModel(@ApiParam(name = "all", value = "是否导出全部") @RequestParam(value = "all", required = false, defaultValue = "true") Boolean all,
                                        @ApiParam(name = "traceId", value = "链路跟踪id") @PathVariable(value = "traceId") Long traceId,
                                        @ApiParam(name = "modelCodes", value = "模型编码列表") @RequestParam(value = "modelCodes", required = false) List<String> modelCodes,
                                        HttpServletResponse response) {
        // 导出审计模型Excel
        dataAuditLogService.exportAuditLogExcelModel(all, traceId, modelCodes, response);
    }

    @ApiOperation(value = "模型启用/不启用审计日志")
    @PostMapping("/servicemanager/model/audit-log")
    public String enableAuditLog(@RequestBody ModelAuditLogReqVO modelAuditLogReqVO) {
        Map<String, String> modelCodeMap = modelAuditLogReqVO.getModelCodeMap();
        Boolean enable = modelAuditLogReqVO.getEnable();
        if (CollectionUtils.isEmpty(modelCodeMap) || enable == null) {
            return "failed";
        }
        return dataAuditLogService.enableAuditLog(modelCodeMap, enable);
    }
}


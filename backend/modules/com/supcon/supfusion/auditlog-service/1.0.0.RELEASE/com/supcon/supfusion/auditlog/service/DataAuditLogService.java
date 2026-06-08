package com.supcon.supfusion.auditlog.service;

import com.supcon.supfusion.auditlog.service.bo.*;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 数据审计业务日志服务
 * @author caokele
 */
public interface DataAuditLogService {

    /**
     * 查询数据日志列表
     * @param current 当前页号
     * @param pageSize 每页数量
     * @param dataAuditLogQuery 查询参数
     * @return 数据审计日志分页结果
     */
    PageResult<DataAuditLogBO> queryDataLogs(Integer current, Integer pageSize, DataAuditLogQueryBO dataAuditLogQuery);

    /**
     * 查询数据日志模型列表
     * @param traceId 链路跟踪ID
     * @param dataModelQueryBO 查询参数
     * @return 数据日志模型列表
     */
    ListResult<DataModelBO> queryDataModels(Long traceId, DataModelQueryBO dataModelQueryBO);

    /**
     * 查询审计数据日志
     * @param traceId 链路跟踪ID
     * @return
     */
    ResponseEntity<byte[]> queryDataLogRes(Long traceId);

    /**
     * 导出数据日志详情
     * @param traceIds
     * @param all
     */
    void exportAuditLogExcelData(List<Long> traceIds, Boolean all, HttpServletResponse response);

    /**
     * 导出模型数据
     * @param all 是否导出全部
     * @param traceId 路径
     * @param modelCodes 模型code
     */
    void exportAuditLogExcelModel(Boolean all, Long traceId, List<String> modelCodes, HttpServletResponse response);

    /**
     * 导入导出审计模型数据
     * @param traceId
     * @param modelCode
     * @param code
     * @return
     */
    ListResult<DataModelPropertyBO> queryImportDataModelProperty(Long traceId, String modelCode, String code);

    /**
     * 模型启用/不启用审计日志
     */
    String enableAuditLog(Map<String, String> modelCodes, Boolean enable);

    /**
     * 导入导出审计日志（分页）
     *
     * @param current
     * @param pageSize
     * @param traceId
     * @param dataModelQueryBO
     * @return
     */
    PageResult<DataModelBO> queryImportDataModels(Integer current, Integer pageSize, Long traceId, DataModelQueryBO dataModelQueryBO);
}

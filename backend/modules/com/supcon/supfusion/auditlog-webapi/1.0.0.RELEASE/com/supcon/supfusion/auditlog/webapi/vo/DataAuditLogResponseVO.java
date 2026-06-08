package com.supcon.supfusion.auditlog.webapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonDeserializer;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;


/**
 * 数据审计日志
 *
 * @author caokele
 */
@Getter
@Setter
@ToString
@ApiModel("数据审计日志")
public class DataAuditLogResponseVO {
    @JsonSerialize(using = IDJsonSerializer.class)
    @JsonDeserialize(using = IDJsonDeserializer.class)
    @ApiModelProperty(value = "链路跟踪ID", example = "89552823343120385")
    private String traceId;

    @ApiModelProperty(value = "模块名称", example = "采购订单模块")
    private String moduleName;

    @ApiModelProperty(value = "表单名称", example = "采购订单")
    private String formName;

    @ApiModelProperty(value = "操作用户名称", example = "caokele")
    private String operateUserName;

    @ApiModelProperty(value = "操作时间", example = "2020-08-03T21:02:02.000+0000")
    private String operateTime;

    @ApiModelProperty(value = "被操作对象名称", example = "订单001")
    private String modelObjName;

    @ApiModelProperty(value = "被操作对象编码", example = "Order001")
    private String modelObjCode;

    @ApiModelProperty(value = "操作类型")
    private SystemCodeResultDTO operateType;

    @ApiModelProperty(value = "IP地址", example = "127.0.0.1")
    private String ipAddress;

    @ApiModelProperty(value = "是否成功", example = "true")
    private Boolean success;

    @ApiModelProperty(value = "操作描述", example = "新增采购订单")
    private String description;

    @ApiModelProperty(value = "操作异常描述", example = "新增的对象不存在！")
    private String exceptionDescription;

    @ApiModelProperty(value = "文件名称", example = "test.xls")
    private String fileName;

    @ApiModelProperty(value = "文件地址", example = "/inter-api/auditlog/file/download")
    private String fileUrl;

    @ApiModelProperty(value = "数据审计日志列表")
    private List<DataAuditLogResponseVO> children;

    public List<DataAuditLogResponseVO> getChildren() {
        if (children == null) {
            return Collections.emptyList();
        }
        return children;
    }
}

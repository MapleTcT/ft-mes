package com.supcon.supfusion.auditlog.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


/**
 * 数据审计志请求模型
 *
 * @author caokele
 */
@Getter
@Setter
@ToString
@ApiModel("数据审计志请求模型")
public class DataAuditLogQueryVO {

    @ApiModelProperty(value = "所属模块名称，支持模糊搜索", example = "采购订单模块")
    private String moduleName;

    @ApiModelProperty(value = "操作用户名数组", example = "[\"zhangsan\",\"lisi\"]")
    private List<String> userNames;

    @ApiModelProperty(value = "操作开始时间", example = "2020-08-03T21:02:02.000+0000")
    private String operateStartTime;

    @ApiModelProperty(value = "操作结束时间", example = "2020-08-03T21:02:02.000+0000")
    private String operateEndTime;

    @ApiModelProperty(value = "表单名称，支持模糊查询", example = "采购订单")
    private String formName;

    @ApiModelProperty(value = "被操作对象编码，支持模糊查询", example = "auditTest_1.0.0_car_Car")
    private String modelObjCode;

    @ApiModelProperty(value = "被操作对象名称，支持模糊查询", example = "39493390497024")
    private String modelObjName;

    @ApiModelProperty(value = "操作类型，支持多选", example = "[\"ADD\"]")
    private List<String> operateTypes;

    @ApiModelProperty(value = "IP地址，支持模糊搜索", example = "127.0.0.1")
    private String ipAddress;

    @ApiModelProperty(value = "排序字段", example = "sort")
    private String sortKey;

    @ApiModelProperty(value = "倒序排序", example = "true")
    private Boolean desc;
}

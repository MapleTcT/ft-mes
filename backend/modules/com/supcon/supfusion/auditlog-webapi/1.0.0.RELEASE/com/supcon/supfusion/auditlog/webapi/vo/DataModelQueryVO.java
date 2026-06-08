package com.supcon.supfusion.auditlog.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 数据模型请求模型
 *
 * @author caokele
 */
@Getter
@Setter
@ToString
@ApiModel("数据模型请求模型")
public class DataModelQueryVO {

    @ApiModelProperty(value = "被操作对象编码，支持模糊查询", example = "auditTest_1.0.0_car_Car")
    private String modelObjCode;

    @ApiModelProperty(value = "被操作对象名称，支持模糊查询", example = "39493390497024")
    private String modelObjName;

    @ApiModelProperty(value = "操作类型，支持多选", example = "[\"ADD\"]")
    private List<String> operateTypes;
}

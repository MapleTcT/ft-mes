package com.supcon.supfusion.auditlog.webapi.vo;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * 数据模型属性响应模型
 * @author caokele
 */
@Getter
@Setter
@ToString
@ApiModel("数据模型属性响应模型")
public class DataModelPropertyResponseVO {
    @ApiModelProperty(value = "属性名", example = "姓名")
    private String propertyName;

    @ApiModelProperty(value = "当前值", example = "张三")
    private String currentValue;

    @ApiModelProperty(value = "历史值", example = "李四")
    private String historyValue;
}

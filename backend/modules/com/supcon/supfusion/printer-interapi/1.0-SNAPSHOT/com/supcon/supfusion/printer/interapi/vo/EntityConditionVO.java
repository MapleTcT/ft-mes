package com.supcon.supfusion.printer.interapi.vo;

import com.supcon.supfusion.printer.common.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntityConditionVO {

    /**
     * 对象实例模型名称
     */
    @NotBlank(message = Constants.PARAM_MODEL_CODE_NECESSARY)
    @ApiModelProperty(value = "模型名称", required = true)
    private String modelCode;

    /**
     * 属性名称
     */
    @NotBlank(message = Constants.PARAM_PROPERTY_CODE_NECESSARY)
    @ApiModelProperty(value = "属性名称", required = true)
    private String propertyCode;

    //@NotBlank(message = Constants.PARAM_TABLE_NAME_NECESSARY)
    @ApiModelProperty(value = "表名", required = false)
    private String tableName;

    //@NotBlank(message = Constants.PARAM_COLUMN_NAME_NECESSARY)
    @ApiModelProperty(value = "列名", required = false)
    private String columnName;
}
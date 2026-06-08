package com.supcon.supfusion.notification.admin.openapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("主题关联的模板关键字")
public class KeyWordVO {
    /**
     * 关键字
     */
    @ApiModelProperty(value = "关键字", required = true)
    private String key;
}

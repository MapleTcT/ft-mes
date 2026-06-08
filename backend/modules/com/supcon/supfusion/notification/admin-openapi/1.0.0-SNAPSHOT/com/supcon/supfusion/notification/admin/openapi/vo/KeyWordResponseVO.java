package com.supcon.supfusion.notification.admin.openapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel()
public class KeyWordResponseVO {
    /**
     * 主题关联的模板关键字
     */
    @ApiModelProperty(value = "主题关联的模板关键字", required = true)
    private List<KeyWordVO> keyWordVOS;
}

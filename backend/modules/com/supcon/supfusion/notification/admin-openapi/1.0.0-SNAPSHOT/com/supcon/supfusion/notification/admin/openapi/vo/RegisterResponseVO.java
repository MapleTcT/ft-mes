package com.supcon.supfusion.notification.admin.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value="注册返回结果")
public class RegisterResponseVO extends VO {
    /**
     * 协议ID
     */
    @ApiModelProperty(value = "协议Id")
    private String id;
}

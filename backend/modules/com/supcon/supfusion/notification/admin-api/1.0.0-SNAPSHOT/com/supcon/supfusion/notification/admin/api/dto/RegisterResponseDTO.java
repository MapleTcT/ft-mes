package com.supcon.supfusion.notification.admin.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value="注册返回结果")
public class RegisterResponseDTO {
    /**
     * 协议ID
     */
    @ApiModelProperty(value = "协议Id")
    private String id;
}

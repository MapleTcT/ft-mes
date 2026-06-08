package com.supcon.supfusion.rbac.webapi.vo.datapermission;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class DataResouceVO extends VO {
    @ApiModelProperty(value = "业务数据编码")
    @NotNull
    private String resourceCode;
    @ApiModelProperty(value = "业务数据名称")
    @NotNull
    private String resourceName;
    @ApiModelProperty(value = "业务数据类型")
    private String resourceType;
}

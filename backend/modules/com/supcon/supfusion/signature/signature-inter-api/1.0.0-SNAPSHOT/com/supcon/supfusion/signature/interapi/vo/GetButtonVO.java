package com.supcon.supfusion.signature.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author zhang yafei
 */
@Data
public class GetButtonVO extends VO {
    @ApiModelProperty("页码")
    private Integer current = 1;
    @ApiModelProperty("分页大小")
    private Integer pageSize = 20;
    @ApiModelProperty("视图code")
    @NotBlank(message = "视图code不能为空")
    private String code;
}

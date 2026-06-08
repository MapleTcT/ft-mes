package com.supcon.supfusion.signature.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhang yafei
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PositionVo extends VO {
    private static final long serialVersionUID = -2579706387040303319L;
    @ApiModelProperty("岗位id")
    private Long id;

    @ApiModelProperty("岗位code")
    private String code;

    @ApiModelProperty("岗位名字")
    private String name;
}

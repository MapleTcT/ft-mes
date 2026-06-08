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
public class PersonVo extends VO {
    private static final long serialVersionUID = -8374538655865435504L;

    @ApiModelProperty("人员id")
    private Long id;

    @ApiModelProperty("人员code")
    private String code;

    @ApiModelProperty("人员名字")
    private String name;

}

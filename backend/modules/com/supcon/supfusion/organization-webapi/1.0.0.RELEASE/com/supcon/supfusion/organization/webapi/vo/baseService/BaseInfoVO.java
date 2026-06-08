package com.supcon.supfusion.organization.webapi.vo.baseService;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 人员相关信息
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BaseInfoVO extends VO {

    /**
     * id主键
     */
    @ApiModelProperty(value = "id主键")
    private Long id;

    /**
     * 编码或编号
     */
    @ApiModelProperty(value = "编码或编号")
    private String code;

    /**
     * 名称
     */
    @ApiModelProperty(value = "名称")
    private String name;


}

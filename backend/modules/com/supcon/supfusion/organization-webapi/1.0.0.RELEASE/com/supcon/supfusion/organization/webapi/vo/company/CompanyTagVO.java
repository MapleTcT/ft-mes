package com.supcon.supfusion.organization.webapi.vo.company;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;

/**
 * 公司标签VO
 */
/**
 * 部门详细信息
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyTagVO extends VO {

    /**
     * 标签类型
     */
    @ApiModelProperty(value = "标签类型", required = false)
    private String type;

    /**
     * 标签名称
     */
    @NotBlank(message = Constants.COMPANY_TAG_NAME_NECESSARY)
    @ApiModelProperty(value = "标签名称", required = true)
    private String name;
}

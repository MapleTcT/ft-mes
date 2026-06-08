package com.supcon.supfusion.organization.webapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;

/**
 * 人员新增信息
 *
 * @author shidongsheng
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonLoginInfoVO extends VO {

    /**
     * 性别的编码值name
     */
    @NotBlank(message = Constants.PERSON_PARAM_GENDER_NECESSARY)
    @ApiModelProperty(value = "人员性别", required = true)
    private String gender;

    /**
     * 头像地址
     */
    @ApiModelProperty(value = "头像地址")
    private String avatarUrl;
}

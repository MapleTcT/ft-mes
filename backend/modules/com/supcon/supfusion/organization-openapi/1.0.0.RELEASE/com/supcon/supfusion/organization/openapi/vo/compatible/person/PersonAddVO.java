package com.supcon.supfusion.organization.openapi.vo.compatible.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 人员新增信息
 *
 * @author zhuangmh
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonAddVO extends VO {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 人员编码
     */
    @NotBlank(message = Constants.PERSON_PARAM_CODE_NECESSARY)
    @Size(min = 1, max = 50, message = Constants.PERSON_CODE_LENGTH_ERROR)
    private String code;

    /**
     * 人员名称
     */
    @NotBlank(message = Constants.PERSON_PARAM_NAME_NECESSARY)
    @Size(min = 1, max = 50, message = Constants.PERSON_NAME_LENGTH_ERROR)
    private String showName;
    
    /**
     * 主岗id
     */
    private Long mainPosition;

    /**
     * 性别的编码值 1-男 0-女 
     */
    private int gender;

    /**
     * 人员状态的编码值 1->离职 0->在职
     */
    private int status;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 描述
     */
    @Size(max = 255, message = Constants.PERSON_DESCRIPTION_LENGTH_ERROR)
    private String description;

}

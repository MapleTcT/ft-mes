package com.supcon.supfusion.organization.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 人员修改信息
 *
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonUpdateDTO extends DTO {

    /**
     * 人员id
     */
    //@NotNull(message = Constants.PERSON_PARAM_ID_NECESSARY)
    //@ApiModelProperty(value = "人员id", required = true)
    private Long id;


    /**
     * 人员姓名
     */
    //@Size(min = 1, max = 200, message = Constants.PERSON_NAME_LENGTH_ERROR)
    //@ApiModelProperty(value = "人员姓名")
    private String name;

    /**
     * 性别
     */
    //@ApiModelProperty(value = "人员性别")
    private String gender;

    /**
     * 主岗id
     */
    //@ApiModelProperty(value = "主岗id")
    private Long mainPosition;

    /**
     * 手机号
     */
    //@ApiModelProperty(value = "手机号")
    private String phone;

    /**
     * 人员状态
     */
    //@ApiModelProperty(value = "人员状态")
    private String status;

    /**
     * 涉密等级
     */
    //@ApiModelProperty(value = "涉密等级")
    private String classifiedLevel;

    /**
     * 邮箱
     */
    //@ApiModelProperty(value = "邮箱地址")
    private String email;

    /**
     * 描述
     */
    //@Size(max = 500, message = Constants.PERSON_DESCRIPTION_LENGTH_ERROR)
    //@ApiModelProperty(value = "人员描述")
    private String description;
}

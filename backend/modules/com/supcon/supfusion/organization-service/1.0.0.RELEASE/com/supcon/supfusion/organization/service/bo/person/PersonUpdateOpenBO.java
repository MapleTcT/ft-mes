package com.supcon.supfusion.organization.service.bo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
public class PersonUpdateOpenBO extends BO {

    /**
     * 人员id
     */
    private String code;


    /**
     * 人员姓名
     */
    private String name;

    /**
     * 性别
     */
    private String gender;

    /**
     * 主岗id
     */
    private String mainPositionCode;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 人员状态
     */
    private String status;

    /**
     * 涉密等级
     */
/*    @ApiModelProperty(value = "涉密等级")
    private String classifiedLevel;*/

    /**
     * 邮箱
     */
    private String email;

    /**
     * 描述
     */
    private String description;

    private String directLeaderCode;

    private String grandLeaderCode;

    /**
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 签名地址
     */
    private String signPicUrl;

    private String entryDate;

    private String title;

    private String qualification;

    private String education;

    private String major;

    private String idNumber;
}

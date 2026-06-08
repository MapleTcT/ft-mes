package com.supcon.supfusion.organization.service.bo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

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
public class PersonAddOpenBO extends BO {
    /**
     * 人员编码
     */
    private String code;

    /**
     * 人员名称
     */
    private String name;

    /**
     * 性别的编码值name
     */
    private String gender;

    /**
     * 主岗id
     */
    private String mainPositionCode;

    /**
     * 公司id
     */
    /*@NotNull(message = Constants.COM_PARAM_ID_NOTNULL)
    @ApiModelProperty(value = "公司id", required = true)
    private Long companyId;*/
    /**
     * 人员状态的编码值name
     */
    private String status;

    /**
     * 涉密等级的编码值name
     */
/*    @ApiModelProperty(value = "涉密等级")
    private String classifiedLevel;*/

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
    private String description;

    /**
     * 是否创建用户账号
     */
/*    @ApiModelProperty(value = "是否创建用户,默认是false")
    private Boolean createUser = false;*/

    /**
     * 用户名
     */
/*    @ApiModelProperty(value = "用户名")
    private String userName;*/

    /**
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 签名地址
     */
    private String signPicUrl;

    /**
     * 密码
     */
    /*@ApiModelProperty(value = "用户密码")
    private String password;

    *//**
     * 用户描述
     *//*
    //@Size(min = 1, max = 255)
    @ApiModelProperty(value = "用户描述")
    private String userDescription;*/

    /**
     * 用户关联的角色id
     */
/*    @ApiModelProperty(value = "角色id")
    private List<Long> roles;

    @ApiModelProperty(value = "角色name")
    private List<String> roleNames;*/

    private String directLeaderCode;

    private String grandLeaderCode;

    private String entryDate;

    private String title;

    private String qualification;

    private String education;

    private String major;

    private String idNumber;
}

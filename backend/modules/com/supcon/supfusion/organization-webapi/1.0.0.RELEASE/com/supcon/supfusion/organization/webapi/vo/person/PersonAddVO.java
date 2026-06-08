package com.supcon.supfusion.organization.webapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

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
public class PersonAddVO extends VO {
    /**
     * 人员编码
     */
    @NotBlank(message = Constants.PERSON_PARAM_CODE_NECESSARY)
    @Size(min = 1, max = 50, message = Constants.PERSON_CODE_LENGTH_ERROR)
    @Pattern(regexp = "^[0-9a-zA-Z_]{1,}$", message = Constants.ORG_CODE_PATTERN)
    @ApiModelProperty(value = "人员编号", required = true)
    private String code;

    /**
     * 人员名称
     */
    @NotBlank(message = Constants.PERSON_NAME_LENGTH_ERROR)
    @Size(min = 1, max = 200, message = Constants.PERSON_NAME_LENGTH_ERROR)
    @ApiModelProperty(value = "人员姓名", required = true)
    private String name;

    /**
     * 性别的编码值name
     */
    @NotBlank(message = Constants.PERSON_PARAM_GENDER_NECESSARY)
    @ApiModelProperty(value = "人员性别", required = true)
    private String gender;

    /**
     * 主岗id
     */
    @NotNull(message = Constants.PERSON_PARAM_MAIN_POSITION_NECESSARY)
    @ApiModelProperty(value = "人员主岗id", required = true)
    private Long mainPosition;

    /**
     * 公司id
     */
    /*@NotNull(message = Constants.COM_PARAM_ID_NOTNULL)
    @ApiModelProperty(value = "公司id", required = true)
    private Long companyId;*/
    /**
     * 人员状态的编码值name
     */
    @ApiModelProperty(value = "人员状态")
    private String status = Constants.ON_WORK_CODE;

    /**
     * 涉密等级的编码值name
     */
    @ApiModelProperty(value = "涉密等级")
    private String classifiedLevel;

    /**
     * 手机号码
     */
    @ApiModelProperty(value = "手机号")
    private String phone;

    /**
     * 邮箱
     */
    @ApiModelProperty(value = "邮箱地址")
    private String email;

    /**
     * 描述
     */
    @Size(max = 500, message = Constants.PERSON_DESCRIPTION_LENGTH_ERROR)
    @ApiModelProperty(value = "人员描述")
    private String description;

    /**
     * 是否创建用户账号
     */
    @ApiModelProperty(value = "是否创建用户,默认是false")
    private Boolean createUser = false;

    /**
     * 用户名
     */
    @ApiModelProperty(value = "用户名")
    private String userName;

    /**
     * 头像地址
     */
    @ApiModelProperty(value = "头像地址")
    private String avatarUrl;

    /**
     * 签名地址
     */
    @ApiModelProperty(value = "签名地址")
    private String signPicUrl;

    /**
     * 密码
     */
    @ApiModelProperty(value = "用户密码")
    private String password;

    /**
     * 用户描述
     */
    //@Size(min = 1, max = 255)
    @ApiModelProperty(value = "用户描述")
    private String userDescription;

    /**
     * 用户关联的角色id
     */
    @ApiModelProperty(value = "角色id")
    private List<Long> roles;

    @ApiModelProperty(value = "角色name")
    private List<String> roleNames;

    @ApiModelProperty(value = "直属领导id")
    private Long directLeaderId;

    @ApiModelProperty(value = "隔级领导id")
    private Long grandLeaderId;

    @ApiModelProperty(value = "入职日期")
    private String entryDate;

    @ApiModelProperty(value = "职称")
    private String title;

    @ApiModelProperty(value = "资质")
    @Size(max = 200, message = Constants.PERSON_QUALIFICATION_LENHTH_ERROR)
    private String qualification;

    @ApiModelProperty(value = "学历")
    private String education;

    @ApiModelProperty(value = "专业")
    @Size(max = 200, message = Constants.PERSON_MAJOR_LENHTH_ERROR)
    private String major;

    @ApiModelProperty(value = "身份证号")
    @Pattern(regexp = "[^\\u4e00-\\u9fa5]+", message = Constants.PERSON_ID_NUMBER_ERROR)
    @Size(max = 200, message = Constants.PERSON_ID_NUMBER_ERROR)
    private String idNumber;
}

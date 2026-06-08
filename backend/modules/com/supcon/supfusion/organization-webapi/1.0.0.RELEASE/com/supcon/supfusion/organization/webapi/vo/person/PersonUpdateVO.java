package com.supcon.supfusion.organization.webapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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
public class PersonUpdateVO extends VO {

    /**
     * 人员id
     */
    @NotNull(message = Constants.PERSON_PARAM_ID_NECESSARY)
    @ApiModelProperty(value = "人员id", required = true)
    private Long id;


    /**
     * 人员姓名
     */
    @Size(min = 1, max = 200, message = Constants.PERSON_NAME_LENGTH_ERROR)
    @ApiModelProperty(value = "人员姓名")
    private String name;

    /**
     * 性别
     */
    @ApiModelProperty(value = "人员性别")
    private String gender;

    /**
     * 主岗id
     */
    @ApiModelProperty(value = "主岗id")
    private Long mainPosition;

    /**
     * 手机号
     */
    @ApiModelProperty(value = "手机号")
    private String phone;

    /**
     * 人员状态
     */
    @ApiModelProperty(value = "人员状态")
    private String status;

    /**
     * 涉密等级
     */
    @ApiModelProperty(value = "涉密等级")
    private String classifiedLevel;

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

    @ApiModelProperty(value = "直属领导id")
    private Long directLeaderId;

    @ApiModelProperty(value = "隔级领导id")
    private Long grandLeaderId;

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

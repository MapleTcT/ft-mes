package com.supcon.supfusion.organization.webapi.vo.company;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class CompanyVO extends VO {

    /**
     * 公司id
     */
    private Long id;

    /**
     * 公司编码
     */
    @NotBlank(message = Constants.COM_PARAM_CODE_NOTNULL)
    @Size(min = 1, max = 50, message = Constants.COMPANY_PARAM_CODE_LENGTH_ERROR)
    @Pattern(regexp = "^[0-9a-zA-Z_]{1,}$", message = Constants.ORG_CODE_PATTERN)
    private String code;

    /**
     * 集团或公司简称
     */
    @NotBlank(message = Constants.COM_PARAM_SHORTNAME_NOTNULL)
    @Size(min = 1, max = 200, message = Constants.COMPANY_PARAM_SHORTNAME_LENGTH_ERROR)
    private String shortName;

    /**
     * 集团或公司全称
     */
    @NotBlank(message = Constants.COM_PARAM_FULLNAME_NOTNULL)
    @Size(min = 1, max = 200, message = Constants.COMPANY_PARAM_FULLNAME_LENGTH_ERROR)
    private String fullName;

    /**
     * 管理员用户名
     */
    @NotBlank(message = Constants.COM_ADMIN_USERNAME_NECESSARY)
    private String userName;

    /**
     * 管理员密码
     */
    @NotBlank(message = Constants.COM_ADMIN_PASSWORD_NECESSARY)
    private String password;
    /**
     * 公司全路径
     */
    private String fullPath;

    /**
     * 节点层级
     */
    private Integer layNo;

    /**
     * 同层级下节点顺序
     */
    private Double sort;

    /**
     * 父级节点id
     */
    private Long parentId;

    /**
     * 描述
     */
    @Size(max = 500, message = Constants.COMPANY_PARAM_DESC_LENGTH_ERROR)
    private String description;

    /**
     * 标签
     */
     private List<String> tags;

}

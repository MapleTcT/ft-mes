package com.supcon.supfusion.organization.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CompanyResultDTO extends DTO {

    /**
     * 公司id
     */
    private Long id;

    /**
     * 公司编码
     */
    //@NotBlank(message = Constants.COM_PARAM_CODE_NOTNULL)
    private String code;

    /**
     * 集团或公司简称
     */
    //@NotBlank(message = Constants.COM_PARAM_SHORTNAME_NOTNULL)
    private String shortName;

    /**
     * 集团或公司全称
     */
    //@NotBlank(message = Constants.COM_PARAM_FULLNAME_NOTNULL)
    private String fullName;
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
    private String description;
}

package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class CompanyUpdateVO extends VO {

    /**
     * 公司id
     */
    private Long id;

    /**
     * 集团或公司简称
     */
    @NotBlank(message = Constants.COM_PARAM_SHORTNAME_NOTNULL)
    private String shortName;

    /**
     * 集团或公司全称
     */
    @NotBlank(message = Constants.COM_PARAM_FULLNAME_NOTNULL)
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

    private String[] labels = {};

    /**
     * 描述
     */
    private String description;

    /**
     * 标签
     */
    private List<String> tags;

}

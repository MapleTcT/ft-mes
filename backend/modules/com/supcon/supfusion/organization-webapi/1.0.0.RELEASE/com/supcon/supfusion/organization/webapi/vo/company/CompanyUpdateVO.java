package com.supcon.supfusion.organization.webapi.vo.company;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonDeserializer;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
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
    @Size(min = 1, max = 200, message = Constants.COMPANY_PARAM_SHORTNAME_LENGTH_ERROR)
    private String shortName;

    /**
     * 集团或公司全称
     */
    @NotBlank(message = Constants.COM_PARAM_FULLNAME_NOTNULL)
    @Size(min = 1, max = 200, message = Constants.COMPANY_PARAM_FULLNAME_LENGTH_ERROR)
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

    //private String[] labels = {};

    /**
     * 描述
     */
    @Size(max = 255, message = Constants.COMPANY_PARAM_DESC_LENGTH_ERROR)
    private String description;

    /**
     * 标签
     */
    private List<String> tags;

}

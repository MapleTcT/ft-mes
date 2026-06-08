package com.supcon.supfusion.organization.service.bo.compatible;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.common.constants.Constants;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 老版本公司响应结果
 */
@Data
public class CompanyResultBO {

    /**
     * 公司code
     */
    private String code;

    /**
     * 公司唯一标识,对应old_id
     */
    private String name;

    /**
     * 公司名称,对应short_name
     */
    private String showName;

    /**
     * 集团或公司全称
     */
    private String orgType = "Company";

    /**
     * 公司地址
     */
    private String address;

    /**
     * 公司全名
     */
    private String fullName;

    /**
     * 描述
     */
    private String description;

}

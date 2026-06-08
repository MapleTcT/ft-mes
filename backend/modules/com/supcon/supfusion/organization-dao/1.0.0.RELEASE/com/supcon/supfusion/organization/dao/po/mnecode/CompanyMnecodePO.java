package com.supcon.supfusion.organization.dao.po.mnecode;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = CompanyMnecodePO.TABLE_NAME, autoResultMap = true)
public class CompanyMnecodePO extends BaseEntity {

    public static final String TABLE_NAME = "org_company_mnecode";

    private Long id;

    /**
     * 版本号
     */
    private Long rowVersion;

    /**
     * 语言
     */
    private String language;

    /**
     * 公司id
     */
    private Long companyId;

    /**
     * 助记码
     */
    private String mneCode;

    /**
     * 公司简称
     */
    private String companyShortName;
}
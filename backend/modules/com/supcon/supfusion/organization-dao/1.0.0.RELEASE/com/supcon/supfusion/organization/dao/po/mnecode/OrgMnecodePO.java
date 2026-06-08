package com.supcon.supfusion.organization.dao.po.mnecode;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = OrgMnecodePO.TABLE_NAME, autoResultMap = true)
public class OrgMnecodePO extends BaseEntity {

    public static final String TABLE_NAME = "org_mnecode";

    private Long id;

    /**
     * 语言
     */
    private String language;

    /**
     * 公司id、岗位id、部门id、人员id
     */
    private Long orgId;

    /**
     * 助记码
     */
    private String mneCode;
}

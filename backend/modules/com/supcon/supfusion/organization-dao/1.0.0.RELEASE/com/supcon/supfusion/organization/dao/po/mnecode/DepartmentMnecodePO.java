package com.supcon.supfusion.organization.dao.po.mnecode;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = DepartmentMnecodePO.TABLE_NAME, autoResultMap = true)
public class DepartmentMnecodePO extends BaseEntity {

    public static final String TABLE_NAME = "org_department_mnecode";

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
     * 部门id
     */
    private Long deptId;

    /**
     * 助记码
     */
    private String mneCode;

    /**
     * 部门名称
     */
    private String deptName;
}
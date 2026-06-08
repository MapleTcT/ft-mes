package com.supcon.supfusion.organization.dao.po.group;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.OptimisticLockerBaseEntity;
import lombok.*;

/**
 * 多公司组PO类
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = GroupPO.TABLE_NAME, autoResultMap = true)
public class GroupPO extends OptimisticLockerBaseEntity {
    public static final String TABLE_NAME = "org_group";
    private Long id;
    private String code;
    private String name;
    private String description;
    private String fullPath;
    private double sort;
    private Long companyId;
    private Long managerId;
    private String managerName;

}

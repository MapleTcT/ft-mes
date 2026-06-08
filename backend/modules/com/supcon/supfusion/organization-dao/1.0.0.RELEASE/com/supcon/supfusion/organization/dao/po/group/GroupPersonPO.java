package com.supcon.supfusion.organization.dao.po.group;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * 岗位人员关系类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = GroupPersonPO.TABLE_NAME, autoResultMap = true)
public class GroupPersonPO extends BaseEntity {

    public static final String TABLE_NAME = "org_group_person";

    /**
     * 主键id
     */
    private Long id;

    /**
     * 岗位id
     */
    private Long groupId;

    /**
     * 人员id
     */
    private Long personId;



    /**
     * 是否有效
     */
    private Boolean valid = true;
}

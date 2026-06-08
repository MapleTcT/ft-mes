package com.supcon.supfusion.organization.dao.po.mnecode;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = PersonMnecodePO.TABLE_NAME, autoResultMap = true)
public class PersonMnecodePO extends BaseEntity {

    public static final String TABLE_NAME = "org_person_mnecode";

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
     * 人员id
     */
    private Long personId;

    /**
     * 助记码
     */
    private String mneCode;

    /**
     * 人员名称
     */
    private String personName;
}
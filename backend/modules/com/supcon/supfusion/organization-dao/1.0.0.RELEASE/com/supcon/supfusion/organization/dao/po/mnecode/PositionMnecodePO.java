package com.supcon.supfusion.organization.dao.po.mnecode;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = PositionMnecodePO.TABLE_NAME, autoResultMap = true)
public class PositionMnecodePO extends BaseEntity {

    public static final String TABLE_NAME = "org_position_mnecode";

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
     * 岗位id
     */
    private Long positionId;

    /**
     * 助记码
     */
    private String mneCode;

    /**
     * 岗位名称
     */
    private String positionName;
}
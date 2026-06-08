package com.supcon.supfusion.printer.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = EntityPageUrlPO.TABLE_NAME, autoResultMap = true)
public class EntityPageUrlPO extends BaseEntity {

    public static final String TABLE_NAME = "printer_object_iframe";

    /**
     * 主键id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long id;

    /**
     * 对象实例数据源展示名
     */
    private String name;

    /**
     * 注册实体服务来源
     */
    private Integer source;

    /**
     * iframe实体url
     */
    private String entityUrl;

    /**
     * 是否有效
     */
    private Boolean valid;
}

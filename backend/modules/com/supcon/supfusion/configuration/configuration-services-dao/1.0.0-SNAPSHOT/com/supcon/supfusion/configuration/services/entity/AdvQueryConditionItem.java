package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * 高级查询子句条目类
 *
 * @author 谭正阳
 */
@Entity
@Table(name = AdvQueryConditionItem.TABLE_NAME)
@Data
public class AdvQueryConditionItem extends AbstractAuditUniqueIdEntity implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 6914588399621174798L;

    /**
     * table name
     */
    public static final String TABLE_NAME = "ec_adv_query_condition_item";
    /**
     * 类型<br>
     * 0:常规查询条件节点<br>
     * 1:逻辑节点<br>
     * 2:关连表<br>
     */
    @Column(length = 1, nullable = true)
    private String type = "0";
    /**
     * 类型<br>
     * 0:自定义查询<br>
     * 1:高级查询<br>
     * <br>
     */
    @Column(length = 1, nullable = true)
    private Integer advType = 0;
    // ~=======================type 0====================
    /**
     * 字段名
     */
    @Column(length = 200, nullable = true)
    private String columnName = null;

    /**
     * 字段类型
     */
    @Enumerated(EnumType.STRING)
    private DbColumnType dbColumnType = DbColumnType.TEXT;

    /**
     * 值匹配串
     */
    @Column(length = 40, nullable = true)
    private String paramStr = "?";

    /**
     * 操作符
     */
    @Column(length = 30, nullable = true)
    private String operator = null;

    /**
     * 值
     */
    @Column(length = 2000, nullable = true)
    private String value = null;

    /**
     * 值(额外)
     */
    @Transient
    private String extraValue = null;

    // ~=======================type 1====================
    /**
     * 逻辑
     */
    @Column(length = 10, nullable = true)
    private String logic = "and";

    // ~=======================type 2====================
    /**
     * 关连信息(关联表表名，关连字段名，本表表名，本表对应字段名)
     */
    @Column(length = 100, nullable = true)
    private String joinInfo = null;

    /**
     * 关连实体ID
     */
    private String modelCode = null;

    /**
     * 直接父查询子句
     */
    @ManyToOne
    @JoinColumn(name = "PARENT_ID")
    private AdvQueryConditionItem parent = null;

    /**
     * 直接父查询条件
     */
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "CONDITION_ID")
    private AdvQueryCondition condition = null;

    /**
     * 直接下级查询子句
     */
    @Transient
    private List<AdvQueryConditionItem> subconds = null;

    /**
     * 条件等级
     */
    private String valuebak = "0";

    private String name = null;

    /**
     * 主实体别名
     */
    @Column(length = 200, nullable = true)
    private String modelAlias = null;

    public String getValue() {
        return value == null ? "" : value.trim();
    }

    public String getModelAlias() {
        return modelAlias != null ? modelAlias : "";
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        String name = AdvQueryConditionItem.class.getName();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AdvQueryConditionItem)) {
            return false;
        }
        AdvQueryConditionItem other = (AdvQueryConditionItem) obj;
        if (getId() == null) {
            if (other.getId() != null) {
                return false;
            }
        } else if (!getId().equals(other.getId())) {
            return false;
        }
        return true;
    }

    @Override
    protected String _getEntityName() {
        return AdvQueryConditionItem.class.getName();
    }


    public AdvQueryConditionItem getParent() {
        return parent;
    }


    public AdvQueryCondition getCondition() {
        return condition;
    }
}

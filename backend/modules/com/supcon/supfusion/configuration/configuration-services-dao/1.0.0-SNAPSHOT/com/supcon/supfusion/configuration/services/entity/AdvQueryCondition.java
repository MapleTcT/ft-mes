package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.base.entities.Role;
import com.supcon.supfusion.base.entities.User;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 高级查询条件类
 *
 * @author 谭正阳
 */
@Data
@Entity
//@Table(name = AdvQueryCondition.TABLE_NAME)
public class AdvQueryCondition extends AbstractAuditUniqueIdEntity implements Serializable {
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    /**
     *
     */
    private static final long serialVersionUID = 6437287216798097678L;

    /**
     * table name
     */
    public static final String TABLE_NAME = "ec_adv_query_condition";

    /**
     * owner
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "OWNER_ID")
    @Fetch(FetchMode.SELECT)
    private User owner = null;

    /**
     * 直接下级查询子句
     */
    @Transient
    private List<AdvQueryConditionItem> subconds = null;

    /**
     * 主实体别名
     */
    @Column(length = 200, nullable = true)
    private String modelAlias = null;

    /**
     * 条件名称
     */
    @Column(length = 200, nullable = true)
    private String condName = null;

    /**
     * 备注
     */
    @Column(length = 2000, nullable = true)
    private String remark = null;

    /**
     * 备注
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "VIEW_CODE", nullable = false)
    @Fetch(FetchMode.SELECT)
    private View view = null;

    /**
     * 是否是公共方案
     */
    @Column(name = "ADMIN_FLAG", columnDefinition = "INTEGER")
    private boolean adminFlag = false;

    /**
     * 生成的sql
     */
    @Transient
    private String sql = null;

    /**
     * 值
     */
    @Transient
    private List<Object> values = new ArrayList<Object>();

    /**
     * 布局名(新布局)
     */
    @Column(length = 200, nullable = true)
    private String layoutName;


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        String name = Role.class.getName();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
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
        if (!(obj instanceof AdvQueryCondition)) {
            return false;
        }
        AdvQueryCondition other = (AdvQueryCondition) obj;
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
        return AdvQueryCondition.class.getName();
    }

}

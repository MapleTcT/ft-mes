package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * 默认视图的高级查询条件类
 *
 * @author 谭正阳
 */
@Data
@javax.persistence.Entity
@Table(name = DefaultAdvCond.TABLE_NAME)
public class DefaultAdvCond extends AbstractCodeEntity implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 6437287216798097678L;
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    /**
     * table name
     */
    public static final String TABLE_NAME = "ec_default_adv_cond";

    /**
     * 内容
     */
    @Lob
    private String content = null;

    /**
     * 视图code
     */
    @Column(name = "VIEW_CODE", length = 2000)
    private String viewCode = null;


    @Override
    protected String _getEntityName() {
        return DefaultAdvCond.class.getName();
    }

}

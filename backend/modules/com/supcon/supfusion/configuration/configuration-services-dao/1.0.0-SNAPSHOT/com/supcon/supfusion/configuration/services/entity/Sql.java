package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Index;

import javax.persistence.Column;
import javax.persistence.Transient;
import java.io.Serializable;

@Data
@javax.persistence.Entity
//@Table(name = Sql.TABLE_NAME)
public class Sql extends AbstractCodeEntity implements Serializable {
    private static final long serialVersionUID = -7436616994105114860L;
        public static final String TABLE_NAME = "ec_sql";
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    @Column(length = 4000, name = "QUERY_SQL")
    private String sql;

    //	private Long viewId;
    @Column(name = "TYPE")
    @Index(name = "ind_sql_type", columnNames = {"type"})
    private Integer type;
    @Column(name = "VIEW_CODE")
    @Index(name = "ind_sql_view_code", columnNames = {"viewCode"})
    private String viewCode;
    private String dataGridCode;        //增强型视图里面的列表PTcode
    private Boolean projFlag;

    public static final int TYPE_LIST_PENDING = 5;
    public static final int TYPE_LIST_QUERY = 6;
    public static final int TYPE_LIST_REFERENCE = 7;
    public static final int TYPE_USED_MNECODE = 8;
    public static final int TYPE_USED_TREE = 9;
    public static final int TYPE_USED_ORDERBY = 4;
    public static final int TYPE_USED_TOTALS = 3;


    @Override
    protected String _getEntityName() {
        return Sql.class.getName();
    }

}

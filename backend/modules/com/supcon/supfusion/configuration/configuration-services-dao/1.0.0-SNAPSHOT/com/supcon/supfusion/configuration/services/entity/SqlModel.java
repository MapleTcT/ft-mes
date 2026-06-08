package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@javax.persistence.Entity
@Table(name = SqlModel.TABLE_NAME)
public class SqlModel extends AbstractCodeEntity {

    public static final String TABLE_NAME = "ec_sql_model";

    private String modelSql;
    private String oracleSql;
    private String sqlserverSql;
    private String mariadbSql;
    private String oracleView;
    private String sqlserverView;
    private String mariadbView;
    @Transient
    private String currentDbSql;
    @Transient
    private String currentDbType;
//    private Boolean isError = false;
    @Transient
    private List<SqlModelColumn> dbColumns;
    @Transient
    private String properties;

    @Override
    protected String _getEntityName() {
        return this.getClass().getName();
    }
}

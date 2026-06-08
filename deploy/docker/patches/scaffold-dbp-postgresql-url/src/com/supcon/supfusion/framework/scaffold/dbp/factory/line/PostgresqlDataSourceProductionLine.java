package com.supcon.supfusion.framework.scaffold.dbp.factory.line;

import com.supcon.supfusion.framework.scaffold.dbp.DataSourceConfig;
import com.supcon.supfusion.framework.scaffold.dbp.factory.DataSourceProductionMaterial;

public class PostgresqlDataSourceProductionLine extends AbstractDataSourceProductionLine {
    @Override
    protected String getJdbcUrl(DataSourceProductionMaterial material, DataSourceConfig config) {
        return String.format(
                "jdbc:postgresql://%s:%s/%s",
                material.getHost(),
                material.getPort().toString(),
                material.getDbName()
        );
    }

    @Override
    protected String getDatabaseType() {
        return "postgresql";
    }

    @Override
    protected String getJdbcDriverClassName() {
        return "org.postgresql.Driver";
    }
}

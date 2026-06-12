package com.supcon.supfusion.framework.scaffold.dbp.factory.line;

import com.supcon.supfusion.framework.scaffold.dbp.DataSourceConfig;
import com.supcon.supfusion.framework.scaffold.dbp.factory.DataSourceProductionMaterial;

public class PostgresqlDataSourceProductionLine extends AbstractDataSourceProductionLine {
    @Override
    protected String getJdbcUrl(DataSourceProductionMaterial material, DataSourceConfig config) {
        String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%s/%s",
                material.getHost(),
                material.getPort().toString(),
                material.getDbName()
        );
        return appendJdbcParameter(jdbcUrl, "defaultAutoCommit=false");
    }

    @Override
    protected String getDatabaseType() {
        return "postgresql";
    }

    @Override
    protected String getJdbcDriverClassName() {
        return "org.postgresql.Driver";
    }

    private String appendJdbcParameter(String jdbcUrl, String parameter) {
        if (jdbcUrl.contains(parameter.substring(0, parameter.indexOf("=") + 1))) {
            return jdbcUrl;
        }
        return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + parameter;
    }
}

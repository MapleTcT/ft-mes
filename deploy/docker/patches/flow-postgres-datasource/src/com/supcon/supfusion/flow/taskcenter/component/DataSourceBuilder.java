package com.supcon.supfusion.flow.taskcenter.component;

import com.baomidou.mybatisplus.annotation.DbType;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.DBProxyException;
import com.supcon.supfusion.framework.boot.scaffold.dbp.DataSourceConnectionProperties;
import com.supcon.supfusion.framework.boot.scaffold.dbp.MultiTenantDataSourceProperties;
import com.supcon.supfusion.framework.scaffold.dbp.factory.DataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.DataSourceProductionMaterial;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.MssqlDataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.MysqlDataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.OracleDataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.PostgresqlDataSourceProductionLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DataSourceBuilder {

    @Autowired
    @Lazy
    private MultiTenantDataSourceProperties dsConfig;
    @Autowired
    private DataSourceConnectionProperties dscp;

    public DataSource build(String dbName, String dbType, String host, int port, String username,
                            String password, String tenantId) {
        DataSourceProductionMaterial material = DataSourceProductionMaterial.builder()
                .dbName(dbName)
                .host(host)
                .port(port)
                .username(username)
                .password(password).build();
        DataSourceProductionLine line;
        switch (DbType.getDbType(dbType)) {
            case MARIADB:
            case MYSQL:
                line = new MysqlDataSourceProductionLine();
                break;
            case POSTGRE_SQL:
                line = new PostgresqlDataSourceProductionLine();
                break;
            case SQL_SERVER:
                line = new MssqlDataSourceProductionLine();
                break;
            case ORACLE:
                line = new OracleDataSourceProductionLine();
                line.setDbVersion(dscp.getSystem().getDbVersion());
                break;
            default:
                throw new DBProxyException(FlowErrorEnum.NOT_SUPPORT_TYPE);
        }
        return line.build(material, dsConfig, tenantId);
    }
}

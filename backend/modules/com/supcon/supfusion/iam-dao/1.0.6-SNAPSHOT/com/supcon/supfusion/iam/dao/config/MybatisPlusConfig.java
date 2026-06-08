package com.supcon.supfusion.iam.dao.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.MariaDBDialect;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.MySqlDialect;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.OracleDialect;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.SQLServer2005Dialect;
import com.baomidou.mybatisplus.extension.plugins.pagination.optimize.JsqlParserCountOptimize;
import com.supcon.supfusion.framework.boot.scaffold.dbp.DataSourceConnectionProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ${description}
 *
 * @author huangjianbo
 * @create 2020/5/14 16:57
 */

@Configuration()
public class MybatisPlusConfig {
    @Autowired
    private DataSourceConnectionProperties dataSourceConnectionProperties;

    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        // 设置请求的页面大于最大页后操作， true调回到首页，false 继续请求  默认false
        // paginationInterceptor.setOverflow(false);
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        // paginationInterceptor.setLimit(500);
        // 开启 count 的 join 优化,只针对部分 left join
        paginationInterceptor.setCountSqlParser(new JsqlParserCountOptimize(true));
        //todo 暂时写死
        if (dataSourceConnectionProperties.getUseSystem()) {
            if ("mariadb".equals(dataSourceConnectionProperties.getSystem().getDbType())) {
                paginationInterceptor.setDbType(DbType.MARIADB);
                MariaDBDialect mariaDBDialect = new MariaDBDialect();
                paginationInterceptor.setDialect(mariaDBDialect);
            } else if ("mysql".equals(dataSourceConnectionProperties.getSystem().getDbType())) {
                paginationInterceptor.setDbType(DbType.MYSQL);
                MySqlDialect mySqlDialect = new MySqlDialect();
                paginationInterceptor.setDialect(mySqlDialect);
            } else if ("oracle".equals(dataSourceConnectionProperties.getSystem().getDbType())) {
                paginationInterceptor.setDbType(DbType.ORACLE);
                OracleDialect oracleDialect = new OracleDialect();
                paginationInterceptor.setDialect(oracleDialect);
            } else if ("sqlserver".equals(dataSourceConnectionProperties.getSystem().getDbType())) {
                paginationInterceptor.setDbType(DbType.SQL_SERVER2005);
                SQLServer2005Dialect sqlServer2005Dialect = new SQLServer2005Dialect();
                paginationInterceptor.setDialect(sqlServer2005Dialect);
            }
        }
        return paginationInterceptor;
    }

}

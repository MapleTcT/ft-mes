package com.supcon.supfusion.auth.service.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.MariaDBDialect;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.OracleDialect;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.SQLServerDialect;
import com.baomidou.mybatisplus.extension.plugins.pagination.optimize.JsqlParserCountOptimize;
import org.springframework.context.annotation.Bean;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/14 16:57
 */

//@Configuration
public class MybatisPlusConfig {

    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        // 设置请求的页面大于最大页后操作， true调回到首页，false 继续请求  默认false
        // paginationInterceptor.setOverflow(false);
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        // paginationInterceptor.setLimit(500);
        // 开启 count 的 join 优化,只针对部分 left join
        paginationInterceptor.setCountSqlParser(new JsqlParserCountOptimize(true));
        String dbType = System.getenv().get("db-type");
        if (DbType.ORACLE.getDb().equals(dbType)) {
            paginationInterceptor.setDbType(DbType.ORACLE);
            OracleDialect oracleDialect = new OracleDialect();
            paginationInterceptor.setDialect(oracleDialect);    
        } else if (DbType.MARIADB.getDb().equals(dbType)) {
            paginationInterceptor.setDbType(DbType.MARIADB);
            MariaDBDialect mariaDBDialect = new MariaDBDialect();
            paginationInterceptor.setDialect(mariaDBDialect);
        } else if (DbType.SQL_SERVER.getDb().equals(dbType)) {
            paginationInterceptor.setDbType(DbType.SQL_SERVER);
            SQLServerDialect sqlServerDialect = new SQLServerDialect();
            paginationInterceptor.setDialect(sqlServerDialect);
        }
        return paginationInterceptor;

    }
}

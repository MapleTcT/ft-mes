/*
package com.supcon.supfusion.configuration.services.projectapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;

*/
/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/1/18
 *//*

@Configuration
public class TransactionManagerConfig implements TransactionManagementConfigurer {
    @Resource(name="transactionManager")
    private PlatformTransactionManager txManager;

//	// 创建事务管理器1
//	@Bean(name = "txManager1")
//	public PlatformTransactionManager txManager(DataSource dataSource) {
//		return new DataSourceTransactionManager(dataSource);
//	}

    // 创建事务管理器
    @Bean(name = "transactionManager")
    public PlatformTransactionManager txManager(EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }

    //其返回值代表在拥有多个事务管理器的情况下默认使用的事务管理
    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return txManager;
    }


}
*/

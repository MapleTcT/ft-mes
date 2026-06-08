package com.supcon.supfusion.configuration.services.projectapi.config;

import com.supcon.supfusion.base.hibernate.BAPNamingStrategy;
import com.supcon.supfusion.framework.scaffold.hibernate.constants.HibernatePropertiesConstant;
import com.supcon.supfusion.framework.scaffold.hibernate.properties.HibernateProperties;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.Vector;

/**
 * entityconf模块bean定义
 * @author
 *
 */
@Configuration
@ImportResource("classpath:applicationContext-bpm.xml")
public class EntityConfig {
    @Autowired
    private DataSource dataSource;
    @Value("${entitymanager.packagesToScan:''}")
    private String packagesToScan;
    @Autowired
    HibernateProperties hibernateProperties;

    @Bean("projSessionFactory")
    public LocalSessionFactoryBean projSessionFactory(){
        return getSessionFactory("PROJECT");
    }
    @Bean("runtimeSessionFactory")
    public LocalSessionFactoryBean runtimeSessionFactory(){
        return getSessionFactory("RUNTIME");
    }

    @Bean("ecSessionFactory")
    public LocalSessionFactoryBean ecSessionFactory(){
        return getSessionFactory("EC");
    }
    @Bean("sessionFactory")
    public LocalSessionFactoryBean sessionFactory(){
        return getSessionFactory("EC");
    }

    public LocalSessionFactoryBean getSessionFactory(String tablePrefix) {
        final Vector<Class<?>> clazz = new Vector<Class<?>>();
        Properties defaultHibernateProperties = hibernateProperty();
        LocalSessionFactoryBean localSessionFactoryBean = new LocalSessionFactoryBean();
        localSessionFactoryBean.setHibernateProperties(defaultHibernateProperties);
        localSessionFactoryBean.setDataSource(dataSource);
        //自动扫描所有业务模块实体类以及自定义实体类 by cwn 20190520
        localSessionFactoryBean.setPackagesToScan(new String[] { "com.supcon.supfusion.configuration.services.entity","com.supcon.supfusion.base.entities",getScanPath(), "org.jbpm.pvm.*"});
        BAPNamingStrategy namingStrategy = new BAPNamingStrategy();
        localSessionFactoryBean.setMappingResources(new String[] { "jbpm.execution.hbm.xml", "jbpm.repository.hbm.xml",
                "jbpm.task.hbm.xml", "jbpm.history.hbm.xml" });
        //传入实体类前缀名称 by cwn 20190525
        namingStrategy.setTableNamePrefix(tablePrefix);
        localSessionFactoryBean.setPhysicalNamingStrategy(namingStrategy);
        localSessionFactoryBean.setAnnotatedClasses(clazz.toArray(new Class[clazz.size()]));

        return localSessionFactoryBean;
    }

    @ConditionalOnMissingBean(HibernateTransactionManager.class)
    @Bean
    public PlatformTransactionManager transactionManager(PhysicalNamingStrategy physicalNamingStrategy) {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactory().getObject());
        return transactionManager;
    }

    private Properties hibernateProperty() {
        Properties properties = new Properties();
        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_JDBC_FETCH_SIZE,
                String.valueOf(hibernateProperties.getFetchSize()));
        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_JDBC_BATCH_SIZE,
                String.valueOf(hibernateProperties.getBatchSize()));
        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_SHOW_SQL,
                String.valueOf(hibernateProperties.getShowSql()));
        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_FORMAT_SQL,
                String.valueOf(hibernateProperties.getFormatSql()));
        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_USE_SQL_COMMENTS,
                String.valueOf(hibernateProperties.getUseSqlComments()));
        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_GENERATE_STATISTICS,
                String.valueOf(hibernateProperties.getGenerateStatistics()));
        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_CACHE_USE_STRUCTURED_ENTRIES,
                String.valueOf(hibernateProperties.getUseStructuredEntries()));
        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_CACHE_USE_QUERY_CACHE,
                String.valueOf(hibernateProperties.getUseQueryCache()));
        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE,
                String.valueOf(hibernateProperties.getUseSecondLevelCache()));
        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_HBM2DDL_AUTO,
                hibernateProperties.getAuto());
        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_PERSISTENCE_VALIDATION_MODE,
                hibernateProperties.getValidationMode());
        properties.setProperty("hibernate.allow_update_outside_transaction","true");
//        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_DIAlECT, hibernateProperties.getDialect());
        return properties;
    }
    private String getScanPath() {
        return hibernateProperties.getPackages();
    }

}

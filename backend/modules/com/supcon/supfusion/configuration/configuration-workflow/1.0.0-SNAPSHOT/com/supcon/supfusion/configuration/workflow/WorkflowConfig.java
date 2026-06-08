package com.supcon.supfusion.configuration.workflow;

import com.supcon.supfusion.framework.scaffold.hibernate.constants.HibernatePropertiesConstant;
import com.supcon.supfusion.framework.scaffold.hibernate.properties.HibernateProperties;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.ImportResource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.Vector;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/25
 */
@Configuration
@ImportResource("classpath:applicationContext-bpm.xml")
public class WorkflowConfig {


    @Autowired
    HibernateProperties hibernateProperties;

    @Autowired
    private DataSource dataSource;

    @ConditionalOnMissingBean(LocalSessionFactoryBean.class)
    @Bean(name = "sessionFactory")
    public LocalSessionFactoryBean sessionFactory(PhysicalNamingStrategy physicalNamingStrategy) {
        final Vector<Class<?>> clazz = new Vector<Class<?>>();
        LocalSessionFactoryBean localSessionFactoryBean = new LocalSessionFactoryBean();
        localSessionFactoryBean.setHibernateProperties(hibernateProperty());
        localSessionFactoryBean.setDataSource(dataSource);
        localSessionFactoryBean
                .setPackagesToScan(new String[]{getScanPath(), "org.jbpm.pvm.*"});
        localSessionFactoryBean.setMappingResources(new String[] { "jbpm.execution.hbm.xml", "jbpm.repository.hbm.xml",
                "jbpm.task.hbm.xml", "jbpm.history.hbm.xml" });
        localSessionFactoryBean.setAnnotatedClasses(clazz.toArray(new Class[clazz.size()]));
        localSessionFactoryBean.setPhysicalNamingStrategy(physicalNamingStrategy);
        return localSessionFactoryBean;
    }

    @ConditionalOnMissingBean(HibernateTransactionManager.class)
    @Bean
    public PlatformTransactionManager transactionManager(PhysicalNamingStrategy physicalNamingStrategy) {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactory(physicalNamingStrategy).getObject());
        return transactionManager;
    }

    @Bean
    public PhysicalNamingStrategy physicalNamingStrategy() {
        return new SpringPhysicalNamingStrategy();
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
//        properties.setProperty(HibernatePropertiesConstant.HIBERNATE_DIAlECT, hibernateProperties.getDialect());
        return properties;
    }

    private String getScanPath() {
        return hibernateProperties.getPackages();
    }

}

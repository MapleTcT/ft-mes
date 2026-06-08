/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.supfusion.scheduler.server.service.job.SpringJobFactory
 *  org.quartz.Scheduler
 *  org.quartz.spi.JobFactory
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Qualifier
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.scheduling.quartz.SchedulerFactoryBean
 */
package com.supcon.supfusion.scheduler.config;

import com.supcon.supfusion.scheduler.server.service.job.SpringJobFactory;
import java.util.Properties;
import javax.sql.DataSource;
import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class InitQuartzConfig {
    private static final Logger log = LoggerFactory.getLogger(InitQuartzConfig.class);
    @Autowired
    private SpringJobFactory springJobFactory;
    @Value(value="${org.quartz.scheduler.instanceName}")
    private String instanceName;
    @Value(value="${org.quartz.scheduler.instanceId}")
    private String instanceId;
    @Value(value="${org.quartz.jobStore.class}")
    private String jobStoreDriverDelegateClass;
    @Value(value="${org.quartz.jobStore.driverDelegateClass}")
    private String jobStoreTablePrefix;
    @Value(value="${org.quartz.jobStore.tablePrefix}")
    private String jobStoreIsClustered;
    @Value(value="${org.quartz.jobStore.isClustered}")
    private String isClustered;
    @Value(value="${org.quartz.jobStore.clusterCheckinInterval}")
    private String clusterCheckinInterval;
    @Value(value="${org.quartz.jobStore.acquireTriggersWithinLock}")
    private String acquireTriggersWithinLock;
    @Value(value="${org.quartz.jobStore.useProperties}")
    private String useProperties;
    @Value(value="${org.quartz.jobStore.misfireThreshold}")
    private String misfireThreshold;
    @Value(value="${org.quartz.threadPool.class}")
    private String threadPoolClass;
    @Value(value="${org.quartz.threadPool.threadCount}")
    private String threadPoolThreadCount;
    @Value(value="${org.quartz.threadPool.threadPriority}")
    private String threadPoolThreadPriority;
    @Value(value="${org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread}")
    private String threadPoolInitializing;
    @Autowired
    @Qualifier(value="dataSource")
    private DataSource dataSource;

    @Bean
    public Scheduler scheduler() {
        return this.schedulerFactoryBean().getScheduler();
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        Properties p = this.getProperties();
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setQuartzProperties(p);
        schedulerFactoryBean.setOverwriteExistingJobs(true);
        schedulerFactoryBean.setStartupDelay(10);
        schedulerFactoryBean.setJobFactory((JobFactory)this.springJobFactory);
        schedulerFactoryBean.setAutoStartup(true);
        schedulerFactoryBean.setDataSource(this.dataSource);
        return schedulerFactoryBean;
    }

    public Properties getProperties() {
        Properties p = new Properties();
        p.setProperty("org.quartz.scheduler.instanceName", this.instanceName);
        p.setProperty("org.quartz.scheduler.instanceId", this.instanceId);
        p.setProperty("org.quartz.jobStore.class", this.jobStoreDriverDelegateClass);
        p.setProperty("org.quartz.jobStore.driverDelegateClass", this.jobStoreTablePrefix);
        p.setProperty("org.quartz.jobStore.tablePrefix", this.jobStoreIsClustered);
        p.setProperty("org.quartz.jobStore.isClustered", this.isClustered);
        p.setProperty("clusterCheckinInterval", this.clusterCheckinInterval);
        p.setProperty("org.quartz.jobStore.useProperties", this.useProperties);
        p.setProperty("org.quartz.threadPool.class", this.threadPoolClass);
        p.setProperty("org.quartz.threadPool.threadCount", this.threadPoolThreadCount);
        p.setProperty("org.quartz.threadPool.threadPriority", this.threadPoolThreadPriority);
        p.setProperty("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", this.threadPoolInitializing);
        p.setProperty("org.quartz.jobStore.acquireTriggersWithinLock", this.acquireTriggersWithinLock);
        p.setProperty("org.quartz.jobStore.misfireThreshold", this.misfireThreshold);
        return p;
    }

    public SpringJobFactory getSpringJobFactory() {
        return this.springJobFactory;
    }

    public String getInstanceName() {
        return this.instanceName;
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public String getJobStoreDriverDelegateClass() {
        return this.jobStoreDriverDelegateClass;
    }

    public String getJobStoreTablePrefix() {
        return this.jobStoreTablePrefix;
    }

    public String getJobStoreIsClustered() {
        return this.jobStoreIsClustered;
    }

    public String getIsClustered() {
        return this.isClustered;
    }

    public String getClusterCheckinInterval() {
        return this.clusterCheckinInterval;
    }

    public String getAcquireTriggersWithinLock() {
        return this.acquireTriggersWithinLock;
    }

    public String getUseProperties() {
        return this.useProperties;
    }

    public String getMisfireThreshold() {
        return this.misfireThreshold;
    }

    public String getThreadPoolClass() {
        return this.threadPoolClass;
    }

    public String getThreadPoolThreadCount() {
        return this.threadPoolThreadCount;
    }

    public String getThreadPoolThreadPriority() {
        return this.threadPoolThreadPriority;
    }

    public String getThreadPoolInitializing() {
        return this.threadPoolInitializing;
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

    public void setSpringJobFactory(SpringJobFactory springJobFactory) {
        this.springJobFactory = springJobFactory;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public void setJobStoreDriverDelegateClass(String jobStoreDriverDelegateClass) {
        this.jobStoreDriverDelegateClass = jobStoreDriverDelegateClass;
    }

    public void setJobStoreTablePrefix(String jobStoreTablePrefix) {
        this.jobStoreTablePrefix = jobStoreTablePrefix;
    }

    public void setJobStoreIsClustered(String jobStoreIsClustered) {
        this.jobStoreIsClustered = jobStoreIsClustered;
    }

    public void setIsClustered(String isClustered) {
        this.isClustered = isClustered;
    }

    public void setClusterCheckinInterval(String clusterCheckinInterval) {
        this.clusterCheckinInterval = clusterCheckinInterval;
    }

    public void setAcquireTriggersWithinLock(String acquireTriggersWithinLock) {
        this.acquireTriggersWithinLock = acquireTriggersWithinLock;
    }

    public void setUseProperties(String useProperties) {
        this.useProperties = useProperties;
    }

    public void setMisfireThreshold(String misfireThreshold) {
        this.misfireThreshold = misfireThreshold;
    }

    public void setThreadPoolClass(String threadPoolClass) {
        this.threadPoolClass = threadPoolClass;
    }

    public void setThreadPoolThreadCount(String threadPoolThreadCount) {
        this.threadPoolThreadCount = threadPoolThreadCount;
    }

    public void setThreadPoolThreadPriority(String threadPoolThreadPriority) {
        this.threadPoolThreadPriority = threadPoolThreadPriority;
    }

    public void setThreadPoolInitializing(String threadPoolInitializing) {
        this.threadPoolInitializing = threadPoolInitializing;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof InitQuartzConfig)) {
            return false;
        }
        InitQuartzConfig other = (InitQuartzConfig)o;
        if (!other.canEqual(this)) {
            return false;
        }
        SpringJobFactory this$springJobFactory = this.getSpringJobFactory();
        SpringJobFactory other$springJobFactory = other.getSpringJobFactory();
        if (this$springJobFactory == null ? other$springJobFactory != null : !this$springJobFactory.equals(other$springJobFactory)) {
            return false;
        }
        String this$instanceName = this.getInstanceName();
        String other$instanceName = other.getInstanceName();
        if (this$instanceName == null ? other$instanceName != null : !this$instanceName.equals(other$instanceName)) {
            return false;
        }
        String this$instanceId = this.getInstanceId();
        String other$instanceId = other.getInstanceId();
        if (this$instanceId == null ? other$instanceId != null : !this$instanceId.equals(other$instanceId)) {
            return false;
        }
        String this$jobStoreDriverDelegateClass = this.getJobStoreDriverDelegateClass();
        String other$jobStoreDriverDelegateClass = other.getJobStoreDriverDelegateClass();
        if (this$jobStoreDriverDelegateClass == null ? other$jobStoreDriverDelegateClass != null : !this$jobStoreDriverDelegateClass.equals(other$jobStoreDriverDelegateClass)) {
            return false;
        }
        String this$jobStoreTablePrefix = this.getJobStoreTablePrefix();
        String other$jobStoreTablePrefix = other.getJobStoreTablePrefix();
        if (this$jobStoreTablePrefix == null ? other$jobStoreTablePrefix != null : !this$jobStoreTablePrefix.equals(other$jobStoreTablePrefix)) {
            return false;
        }
        String this$jobStoreIsClustered = this.getJobStoreIsClustered();
        String other$jobStoreIsClustered = other.getJobStoreIsClustered();
        if (this$jobStoreIsClustered == null ? other$jobStoreIsClustered != null : !this$jobStoreIsClustered.equals(other$jobStoreIsClustered)) {
            return false;
        }
        String this$isClustered = this.getIsClustered();
        String other$isClustered = other.getIsClustered();
        if (this$isClustered == null ? other$isClustered != null : !this$isClustered.equals(other$isClustered)) {
            return false;
        }
        String this$clusterCheckinInterval = this.getClusterCheckinInterval();
        String other$clusterCheckinInterval = other.getClusterCheckinInterval();
        if (this$clusterCheckinInterval == null ? other$clusterCheckinInterval != null : !this$clusterCheckinInterval.equals(other$clusterCheckinInterval)) {
            return false;
        }
        String this$acquireTriggersWithinLock = this.getAcquireTriggersWithinLock();
        String other$acquireTriggersWithinLock = other.getAcquireTriggersWithinLock();
        if (this$acquireTriggersWithinLock == null ? other$acquireTriggersWithinLock != null : !this$acquireTriggersWithinLock.equals(other$acquireTriggersWithinLock)) {
            return false;
        }
        String this$useProperties = this.getUseProperties();
        String other$useProperties = other.getUseProperties();
        if (this$useProperties == null ? other$useProperties != null : !this$useProperties.equals(other$useProperties)) {
            return false;
        }
        String this$misfireThreshold = this.getMisfireThreshold();
        String other$misfireThreshold = other.getMisfireThreshold();
        if (this$misfireThreshold == null ? other$misfireThreshold != null : !this$misfireThreshold.equals(other$misfireThreshold)) {
            return false;
        }
        String this$threadPoolClass = this.getThreadPoolClass();
        String other$threadPoolClass = other.getThreadPoolClass();
        if (this$threadPoolClass == null ? other$threadPoolClass != null : !this$threadPoolClass.equals(other$threadPoolClass)) {
            return false;
        }
        String this$threadPoolThreadCount = this.getThreadPoolThreadCount();
        String other$threadPoolThreadCount = other.getThreadPoolThreadCount();
        if (this$threadPoolThreadCount == null ? other$threadPoolThreadCount != null : !this$threadPoolThreadCount.equals(other$threadPoolThreadCount)) {
            return false;
        }
        String this$threadPoolThreadPriority = this.getThreadPoolThreadPriority();
        String other$threadPoolThreadPriority = other.getThreadPoolThreadPriority();
        if (this$threadPoolThreadPriority == null ? other$threadPoolThreadPriority != null : !this$threadPoolThreadPriority.equals(other$threadPoolThreadPriority)) {
            return false;
        }
        String this$threadPoolInitializing = this.getThreadPoolInitializing();
        String other$threadPoolInitializing = other.getThreadPoolInitializing();
        if (this$threadPoolInitializing == null ? other$threadPoolInitializing != null : !this$threadPoolInitializing.equals(other$threadPoolInitializing)) {
            return false;
        }
        DataSource this$dataSource = this.getDataSource();
        DataSource other$dataSource = other.getDataSource();
        return !(this$dataSource == null ? other$dataSource != null : !this$dataSource.equals(other$dataSource));
    }

    protected boolean canEqual(Object other) {
        return other instanceof InitQuartzConfig;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        SpringJobFactory $springJobFactory = this.getSpringJobFactory();
        result = result * 59 + ($springJobFactory == null ? 43 : $springJobFactory.hashCode());
        String $instanceName = this.getInstanceName();
        result = result * 59 + ($instanceName == null ? 43 : $instanceName.hashCode());
        String $instanceId = this.getInstanceId();
        result = result * 59 + ($instanceId == null ? 43 : $instanceId.hashCode());
        String $jobStoreDriverDelegateClass = this.getJobStoreDriverDelegateClass();
        result = result * 59 + ($jobStoreDriverDelegateClass == null ? 43 : $jobStoreDriverDelegateClass.hashCode());
        String $jobStoreTablePrefix = this.getJobStoreTablePrefix();
        result = result * 59 + ($jobStoreTablePrefix == null ? 43 : $jobStoreTablePrefix.hashCode());
        String $jobStoreIsClustered = this.getJobStoreIsClustered();
        result = result * 59 + ($jobStoreIsClustered == null ? 43 : $jobStoreIsClustered.hashCode());
        String $isClustered = this.getIsClustered();
        result = result * 59 + ($isClustered == null ? 43 : $isClustered.hashCode());
        String $clusterCheckinInterval = this.getClusterCheckinInterval();
        result = result * 59 + ($clusterCheckinInterval == null ? 43 : $clusterCheckinInterval.hashCode());
        String $acquireTriggersWithinLock = this.getAcquireTriggersWithinLock();
        result = result * 59 + ($acquireTriggersWithinLock == null ? 43 : $acquireTriggersWithinLock.hashCode());
        String $useProperties = this.getUseProperties();
        result = result * 59 + ($useProperties == null ? 43 : $useProperties.hashCode());
        String $misfireThreshold = this.getMisfireThreshold();
        result = result * 59 + ($misfireThreshold == null ? 43 : $misfireThreshold.hashCode());
        String $threadPoolClass = this.getThreadPoolClass();
        result = result * 59 + ($threadPoolClass == null ? 43 : $threadPoolClass.hashCode());
        String $threadPoolThreadCount = this.getThreadPoolThreadCount();
        result = result * 59 + ($threadPoolThreadCount == null ? 43 : $threadPoolThreadCount.hashCode());
        String $threadPoolThreadPriority = this.getThreadPoolThreadPriority();
        result = result * 59 + ($threadPoolThreadPriority == null ? 43 : $threadPoolThreadPriority.hashCode());
        String $threadPoolInitializing = this.getThreadPoolInitializing();
        result = result * 59 + ($threadPoolInitializing == null ? 43 : $threadPoolInitializing.hashCode());
        DataSource $dataSource = this.getDataSource();
        result = result * 59 + ($dataSource == null ? 43 : $dataSource.hashCode());
        return result;
    }

    public String toString() {
        return "InitQuartzConfig(springJobFactory=" + this.getSpringJobFactory() + ", instanceName=" + this.getInstanceName() + ", instanceId=" + this.getInstanceId() + ", jobStoreDriverDelegateClass=" + this.getJobStoreDriverDelegateClass() + ", jobStoreTablePrefix=" + this.getJobStoreTablePrefix() + ", jobStoreIsClustered=" + this.getJobStoreIsClustered() + ", isClustered=" + this.getIsClustered() + ", clusterCheckinInterval=" + this.getClusterCheckinInterval() + ", acquireTriggersWithinLock=" + this.getAcquireTriggersWithinLock() + ", useProperties=" + this.getUseProperties() + ", misfireThreshold=" + this.getMisfireThreshold() + ", threadPoolClass=" + this.getThreadPoolClass() + ", threadPoolThreadCount=" + this.getThreadPoolThreadCount() + ", threadPoolThreadPriority=" + this.getThreadPoolThreadPriority() + ", threadPoolInitializing=" + this.getThreadPoolInitializing() + ", dataSource=" + this.getDataSource() + ")";
    }
}


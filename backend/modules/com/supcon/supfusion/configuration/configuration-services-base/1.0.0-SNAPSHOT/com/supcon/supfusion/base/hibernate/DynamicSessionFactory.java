
package com.supcon.supfusion.base.hibernate;

import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.base.utils.RuntimeFlagHolder;
import org.hibernate.Cache;
import org.hibernate.*;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import java.lang.ref.SoftReference;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/1/15
 */
@Component
@Primary
@ConditionalOnClass(name={"com.supcon.supfusion.configuration.services.projectapi.config.EntityConfig"})
public class DynamicSessionFactory implements SessionFactory, InitializingBean {
    private Boolean  projFlag = ProjectFlagHolder.getInstance().getProjFlag().get();
    private static ConcurrentLinkedQueue<SessionReference> sessionReferences = new ConcurrentLinkedQueue<>();
    @PersistenceContext
    protected EntityManager entityManager;


    @Autowired(required = false)
    @Qualifier(value = "sessionFactory")
    private SessionFactory ecSessionFactory;
    @Autowired(required = false)
    @Qualifier(value = "projSessionFactory")
    private SessionFactory projSessionFactory;
    @Autowired(required = false)
    @Qualifier(value = "runtimeSessionFactory")
    private SessionFactory runtimeSessionFactory;

    public void setEcSessionFactory(SessionFactory ecSessionFactory) {
        this.ecSessionFactory = ecSessionFactory;
    }
    public SessionFactory getProjSessionFactory() {
        return projSessionFactory;
    }
    public void setProjSessionFactory(SessionFactory projSessionFactory) {
        this.projSessionFactory = projSessionFactory;
    }

    private SessionFactory getRealSessionFactory() {
        if(ProjectFlagHolder.getInstance().getProjFlag().get()!=null&&ProjectFlagHolder.getInstance().getProjFlag().get()){
            return projSessionFactory;
        }else if(RuntimeFlagHolder.getInstance().getRuntimeFlag().get()!=null&&RuntimeFlagHolder.getInstance().getRuntimeFlag().get()){
            return runtimeSessionFactory;
        }else{
            return ecSessionFactory;
        }
    }

    @Override
    public SessionFactoryOptions getSessionFactoryOptions() {
        return getRealSessionFactory().getSessionFactoryOptions();
    }

    @Override
    public SessionBuilder withOptions() {
        return getRealSessionFactory().withOptions();
    }

    @Override
    public Session openSession() throws HibernateException {
        return getRealSessionFactory().openSession();
    }

    @Override
    public Session getCurrentSession() throws HibernateException {
/*        Session session = entityManager.unwrap(Session.class);
        if (projFlag != ProjectFlagHolder.getInstance().getProjFlag().get()) {
            sessionReferences.add(new SessionReference(session));
            session = openSession();
            projFlag = ProjectFlagHolder.getInstance().getProjFlag().get();
        }*/
        return getRealSessionFactory().getCurrentSession();
    }

    @Override
    public StatelessSessionBuilder withStatelessOptions() {
        return getRealSessionFactory().withStatelessOptions();
    }

    @Override
    public StatelessSession openStatelessSession() {
        return getRealSessionFactory().openStatelessSession();
    }

    @Override
    public StatelessSession openStatelessSession(Connection connection) {
        return getRealSessionFactory().openStatelessSession(connection);
    }

    @Override
    public Statistics getStatistics() {
        return getRealSessionFactory().getStatistics();
    }

    @Override
    public void close() throws HibernateException {
        getRealSessionFactory().close();
    }

    @Override
    public Map<String, Object> getProperties() {
        return getRealSessionFactory().getProperties();
    }

    @Override
    public boolean isClosed() {
        return getRealSessionFactory().isClosed();
    }

    @Override
    public Cache getCache() {
        return getRealSessionFactory().getCache();
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return getRealSessionFactory().getPersistenceUnitUtil();
    }

    @Override
    public void addNamedQuery(String name, javax.persistence.Query query) {
        getRealSessionFactory().addNamedQuery(name, query);
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return getRealSessionFactory().unwrap(cls);
    }

    @Override
    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
        getRealSessionFactory().addNamedEntityGraph(graphName, entityGraph);
    }

    @Override
    public Set getDefinedFilterNames() {
        return getRealSessionFactory().getDefinedFilterNames();
    }

    @Override
    public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
        return getRealSessionFactory().getFilterDefinition(filterName);
    }

    @Override
    public boolean containsFetchProfileDefinition(String name) {
        return getRealSessionFactory().containsFetchProfileDefinition(name);
    }

    @Override
    public TypeHelper getTypeHelper() {
        return getRealSessionFactory().getTypeHelper();
    }

    @Override
    public ClassMetadata getClassMetadata(Class entityClass) {
        return getRealSessionFactory().getClassMetadata(entityClass);
    }

    @Override
    public ClassMetadata getClassMetadata(String entityName) {
        return getRealSessionFactory().getClassMetadata(entityName);
    }

    @Override
    public CollectionMetadata getCollectionMetadata(String roleName) {
        return getRealSessionFactory().getCollectionMetadata(roleName);
    }

    @Override
    public Map<String, ClassMetadata> getAllClassMetadata() {
        return getRealSessionFactory().getAllClassMetadata();
    }

    @Override
    public Map getAllCollectionMetadata() {
        return getRealSessionFactory().getAllCollectionMetadata();
    }

    @Override
    public Reference getReference() throws NamingException {
        return getRealSessionFactory().getReference();
    }

    @Override
    public <T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass) {
        return getRealSessionFactory().findEntityGraphsByType(entityClass);
    }

    @Override
    public EntityManager createEntityManager() {
        return getRealSessionFactory().createEntityManager();
    }

    @Override
    public EntityManager createEntityManager(Map map) {
        return getRealSessionFactory().createEntityManager(map);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        return getRealSessionFactory().createEntityManager(synchronizationType);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
        return getRealSessionFactory().createEntityManager(synchronizationType, map);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return getRealSessionFactory().getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return getRealSessionFactory().getMetamodel();
    }

    @Override
    public boolean isOpen() {
        return getRealSessionFactory().isOpen();
    }

    private long remainTime = 600000; // 10m
    private ScheduledExecutorService scheduledExecutor;

    @Override
    public void afterPropertiesSet() throws Exception {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "closeSession");
            }
        });
        scheduledExecutor.scheduleAtFixedRate(new Runnable(){
            @Override
            public void run() {
                closeSession();
            }

        }, 30, 300, TimeUnit.SECONDS);
    }

    public void closeSession() {
        SessionReference sessionReference = null;
        while (null != (sessionReference = sessionReferences.peek())) {
            long time = System.currentTimeMillis() - sessionReference.getCreateTime();
            if (time > remainTime) {
                Session session = sessionReference.getSession();
                if(null != session){
                    session.close();
                    session = null;
                }
                sessionReferences.poll();
                sessionReference.clearRef();
                sessionReference = null;
            } else {
                //System.out.println("清理不用的session后，session数量：" + sessionReferences.size());
                return;
            }
        }
    }

    @PreDestroy
    public void destroy(){
        scheduledExecutor.shutdown();
        SessionReference sessionReference = null;
        while (null != (sessionReference = sessionReferences.peek())) {
            long time = System.currentTimeMillis() - sessionReference.getCreateTime();
            Session session = sessionReference.getSession();
            if(null != session){
                session.close();
                session = null;
            }
            sessionReferences.poll();
            sessionReference.clearRef();
            sessionReference = null;
        }
    }

    private class SessionReference {
        private SoftReference<Session> ref;
        private long createTime;

        public SessionReference(Session session) {
            ref = new SoftReference<Session>(session);
            createTime = System.currentTimeMillis();
        }

        public Session getSession() {
            return ref.get();
        }

        public long getCreateTime() {
            return createTime;
        }

        public void clearRef() {
            this.ref = null;
        }
    }
}

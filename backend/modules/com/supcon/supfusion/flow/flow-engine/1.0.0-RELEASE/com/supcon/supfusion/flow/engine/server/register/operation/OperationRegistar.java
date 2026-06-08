package com.supcon.supfusion.flow.engine.server.register.operation;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.flow.common.annotation.OperationType;
import com.supcon.supfusion.flow.engine.server.register.AbstractBeanRegistar;
import com.supcon.supfusion.flow.engine.server.register.OperationContext;

/**
 * 注册待办候选人处理实例
 */
@Component
public class OperationRegistar extends AbstractBeanRegistar implements ApplicationContextAware {

    private static final String INSTANCE_PACKAGE = "com.supcon.supfusion.flow.engine.server.service.impl.operation";
    private ApplicationContext springContext;
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        Map<Object, Class<?>> instanceMap = super.scanInstancePath(INSTANCE_PACKAGE, OperationType.class);
        OperationContext operationContext = new OperationContext(instanceMap, springContext);
        beanFactory.registerSingleton(OperationContext.class.getName(), operationContext);
    }

    /**
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.springContext = applicationContext;
    }

}

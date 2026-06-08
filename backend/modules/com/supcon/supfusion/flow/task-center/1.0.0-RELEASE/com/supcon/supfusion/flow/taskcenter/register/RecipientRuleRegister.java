package com.supcon.supfusion.flow.taskcenter.register;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.flow.common.annotation.RecipientRule;
import com.supcon.supfusion.flow.engine.server.register.AbstractBeanRegistar;

/**
 * 注册待办候选人处理实例
 */
@Component
public class RecipientRuleRegister extends AbstractBeanRegistar implements ApplicationContextAware {

    private static final String INSTANCE_PACKAGE = "com.supcon.supfusion.flow.taskcenter.service.rule";
    private ApplicationContext springContext;
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        Map<Object, Class<?>> instanceMap = super.scanInstancePath(INSTANCE_PACKAGE, RecipientRule.class);
        RecipientRuleContext recipientRuleContext = new RecipientRuleContext(instanceMap, springContext);
        beanFactory.registerSingleton(RecipientRuleContext.class.getName(), recipientRuleContext);
    }

    /**
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.springContext = applicationContext;
    }

}

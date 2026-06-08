package com.supcon.supfusion.framework.scaffold.kafka.tenant;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.stream.binding.BindingBeanDefinitionRegistryUtils;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * @author tomcat
 * @date 20-6-4 上午10:26
 */
public class TenantEventSinkBindingBeansRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        Class<?> type = TenantEventSink.class;
        if (!registry.containsBeanDefinition(type.getName())) {
            BindingBeanDefinitionRegistryUtils.registerBindingTargetBeanDefinitions(type, type.getName(), registry);
            BindingBeanDefinitionRegistryUtils.registerBindingTargetsQualifiedBeanDefinitions(ClassUtils.resolveClassName(metadata.getClassName(), null), type, registry);
        }
    }
}

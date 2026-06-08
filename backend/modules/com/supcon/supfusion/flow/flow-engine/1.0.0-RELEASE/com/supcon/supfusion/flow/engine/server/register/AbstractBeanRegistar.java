package com.supcon.supfusion.flow.engine.server.register;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

public abstract class AbstractBeanRegistar implements BeanFactoryPostProcessor {
    
    /**
     * 实例化path路径下的class, 并放入map缓存
     * @param path bean实例路径
     * @param clazz bean类型
     * @return
     */
    protected Map<Object, Class<?>> scanInstancePath(String path, Class<? extends Annotation> clazz) {
        Map<Object, Class<?>> instanceMap = new HashMap<>();
        GenericApplicationContext context = new GenericApplicationContext();
        InstanceScanner scanner = new InstanceScanner(context, clazz);
        scanner.registerTypeFilter();
        scanner.scan(path);
        context.refresh();
        context.getBeansWithAnnotation(clazz).forEach((name, bean) -> {
            Annotation annotation = AnnotationUtils.findAnnotation(bean.getClass(), clazz);
            Object type = AnnotationUtils.getValue(annotation, "value");
            if (type instanceof Object[]) {
                for (Object obj : (Object[])type) {
                    instanceMap.put(obj, bean.getClass());
                }
            } else {
                instanceMap.put(type, bean.getClass());
            }
        });
        return instanceMap;
    }
}

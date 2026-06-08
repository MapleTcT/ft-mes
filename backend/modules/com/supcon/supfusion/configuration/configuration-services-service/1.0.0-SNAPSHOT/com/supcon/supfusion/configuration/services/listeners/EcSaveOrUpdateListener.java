package com.supcon.supfusion.configuration.services.listeners;

import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.annotation.International;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.event.spi.SaveOrUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class EcSaveOrUpdateListener implements SaveOrUpdateEventListener {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private InternationalService internationalService;

	private ConcurrentMap<String, Set<Field>> i18nFieldsCache = new ConcurrentHashMap<>();

	@Override
	public void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException {
		Object entity = event.getObject();

		if(!(entity instanceof AbstractAuditUniqueCodeEntity || entity instanceof AbstractCodeEntity)) {
			return;
		}
		Class clazz = Hibernate.getClass(entity);
		dealVersionNotNull(clazz, entity);
		dealI18n(clazz, entity);
	}

	@PostConstruct
	private void init() {
		SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
		EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
		registry.getEventListenerGroup(EventType.SAVE_UPDATE).appendListener(this);
		ProjectFlagHolder.getInstance().getProjFlag().set(true);
		SessionFactoryImpl projsessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
		EventListenerRegistry projregistry = projsessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
		projregistry.getEventListenerGroup(EventType.SAVE_UPDATE).appendListener(this);
		ProjectFlagHolder.getInstance().getProjFlag().set(false);
	}

	private void dealVersionNotNull(Class clazz, Object entity) {
		try {
			Field version = FieldUtils.getField(clazz, "version", true);
			Object value = version.get(entity);
			if (value == null) {
				version.set(entity, Integer.valueOf(0));
			}
		} catch (IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}
	}

	private void dealI18n(Class clazz, Object entity) {
		String clazzName = clazz.getName();
		Set<Field> fields = i18nFieldsCache.get(clazzName);
		if(null == fields) {
			// 遍历所有字段，找到符合条件的将其添加到缓存
			fields = new HashSet<>();
			Field[] f = clazz.getDeclaredFields();//只取当前类，不去查询父类
			for(Field field:f){
				if(field.isAnnotationPresent(International.class)){
					fields.add(field);
					field.setAccessible(true);
				}
			}
			if (fields == null || fields.size() == 0) {
				return;
			}
			fields = new CopyOnWriteArraySet<>(fields);
			i18nFieldsCache.putIfAbsent(clazzName, fields);
		}

		for(Field field:fields){
			Object value = null;
			try {
				value = field.get(entity);
			} catch (IllegalAccessException e) {
				log.error(e.getMessage(), e);
			}
			if (value == null) {
				continue;
			}
			String[] values = ((String)value).split("\\$&#");
			if (!(values.length > 0 && values[0].startsWith("key="))) {
				continue;
			}
			String key = values[0].substring(4);

			try {
				field.set(entity,key);
			} catch (IllegalAccessException e) {
				log.error(e.getMessage(), e);
			}
			Map i18nMap = new HashMap();
			for (int i = 1; i <values.length; i++) {
				if (values[i].indexOf("=") < 0 || values[i].endsWith("=")) continue;
				String[] split = values[i].split("=");
				i18nMap.put(split[0], split[1]);
			}
			internationalService.addInternational(key, i18nMap);
		}
	}
	
}

package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.CounterRule;
import com.supcon.supfusion.configuration.services.entity.CounterRuleField;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.dao.PropertyDaoImpl;
import com.supcon.supfusion.configuration.services.service.CounterService;
import com.supcon.supfusion.configuration.services.service.PropertyService;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.counter.common.constants.AutoType;
import com.supcon.supfusion.counter.common.constants.FieldType;
import com.supcon.supfusion.counter.common.constants.TheCase;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class PropertyServiceImpl implements PropertyService {
	@Autowired
	private PropertyDaoImpl propertyDao;

	@Autowired
	private CounterService counterService;

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Property getProperty(String code) {
		// return propertyDao.findEntityByHql("from Property where code = ? and valid = true", code);
		Property p = null;
		List<Property> list = propertyDao.findByCriteria(Restrictions.eq("code", code), Restrictions.eq("valid", true));
		if (null != list && !list.isEmpty()) {
			p = list.get(0);
		}
		return p;
	}

	@Override
	public List<Property> getProperties(String modelCode) {
		List<Property> list = propertyDao.findByCriteria(Restrictions.eq("model.code", modelCode), Restrictions.eq("valid", true));
		return list;
	}

	@Override
	public synchronized void addCounterRule(Property property) {
		return;
		/*if (StringUtils.isEmpty(property.getAttributes()) || property.getAttributesMap() == null) {
			return;
		}
		Map map = property.getAttributesMap();
		List config = (List)map.get("config");
		if (config.size() <= 0) {
			return;
		}
		CounterRule counterRule = counterService.findByName(property.getCode());
		if (counterRule == null) {
			counterRule = new CounterRule();
			counterRule.setRuleName(property.getCode());
		}
		List<CounterRuleField> ruleFields = new ArrayList<>(config.size());
		int order = 0;
		for (int i=0; i<config.size(); i++) {
			Object o = config.get(0);
			if (o instanceof Map) {
				CounterRuleField ruleField = new CounterRuleField();
				Map rule = (Map)o;
				String type = String.valueOf(rule.get("type"));
				String value = String.valueOf(rule.get("value"));
				switch (type) {
					case "property":
						ruleField.setFieldType(FieldType.PROPERTY);
						break;
					case "inherent":
						ruleField.setFieldType(FieldType.INHERENT);
						break;
					case "date":
						ruleField.setFieldType(FieldType.DATE);
						if ("_systemdate".equals(value)) {
 							value = "systemdate";
						}
						String dateType = String.valueOf(rule.get("dateType"));
						if ("YearA".equals(dateType)) {
							ruleField.setDateFormatter("yyyy");
						} else if ("YearB".equals(dateType)) {
							ruleField.setDateFormatter("yy");
						} else if ("Month".equals(dateType)) {
							ruleField.setDateFormatter("yyyyMM");
						} else if ("Date".equals(dateType)) {
							ruleField.setDateFormatter("yyyyMMdd");
						}
						break;
					case "custom":
						ruleField.setFieldType(FieldType.CUSTOM);
						break;
					case "auto":
						ruleField.setFieldType(FieldType.AUTO);
						ruleField.setAutoLength(Integer.valueOf(rule.get("digit").toString()));
						String autoType = String.valueOf(rule.get("autoType"));
						if ("Code".equals(autoType)) {
							ruleField.setAutoType(AutoType.CODE);
						} else if ("Date".equals(autoType)) {
							ruleField.setAutoType(AutoType.DATE);
						}
						if ("_systemdate".equals(value)) {
							value = "systemdate";
						}
						break;
				}
				if (value == null) {
					value = "";
				}
				ruleField.setFieldValue(value);
				String thecase = String.valueOf(rule.get("thecase"));
				switch (thecase) {
					case "original":
						ruleField.setThecase(TheCase.ORIGINAL);
						break;
					case "upper":
						ruleField.setThecase(TheCase.UPPER);
						break;
					case "lower":
						ruleField.setThecase(TheCase.LOWER);
						break;
				}
				ruleField.setFieldOrder(order++);
				ruleFields.add(ruleField);
				if (i < (config.size()-1)) {
					CounterRuleField separator = new CounterRuleField();
					separator.setThecase(TheCase.ORIGINAL);
					separator.setFieldValue(String.valueOf(rule.get("separator")));
					separator.setFieldOrder(order++);
					ruleFields.add(separator);
				}
			}
		}
		counterRule.setRuleFields(ruleFields);
		if (counterRule.getId() != null) {
			counterService.modify(counterRule);
			return;
		}
		log.info("保存编码配置：" + counterRule.toString());
		Long counterId = counterService.add(counterRule);

		String update = "update EC_PROPERTY set COUNTER_RULE_ID=? where code=?";
		propertyDao.createNativeQuery(update, counterId, property.getCode()).executeUpdate();*/
	}

	@Override
	public void deleteCounter(Property property) {
		counterService.delete(property.getCounterRuleId());
	}

	@Override
	public void save(Property property) {
		propertyDao.save(property);
	}

}

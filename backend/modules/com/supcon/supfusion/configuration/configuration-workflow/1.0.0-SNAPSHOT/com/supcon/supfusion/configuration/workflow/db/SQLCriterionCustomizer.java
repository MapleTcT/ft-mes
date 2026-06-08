package com.supcon.supfusion.configuration.workflow.db;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.type.Type;

import java.util.Iterator;

/**
 * A SQL fragment. The string {alias} will be replaced by the alias of the root
 * entity; The string {t} will be replaced by the alias of the sub entity, t is
 * the alias place-hold
 * 
 * @author WangSenming
 */
public class SQLCriterionCustomizer implements Criterion {

	private static final long serialVersionUID = -2128223435772427745L;

	private final String sql;

	private final TypedValue[] typedValues;

	@SuppressWarnings("rawtypes")
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
			throws HibernateException {
		CriteriaImpl rootCriteria = null;
		if (criteria instanceof CriteriaImpl) {
			rootCriteria = (CriteriaImpl) criteria;
		} else if (criteria instanceof CriteriaImpl.Subcriteria) {
			rootCriteria = (CriteriaImpl) ((CriteriaImpl.Subcriteria) criteria).getParent();
		} else {
			throw new HibernateException(
					"Other Criteria implementation not supported");
		}

		Iterator iterateSubcriteria = rootCriteria.iterateSubcriteria();
		String tempSql = sql;

		// Replace sub criteria' alias
		while (iterateSubcriteria.hasNext()) {
			CriteriaImpl.Subcriteria subCriteria = (CriteriaImpl.Subcriteria) iterateSubcriteria.next();
			tempSql = StringHelper.replace(tempSql,
					"{" + subCriteria.getAlias() + "}",
					criteriaQuery.getSQLAlias(subCriteria));
		}
		return StringHelper.replace(tempSql, "{alias}",
				criteriaQuery.getSQLAlias(criteria));
	}

	public TypedValue[] getTypedValues(Criteria criteria,
			CriteriaQuery criteriaQuery) throws HibernateException {
		return typedValues;
	}

	public String toString() {
		return sql;
	}

	protected SQLCriterionCustomizer(String sql, Object[] values, Type[] types) {
		this.sql = sql;
		typedValues = new TypedValue[values.length];
		for (int i = 0; i < typedValues.length; i++) {
			typedValues[i] = new TypedValue(types[i], values[i],
					EntityMode.POJO);
		}
	}
}

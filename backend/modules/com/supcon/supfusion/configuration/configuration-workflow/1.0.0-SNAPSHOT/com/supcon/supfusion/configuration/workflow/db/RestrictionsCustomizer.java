package com.supcon.supfusion.configuration.workflow.db;


/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.type.Type;

/**
 * The <tt>criterion</tt> package may be used by applications as a framework for
 * building new kinds of <tt>Criterion</tt>. However, it is intended that most
 * applications will simply use the built-in criterion types via the static
 * factory methods of this class.
 *
 * @see org.hibernate.Criteria
 * @see Projections factory methods for <tt>Projection</tt> instances
 * @author Gavin King
 * @author WangSenming
 */
public final class RestrictionsCustomizer {

	private RestrictionsCustomizer() {
		// cannot be instantiated
	}

	/**
	 * Apply a constraint expressed in SQL. Any occurrences of <tt>{alias}</tt>
	 * will be replaced by the main table alias, Any occurrences of <tt>{T}</tt> will be replaced by the sub table alias.
	 * 
	 * example:
	 * 
	 * DetachedCriteria detachedCriteria = DetachedCriteria.forClass(DepartmentWork.class).createAlias("department", "d").createAlias("staff", "s").createAlias("s.mainPosition", "p");
	 * 
	 * detachedCriteria.add(RestrictionsCustomizer.sqlRestriction("{s}.name like '%" + SqlParser.afterEscape(staffName) + "%' " + SqlParser.ESCAPE));
	 * 
	 * {s} will be replaced by the staff's alias;
	 *
	 * @param sql
	 * @return Criterion
	 */	
	public static Criterion sqlRestriction(String sql) {
		return new SQLCriterionCustomizer(sql, new Object[] {}, new Type[] {});
	}
}

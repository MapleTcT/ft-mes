/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.type.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.orm.hibernate5.HibernateTemplate;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author songjiawei
 *
 */
public class DbUtils {

	private static final Logger logger = LoggerFactory.getLogger(DbUtils.class);

	private static final DbUtils db = new DbUtils();
	private static String dbName;
	private static final DbUtils hibernateDb = new DbUtils();

	public static DbUtils getInstance() {
		return db;
	}

	public static DbUtils getHibernateDbInstance() {
		return hibernateDb;
	}

	private static HibernateTemplate hibernateTemplate;
	private static JdbcTemplate jdbcTemplate;

	public static HibernateTemplate getHibernateTemplate(SessionFactory sessionFactory) {
		if (null == hibernateTemplate) {
			hibernateTemplate = new HibernateTemplate(sessionFactory);
			if (null == hibernateTemplate)
				throw new NullPointerException("hibernateTemplate can not be null.");
		}
		return hibernateTemplate;
	}

	public static JdbcTemplate getJdbcTemplate() {
		if (null == jdbcTemplate) {
			jdbcTemplate = SpringContextHolder.getBean("jdbcTemplate");
			if (null == jdbcTemplate)
				throw new NullPointerException("jdbcTemplate can not be null.");
		}

		return jdbcTemplate;
	}

	public static String getDbName() {
		if (null == dbName) {
			Connection conn = null;
			try {
				conn = getJdbcTemplate().getDataSource().getConnection();
				dbName = conn.getMetaData().getDatabaseProductName().toLowerCase();
				if (dbName.startsWith("oracle"))
					dbName = "oracle";
				if (dbName.startsWith("mysql"))
					dbName = "mysql";
				if (dbName.startsWith("microsoft sql server"))
					dbName = "sqlserver";
				if (dbName.startsWith("mariadb"))
					dbName = "mariadb";
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (null != conn) {
					try {
						conn.close();
					} catch (SQLException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
		}
		return dbName;
	}

	public static Connection getConnection() throws SQLException {
		return getJdbcTemplate().getDataSource().getConnection();
	}

	// public static long id(String tableName) {
	// return id(tableName, 1);
	// }
	//
	// public static long id(String tableName, int step) {
	// return IdGenerator.getGenerator().getNextId(tableName, step);
	// }

	public static <T> T read(JdbcTemplate jdbcTemplate, Class<T> beanClass, String sql, Object... params) {
		try {
			logger.debug("SQL: " + sql);
			return jdbcTemplate.queryForObject(sql, beanClass, params);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public static <T> T read(Class<T> beanClass, String sql, Object... params) {
		return read(getJdbcTemplate(), beanClass, sql, params);
	}

	public static Map<String, Object> read(JdbcTemplate jdbcTemplate, String sql, Object... params) {
		try {
			logger.debug("SQL: " + sql);
			return jdbcTemplate.queryForMap(sql, params);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public static Map<String, Object> read(String sql, Object... params) {
		return read(getJdbcTemplate(), sql, params);
	}

	public static <T> List<T> query(JdbcTemplate jdbcTemplate, final Class<T> beanClass, String sql, Object... params) {
		logger.debug("SQL: " + sql);
		jdbcTemplate.query(sql, new RowMapper<T>() {
			@Override
			public T mapRow(ResultSet rs, int rowNum) throws SQLException {
				T t = null;
				try {
					t = beanClass.newInstance();
				} catch (InstantiationException e) {
				} catch (IllegalAccessException e) {
				}
				int columnCount = rs.getMetaData().getColumnCount();
				Map<String, Object> map = new HashMap<String, Object>(columnCount);
				for (int i = 0; i < columnCount;) {
					String label = rs.getMetaData().getColumnLabel(++i);
					map.put(column2Property(label), rs.getObject(i));
				}
				try {
					if (null != t)
						BeanUtils.populate(t, map);
				} catch (IllegalAccessException e) {
				} catch (InvocationTargetException e) {
				}
				return t;
			}
		}, params);
		return jdbcTemplate.queryForList(sql, beanClass, params);
	}

	private static String column2Property(String column) {
		char[] chs = column.toLowerCase().toCharArray();
		StringBuilder builder = new StringBuilder();
		boolean sym = false;
		for (int i = 0; i < chs.length; i++) {
			char ch = chs[i];
			if (ch == '_') {
				sym = true;
				continue;
			} else {
				sym = false;
			}
			if (sym) {
				builder.append(Character.toUpperCase(ch));
			} else {
				builder.append(ch);
			}
		}
		return builder.toString();
	}

	public static <T> List<T> query(Class<T> beanClass, String sql, Object... params) {
		return query(getJdbcTemplate(), beanClass, sql, params);
	}

	public static List<Map<String, Object>> query(JdbcTemplate jdbcTemplate, String sql, Object... params) {
		logger.debug("SQL: " + sql);
		return jdbcTemplate.queryForList(sql, params);
	}

	public static List<Map<String, Object>> query(String sql, Object... params) {
		return query(getJdbcTemplate(), sql, params);
	}


	public static int update(JdbcTemplate jdbcTemplate, String sql, Object... params) {
		logger.debug("SQL: " + sql);
		return jdbcTemplate.update(sql, params);
	}

	public static int update(String sql, Object... params) {
		return update(getJdbcTemplate(), sql, params);
	}

	public static int[] batch(JdbcTemplate jdbcTemplate, final String sql, final Object[][] params) {
		logger.debug("BATCH SQL: " + sql);
		return jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				Object[] args = params[i];
				for (int j = 1; j <= args.length; j++)
					ps.setObject(j, args[j - 1]);
			}

			@Override
			public int getBatchSize() {
				return params.length;
			}
		});
	}

	public static int[] batch(final String sql, final Object[][] params) {
		return batch(getJdbcTemplate(), sql, params);
	}

	public static String getDealInfoTable(String businessTable) {
		if (businessTable == null) {
			return null;
		}
		return businessTable.trim() + "_DI";
	}

	public static String getPayAttentionTable(String businessTable) {
		if (businessTable == null) {
			return null;
		}
		return businessTable.trim() + "_PA";
	}

	public static String getSupervisionTable(String businessTable) {
		if (businessTable == null) {
			return null;
		}
		return businessTable.trim() + "_SV";
	}

	public static String getMneTable(String businessTable) {
		if (businessTable == null) {
			return null;
		}
		return businessTable.trim() + "_MC";
	}

	public static String getGroupTable(String businessTable) {
		if (businessTable == null) {
			return null;
		}
		return businessTable.trim();
	}

	public static String getAccessControlListTable(String businessTable) {
		if (businessTable == null) {
			return null;
		}
		return businessTable.trim() + "_ACL";
	}

	public static Type[] getHibernateTypeByJavaType(List<Object> params) {
		if(params !=null){
			Type[] types = new Type[params.size()];
			if (params != null && params.size() > 0) {
				for (int i = 0; i < params.size(); i++) {
					if (params.get(i) instanceof String) {
						types[i] = new StringType();
					} else if (params.get(i) instanceof AbstractIdEntity || params.get(i) instanceof Long) {
						types[i] = new LongType();
					} else if (params.get(i) instanceof Date) {
						types[i] = new DateType();
					} else if (params.get(i) instanceof Timestamp) {
						types[i] = new TimestampType();
					} else if (params.get(i) instanceof BigDecimal || params.get(i) instanceof Double) {
						types[i] = new DoubleType();
					} else if (params.get(i) instanceof Float) {
						types[i] = new FloatType();
					} else if (params.get(i) instanceof Integer) {
						types[i] = new IntegerType();
					} else {
						types[i] = new StringType();
					}
				}
			}
			return types;
		}else{
			return null;
		}
	}


	/**
	 *获取当前数据库名称
	 */
	public static String getCurrentDBName() {
		String url = "";
		Connection conn = null;
			try {
				conn = getJdbcTemplate().getDataSource().getConnection();
				 url = conn.getMetaData().getURL();
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (null != conn) {
					try {
						conn.close();
					} catch (SQLException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
		return url.split("\\?")[0].split("/")[3];
	}

}
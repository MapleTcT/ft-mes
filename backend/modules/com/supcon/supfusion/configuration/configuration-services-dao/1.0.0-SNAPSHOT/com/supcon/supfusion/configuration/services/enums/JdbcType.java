package com.supcon.supfusion.configuration.services.enums;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;


public enum JdbcType {
	BIT(Types.BIT),
	TINYINT(Types.TINYINT),
	SMALLINT(Types.SMALLINT),
	INTEGER(Types.INTEGER),
	BIGINT(Types.BIGINT),
	FLOAT(Types.FLOAT),
	REAL(Types.REAL),
	DOUBLE(Types.DOUBLE),
	NUMERIC(Types.NUMERIC),
	DECIMAL(Types.DECIMAL),
	CHAR(Types.CHAR),
	VARCHAR(Types.VARCHAR),
	LONGVARCHAR(Types.LONGVARCHAR),
	DATE(Types.DATE),
	TIME(Types.TIME),
	TIMESTAMP(Types.TIMESTAMP),
	BINARY(Types.BINARY),
	VARBINARY(Types.VARBINARY),
	LONGVARBINARY(Types.LONGVARBINARY),
	NULL(Types.NULL),
	OTHER(Types.OTHER),
	BLOB(Types.BLOB),
	CLOB(Types.CLOB),
	BOOLEAN(Types.BOOLEAN),
	CURSOR(-10), // Oracle
	UNDEFINED(Integer.MIN_VALUE + 1000),
	NVARCHAR(-9), // JDK6
	NCHAR(-15), // JDK6
	NCLOB(2011), // JDK6
	STRUCT(Types.STRUCT);

	public final int TYPE_CODE;
	private static Map<Integer, JdbcType> codeLookup = new HashMap<Integer, JdbcType>();

	static {
		for (JdbcType type : JdbcType.values()) {
			codeLookup.put(type.TYPE_CODE, type);
		}
	}

	JdbcType(int code) {
		this.TYPE_CODE = code;
	}

	public static JdbcType forCode(int code) {
		return codeLookup.get(code);
	}
	
	/**
	 * 用于SQL模型字段对应类型，不是一对一关系
	 * @return
	 */
	public DbColumnType getDbColumnType() {
		switch (this) {
		case VARCHAR:
			return DbColumnType.TEXT;
		case BINARY:
			return DbColumnType.BINARY;
		case BOOLEAN:
			return DbColumnType.BOOLEAN;
		case DATE:
			return DbColumnType.DATE;
		case TIMESTAMP:
			return DbColumnType.DATETIME;
		case TIME:
			return DbColumnType.TIME;
		case DECIMAL:
			return DbColumnType.DECIMAL;
		case NUMERIC:
			return DbColumnType.DECIMAL;
		case INTEGER:
			return DbColumnType.INTEGER;
		case TINYINT:
			return DbColumnType.BOOLEAN;
		case CLOB:
			return DbColumnType.LONGTEXT;
		case LONGVARCHAR:
			return DbColumnType.LONGTEXT;
		case BIGINT:
			return DbColumnType.DECIMAL;
		default:
			return DbColumnType.TEXT;
		}
	}
}

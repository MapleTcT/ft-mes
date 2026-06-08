package com.supcon.supfusion.signature.dao.enums;

public enum DbColumnType {
	TEXT("ec.property.char"), // 字符
	BAPCODE("ec.property.bapcode"), // 编码格式
	SUMMARY("ec.property.summary"), // 摘要
	INTEGER("ec.property.integer"), // 整数
	DECIMAL("ec.property.decimal"), // 小数
	DATE("ec.property.date"), // 日期
	TIME("ec.property.time"), // 时间
	DATETIME("ec.property.datetime"), // 日期时间
	BINARY("ec.property.binary"), // BLOB
	BOOLEAN("ec.property.boolean"), // 布尔
	LONGTEXT("ec.property.longText"), // 长文本
	LONG("ec.property.longInt"), // 长整型
	OBJECT("ec.property.object"), // 对象
	SYSTEMCODE("ec.property.systemcode"), // 系统编码
	ENUMERATE("ec.property.enumerate"), // 枚举
	MONEY("ec.property.money"), // 货币
	PASSWORD("ec.property.password"), // 密码
	PICTURE("ec.property.picture"),// 图片
	PROPERTYATTACHMENT("ec.property.attachment"), //附件
	OFFICE("ec.property.officeplugin"), //Office文档
	TAGNUMBER("ec.property.tagnumber"),//位号类型
	COLOR("ec.property.color"),//颜色字段
	LAYER("ec.property.layer");//图层
	private String value;

	private DbColumnType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public JdbcType getJdbcType() {
		switch (this) {
		case TEXT:
		case PASSWORD:
		case BAPCODE:
		case COLOR:
		case LAYER:
			return JdbcType.VARCHAR;
		case BINARY:
			return JdbcType.BINARY;
		case BOOLEAN:
			return JdbcType.BOOLEAN;
		case DATE:
			return JdbcType.DATE;
		case DATETIME:
			return JdbcType.TIMESTAMP;
		case TIME:
			return JdbcType.TIME;
		case MONEY:
		case DECIMAL:
			return JdbcType.DECIMAL;
		case INTEGER:
		case LONG:
			return JdbcType.INTEGER;
		case LONGTEXT:
			return JdbcType.CLOB;
		case PICTURE:
			return JdbcType.VARCHAR;
		case PROPERTYATTACHMENT:
			return JdbcType.VARCHAR;
		case OFFICE:
			return JdbcType.CLOB;
		default:
		}
		return null;
	}
	
	public FieldType getDefaultFieldType() {
		switch (this) {
		case TEXT:
		case BAPCODE:
		case SUMMARY:
		case INTEGER:
		case DECIMAL:
		case LONGTEXT:
		case LONG:
		case MONEY:
		case LAYER:
			return FieldType.TEXTFIELD;
		case DATE:
			return FieldType.DATE;
		case DATETIME:
			return FieldType.DATETIME;
		case BOOLEAN:
			return FieldType.SELECT;
		case OBJECT:
		case SYSTEMCODE:
			return FieldType.SELECTCOMP;
		case PASSWORD:
			return FieldType.PASSWORDFIELD;
		case PICTURE:
			return FieldType.PICTURE;
		case PROPERTYATTACHMENT:
			return FieldType.PROPERTYATTACHMENT;
		case OFFICE:
			return FieldType.OFFICE;
		case TAGNUMBER:
			return FieldType.SELECTTAGNUMBER;
		case COLOR:
			return  FieldType.COLOR;
		default:
			return FieldType.TEXTFIELD;
		}
	}
	
	public ShowFormat getDefaultShowFormat() {
		switch (this) {
		case TEXT:
		case PASSWORD:
		case BAPCODE:
		case SUMMARY:
		case INTEGER:
		case DECIMAL:
		case LONGTEXT:
		case LONG:
		case LAYER:
			return ShowFormat.TEXT;
		case DATE:
			return ShowFormat.YMD;
		case DATETIME:
			return ShowFormat.YMD_HMS;
		case BOOLEAN:
			return ShowFormat.SELECT;
		case OBJECT:
		case SYSTEMCODE:
		case PROPERTYATTACHMENT:
			return ShowFormat.SELECTCOMP;
		case MONEY:
			return ShowFormat.THOUSAND;
		case PICTURE:
			return ShowFormat.PICTURE;
		case OFFICE:
			return ShowFormat.OFFICE;
		case TAGNUMBER:
			return ShowFormat.SELECTTAGNUMBER;
		default:
			return ShowFormat.TEXT;
		}
	}
	
}

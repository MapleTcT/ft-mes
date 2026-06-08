package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import flexjson.*;
import flexjson.factories.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询条件辅助类
 * 
 * @author 谭正阳
 * 
 */
public class JSONUtil {

	public static final String SPLIT_DOT = ".";
	
	private static Pattern pattern = Pattern.compile("\\D+");

	@SuppressWarnings("rawtypes")
	public static Object generateObjectFromJson(String jsonStr, final Class clazz, final SessionFactory sessionFactory) {
		// 自定义boolean转换，针对boolean值两边带引号和不带引号的情况
		class BooleanObjectFactory implements ObjectFactory {
			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				if (value == null) {
					return null;
				}
				try {
					return Boolean.valueOf(value.toString());
				} catch (Exception e) {
					throw new JSONException(String.format("Failed to cast string %s to boolean.", value), e);
				}
			}
		}

		// 自定义boolean转换，针对boolean值两边带引号和不带引号的情况
		class DateObjectFactory implements ObjectFactory {

			private static final String DATE_FORMAT_FULL = "yyyy-MM-dd HH:mm:ss";
			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				// 时间戳类型
				if (value instanceof JsonNumber) {
					return new Date(((JsonNumber)value).toLong());
				} else if (value instanceof String && value.toString().length() > 0) {
					// 日期的字符串类型
					String str = (String) value;
					if (str.length() == 0) {
						return null;
					}
					// 如果包含非数字或者-，报错
					String tmp = str.replaceAll("-", "");
					//Pattern pattern = Pattern.compile("\\D+");
					Matcher matcher = pattern.matcher(tmp);
					if (matcher.matches()) {
						throw new EcException(EcException.Code.OTHER);
					}
					String formatStr = null;
					int length = str.split("-").length;
					int length2 = str.split(" ").length;
					int length3 = str.split(":").length;
					if (length == 1) {
						formatStr = "yyyy";
					} else if (length == 2) {
						formatStr = "yyyy-MM";
					} else if (length == 3) {
						formatStr = "yyyy-MM-dd";
						if (length2 == 2) {
							formatStr = "yyyy-MM-dd HH";
						}
						if (length3 == 2) {
							formatStr = "yyyy-MM-dd HH:mm";
						} else if (length3 == 3) {
							formatStr = DATE_FORMAT_FULL;
						}
					}
					if (str.length() > DATE_FORMAT_FULL.length()) {
						return null;
					} else {
						SimpleDateFormat format = new SimpleDateFormat(formatStr);
						try {
							return format.parse(str);
						} catch (ParseException e) {
							throw new EcException(EcException.Code.OTHER);
						}
					}
				} else {
					return null;
				}
			}
		}

		// 自定义SystemCode转换
		class SystemCodeObjectFactory implements ObjectFactory {

			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				if (value != null && value instanceof Map) {
					Map m = (Map) value;
					String id = (String) m.get("id");
					SystemCode sysCode = new SystemCode(id);
					return sysCode;
				}
				return null;
			}
		}

		// 自定义Integer转换
		class BAPIntegerFactory extends IntegerObjectFactory {

			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				if (value != null && value instanceof String && "".equals(value)) {
					if (targetClass.getName().equals(int.class.getName())) {
						return 0;
					}
					return null;
				} else {
					return super.instantiate(context, value, targetType, targetClass);
				}
			}
		}

		// 自定义Long转换
		class BAPLongFactory extends LongObjectFactory {

			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				if (value != null && value instanceof String && "".equals(value)) {
					if (targetClass.getName().equals(long.class.getName())) {
						return 0;
					}
					return null;
				} else {
					return super.instantiate(context, value, targetType, targetClass);
				}
			}
		}
		// 自定义BigInteger转换
		class BAPBigIntegerFactory extends BigIntegerFactory {

			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				if (value != null && value instanceof String && "".equals(value)) {
					return null;
				} else {
					return super.instantiate(context, value, targetType, targetClass);
				}
			}
		}
		// 自定义Float转换
		class BAPFloatFactory extends FloatObjectFactory {

			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				if (value != null && value instanceof String && "".equals(value)) {
					if (targetClass.getName().equals(float.class.getName())) {
						return 0;
					}
					return null;
				} else {
					return super.instantiate(context, value, targetType, targetClass);
				}
			}
		}
		// 自定义Short转换
		class BAPShortFactory extends ShortObjectFactory {

			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				if (value != null && value instanceof String && "".equals(value)) {
					if (targetClass.getName().equals(short.class.getName())) {
						return 0;
					}
					return null;
				} else {
					return super.instantiate(context, value, targetType, targetClass);
				}
			}
		}
		// 自定义Double转换
		class BAPDoubleFactory extends DoubleObjectFactory {

			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				if (value != null && value instanceof String && "".equals(value)) {
					if (targetClass.getName().equals(double.class.getName())) {
						return 0;
					}
					return null;
				} else {
					return super.instantiate(context, value, targetType, targetClass);
				}
			}
		}

		// 自定义BigDecimal转换
		class BAPBigDecimalFactory extends BigDecimalFactory {

			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				if (value != null && value instanceof String && "".equals(value)) {
					return null;
				} else {
					return super.instantiate(context, value, targetType, targetClass);
				}
			}
		}

		// BAP Entity 转换
		class BAPEntityObjectFactory implements ObjectFactory {

			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				Object obj = null;
				if (value != null && value instanceof Map) {
					Map m = (Map) value;
					Object id = null;
					if ("".equals(m.get("id"))) {
						m.remove("id");
					}
					if (m.get("id") != null) {
						if (m.get("id") instanceof String) {
							id = Long.valueOf((String) m.get("id"));
						} else {
							id = ((JsonNumber)m.get("id")).toLong();
						}
						Session session = null;
						if (sessionFactory != null) {
							session = sessionFactory.openSession();
							obj = session.get(clazz, (Serializable) id);
							session.close();
						}
					}
				}
				if (obj == null) {
					try {
						obj = clazz.newInstance();
					} catch (Exception e) {
						throw new EcException(EcException.Code.OTHER, e);
					}
				}
				if (obj != null) {
					context.bindIntoObject((Map) value, obj, targetType);
				}
				return obj;
			}
		}

		JSONDeserializer deserializer = new JSONDeserializer();
		// IEcEntity
		return deserializer.use("values", clazz).use(new BAPEntityObjectFactory(), "values").use(Date.class, new DateObjectFactory())
				.use(boolean.class, new BooleanObjectFactory()).use(Boolean.class, new BooleanObjectFactory())
				.use(SystemCode.class, new SystemCodeObjectFactory()).use(Integer.class, new BAPIntegerFactory())
				.use(int.class, new BAPIntegerFactory()).use(Long.class, new BAPLongFactory()).use(long.class, new BAPLongFactory())
				.use(BigInteger.class, new BAPBigIntegerFactory()).use(Short.class, new BAPShortFactory())
				.use(short.class, new BAPShortFactory()).use(BigDecimal.class, new BAPBigDecimalFactory())
				.use(Double.class, new BAPDoubleFactory()).use(double.class, new BAPDoubleFactory())
				.use(Float.class, new BAPFloatFactory()).use(float.class, new BAPFloatFactory())
				/* .use(IdEntity.class, new BAPEntityObjectFactory()) */.deserialize(jsonStr);
	}

	public static Object generateMapFromJson(String jsonStr) {
		JSONDeserializer deserializer = new JSONDeserializer();
		return deserializer.deserialize(jsonStr);
	}


}

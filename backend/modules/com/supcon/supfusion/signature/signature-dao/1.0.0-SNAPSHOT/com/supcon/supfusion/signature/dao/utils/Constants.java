package com.supcon.supfusion.signature.dao.utils;

public interface Constants {

	// common 操作符
	/**
	 * 等于
	 */
	String EQ = "=";

	/**
	 * 不等于
	 */
	String NE = "<>";

	/**
	 * 为空
	 */
	String NULL = "is null";

	/**
	 * 不为空
	 */
	String NOT_NULL = "is not null";

	// 字符串专用操作符
	/**
	 * 包含、开始等于、结束等于
	 */
	String STR_CONTAINS = "like";

	/**
	 * 不包含、开始不等于、结束不等于
	 */
	String STR_DOES_NOT_CONTAIN = "not like";

	// 日期类专用
	/**
	 * 等于
	 */
	String DATE_ON = "=";

	/**
	 * 晚于(包括当天)
	 */
	String DATE_ON_OR_AFTER = ">=";

	/**
	 * 早于(包括当天)
	 */
	String DATE_ON_OR_BEFORE = "<=";

	/**
	 * 晚于(不包括当天)
	 */
	String DATE_AFTER = ">";

	/**
	 * 早于(不包括当天)
	 */
	String DATE_BEFORE = "<";

	// 数值类专用
	/**
	 * 大于
	 */
	String NUM_GT = ">";

	/**
	 * 大于等于
	 */
	String NUM_GE = ">=";

	/**
	 * 小于
	 */
	String NUM_LT = "<";

	/**
	 * 小于等于
	 */
	String NUM_LE = "<=";

	// 常用串
	/**
	 * SQL关键字select
	 */
	String SQL_KEYWORDS_SELECT = "select";

	/**
	 * SQL关键字from
	 */
	String SQL_KEYWORDS_FROM = "from";

	/**
	 * SQL关键字and
	 */
	String SQL_KEYWORDS_AND = "and";

	/**
	 * SQL关键字or
	 */
	String SQL_KEYWORDS_OR = "or";

	/**
	 * SQL关键字where
	 */
	String SQL_KEYWORDS_WHERE = "where";

	/**
	 * SQL关键字not
	 */
	String SQL_KEYWORDS_NOT = "not";

	/**
	 * SQL关键字exists
	 */
	String SQL_KEYWORDS_EXISTS = "exists";

	/**
	 * SQL关键字to_date
	 */
	String SQL_KEYWORDS_TO_DATE = "to_date(?, 'yyyy-mm-dd')";
	/**
	 * SQL关键字to_date
	 */
	String SQL_KEYWORDS_TO_DATETIME = "to_date(?, 'yyyy-mm-dd HH24:mi:ss')";
	/**
	 * SQL关键字in
	 */
	String SQL_KEYWORDS_IN = "in";

	/**
	 * SQL关键字escape
	 */
	String SQL_KEYWORDS_ESCAPE = "escape";

	/**
	 * 右括号
	 */
	String SYMBOL_RIGHT_BRACKET = ")";

	/**
	 * 左括号
	 */
	String SYMBOL_LEFT_BRACKET = "(";

	/**
	 * 问号
	 */
	String SYMBOL_QUESTION = "?";

	/**
	 * 逗号
	 */
	String SYMBOL_COMMA = ",";

	/**
	 * 单引号
	 */
	String SYMBOL_SINGLE_QUOTE = "'";

	/**
	 * 双引号
	 */
	String SYMBOL_DOUBLE_QUOTE = "\"";

	/**
	 * 半角空格
	 */
	String SYMBOL_HALF_BLANK = " ";

	/**
	 * 句点
	 */
	String SYMBOL_HALF_POINT = ".";
	/**
	 * 全角空格
	 */
	String SYMBOL_FULL_BLANK = "　";

	/**
	 * 空串
	 */
	String SYMBOL_EMPTY = "";
	/**
	 * 任意时间
	 */
	String ANY_TIME = "anytime";
	/**
	 * X个月以前
	 */
	String X_MONTH_BEFORE = "Xmonthbefore";
	/**
	 * 昨天
	 */
	String YESTERDAY = "yesterday";
	/**
	 * 今天
	 */
	String TODAY = "today";
	/**
	 * 明天
	 */
	String TOMORROW = "tomorrow";
	/**
	 * 七天以后
	 */
	String NEXT_SEVENDAYS = "nextsevendays";
	/**
	 * 七天以前
	 */
	String LAST_SEVENDAYS = "lastsevendays";
	/**
	 * 下周
	 */
	String NEXT_WEEK = "nextweek";
	/**
	 * 上周
	 */
	String LAST_WEEK = "lastweek";
	/**
	 * 本周
	 */
	String THIS_WEEK = "thisweek";
	/**
	 * 下月
	 */
	String NEXT_MONTH = "nextmonth";
	/**
	 * 上月
	 */
	String LAST_MONTH = "lastmonth";
	/**
	 * 本月
	 */
	String THIS_MONTH = "thismonth";
	/**
	 * 明年
	 */
	String NEXT_YEAR = "nextyear";
	/**
	 * 去年
	 */
	String LAST_YEAR = "lastyear";
	/**
	 * 今年
	 */
	String THIS_YEAR = "thisyear";
	/**
	 * 最近X小时
	 */
	String LAST_X_HOUR = "lastXhour";
	/**
	 * 今后X小时
	 */
	String NEXT_X_HOUR = "nextXhour";
	/**
	 * 最近X天
	 */
	String LAST_X_DAY = "lastXday";
	/**
	 * 今后X天
	 */
	String NEXT_X_DAY = "nextXday";
	/**
	 * 最近X周
	 */
	String LAST_X_WEEK = "lastXweek";
	/**
	 * 今后X周
	 */
	String NEXT_X_WEEK = "nextXweek";
	/**
	 * 最近X月
	 */
	String LAST_X_MONTH = "lastXmonth";
	/**
	 * 今后X月
	 */
	String NEXT_X_MONTH = "nextXmonth";
	/**
	 * 最近X年
	 */
	String LAST_X_YEAR = "lastXyear";
	/**
	 * 今后X年
	 */
	String NEXT_X_YEAR = "nextXyear";
	/**
	 * 小于当前时间
	 */
	String BEFORE_NOW = "beforeNow";
	/**
	 * 大于当前时间
	 */
	String AFTER_NOW = "afterNow";

}

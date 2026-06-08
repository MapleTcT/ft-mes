package com.supcon.supfusion.license.common.utils.date;

/** 时间格式化 **/
public enum DateFormat implements StringEnum {
    ShortNumDate("yyMMdd"),

    NumDate("yyyyMMdd"),

    StrikeDate("yyyy-MM-dd"),

    SplitterYMD("yyyy/MM/dd"),

    NumDateTime("yyyyMMddHHmmss"),

    NumDateMinute("yyyyMMddHHmm"),

    TwoYearNumDateTime("yyMMddHHmmss"),

    StrikeDateTime("yyyy-MM-dd HH:mm:ss"),

    DoubleDateTime("yyyyMMddHHmmss.SSS"),

    MillisecondTime("yyyy-MM-dd HH:mm:ss SSS"),

    NumTime("HHmmss"),

    ColonTime("HH:mm:ss"),
    YearMonth("yyyy-mm");

    DateFormat(String value) {
	    changeNameTo(this, value);
    }
}

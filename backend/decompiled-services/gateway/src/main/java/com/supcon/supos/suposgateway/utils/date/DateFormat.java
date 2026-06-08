/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.utils.date;

import com.supcon.supos.suposgateway.utils.date.StringEnum;

public enum DateFormat implements StringEnum
{
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


    private DateFormat(String value) {
        this.changeNameTo(this, value);
    }
}


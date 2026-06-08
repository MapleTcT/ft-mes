/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.util.Assert
 */
package com.supcon.supfusion.framework.cloud.common.util;

import com.supcon.supfusion.framework.cloud.common.exception.ExceptionTool;
import com.supcon.supfusion.framework.cloud.common.time.ConcurrentDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import org.springframework.util.Assert;

public class DateUtil {
    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_DATE = "yyyy-MM-dd";
    public static final String PATTERN_TIME = "HH:mm:ss";
    public static final ConcurrentDateFormat DATETIME_FORMAT = ConcurrentDateFormat.of("yyyy-MM-dd HH:mm:ss");
    public static final ConcurrentDateFormat DATE_FORMAT = ConcurrentDateFormat.of("yyyy-MM-dd");
    public static final ConcurrentDateFormat TIME_FORMAT = ConcurrentDateFormat.of("HH:mm:ss");

    public static Date setYears(Date date, int amount) {
        return DateUtil.set(date, 1, amount);
    }

    public static Date setMonths(Date date, int amount) {
        return DateUtil.set(date, 2, amount);
    }

    public static Date setWeeks(Date date, int amount) {
        return DateUtil.set(date, 3, amount);
    }

    public static Date setDays(Date date, int amount) {
        return DateUtil.set(date, 5, amount);
    }

    public static Date setHours(Date date, int amount) {
        return DateUtil.set(date, 11, amount);
    }

    public static Date setMinutes(Date date, int amount) {
        return DateUtil.set(date, 12, amount);
    }

    public static Date setSeconds(Date date, int amount) {
        return DateUtil.set(date, 13, amount);
    }

    public static Date setMilliseconds(Date date, int amount) {
        return DateUtil.set(date, 14, amount);
    }

    private static Date set(Date date, int calendarField, int amount) {
        Assert.notNull((Object)date, (String)"The date must not be null");
        Calendar c = Calendar.getInstance();
        c.setLenient(false);
        c.setTime(date);
        c.add(calendarField, amount);
        return c.getTime();
    }

    public static String formatDateTime(Date date) {
        return DATETIME_FORMAT.format(date);
    }

    public static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    public static String formatTime(Date date) {
        return TIME_FORMAT.format(date);
    }

    public static String format(Date date, String pattern) {
        return ConcurrentDateFormat.of(pattern).format(date);
    }

    public static Date parse(String dateStr, String pattern) {
        ConcurrentDateFormat format = ConcurrentDateFormat.of(pattern);
        try {
            return format.parse(dateStr);
        }
        catch (ParseException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static Date parse(String dateStr, ConcurrentDateFormat format) {
        try {
            return format.parse(dateStr);
        }
        catch (ParseException e) {
            throw ExceptionTool.unchecked(e);
        }
    }
}


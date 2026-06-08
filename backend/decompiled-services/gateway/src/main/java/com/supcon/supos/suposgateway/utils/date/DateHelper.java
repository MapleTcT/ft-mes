/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 */
package com.supcon.supos.suposgateway.utils.date;

import com.google.common.collect.Sets;
import com.supcon.supos.suposgateway.utils.date.DateFormat;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

public final class DateHelper {
    public static Date MIN = DateHelper.ofDate(0L);
    public static final long SECOND_TIME = 1000L;
    public static final long MINUTE_TIME = 60000L;
    public static final long HOUR_TIME = 3600000L;
    public static final long DAY_TIME = 86400000L;
    private static final Set<DateFormat> DF = Sets.newHashSet((Object[])new DateFormat[]{DateFormat.ShortNumDate, DateFormat.NumDate, DateFormat.StrikeDate});

    private DateHelper() {
    }

    public static String format(Date date, DateFormat format) {
        return null == date ? format.name() : new SimpleDateFormat(format.name()).format(date);
    }

    public static String format(LocalDate date, DateFormat format) {
        return null == date ? format.name() : date.format(DateTimeFormatter.ofPattern(format.name()));
    }

    public static String format(LocalDateTime date, DateFormat format) {
        return null == date ? format.name() : date.format(DateTimeFormatter.ofPattern(format.name()));
    }

    public static String format(long time, DateFormat format) {
        return DateHelper.format(DateHelper.ofLocalDateTime(time), format);
    }

    public static Date ofDate(String sDate, DateFormat format) {
        try {
            return new SimpleDateFormat(format.name()).parse(sDate);
        }
        catch (Exception e) {
            throw new RuntimeException("ofDate error, sDate: " + sDate + " format: " + format.name(), e);
        }
    }

    public static Date ofDate(long time) {
        return Date.from(Instant.ofEpochMilli(time));
    }

    public static LocalDate ofLocalDate(String sDate, DateFormat format) {
        if (!DF.contains(format)) {
            throw new RuntimeException("ofLocalDate only use to parse day can not parse time");
        }
        return LocalDate.parse(sDate, DateTimeFormatter.ofPattern(format.name()));
    }

    public static LocalDate ofLocalDate(long time) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDateTime ofLocalDateTime(String sDate, DateFormat format) {
        return LocalDateTime.parse(sDate, DateTimeFormatter.ofPattern(format.name()));
    }

    public static LocalDateTime ofLocalDateTime(long time) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
    }

    public static LocalDate date2Local(long date) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalDateTime time2Local(long time) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
    }

    public static Date now() {
        return Calendar.getInstance().getTime();
    }

    public static String now(DateFormat format) {
        return DateHelper.format(DateHelper.now(), format);
    }

    public static long second() {
        return Instant.now().getEpochSecond();
    }

    public static long second(Date date) {
        return null == date ? 0L : date.getTime() / 1000L;
    }

    public static long second(LocalDate date) {
        return null == date ? 0L : date.atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    public static long second(LocalDateTime dateTime) {
        return null == dateTime ? 0L : dateTime.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
    }

    public static long time() {
        return System.currentTimeMillis();
    }

    public static double doubleTime() {
        return DateHelper.doubleTime(System.currentTimeMillis());
    }

    public static long time(Date date) {
        return null == date ? 0L : date.getTime();
    }

    public static double doubleTime(long time) {
        return Double.parseDouble(DateHelper.format(time, DateFormat.DoubleDateTime));
    }

    public static long time(LocalDate date) {
        return null == date ? 0L : date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static long time(LocalDateTime dateTime) {
        return null == dateTime ? 0L : dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static Date hourAt(int hour) {
        if (hour < 0 || hour > 23) {
            throw new RuntimeException("hour must in [0, 23], but actual is " + hour);
        }
        return DateHelper.instanceDate(hour, 0, 0);
    }

    public static Date minuteAt(int hour, int minute) {
        DateHelper.checkHourMinute(hour, minute);
        return DateHelper.instanceDate(hour, minute, 0);
    }

    public static Date secondAt(int hour, int minute, int second) {
        DateHelper.checkHourMinute(hour, minute);
        if (second < 0 || second > 59) {
            throw new RuntimeException("second must in [0, 59], but actual is " + second);
        }
        return DateHelper.instanceDate(hour, minute, second);
    }

    public static long timeSlot(Date time, Long period) {
        long timeLong;
        long now = DateHelper.time();
        if (now < (timeLong = time.getTime())) {
            if (timeLong - now > period) {
                return (timeLong - now) % period;
            }
            return timeLong - now;
        }
        if (now - timeLong >= period) {
            long slot = (now - timeLong) % period;
            return 0L == slot ? 0L : period - slot;
        }
        return period - (now - timeLong);
    }

    public static long ofDayStart(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DateHelper.ofDate(time));
        return DateHelper.instanceDate(0, 0, 0, calendar).getTime();
    }

    public static Date addMinutes(Date date, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(12, minutes);
        return calendar.getTime();
    }

    public static Date addHours(Date date, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(10, hours);
        return calendar.getTime();
    }

    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(5, days);
        return calendar.getTime();
    }

    public static Date addYears(int years) {
        Date date = DateHelper.now();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(1, years);
        return calendar.getTime();
    }

    public static Date addYears(Date date, int years) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(1, years);
        return calendar.getTime();
    }

    public static Date addMonths(Date date, int months) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(2, months);
        return calendar.getTime();
    }

    public static int dayOfWeek(Date date) {
        return null == date ? 0 : DateHelper.ofLocalDate(date.getTime()).getDayOfWeek().getValue();
    }

    public static int dayOfWeek(LocalDate date) {
        return null == date ? 0 : date.getDayOfWeek().getValue();
    }

    public static int dayOfWeek() {
        return LocalDate.now().getDayOfWeek().getValue();
    }

    public static BigDecimal daySize(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return new BigDecimal("-1");
        }
        long timMillis = Math.abs(startDate.getTime() - endDate.getTime());
        return BigDecimal.valueOf(timMillis).divide(BigDecimal.valueOf(86400000L), 6, 4);
    }

    public static int monthDays(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1, year);
        calendar.set(2, month - 1);
        calendar.set(5, 1);
        calendar.roll(5, -1);
        return calendar.get(5);
    }

    public static Date firstDayOfMonth(Date date) {
        if (null == date) {
            return MIN;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(5, 1);
        return calendar.getTime();
    }

    public static Date lastDayOfMonth(Date date) {
        if (null == date) {
            return MIN;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(5, DateHelper.monthDays(calendar.get(1), calendar.get(2) + 1));
        return calendar.getTime();
    }

    public static boolean isToday(LocalDate date) {
        LocalDate now = LocalDate.now();
        return now.getYear() == date.getYear() && now.getMonthValue() == date.getMonthValue() && now.getDayOfMonth() == date.getDayOfMonth();
    }

    public static Date firstDayOfWeek(Date date) {
        if (null == date) {
            return MIN;
        }
        Calendar calendar = DateHelper.newCalendar(date);
        calendar.set(7, calendar.getFirstDayOfWeek());
        return calendar.getTime();
    }

    public static Date lastDayOfWeek(Date date) {
        if (null == date) {
            return MIN;
        }
        Calendar calendar = DateHelper.newCalendar(date);
        calendar.set(7, calendar.getFirstDayOfWeek() + 6);
        return calendar.getTime();
    }

    private static Date instanceDate(int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        return DateHelper.instanceDate(hour, minute, second, calendar);
    }

    private static Date instanceDate(int hour, int minute, int second, Calendar calendar) {
        calendar.set(11, hour);
        calendar.set(12, minute);
        calendar.set(13, second);
        calendar.set(14, 0);
        return calendar.getTime();
    }

    private static void checkHourMinute(int hour, int minute) {
        if (hour < 0 || hour > 23) {
            throw new RuntimeException("hour must in [0, 23], but actual is " + hour);
        }
        if (minute < 0 || minute > 59) {
            throw new RuntimeException("minute must in [0, 59], but actual is " + minute);
        }
    }

    private static Calendar newCalendar(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(2);
        calendar.setTime(date);
        return calendar;
    }
}


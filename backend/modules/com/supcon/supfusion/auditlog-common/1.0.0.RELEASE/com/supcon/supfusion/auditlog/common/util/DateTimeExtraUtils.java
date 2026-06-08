package com.supcon.supfusion.auditlog.common.util;

import com.supcon.supfusion.framework.cloud.common.util.DateTimeUtil;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 日期时间扩展类
 * @author caokele
 */
public class DateTimeExtraUtils extends DateTimeUtil {

    /**
     * 时间戳转0时区格式
     * @param timestamp 时间戳
     */
    public static String formatUTC0(long timestamp) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"));
        return ZonedDateTime.of(localDateTime, ZoneId.of("UTC")).format(UTC0_FORMAT);
    }

    /**
     * 0时区格式转时间戳
     * @param formatTime 时间戳
     */
    public static long parseUTC0(String formatTime) {
        LocalDateTime localDateTime = LocalDateTime.parse(formatTime, UTC0_FORMAT);
        return localDateTime.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
    }

    /**
     * 将Long类型的时间戳转成string类型的时间格式，时间格式：yyyy-MM-dd HH:mm:ss
     */
    public static String timeToString(Long time){
        Assert.notNull(time, "time is null");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dtf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
    }

    /**
     * 将字符串转日期成Long类型的时间戳，格式为：yyyy-MM-dd HH:mm:ss
     */
    public static Long timeToLong(String time) {
        Assert.notNull(time, "time is null");
        DateTimeFormatter formatString = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime parse = LocalDateTime.parse(time, formatString);
        return LocalDateTime.from(parse).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static void main(String[] args) {
        //Date date = new Date();
        //long time = date.getTime();
        //System.out.println(time);
        //String str = timeToString(time);
        //System.out.println(str);
        //time = timeToLong(str);
        //System.out.println(time);
        //String str2 = timeToString(time);
        //System.out.println(str2);
    }
}

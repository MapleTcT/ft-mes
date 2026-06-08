package com.supcon.supfusion.i18n.common.until;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class TimeUtils {

    public String setTimestampToString(long timestamp) {
        // 转成UTC时间
        LocalDateTime utc = LocalDateTime.ofInstant(new Date(timestamp).toInstant(), ZoneId.of("UTC")); // 2020-01-13T10:14:35.300Z
        // 转成UTC+0时间
        ZonedDateTime utc0 = ZonedDateTime.of(utc, ZoneId.of("UTC"));
        // 格式化UTC+偏移量格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        String fmt = utc0.format(formatter); // 2020-01-13T10:14:35.300+0000
        return fmt;

    }

//    public void setStringToTimestamp(String utc8) {
//        // 前端请求输入的为UTC+时区偏移量
//        //String utc8 = "2020-01-13T18:14:35.300+0800"; // +0800，表示北京时间
//        // 转换为UTC+0格式，表达式为yyyy-MM-dd'T'HH:mm:ss.SSSZ，注意Z字母无单引号包围，Z表示时区偏移量
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
//        ZonedDateTime utc0 = ZonedDateTime.parse(utc8, formatter).withZoneSameInstant(ZoneId.of("UTC")); // 等同于ZoneId.of("GMT0")
//        // 转成毫秒时间戳，用于存储
//        long timestamp = utc0.toLocalDateTime().toInstant(ZoneOffset.UTC).toEpochmilli(); // 1578910475300 = 2020-01-13T10:14:35.300+0000
//        // 存储至数据库
//        PreparedStatement.setTimestamp(columnIndex, new Timestamp(timestamp));
//    }
}

package com.supcon.supfusion.file.server.common.utils;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    public long getTimestamp() {
        // 前端请求输入的为UTC+时区偏移量
        String utc8 = "2020-01-13T18:14:35.300+0800"; // +0800，表示北京时间
        // 转换为UTC+0格式，表达式为yyyy-MM-dd'T'HH:mm:ss.SSSZ，注意Z字母无单引号包围，Z表示时区偏移量
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        ZonedDateTime utc0 = ZonedDateTime.parse(utc8, formatter).withZoneSameInstant(ZoneId.of("UTC")); // 等同于ZoneId.of("GMT0")
        // 转成毫秒时间戳，用于存储
        long timestamp = utc0.toLocalDateTime().toInstant(ZoneOffset.UTC).toEpochMilli(); // 1578910475300 = 2020-01-13T10:14:35.300+0000
        return timestamp;
    }


}

package com.supcon.supfusion.rbac.common.utils;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
public class TimeTransferUtils {
    /**
     * 时间格式校验,转换
     * 时间格式: 2021-01-31T10:30:20.000+1000
     */
    public static String timeTransfer(String modifyTime){
        Date modifyDate = null;
        if (StringUtils.isNotBlank(modifyTime)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            try {
                modifyDate = format.parse(modifyTime);
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf1.setTimeZone(TimeZone.getTimeZone("UTC"));
                modifyTime = sdf1.format(modifyDate);
            } catch (ParseException e) {
                log.error(e.getMessage());
            }
        }
        return modifyTime;
    }

    /**
     * 时间格式校验,转换
     * 时间格式: 2021-01-31 10:30:20
     */
    public static String responseFormatTime(String time) {
        // 时间格式处理
        String formatModifyTime = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            formatModifyTime = sdf.format(sdf2.parse(time));
        } catch (ParseException e) {
            log.error("时间格式处理发生错误");
        }
        return formatModifyTime;
    }

    public static void main(String[] args) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        Date date = new Date();
        String format1 = format.format(date);
        System.out.println(format1);
        String s = timeTransfer(format1);
        System.out.println(s);
    }
}

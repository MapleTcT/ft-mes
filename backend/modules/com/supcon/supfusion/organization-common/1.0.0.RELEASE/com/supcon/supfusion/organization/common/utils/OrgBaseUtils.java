package com.supcon.supfusion.organization.common.utils;


import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Slf4j
public class OrgBaseUtils {

    public static String splitCompanyFullPath(String deptFullPath, String comShortName, String comFullPath) {
        if (StringUtils.isBlank(deptFullPath) || StringUtils.isBlank(comShortName) || StringUtils.isBlank(comFullPath)) {
            return "";
        }
        return comShortName + deptFullPath.substring(comFullPath.length(), deptFullPath.length());
    }

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
}

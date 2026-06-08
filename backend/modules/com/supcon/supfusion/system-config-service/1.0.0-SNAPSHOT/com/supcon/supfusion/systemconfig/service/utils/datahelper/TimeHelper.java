package com.supcon.supfusion.systemconfig.service.utils.datahelper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeHelper {
    public static String getNowTime() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String format = simpleDateFormat.format(date);
        return format;
    }
}

package com.supcon.supfusion.license.common.utils.date;

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

/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.utils;

import java.util.Calendar;

public class DateUtils {
    public static String getDateStr() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        return calendar.get(1) + "_" + (calendar.get(2) + 1);
    }
}


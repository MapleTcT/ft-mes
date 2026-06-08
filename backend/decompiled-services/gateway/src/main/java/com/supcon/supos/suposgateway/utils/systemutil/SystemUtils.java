/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.utils.systemutil;

import com.supcon.supos.suposgateway.utils.systemutil.SupportOS;

public class SystemUtils {
    public static SupportOS getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("linux") >= 0) {
            return SupportOS.LINUX;
        }
        return SupportOS.WINDOWS;
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.qos.logback.core.PropertyDefinerBase
 */
package com.supcon.supfusion.framework.cloud.logger.logback;

import ch.qos.logback.core.PropertyDefinerBase;
import java.net.InetAddress;

public class LogBackIpProperty
extends PropertyDefinerBase {
    public String getPropertyValue() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        }
        catch (Exception e) {
            return "unknow";
        }
    }
}


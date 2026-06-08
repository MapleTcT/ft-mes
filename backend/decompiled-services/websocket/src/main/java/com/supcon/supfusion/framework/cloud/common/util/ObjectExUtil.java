/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.lang.Nullable
 *  org.springframework.util.ObjectUtils
 */
package com.supcon.supfusion.framework.cloud.common.util;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

public class ObjectExUtil
extends ObjectUtils {
    public static boolean isNotEmpty(@Nullable Object obj) {
        return !ObjectUtils.isEmpty((Object)obj);
    }
}


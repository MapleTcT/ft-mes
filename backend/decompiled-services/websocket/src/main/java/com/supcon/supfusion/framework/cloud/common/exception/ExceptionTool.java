/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;
import com.supcon.supfusion.framework.cloud.common.exception.SystemException;
import com.supcon.supfusion.framework.cloud.common.supports.FastStringWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

public class ExceptionTool {
    public static SystemException unchecked(ErrorDefinition errorDefinition, Throwable e) {
        return new SystemException(errorDefinition, e);
    }

    public static SystemException unchecked(Throwable e) {
        if (e instanceof IllegalAccessException || e instanceof IllegalArgumentException || e instanceof NoSuchMethodException) {
            return new SystemException((ErrorDefinition)BizErrorEnum.ARGUMENT_ILLEGAL, e);
        }
        if (e instanceof InvocationTargetException) {
            return new SystemException((ErrorDefinition)BizErrorEnum.INVOCATION_TARGET_ERROR, ((InvocationTargetException)e).getTargetException());
        }
        if (e instanceof RuntimeException) {
            return (SystemException)e;
        }
        return new SystemException((ErrorDefinition)BizErrorEnum.SYSTEM_ERROR, e);
    }

    public static Throwable unwrap(Throwable wrapped) {
        Throwable unwrapped = wrapped;
        while (true) {
            if (unwrapped instanceof InvocationTargetException) {
                unwrapped = ((InvocationTargetException)unwrapped).getTargetException();
                continue;
            }
            if (!(unwrapped instanceof UndeclaredThrowableException)) break;
            unwrapped = ((UndeclaredThrowableException)unwrapped).getUndeclaredThrowable();
        }
        return unwrapped;
    }

    public static String getStackTraceAsString(Throwable ex) {
        FastStringWriter stringWriter = new FastStringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}


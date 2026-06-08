/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.util;

import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;

/**
 * @author: zhuangmh
 * @date: 2020年5月19日 上午11:31:21
 */
public class CodeGenerator {
    
    private CodeGenerator() {
        throw new IllegalStateException("CodeGenerator is utility class, do not instantiate");
    }
    /**
     * @return 返回流程编码
     */
    public static String generateProcessKey() {
        StringBuilder sb = new StringBuilder("K");
        sb.append(generateUUID());
        return sb.toString();
    }
    /**
     * @return 返回全局唯一编码
     */
    public static Long generateUUID() {
        return IDGenerator.newInstance().generate().longValue();
    }
}

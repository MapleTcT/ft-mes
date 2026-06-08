/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年8月25日 上午9:58:28
 */
public enum AppImportStatus {
    /**
     * 正在导入
     */
    IMPORTING(1),
    /**
     * 导出成功
     */
    IMPORT_SUCCESS(2),
    /**
     */
    IMPORT_FAIL(3);
    
    
    private final int status;
    
    private AppImportStatus(final int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
    
}

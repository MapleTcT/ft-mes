/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.enumeration;
/**
 * @author: zhuangmh
 * @date: 2020年8月25日 上午9:58:28
 */
public enum AppExportStatus {
    /**
     * 正在导出
     */
    EXPORTING(1),
    /**
     * 导出成功
     */
    EXPORT_SUCCESS(2),
    /**
     * 导出失败
     */
    EXPORT_FAIL(3);
    
    
    private final int status;
    
    private AppExportStatus(final int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
    
}

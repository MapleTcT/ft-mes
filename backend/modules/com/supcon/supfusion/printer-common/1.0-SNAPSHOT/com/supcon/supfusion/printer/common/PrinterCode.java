package com.supcon.supfusion.printer.common;

/**
 * 打印服务系统编码
 * @author liyiming
 * @date 2020/12/15 2:00 下午
 */
public enum PrinterCode {
    TEMPLATE_CODE_UNIQUE_ERROR(100118000, "printer.src_routes_reporter_Printer_templateCodeUnique"),
    CUSTOM_SERVICE_URL_INVALID(100118001, "printer.src_routes_reporter_Printer_customServiceUrlInvalid");

    private int code;

    private String message;

    PrinterCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

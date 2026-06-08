package com.supcon.supfusion.signature.base.constant;

/**
 * @author zhang yafei
 */
public class SinatureConstants {
    public static final String XLSX_LOW = "xlsx";
    public static final String STR_IMPORT = "import";
    public static final String STR_EXPORT = "export";
    public static final String SYSTEM_PATH = System.getProperty("user.dir");
    //空格字符串和空字符串
    public static final String STR_SPACE = " ";
    public static final String STR_NO_SPACE = "";
    public static final String STR_POINT = ".";
    public static final String STR_LINE = "_";
    public static final String STR_POINT_DOU = ",";
    public static final String STR_POINT_SHU = "|";
    public static final String STR_POINT_M = ":";
    public static final String SYSTEM_PATH_SPLITTER = System.getProperty("file.separator");

    /**
     * excel存储路径
     */
    public static final String EXCEL_PATH = SinatureConstants.SYSTEM_PATH
            + "signatureLog"
            + SinatureConstants.SYSTEM_PATH_SPLITTER
            + "excel"
            + SinatureConstants.SYSTEM_PATH_SPLITTER
            + "export"
            + SinatureConstants.SYSTEM_PATH_SPLITTER;
}
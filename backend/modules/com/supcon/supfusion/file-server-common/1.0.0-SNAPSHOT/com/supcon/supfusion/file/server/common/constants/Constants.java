package com.supcon.supfusion.file.server.common.constants;

import lombok.Data;

@Data
public class Constants {

    public static final String DEFAULT_TENANT_ID = "default";
    public static final String DEFAULT_FOLDER = "default";
    public static final String TEMP_FOLDER = "temp";
    public static final String DT_TENANT_ID = "dt";
    public static final String BUCKET = "bucket";


    /**
     * 配置常量
     */
    final static String BAP_LIST_MAX_PAGE_SIZE = "bap.list.maxPageSize";
    final static String BAP_IMPORT_EXCEL_MAX_SIZE = "bap.import.excel.maxsize";

    public static final String ATTACHMENT = "attachment";
    public static final String OFFICE = "office";
    public static final String USER_DIR = "user.dir";
    public static final String PATH = "/";
    public static final String TEMP_PATH = "temp/";
    public static final String CONVERT_PATH = "convert";
    public static final String STR_KONG = "";

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";

    public static final String HTTP_HEAD_PATH = "http://";
    public static final String MAO = ":";
    public static final String QUE = "?";
    public static final String HE = "&";
    public static final String DE = "=";
    public static final String PARAM_FILE_PATH = "filePath";

    public static final String PARAM_ENTITY_CODE = "entityCode";
    public static final String PARAM_LINK_ID = "linkId";
    public static final String PARAM_ID = "id";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_IS_MAIN_MODEL = "isMainModel";
    public static final String PARAM_PROPERTY_CODE = "propertyCode";
    public static final String PARAM_SHOW_TYPE = "showType";
    public static final String PARAM_USER_ID = "userId";
    public static final String LINUX = "linux";
    public static final String WINDOWS = "windows";
    public static final String VIDEO_CONVERT_PATH = "/greenDill/static/videoConvert/";
}

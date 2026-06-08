package com.supcon.supfusion.i18n.common.execption;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;
import com.supcon.supfusion.i18n.common.until.Constants;

public enum I18nErrorEnum implements ErrorDefinition {
    //参数具体描述
    // 国际化服务  100107000 ~ 100107999
    XLSX_UPLOAD_MAX_SIZE_ERROR(100107000, Constants.XLSX_UPLOAD_MAX_SIZE_ERROR, "i18n.exception.xlsx_upload_max_size_error"),
    XLSX_UPLOAD_MAX_NUM_ERROR(100107001, Constants.XLSX_UPLOAD_MAX_NUM_ERROR, "i18n.exception.xlsx_upload_max_num_error"),
    FILE_RESOLVER_UN_KNOW_ERROR(100107002, Constants.EXCEL_IMPORTING_ERROR, "i18n.exception.file_resolver_un_know_error"),
    FILE_TRANSPORT_ERROR(100107003, Constants.FILE_TRANSPORT_ERROR, "i18n.exception.file_transport_error"),
    FILE_RESOLVER_SHEET_ERROR(100107004, Constants.FILE_RESOLVER_SHEET_ERROR, "i18n.exception.file_resolver_sheet_error"),
    FILE_RESOLVER_SHEET_RM_ERROR(100107005, Constants.EXCEL_IMPORTING_OOM_ERROR, "i18n.exception.file_resolver_sheet_rm_error"),
    FILE_RESOLVER_SAVE_FILE_ERROR(100107006, Constants.FILE_RESOLVER_SAVE_FILE_ERROR, "i18n.exception.file_resolver_save_file_error"),
    FILE_RESOLVER_ERROR(100107007, Constants.FILE_RESOLVER_ERROR, "i18n.exception.file_resolver_error"),
    FILE_NO_MODULE_ERROR(100107008, Constants.NO_MODULE_CODE, "i18n.exception.file_no_module_error"),
    FILE_FORMAT_ERROR(100107009, Constants.EXCEL_FILE_ERROR, "i18n.exception.file_format_error"),
    FILE_DOWNLOAD_ERROR(100107010, Constants.FILE_DOWNLOAD_ERROR, "i18n.exception.file_download_error"),
    FILE_EXPORT_ERROR(100107011, Constants.FILE_EXPORT_ERROR, "i18n.exception.file_export_error"),
    MODULE_VERSION_RESOLVE_ERROR(100107012, Constants.MODULE_VERSION_RESOLVE_ERROR, "i18n.exception.module_version_resolve_error"),
    FILE_NO_MODULE_AND_VERSION_ERROR(100107013, Constants.NO_VERSION_CODE, "i18n.exception.file_no_module_and_version_error"),
    FILE_IS_TOKEN_ERROR(100107014, Constants.FILE_IS_TOKEN_ERROR, "i18n.exception.file_is_token_error"),
    FILE_NO_TOKEN_ERROR(100107015, Constants.FILE_NO_TOKEN_ERROR, "i18n.exception.file_no_token_error"),
    FILE_TOKEN_ERROR(100107016, Constants.TOKEN_ERROR, "i18n.exception.file_token_error"),
    FILE_VERSION_TOKEN_ERROR(100107017, Constants.VERSION_TOKEN_ERROR, "i18n.exception.file_version_token_error"),
    FILE_MODE_UPLOAD_ZIP_ERROR(100107018, Constants.UPLOAD_FILE_NO_ZIP_ERROR, "i18n.exception.file_mode_upload_zip_error"),
    FILE_HEAD_RESOLVER_ERROR(100107019, Constants.FILE_HEAD_RESOLVER_ERROR, "i18n.exception.file_head_resolver_error"),
    FILE_ERROR_MESSAGE(100107020, Constants.FILE_ERROR_MESSAGE, "i18n.exception.file_error_message"),
    FILE_ZIP_CREATE_ERROR(100107021, Constants.FILE_ZIP_CREATE_ERROR, "i18n.exception.file_zip_create_error"),
    PARAM_ERROR(100107022, Constants.PARAM_ERROR, "i18n.exception.param_error"),
    PARAM_LOST(100107023, Constants.PARAM_LOST, "i18n.exception.param_lost"),
    MODULE_CODE_ERROR(100107024, Constants.MODULE_CODE_ERROR, "i18n.exception.module_code_error"),
    PARAM_LOST_I18N_KEY(100107025, Constants.PARAM_LOST_I18N_KEY, "i18n.exception.param_lost_i18n_key"),
    PAGE_PARAM_ERROR(100107026, Constants.PAGE_PARAM_ERROR, "i18n.exception.page_param_error"),
    SERVER_ERROR(100107027, Constants.SERVER_ERROR, "i18n.exception.server_error"),
    DATA_INPUT_FILE_ERROR(100107028, Constants.DATA_INPUT_FILE_ERROR, "i18n.exception.data_input_file_error"),
    MODE_EXPORT_ERROR(100107029, Constants.MODE_EXPORT_ERROR, "i18n.exception.mode_export_error"),
    MODULE_INDEX_ERROR(100107030, Constants.MODULE_INDEX_ERROR, "i18n.exception.module_index_error"),
    PARAM_LOST_I18N_VALUE(100107031, Constants.PARAM_LOST_I18N_VALUE, "i18n.exception.param_lost_i18n_value"),
    I18N_KEY_EXIST(100107032, Constants.I18N_KEY_EXIST, "i18n.exception.i18n_key_exist"),
    I18N_KEY_ERROR(100107033, Constants.I18N_KEY_ERROR, "i18n.exception.i18n_key_error"),
    I18N_VALUE_LENGTH_ERROR(100107034, Constants.I18N_VALUE_LENGTH_ERROR, "i18n.exception.i18n_value_length_error"),
    I18N_KEY_LENGTH_ERROR(100107035, Constants.I18N_KEY_LENGTH_ERROR, "i18n.exception.i18n_key_length_error"),
    FILE_COPY_ERROR(100107036, Constants.FILE_COPY_ERROR, "i18n.exception.file_copy_error"),
    FILE_EXPORT_DB_ERROR(100107037, Constants.FILE_EXPORT_DB_ERROR, "i18n.exception.file_export_db_error"),
    FILE_SHEET_NONE_ERROR(100107038, Constants.FILE_SHEET_NONE_ERROR, "i18n.exception.file_sheet_none_error"),
    LANGUAGE_HAS_USED_ERROR(100107039, Constants.LANGUAGE_HAS_USED_ERROR, "i18n.exception.language_has_used_error"),
    RESOURCE_NOT_FIND_ERROR(100107040, Constants.RESOURCE_NOT_FIND_ERROR, "i18n.exception.resource_not_find_error"),
    RESOURCE_UNZIP_ERROR(100107041, Constants.RESOURCE_UNZIP_ERROR, "i18n.exception.resource_unzip_error"),
    RESOURCE_FILE_COPY_ERROR(100107042, Constants.RESOURCE_FILE_COPY_ERROR, "i18n.exception.resource_file_copy_error"),
    RESOURCE_HAS_EXIST_ERROR(100107043, Constants.RESOURCE_EXIST, "i18n.exception.resource_has_exist_error"),
    RESOURCE_ERROR(100107044, Constants.VERSION_LOW_DB_VERSION, "i18n.exception.resource_error"),
    UPLOAD_FILE_NO_PROPERTIES_ERROR(100107045, Constants.UPLOAD_FILE_NO_PROPERTIES_ERROR, "i18n.exception.upload_file_no_properties_error"),
    RESOURCE_IS_UPLOADING(100107046, Constants.RESOURCE_IS_UPLOADING, "i18n.exception.resource_is_uploading"),
    XLSX_UPLOAD_KEY_BLANK_ERROR(100107047, Constants.XLSX_UPLOAD_KEY_BLANK_ERROR, "i18n.exception.xlsx_upload_key_blank_error"),
    XLSX_UPLOAD_KEY_REPEAT_ERROR(100107048, Constants.XLSX_UPLOAD_KEY_REPEAT_ERROR, "i18n.exception.xlsx_upload_key_repeat_error"),
    FILE_UPLOAD_NO_FILE_ERROR(100107049, Constants.FILE_UPLOAD_NO_FILE_ERROR, "i18n.exception.file_upload_no_file_error"),
    MODULE_NUM_NOT_VERSION_NUM(100107050, Constants.MODULE_NUM_NOT_VERSION_NUM, "i18n.exception.module_num_not_version_num"),
    FIND_NO_VALUE_ERROR(100107051, Constants.FIND_NO_VALUE, "i18n.exception.find_no_value"),
    FIND_NO_LANGUAGE_ERROR(100107052, Constants.FIND_NO_LANGUAGE, "i18n.exception.find_no_language"),
    FILE_UPLOADING_ERROR(100107053, Constants.FILE_UPLOADING_ERROR, "i18n.exception.file_uploading_error"),
    MODULE_CODE_EXIST_ERROR(100107054, Constants.MODULE_CODE_EXIST_ERROR, "i18n.exception.module_code_exist_error"),
    I18N_KEY_START_ERROR(100107055, Constants.I18N_KEY_START_ERROR, "i18n.exception.i18n_key_start_error"),
    PARAM_LOST_I18N_LANGUAGE(100107056, Constants.PARAM_LOST_I18N_LANGUAGE, "i18n.exception.param_lost_i18n_language"),
    RESOURCE_VERSION_ERROR(100107057, Constants.RESOURCE_VERSION_ERROR, "i18n.exception.resource_version_error"),
    LANGUAGE_HAS_NO_ERROR(100107058, Constants.LANGUAGE_HAS_NO_ERROR, "i18n.exception.language_has_no_error"),
    NO_MORE_THAN_ERROR(100107059, Constants.NO_MORE_THAN_ERROR, "i18n.exception.no_more_than_error"),
    NO_LESS_THAN_ERROR(100107060, Constants.NO_LESS_THAN_ERROR, "i18n.exception.no_less_than_error"),
	I18N_DELETE_DENY_ERROR(100107061, Constants.DELETE_ERROR, "i18n.exception.delete_deny");
	
    I18nErrorEnum(Integer code, String defaultMessage, String key) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.key = key;
    }

    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    /**
     * 默认异常信息
     */
    private String key;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return defaultMessage;
    }

    @Override
    public String getInfo() {
        return key;
    }

    @Override
    public String getSimpleMessage() {
        return "[" + this.getCode() + "]" + this.getMessage();
    }
}

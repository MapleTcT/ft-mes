package com.supcon.supfusion.file.server.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum FileServerErrorEnum implements ErrorDefinition {

    FILE_PARAMS_ERROR(100117000, "参数错误", "fileServer.fileParamsError"),
    FILE_UPLOAD_ERROR(100117001, "文件上传失败", "fileServer.fileUploadError"),
    FILE_DOWNLOAD_ERROR(100117002, "文件下载失败", "fileServer.fileDownloadError"),
    FILE_REMOVE_ERROR(100117003, "文件删除失败", "fileServer.fileRemoveError"),
    FILE_QUERY_ERROR(100117004, "文件查询失败", "fileServer.fileQueryError"),
    FILE_MOVE_ERROR(100117005, "文件移动失败", "fileServer.fileMoveError"),
    FILE_TYPE_ERROR(100117006, "文件类型不符", "fileServer.fileTypeError"),
    FILE_EXISTS_ERROR(100117007, "文件已存在", "fileServer.fileExistsError"),
    FILE_EMPTY_ERROR(100117008, "文件为空", "fileServer.fileEmptyError"),
    FILE_PARAM_NUM_ERROR(100117009, "参数个数不对应", "fileServer.fileParamNumError"),
    FILE_PARAM_LOST_ERROR(100117010, "参数丢失", "fileServer.fileParamLostError"),
    FILE_PARAM_METHOD_TYPE_ERROR(100117011, "参数请求方式错误", "fileServer.fileParamMethodTypeError"),
    FILE_PARAM_PORT_ERROR(100117012, "参数port错误", "fileServer.fileParamPortError"),
    FILE_PARAM_URL_ERROR(100117013, "参数url错误", "fileServer.fileParamUrlError"),
    FILE_PARAM_USERID_ERROR(100117014, "参数userId错误", "fileServer.fileParamUserIdError"),
    FILE_PARAM_LINKID_ERROR(100117015, "参数附件id错误", "fileServer.fileParamLinkIdError"),
    FILE_PARAM_FILEPATH_ERROR(100117016, "参数filepath错误", "fileServer.fileParamFilepathError"),
    FILE_DOWN_AUTH_GET_ERROR(100117017, "下载权限认证过程失败", "fileServer.fileDownAuthGetError"),
    FILE_DOWNLOAD_AUTH_ERROR(100117018, "没有下载权限", "fileServer.fileParamFilePathError"),
    FILE_FILE_ID_ERROR(100117019, "附件id为空", "fileServer.fileFileIdError"),
    FILE_LINK_ID_ERROR(100117020, "linkId为空", "fileServer.fileLinkIdError"),
    FILE_NO_ERROR(100117021, "文件不存在", "fileServer.fileNoError"),
    FILE_OVERVIEW_AUTH_ERROR(100117022, "没有预览权限", "fileServer.fileOverviewAuthError"),
    FILE_OVERVIEW__ERROR(100117023, "预览发生错误", "fileServer.fileOverviewError"),
    FILE_OVERVIEW_CONVERT(100117024, "预览文件正在转换，请稍候", "fileServer.fileOverviewConvert"),
    FILE_OVERVIEW_NO_SUPPORT(100117025, "当前文件不支持预览", "fileServer.fileOverviewNoSupport"),
    FILE_UPLOAD_FAIL_AND_REASON(100117026, "%s文件上传失败! 原因：%s", "fileServer.fileUploadFailAndReason");

    private Integer code;

    private String defaultMessage;

    private String key;


    FileServerErrorEnum(Integer code, String defaultMessage, String key) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.key = key;
    }

    public Integer getCode() {

        return code;
    }

    public String getMessage() {

        return defaultMessage;
    }

    public String getInfo() {
        return key;
    }
}
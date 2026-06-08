package com.supcon.supfusion.organization.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 岗位相关错误码和错误信息的枚举类
 * @author shidongsheng
 *
 */
public enum PositionErrorEnum implements ErrorDefinition {
    POSITION_THIS_CODE_EXISTS(100104009, "岗位编码已经存在!"),
    POSITION_COMPANYID_NOT_EXISTS(100104010, "指定公司不存在!"),
    POSITION_PARENTID_NOT_EXISTS(100104011, "指定上级岗位不存在!"),
    POSITION_NAME_EXISTS(100104012, "岗位名称已存在!"),
    POSITION_ID_NOT_EXISTS(100104013, "指定岗位不存在！"),
    POSITION_LOCATION_PARAM_ERROR(100104014, "岗位移动位置不正确！"),
    POSITION_EXCEL_IMPORT_FILE_NOT_EXISTS(100104015, "请导入岗位的Excel文件！"),
    POSITION_EXCEL_FILENAME_ERROR(100104016, "Excel文件格式不合法！"),
    POSITION_EXCEL_SHEET_EMPTH(100104017, "Excel文件Sheet页为空！"),
    POSITION_DEPID_NOT_EXISTS(100104018, "指定的关联部门不存在！"),
    POSITION_DEPARTMENT_COMPANY_NOT_MATCH(100104034, "岗位只能关联当前公司下的部门!"),
    POSITION_HAVE_RELATION_PERSON_DELETE_ERROR(100104035, "岗位或子岗位存在关联人员,不允许删除!"),
    POSITION_PERSON_HAVE_TASK_DELETE_ERROR(100104036, "岗位关联人员存在待办,不允许删除!"),
    POSITION_CODE_NOT_ALLOW_NULL(100104037, "岗位编码不能为空!");

    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    PositionErrorEnum(Integer code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

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
        return null;
    }
}

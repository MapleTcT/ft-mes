package com.supcon.supfusion.organization.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 部门相关错误码和错误信息的枚举类
 * @author shidongsheng
 *
 */
public enum DepartmentErrorEnum implements ErrorDefinition {
    DEPARTMENT_THIS_CODE_EXISTS(100104000, "部门编码已经存在!"),
    DEPARTMENT_COMPANYID_NOT_EXISTS(100104001, "指定的公司不存在!"),
    DEPARTMENT_PARENTID_NOT_EXISTS(100104002, "指定的上级部门不存在!"),
    DEPARTMENT_NAME_EXISTS(100104003, "部门名称已存在!"),
    DEPARTMENT_ID_NOT_EXISTS(100104004, "指定部门不存在！"),
    DEPARTMENT_LOCATION_PARAM_ERROR(100104005, "部门移动位置不正确！"),
    DEPARTMENT_EXCEL_IMPORT_FILE_NOT_EXISTS(100104006, "请导入部门的Excel文件！"),
    DEPARTMENT_EXCEL_FILENAME_ERROR(100104007, "Excel文件格式不合法！"),
    DEPARTMENT_EXCEL_SHEET_EMPTH(100104008, "Excel文件Sheet页为空！"),
    DEPARTMENT_EXCEL_TITLE_ERROR(100104019, "Excel标题不正确!"),
    COM_CAN_NOT_DELETE_HAS_CHILD_NODE(100104301, "存在子公司无法删除！"),
    COM_PARENTID_NOT_EXISTS(100104302, "父公司不存在！"),
    DEPARTMENT_HAVE_POSITION_DELETE_ERROR(100104035, "部门或子部门关联的岗位存在人员,不允许删除！");

    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

    DepartmentErrorEnum(Integer code, String defaultMessage) {
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

package com.supcon.supfusion.auth.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 部门相关错误码和错误信息的枚举类
 *
 * @author shidongsheng
 */
public enum UserErrorEnum implements ErrorDefinition {
    USER_NOT_EXIST(100106001, "用户不存在", "userManagement.userNotExist"),
    USER_NAME_NOT_EXIST(100106002, "用户名称必填", "userManagement.userNameIsRequire"),
    USER_HAS_LOCK(100106008, "用户被锁定", "userManagement.userDisable"),
    USER_PASSWORD_NOT_RIGHT(100106009, "新密码和确认密码输入不一致", "userManagement.passwordIsNotRight"),
    STAFF_NAME_NOT_EXIST(100106003, "人员名称必填", "userManagement.personNameIsRequire"),
    USER_NAME_REPEATE(100106004, "用户名称重复", "userManagement.userNameRepeat"),
    USER_ONLIEN_LIMITE(100106087, "超过系统配置在线用户数", "userManagement.userOnlineLimit"),
    USER_ONLIEN_ZERO(100106088, "系统配额为0", "userManagement.userOnlineZero"),
    USER_PERSON_REPEATE(100106005, "人员已关联用户", "userManagement.userPersonIsExist"),
    USER_PASSWORD_ERROR(100106006, "密码错误", "userManagement.passwordIswrong"),
    USER_PERSON_NOT_RELATION(100106007, "该用户未绑定人员", "userManagement.userPersonIsNotExist"),
    USER_ROLE_NOT_DELETE(100106020, "系统管理员用户不可解除角色", "userManagement.userRoleNotDelete"),
    USER_PASSWORD_RULE_ERROR1(100106021, "%s", "userManagement.passwordRule1"),
    CURRENT_USER_CANNOT_DELETE(100106007, "%s用户为当前登录用户，不允许删除！", "userManagemer.currentUserCannotDelete"),
    CURRENT_USER_CANNOT_LOCK(100106008, "%s用户为当前登录用户，不允许锁定！", "userManagemer.currentUserCannotLock"),
    ADMIN_USER_CANNOT_DELETE(100106009, "admin用户不允许删除！", "userManager.adminUserCannotDelete"),
    USER_TASK_CANNOT_DELETE(100106031, "用户有未完成的待办，不允许删除", "userManager.UserTaskCannotDelete"),
    ADMIN_USER_CANNOT_LOCK(100106011, "admin用户不允许锁定！", "userManager.adminUserCannotLock"),
    USER_PASSWORD_CURRENT_ERROR(100106010, "现用密码错误", "userManagement.passwordCurrentIswrong"),
    USER_NAME_RULE(100106087, "用户名称支持数字 字母 _", "userManager.usernameRule"),
    GET_IP_FAILED(100106208, "获取ip失败", "userManagemer.INTERNAL_ERROR"),
    ACCESS_IP_FORBIDDEN(100106207, "ip已被限制拒绝访问", "userManagemer.USER_DISABLED"),
    USER_NOT_IN_COMPANY(100106088, "用户不在该公司", "userManagement.userNotInCompany"),
    USER_OR_PASSWORD_ERROR(100106211, "用户名或密码错误", "userManagement.userPasswordWrong"),
    EXCEL_SIZE_EMPTY(100106024, "请导入Excel!", "userManagement.excelimport"),
    EXCEL_FILE_CREATE_ERROR(100106031, "Excel目录创建失败!", "userManagement.excelCatalogFail"),
    EXCEL_IMPORT_TASH_NOT_EXISTS_ERROR(100106032, "指定的导入任务不存在!", "userManagement.excelTaskNotExist"),
    EXCEL_IMPORT_FAILED_ROWS_EMPTY(100106073, "导入失败！文件数据为空", "userManagement.importContentIsNull"),
    EXCEL_IS_NOT(100106024, "错误的excel文件格式", "userManagement.noExcelformat"),
    EXCEL_TITLE_WRONG(100106073, "excel文件标题错误","userManagement.excelTitleWrong"),
    EMAIL_NOT_EXIST(100106074, "用户未绑定邮箱，请联系管理员","userManagement.emailNotExist"),
    VERIFICATION_CODE_EXPIRE(100106075, "验证码已过期","userManagement.verificationCodeExpire"),
    VERIFICATION_CODE_WRONG(100106080, "验证码错误","userManagement.verificationCodeWrong"),
    PASSWD_REGREX_WRONG(100106079, "密码正则表达式错误","userManagement.passwd_regrex_error"),
    EMAIL_NOT_MATCH(100106076, "用户名或邮箱不匹配，请重新输入","userManagement.emailNotMatch"),
    EMAIL_CONFIG_NOT_SET(100106078, "未配置系统邮箱，联系管理员","userManagement.emailConfigNotSet"),
    IMAGE_CODE_NOT_MATCH(100106077, "图形验证码错误或已过期","userManagement.imageCodeNotMatch");
    /**
     * 异常码
     */
    private Integer code;

    /**
     * 默认异常信息
     */
    private String defaultMessage;

    private String key;


    UserErrorEnum(Integer code, String defaultMessage, String key) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.key = key;
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
        return key;
    }


}

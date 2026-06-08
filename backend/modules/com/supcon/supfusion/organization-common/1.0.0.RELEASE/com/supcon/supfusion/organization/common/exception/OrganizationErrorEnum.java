package com.supcon.supfusion.organization.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public enum OrganizationErrorEnum implements ErrorDefinition {
    PERSON_POSITION_ID_NOT_EXISTS(100104019, "指定岗位不存在!"),
    PERSON_ID_NOT_EXISTS(100104020, "指定人员不存在!"),
    PERSON_CODE_EXISTS(100104021, "人员编号已经存在!"),
    PERSON_MUST_HAVE_ONE_POSITION(100104022, "人员只有一个岗位,不可以删除！"),
    COMPANY_HAVE_SUB_COMPANY_CAN_NOT_DELETE(100104023,"该公司存在子公司,不允许删除!"),
    EXCEL_SIZE_EMPTY(100104024, "选择的文件不是xlsx文件，请重新选择！"),
    EXCEL_WORKBOOK_FILE_ERROR(100104025, "转换Excel文件错误!"),
    EXCEL_SHEET_ERROR(100104026, "模板格式不正确，请选择正确的导入模板！"),
    EXCEL_IMPORT_TYPE_ERROR(100104027, "导入的文件类型不正确!"),
    EXCEL_IMPORT_TITLE_ERROR(100104028, "模板格式不正确，请选择正确的导入模板！"),
    EXCEL_IMPORT_PROCESS_ERROR(100104029, "Excel导入数据处理异常!"),
    EXCEL_TYPE_TEMPLATE_NOT_EXISTS(100104030, "指定类型的模板不存在!"),
    EXCEL_FILE_CREATE_ERROR(100104031, "Excel目录创建失败!"),
    EXCEL_IMPORT_TASH_NOT_EXISTS_ERROR(100104032, "指定的导入任务不存在!"),
    COMPANY_THIS_CODE_EXISTS(100104036, "导出指定数据的id参数为空!"),
    PERSON_BINDING_USER_DELETE_ERROR(1001040367, "人员绑定了用户不可以删除!"),

    EXCEL_EXPORT_IDS_EMPTY(100104033, "请选择导入的数据条目!"),
    DEFAULT_COMPANY_DELETE_FORBIDEN(100104038, "默认公司不可以删除!"),
    CREATE_COMPANY_USER_ERROR(100104039, "创建公司管理员失败!"),
    CREATE_PERSON_USER_ERROR(100104039, "创建用户失败!"),
    COMPANY_PARAM_ID_NECESSARY(100104040, "公司不存在!"),
    NO_LOGIN(100104041, "请登录后访问!"),
    COMPANY_CODE_EXISTS(100104042, "公司编码已存在!"),
    COMPANY_SHORTNAME_EXISTS(100104043, "公司简称已存在!"),
    COMPANY_FULLNAME_EXISTS(100104044, "公司全称已存在!"),
    COMPANY_DELETE_ERROR(100104045, "公司下岗位存在人员，不可删除!"),
    COMPANY_DELETE_LOGIN_ERROR(100104046, "当前用户登录的公司不允许删除!"),
    PERSON_RELATION_POSITION_LESS_THAN_ONE(1001040368, "人员至少所属一个岗位!"),
    PERSON_RELATION_POSITION_MUST_NOT_MAIN_POSITION(1001040369, "设置的人员主岗不可以同时被调离!"),
    PERSON_MAIN_POSITION_NOT_EXISTS(1001040370, "主岗不存在!"),
    TAG_LENGTH_BIGGER_THAN_THIRTY(1001040371, "标签长度不超过50!"),
    PERSON_RELATED_USER_DELETE_FORBIDDEN(1001040372, "人员绑定了用户不允许删除!"),
    EXCEL_IMPORT_FAILED_ROWS_EMPTY(1001040373, "导入失败！文件数据为空"),
    DELETE_COMPANY_USER_ERROR(1001040374, "删除公司管理员失败！"),
    DELETE_COMPANY_RBAC_ERROR(1001040374, "删除公司管理员菜单失败！"),
    ROLE_CHANGE_ERROR(1001040375, "角色变更失败！"),
    USER_DELETE_ERROR(1001040376, "用户删除失败！"),
    DATE_FORMAT_ERROR(1001040377, "日期格式错误"),
    USER_NOT_EXISTS(1001040378, "指定用户不存在"),
    NO_QUERY_PERSON_PERMISSION(1001040379, "无法查看其它人员信息"),
    TENANT_ID_NECESSARY(1001040373, "租户参数必填!"),
    PERSON_IMAGE_NOT_FORMATE(1001040379, "图片格式错误"),
    FILE_CREATE_ERROR(1001040380, "文件目录创建失败!"),
    FILE_PARAM_FILEPATH_ERROR(1001040381, "参数filePaths错误"),
    ID_NUMBER_EXISTS(1001040383, "身份证号已存在!"),
    REQUEST_ADD_EXISTS_MULTI_PERSON_CODE(1001040384, "批量新增人员存在重复编号!"),
    DIRECT_LEADER_CODE_NOT_EXISTS(1001040385, "直属领导不存在!"),
    GRAND_LEADER_CODE_NOT_EXISTS(1001040386, "隔级领导不存在!"),
    IMAGE_PARAM_ERROR(1001040387, "imageType参数错误!"),
    FILE_NOT_EXISTS(1001040388, "图片不存在!"),
    GENDER_SYS_CODE_ERROR(1001040389, "性别系统编码不正确!"),
    STATUS_SYS_CODE_ERROR(1001040390, "人员状态系统编码不正确!"),
    TITLE_SYS_CODE_ERROR(1001040391, "职称系统编码不正确!"),
    EDUCATION_SYS_CODE_ERROR(1001040392, "学历系统编码不正确!"),
    PERSON_CODE_NECESSARY(1001040393, "人员编号为必填项,不可以为空,并且为编码、数字、下划线组合,长度不超过50!"),
    PERSON_NAME_NECESSARY(1001040394, "人员名称为必填项,不可以为空,并且长度不超过200!"),
    PERSON_GENDER_NECESSARY(1001040395, "人员性别为必填项，不可以为空"),
    PERSON_STATUS_NECESSARY(1001040396, "人员状态为必填项，不可以为空"),
    PERSON_MAIN_POSITION_NECESSARY(1001040397, "人员主岗为必填项，不可以为空"),
    PERSON_DESCRIPTION_ERROR(1001040398, "描述长度不超过500!"),
    PERSON_ID_NUMBER_ERROR(1001040399, "身份证号由数字、大小写字母、特殊字符组成，长度不超过200字符！"),
    PERSON_ID_NUMBER_MULTI(1001040400, "身份证号不可以重复!")
    ;


    OrganizationErrorEnum(Integer code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
    /**
     * 异常码
     */
    private Integer code;
    /**
     * 默认异常信息
     */
    private String defaultMessage;

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
